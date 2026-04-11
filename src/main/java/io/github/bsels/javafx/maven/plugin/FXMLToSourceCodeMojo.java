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
///
/// This class extends the AbstractMojo class and provides functionality to process FXML files,
/// generate corresponding Java source code, and manage Maven project configurations for the generated source files.
/// The plugin's execution process involves reading FXML files, processing their internal structure,
/// generating source files, and writing them to a configured directory.
///
/// Fields:
/// - `project`: The Maven project instance, used to access project configurations and update source roots.
/// - `fxmlDirectory`: The directory containing the FXML files to be processed.
/// - `packageName`: The base package name for the generated Java source code.
/// - `resourceBundleObject`: The resource bundle configuration, used for localization support in FXML files.
/// - `generatedSourceDirectory`: The directory where the generated source files will be written.
/// - `debugInternalModel`: A flag indicating whether to enable debugging for the internal FXML models.
/// - `fxmlParameterizations`: A list of configuration settings or objects representing FXML parameterization details.
@Mojo(name = "fxml-source", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public final class FXMLToSourceCodeMojo extends AbstractMojo {
    /// Represents the Maven project associated with the plugin execution.
    ///
    /// This variable is automatically injected by Maven during the plugin execution and provides access to the current
    /// Maven project's configuration and metadata.
    /// It is marked as read-only and cannot be modified by the plugin.
    ///
    /// The MavenProject instance contains details such as the project's artifacts, dependencies, build configuration,
    /// and other metadata.
    /// It is used throughout the plugin's execution to reference or update project-related settings, such as adding
    /// generated source directories to the project's compiled source roots.
    ///
    /// The value of this field is resolved by Maven using the `${project}` expression and is typically not manually
    /// configured by users.
    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;
    /// A list of directories containing FXML files to be processed by the Maven plugin.
    ///
    /// This field is required and specifies the source directories that are scanned
    /// for FXML files during the plugin's execution.
    /// Each directory in the list is expected to contain FXML files that need to be converted into Java source code.
    ///
    /// Additional considerations:
    /// - The field is annotated with `@Parameter(required = true)` to indicate that it must be provided in
    ///   the plugin configuration.
    /// - Future improvements may include combining this with a list of package names
    ///   and supporting exclusion lists for more granular control of which FXML files are processed.
    @Parameter(required = true)
    List<FXMLDirectory> fxmlDirectories;
    /// Represents the resource bundle configuration for FXML processing.
    ///
    /// This variable is used to specify the property value for a resource bundle that provides localized strings or
    /// other resources for use within FXML files.
    /// If provided, the specified resource bundle is used to resolve keys referenced in the FXML file during
    /// source code generation or runtime evaluation.
    ///
    /// Users can configure this property in the Maven build process via the parameter
    /// `javafx.fxml.resourceBundleObject`.
    /// The value of this parameter must indicate the fully qualified class name or resource bundle path that
    /// corresponds to the required bundle.
    ///
    /// It can be used to integrate internationalization (i18n) support or provide additional data resources linked to
    /// the FXML views being processed.
    @Parameter(property = "javafx.fxml.resourceBundleObject")
    String resourceBundleObject;
    /// Specifies the directory where the Java source code generated from FXML files will be written.
    ///
    /// This directory will contain all the source files derived from the processing of FXML files within the designated
    /// FXML directory.
    /// The default location for the generated source directory is in the "generated-sources/fxml" folder inside the
    /// project's build directory.
    /// This path can be overridden by configuring the property `javafx.fxml.generatedSourceDirectory`.
    ///
    /// It is a required parameter, and the Maven project will include this directory in its compiler source roots to
    /// ensure that the generated files are accessible during the compilation phase.
    @Parameter(
            property = "javafx.fxml.generatedSourceDirectory",
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/fxml"
    )
    Path generatedSourceDirectory;
    /// Indicates whether to include source files in the discovery process of classes when processing FXML files.
    ///
    /// This parameter controls the behavior of the class discovery mechanism within the plugin.
    /// When set to `true`, source files that can be compiled without the generated sources are included along
    /// with compiled classes during the discovery of FXML-related classes.
    /// This can be useful for scenarios where source files contain classes that are used in the FXML files.
    /// Setting this to `false` excludes source files from the discovery process, relying solely on compiled classes.
    ///
    /// Customizable through the Maven property `javafx.fxml.include.source.discovery`.
    ///
    /// Default value: `true`.
    @Parameter(property = "javafx.fxml.include.source.discovery", defaultValue = "true")
    boolean includeSourceFilesInClassDiscovery = true;
    /// Specifies the character set to be used when reading FXML files and generating source code.
    ///
    /// The default value is UTF-8.
    @Parameter(property = "javafx.fxml.charset", defaultValue = "UTF-8", required = true)
    String defaultCharset = "UTF-8";
    /// Indicates whether the `@Generated` annotation should be added to the generated Java source files.
    ///
    /// When set to `true`, the annotation is included, providing metadata about the source of the generated file.
    @Parameter(property = "javafx.fxml.add.generated.annotation", defaultValue = "true", required = true)
    boolean addGeneratedAnnotation = true;

    /// Constructs a new instance of the [FXMLToSourceCodeMojo] class.
    ///
    /// This constructor initializes the object by invoking the superclass constructor of [AbstractMojo].
    /// The class is responsible for converting FXML files into Java source code through its Maven plugin execution
    /// logic.
    /// Initialization of fields and dependencies required for the plugin's functionality
    /// is performed within the context of the class configuration and execution lifecycle.
    public FXMLToSourceCodeMojo() {
        super();
    }

    /// Executes the Maven plugin logic for generating FXML source code.
    ///
    /// This method reads FXML files from the specified directory, processes them to create an internal representation
    /// of their structure, and generates corresponding Java source code based on the processed representation.
    /// The generated source files are written to a directory determined by configuration.
    ///
    /// The execution process involves several steps:
    /// - Reading the FXML files using the [FXMLReader] component.
    /// - Generating source code for each processed FXML model.
    /// - Writing the generated source code to files in the appropriate package directory.
    ///
    /// Additional functionality includes logging information at various stages of the execution process and debugging
    /// the internal models if enabled.
    /// The Maven project configuration is updated to include the generated source directory in the project's compiler
    /// source roots.
    ///
    /// The method handles potential issues such as:
    /// - Missing or blank resource bundle configurations.
    /// - [IOException] or other issues during file writing.
    /// - JSON processing exceptions for debugging purposes.
    ///
    /// @throws MojoExecutionException if an error occurs during any stage of the execution process, such as file I/O operations or failure to process FXML files.
    /// @throws MojoFailureException   if the execution fails due to configuration issues or other non-recoverable errors.
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

    /// Executes the given [ThrowableRunner] within a context that optimistically compiles
    /// and includes source files from the project in the class loader.
    ///
    /// This method ensures that the provided `runnable` is executed with a class loader extended to include source
    /// files discovered in the project's compiler source roots.
    /// The class loader extension is applied temporarily and is reverted to its original state once the execution
    /// completes.
    ///
    /// @param runnable the [ThrowableRunner] containing the logic to be executed. This runner may throw [MojoExecutionException] or [MojoFailureException], and its execution must comply with the provided contract.
    /// @throws MojoExecutionException if an error occurs during the extension of the class loader or the execution of the supplied `runnable`.
    /// @throws MojoFailureException   if the execution fails due to configuration issues or other unrecoverable errors encountered by the `runnable`.
    private void executeWithOptimisticCompiler(ThrowableRunner runnable)
            throws MojoExecutionException, MojoFailureException {
        try (var _ = new ContextClassLoaderClosable(this::sourceFilesClassLoaderExtender)) {
            runnable.run();
        }
    }

    /// Performs the internal execution logic for generating source code from FXML files.
    /// This involves reading FXML files, parsing them, and generating Java source files in the specified directory.
    ///
    /// @param charset the character set to use for reading and parsing FXML files.
    /// @throws MojoExecutionException if any error occurs during FXML reading, parsing, or source code writing.
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

    /// Constructs an [ClassLoader] instance that includes all runtime and compile classpath dependencies of
    /// the Maven project.
    /// This allows loading of classes from the project's dependencies during execution.
    ///
    /// @param currentClassLoader the parent class loader to be used for delegating class loading. It is typically the current thread context class loader or a class loader passed to the method.
    /// @return a new [ClassLoader] that has access to the project's runtime and compile classpath elements.
    /// @throws MojoFailureException if there is a failure in resolving the required dependencies for constructing the class loader.
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

    /// Extends the provided [ClassLoader] to include source files in the class discovery process.
    ///
    /// This method retrieves the project's compiler source roots and compiles them using an in-memory compiler.
    /// The resulting classes are integrated into a new [ClassLoader] which extends the scope of the provided
    /// `currentClassLoader`.
    ///
    /// @param currentClassLoader the parent class loader to extend. Typically, this is the current thread context class loader or another class loader relevant to the Maven plugin's execution context.
    /// @return a new [ClassLoader] instance that includes the compiled source files from the project's compiler source roots.
    /// @throws MojoExecutionException if an error occurs during the compilation process, or if it fails to create the extended [ClassLoader].
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

    /// Creates a directory for the generated package based on the resolved path derived from the configured package name
    /// and the generated source directory.
    /// If the directory already exists, it is reused.
    /// If the directory cannot be created due to an [IOException], a [MojoExecutionException] is thrown.
    ///
    /// @param fxmlDirectory the FXML directory configuration containing the package name and the generated source directory.
    /// @return the path to the generated package directory
    /// @throws MojoExecutionException if the directory creation fails
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

    /// Scans the specified directory for FXML files and returns a list of paths to the files found.
    /// Only regular files with the ".fxml" extension are included in the result.
    /// Logs the count and details of the FXML files detected.
    ///
    /// @param fxmlDirectory the directory to scan for FXML files
    /// @return a list of paths to the FXML files found in the directory
    /// @throws MojoExecutionException if an error occurs while accessing the directory or reading its contents
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
