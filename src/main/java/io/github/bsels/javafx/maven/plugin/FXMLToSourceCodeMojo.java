package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder;
import io.github.bsels.javafx.maven.plugin.in.memory.compiler.OptimisticInMemoryCompiler;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import io.github.bsels.javafx.maven.plugin.parameters.FXMLDirectory;
import io.github.bsels.javafx.maven.plugin.utils.ContextClassLoaderClosable;
import io.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider;
import io.github.bsels.javafx.maven.plugin.utils.ThrowableRunner;
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/// Maven plugin for converting FXML files into Java source code.
@Mojo(name = "fxml-source", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public final class FXMLToSourceCodeMojo extends AbstractMojo {
    /// The Maven project.
    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;
    /// Directories containing FXML files to process.
    @Parameter(required = true)
    List<FXMLDirectory> fxmlDirectories;
    /// Resource bundle configuration for FXML processing.
    @Parameter(property = "javafx.fxml.resourceBundleObject")
    String resourceBundleObject;
    /// Directory where generated Java source code is written.
    @Parameter(
            property = "javafx.fxml.generatedSourceDirectory",
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/fxml"
    )
    Path generatedSourceDirectory;
    /// Whether to include project source files in class discovery.
    @Parameter(property = "javafx.fxml.include.source.discovery", defaultValue = "true")
    boolean includeSourceFilesInClassDiscovery = true;
    /// Character set for reading FXML and generating source code.
    @Parameter(property = "javafx.fxml.charset", defaultValue = "UTF-8", required = true)
    String defaultCharset = "UTF-8";
    /// Whether to add the `@Generated` annotation to generated files.
    @Parameter(property = "javafx.fxml.add.generated.annotation", defaultValue = "true", required = true)
    boolean addGeneratedAnnotation = true;

    /// Initializes a new [FXMLToSourceCodeMojo] instance.
    ///
    /// This constructor calls the super constructor of [AbstractMojo].
    public FXMLToSourceCodeMojo() {
        super();
    }

    /// Executes the mojo to generate Java source code from FXML files.
    ///
    /// The execution follows these steps:
    /// 1. Validates that all required parameters (FXML directories, project, etc.) are set.
    /// 2. Configures the class loader to include project dependencies.
    /// 3. If `includeSourceFilesInClassDiscovery` is enabled, it uses an in-memory compiler to include project source
    ///    files in the class discovery process.
    /// 4. Calls [executeInternal] to perform the actual FXML parsing and source code generation.
    ///
    /// @throws MojoExecutionException If an error occurs during the FXML processing or file writing
    /// @throws MojoFailureException   If the configuration is invalid or no FXML directories are specified
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Objects.requireNonNull(fxmlDirectories, "FXML directories must be specified");
        Objects.requireNonNull(project, "Maven project must be specified");
        Objects.requireNonNull(generatedSourceDirectory, "Generated source directory must be specified");
        Objects.requireNonNull(defaultCharset, "Default charset must be specified");
        Charset charset = Charset.forName(defaultCharset);
        resourceBundleObject = Optional.ofNullable(resourceBundleObject)
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        if (fxmlDirectories.isEmpty()) {
            throw new MojoFailureException("No FXML directories specified");
        }
        fxmlDirectories.forEach(FXMLDirectory::validate);
        try (var _ = new ContextClassLoaderClosable(this::constructClasspathDependenciesClassLoader)) {
            if (includeSourceFilesInClassDiscovery) {
                executeWithOptimisticCompiler(() -> executeInternal(charset));
            }
            executeInternal(charset);
        }
    }

    /// Executes the given runner within a scoped class loader that includes compiled project source files.
    ///
    /// This is used to allow the FXML parser to "see" classes that are part of the current project but not yet compiled
    /// to disk.
    ///
    /// @param runnable The logic to execute with the extended class loader
    /// @throws MojoExecutionException If an error occurs during execution
    /// @throws MojoFailureException   If the execution fails due to configuration issues
    private void executeWithOptimisticCompiler(ThrowableRunner runnable)
            throws MojoExecutionException, MojoFailureException {
        try (var _ = new ContextClassLoaderClosable(this::sourceFilesClassLoaderExtender)) {
            runnable.run();
        }
    }

    /// Performs the core logic for generating Java source code from FXML files.
    ///
    /// This method iterates through all configured FXML directories and files, performing:
    /// 1. Initialization of the FXML reader, parser, and source code builder.
    /// 2. Registration of the generated source directory as a compiler source root in the Maven project.
    /// 3. For each FXML file:
    ///    1. Reading the raw FXML content.
    ///    2. Parsing the FXML into an [FXMLDocument] model.
    ///    3. Generating Java source code from the model.
    ///    4. Writing the generated code to a `.java` file in the appropriate package directory.
    ///
    /// @param charset The character set to use for reading FXML files and writing Java files
    /// @throws MojoExecutionException If an I/O error occurs or FXML processing fails
    private void executeInternal(Charset charset) throws MojoExecutionException {
        Log log = getLog();
        FXMLReader fxmlReader = new FXMLReader(log);
        FXMLDocumentParser fxmlDocumentParser = new FXMLDocumentParser(log, charset);
        FXMLSourceCodeBuilder fxmlSourceCodeBuilder = new FXMLSourceCodeBuilder(
                log,
                resourceBundleObject,
                addGeneratedAnnotation
        );
        project.addCompileSourceRoot(generatedSourceDirectory.toAbsolutePath().toString());
        for (FXMLDirectory fxmlDirectory : fxmlDirectories) {
            log.info("Processing FXML directory: %s".formatted(fxmlDirectory.getDirectory()));
            Path generatedDirectory = createGeneratedPackageDirectory(fxmlDirectory);
            List<Path> fxmlFiles = findFXMLFiles(fxmlDirectory);
            for (Path fxmlFile : fxmlFiles) {
                log.info("Processing FXML file: %s".formatted(fxmlFile));
                ParsedFXML parsedFXML = fxmlReader.readFXML(fxmlFile);
                if (log.isDebugEnabled()) {
                    log.debug("Read FXML:%n%s".formatted(ObjectMapperProvider.prettyPrint(parsedFXML)));
                }
                log.info("Processing FXML file: %s".formatted(fxmlFile));
                FXMLDocument fxmlDocument = fxmlDocumentParser.parse(parsedFXML, "/", fxmlDirectory.getDirectory());
                if (log.isDebugEnabled()) {
                    log.debug("Parsed FXML Document:%n%s".formatted(ObjectMapperProvider.prettyPrint(fxmlDocument)));
                }
                String sourceCode = fxmlSourceCodeBuilder.generateSourceCode(
                        fxmlDocument,
                        fxmlDirectory.getPackageName()
                );
                String fxmlFileName = fxmlFile.getFileName().toString();
                Path sourceCodeFile = generatedDirectory.resolve(fxmlFileName.substring(
                        0,
                        fxmlFileName.length() - 5
                ) + ".java");
                try {
                    Files.writeString(
                            sourceCodeFile,
                            sourceCode,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    );
                } catch (IOException e) {
                    log.error("Failed to write source code file: %s".formatted(sourceCodeFile), e);
                    throw new MojoExecutionException("Failed to write source code file", e);
                }
            }
        }
        log.info("FXML source code generation completed successfully");
    }

    /// Constructs [ClassLoader] that include all runtime and compile-time dependencies of the project.
    ///
    /// This class loader allows the plugin to resolve classes referenced in FXML files that are part of the project's
    /// dependency graph.
    ///
    /// @param currentClassLoader The parent class loader to use
    /// @return A new [URLClassLoader] containing the project's dependency URLs
    /// @throws MojoFailureException If project dependencies cannot be resolved
    private ClassLoader constructClasspathDependenciesClassLoader(ClassLoader currentClassLoader)
            throws MojoFailureException {
        try {
            Log log = getLog();
            log.debug("Constructing class loader for project dependencies");
            URL[] urls = Stream.concat(
                            project.getRuntimeClasspathElements().stream(),
                            project.getCompileClasspathElements().stream()
                    )
                    .distinct()
                    .map(Path::of)
                    .map(Path::toUri)
                    .map(Utils::uriToUrl)
                    .toArray(URL[]::new);
            log.debug("Classpath elements: %s".formatted(Arrays.toString(urls)));
            return URLClassLoader.newInstance(urls, currentClassLoader);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoFailureException(e);
        }
    }

    /// Extends the current class loader by compiling project source files in memory.
    ///
    /// This method uses the [OptimisticInMemoryCompiler] to scan the project's compiler source roots
    /// and compile them into a class loader.
    /// This ensures that the FXML parser can resolve custom controllers or types defined in the project.
    ///
    /// @param currentClassLoader The parent class loader to extend
    /// @return A new class loader that includes the in-memory compiled project classes
    /// @throws MojoExecutionException If compilation fails or an I/O error occurs
    private ClassLoader sourceFilesClassLoaderExtender(ClassLoader currentClassLoader)
            throws MojoExecutionException {
        Log log = getLog();
        List<URL> classpath = new ArrayList<>();
        log.debug("Extending class loader with source files");
        ClassLoader classLoader = currentClassLoader;
        while (classLoader instanceof URLClassLoader urlClassLoader) {
            List<URL> urLs = Arrays.asList(urlClassLoader.getURLs());
            log.debug("Extending class loader with source files from %s".formatted(urLs));
            classpath.addAll(urLs);
            classLoader = urlClassLoader.getParent();
        }
        log.info("Including source files in class discovery");
        List<Path> sourceFolders = project.getCompileSourceRoots()
                .stream()
                .map(Path::of)
                .toList();
        OptimisticInMemoryCompiler optimisticInMemoryCompiler = new OptimisticInMemoryCompiler(classpath);
        try {
            return optimisticInMemoryCompiler.optimisticCompileIntoClassLoader(log, sourceFolders)
                    .apply(currentClassLoader);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to compile source files", e);
        }
    }

    /// Creates the directory structure for the generated Java package on disk.
    ///
    /// If a package name is provided in the [FXMLDirectory] configuration,
    /// the directory path is constructed by replacing dots with slashes and resolving it against the base
    /// `generatedSourceDirectory`.
    ///
    /// @param fxmlDirectory The configuration object containing the package name
    /// @return The [Path] to the created (or existing) package directory
    /// @throws MojoExecutionException If the directory cannot be created due to an I/O error
    private Path createGeneratedPackageDirectory(FXMLDirectory fxmlDirectory) throws MojoExecutionException {
        Log log = getLog();
        Path generatedPackageDirectory;
        String packageName = fxmlDirectory.getPackageName();
        if (packageName == null) {
            log.debug("No package name specified, using default package");
            generatedPackageDirectory = generatedSourceDirectory;
        } else {
            log.debug("Using package name: %s".formatted(packageName));
            generatedPackageDirectory = generatedSourceDirectory.resolve(packageName.replace('.', '/'));
        }
        try {
            Files.createDirectories(generatedPackageDirectory);
            log.debug("Created generated package directory at: %s".formatted(generatedPackageDirectory));
            return generatedPackageDirectory;
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create generated package directory", e);
        }
    }

    /// Scans a directory for FXML files, excluding those explicitly marked for exclusion.
    ///
    /// This method performs a shallow walk (depth of 1) of the specified directory
    /// and filters for files ending with the FXML extension.
    ///
    /// @param fxmlDirectory The configuration object specifying the directory to scan and exclusions
    /// @return A list of absolute paths to the discovered FXML files
    /// @throws MojoExecutionException If an I/O error occurs while reading the directory
    private List<Path> findFXMLFiles(FXMLDirectory fxmlDirectory) throws MojoExecutionException {
        Log log = getLog();
        List<Path> excluded = fxmlDirectory.getExcludedFiles();
        try (Stream<Path> files = Files.walk(fxmlDirectory.getDirectory(), 1)) {
            List<Path> fxmlList = files.filter(Files::isRegularFile)
                    .filter(
                            f -> f.getFileName()
                                    .toString()
                                    .toLowerCase(Locale.ROOT)
                                    .endsWith(FXMLConstants.FXML_EXTENSION)
                    )
                    .map(Path::toAbsolutePath)
                    .filter(Predicate.not(excluded::contains))
                    .toList();
            log.info("Found %d FXML files".formatted(fxmlList.size()));
            log.debug("FXML files: %s".formatted(fxmlList));
            return fxmlList;
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read FXML directory", e);
        }
    }
}
