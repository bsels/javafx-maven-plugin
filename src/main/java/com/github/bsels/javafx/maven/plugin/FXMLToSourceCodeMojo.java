package com.github.bsels.javafx.maven.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLField;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.ProcessedFXML;
import com.github.bsels.javafx.maven.plugin.in.memory.compiler.OptimisticInMemoryCompiler;
import com.github.bsels.javafx.maven.plugin.io.FXMLReader;
import com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.parameters.FXMLParameterized;
import com.github.bsels.javafx.maven.plugin.parameters.InterfacesWithMethod;
import com.github.bsels.javafx.maven.plugin.utils.FXMLProcessor;
import com.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider;
import com.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
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
    /// It is used throughout the plugin's execution to reference or update project-related settings, such  as adding
    /// generated source directories to the project's compiled source roots.
    ///
    /// The value of this field is resolved by Maven using the `${project}` expression and is typically not manually
    /// configured by users.
    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;
    /// The path to the directory containing FXML files that will be processed.
    /// This directory is required and must be specified as a configuration parameter for the Maven plugin.
    /// The plugin will search this directory for FXML files to generate corresponding Java source code.
    ///
    /// This variable is tied to the Maven property "javafx.fxml.directory" and must be explicitly provided by the user
    /// when configuring the plugin.
    /// It is used during the execution process to locate and read FXML files for further processing.
    @Parameter(property = "javafx.fxml.directory", required = true)
    Path fxmlDirectory;
    /// Specifies the base package name for the generated Java source files.
    ///
    /// This configuration parameter is used to determine the package structure of the source code generated from the
    /// FXML files.
    /// The value should be a valid Java package name, complying with Java naming conventions.
    ///
    /// The package name serves as the root for organizing the generated classes and ensures proper namespace
    /// segregation within the project.
    /// If not explicitly set, a default package structure may be used, but specifying the package name provides more
    /// control over code organization.
    ///
    /// This parameter is particularly useful in scenarios where multiple modules or teams contribute to a project,
    /// preventing name conflicts and promoting maintainability.
    ///
    /// Maven users can configure this property in the project's POM file or pass it as a system property during the
    /// build process.
    @Parameter(property = "javafx.fxml.package")
    String packageName;
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
    /// Configuration option to enable or disable debugging of the internal FXML model during the execution of
    /// the Maven plugin.
    /// When enabled, this option facilitates the inspection of the internal representation of the FXML structure as it
    /// is processed and converted into source code.
    ///
    /// This option is primarily intended for diagnostic purposes to help investigate issues during the FXML processing
    /// and source code generation workflow.
    ///
    /// By default, this setting is disabled, and the internal model is not exposed for debugging.
    ///
    /// Property Name: "javafx.fxml.debug.internal.model"
    /// Default Value: false
    @Parameter(property = "javafx.fxml.debug.internal.model", defaultValue = "false")
    boolean debugInternalModel;
    /// Represents a Maven plugin parameter for configuring FXML parameterization.
    ///
    /// This field is used to specify a list of `FXMLParameterized` objects that define additional parameters for
    /// processing FXML files during the plugin execution.
    /// The parameterization may include customizations or mappings that enhance the handling of FXML structures,
    /// such as injecting root parameters, identifying parameters in the FXML structure, or mapping various interfaces.
    ///
    /// This parameter is configurable via the Maven project's `javafx.fxml.parameterization` property and is intended
    /// to be used during the execution of the plugin's logic to process FXML files and generate corresponding
    /// source code.
    ///
    /// If this field is null, the plugin may default to using empty or standard parameterization.
    @Parameter(property = "javafx.fxml.parameterization")
    List<FXMLParameterized> fxmlParameterizations;
    /// Indicates whether to include source files in the discovery process of classes when processing FXML files.
    ///
    /// This parameter controls the behavior of the class discovery mechanism within the plugin.
    /// When set to `true`,
    /// source files are included along with compiled classes during the discovery of FXML-related classes.
    /// This can be useful for scenarios where source files contain classes that are used in the FXML files.
    /// Setting this to `false` excludes source files from the discovery process, relying solely on compiled classes.
    ///
    /// Customizable through the Maven property `javafx.fxml.include.source.discovery`.
    ///
    /// Default value: `false`.
    @Parameter(property = "javafx.fxml.include.source.discovery", defaultValue = "false")
    boolean includeSourceFilesInClassDiscovery = false;

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
    /// - Processing the FXML models using the [FXMLProcessor] component.
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
    @Override
    public void execute() throws MojoExecutionException {
        Objects.requireNonNull(fxmlDirectory, "FXML directory must be specified");
        Objects.requireNonNull(project, "Maven project must be specified");
        Objects.requireNonNull(generatedSourceDirectory, "Generated source directory must be specified");

        ClassLoader classLoader = addClassPathToThreadContext();
        try {
            Log log = getLog();
            log.info("Generating FXML source code");
            FXMLReader fxmlReader = new FXMLReader(log);
            FXMLProcessor fxmlProcessor = new FXMLProcessor(log);
            project.addCompileSourceRoot(generatedSourceDirectory.toAbsolutePath().toString());
            if (packageName == null || packageName.isBlank()) {
                packageName = null;
            }
            if (resourceBundleObject == null || resourceBundleObject.isBlank()) {
                resourceBundleObject = null;
            }
            List<Path> fxmls = findFXMLFiles();
            Path generatedPackageDirectory = createGeneratedPackageDirectory();
            Map<String, FXMLParameterized> mappedFXMLParametrization = getClassNameToFXMLParameterizedMap();
            for (Path fxmlFile : fxmls) {
                ParsedFXML parsedFXML = fxmlReader.readFXML(fxmlFile);
                if (debugInternalModel) {
                    try {
                        log.info("Parsed FXML:%n%s".formatted(
                                ObjectMapperProvider.getObjectMapper()
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(parsedFXML))
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                ProcessedFXML processedFXML = fxmlProcessor.process(parsedFXML);
                if (debugInternalModel) {
                    try {
                        log.info("Processed FXML:%n%s".formatted(
                                ObjectMapperProvider.getObjectMapper()
                                        .writerWithDefaultPrettyPrinter()
                                        .writeValueAsString(processedFXML)
                        ));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                String sourceCode = convertFXMLToSourceCode(
                        processedFXML,
                        mappedFXMLParametrization.getOrDefault(processedFXML.className(), new FXMLParameterized())
                );
                Path sourceCodeFile = generatedPackageDirectory.resolve(processedFXML.className() + ".java");
                try {
                    Files.writeString(sourceCodeFile, sourceCode, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new MojoExecutionException("Unable to write FXML source code to file", e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    /// Generates a mapping of class names to their corresponding [FXMLParameterized] instances
    /// based on the configured FXML parameterization.
    ///
    /// This method processes the field `fxmlParameterizations`, which is expected to be a list of  [FXMLParameterized]
    /// objects, and creates a map where the key is the class name of each [FXMLParameterized] and the value is the
    /// corresponding object instance.
    ///
    /// If `fxmlParameterizations` is null, an empty map is returned.
    ///
    /// @return a map where the keys are class names as strings, and the values are [FXMLParameterized] objects
    private Map<String, FXMLParameterized> getClassNameToFXMLParameterizedMap() {
        return Optional.ofNullable(fxmlParameterizations)
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(FXMLParameterized::getClassName, Function.identity()));
    }

    /**
     * Creates a directory for the generated package based on the resolved path derived from the configured package name
     * and the generated source directory.
     * If the directory already exists, it is reused.
     * If the directory cannot be created due to an [IOException], a [MojoExecutionException] is thrown.
     *
     * @return the path to the generated package directory
     * @throws MojoExecutionException if the directory creation fails
     */
    private Path createGeneratedPackageDirectory() throws MojoExecutionException {
        Path generatedPackageDirectory;
        if (packageName == null) {
            generatedPackageDirectory = generatedSourceDirectory;
        } else {
            generatedPackageDirectory = generatedSourceDirectory.resolve(packageName.replace('.', '/'));
        }
        try {
            Files.createDirectories(generatedPackageDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create generated package directory", e);
        }
        return generatedPackageDirectory;
    }

    /// Scans the specified directory for FXML files and returns a list of paths to the files found.
    /// Only regular files with the ".fxml" extension are included in the result.
    /// Logs the count and details of the FXML files detected.
    ///
    /// @return a list of paths to the FXML files found in the directory
    /// @throws MojoExecutionException if an error occurs while accessing the directory or reading its contents
    private List<Path> findFXMLFiles() throws MojoExecutionException {
        try (Stream<Path> files = Files.walk(fxmlDirectory)) {
            List<Path> fxmls = files.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fxml"))
                    .toList();
            getLog().info("Found %d FXML files".formatted(fxmls.size()));
            getLog().debug("FXML files: %s".formatted(fxmls));
            return fxmls;
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read FXML directory", e);
        }
    }

    /// Adds the runtime and compile classpath, and optionally the source code files of the Maven project to
    /// the thread's context class loader.
    ///
    /// This method constructs a classpath based on the runtime and compile dependencies of the Maven project.
    /// It creates a new [URLClassLoader] with the combined classpath and sets it as the thread's context class loader.
    /// The process ensures that dependencies required at runtime or during compilation  are accessible within the
    /// thread's execution context.
    ///
    /// @return return the current class loader of the thread
    /// @throws MojoExecutionException if the Maven project classpath cannot be resolved or if a URL conversion error occurs
    private ClassLoader addClassPathToThreadContext() throws MojoExecutionException {
        try {
            List<URL> urls = Stream.concat(
                            project.getRuntimeClasspathElements().stream(),
                            project.getCompileClasspathElements().stream()
                    )
                    .distinct()
                    .map(Path::of)
                    .map(Path::toUri)
                    .map(Utils::uriToUrl)
                    .toList();
            ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader classLoader = URLClassLoader.newInstance(urls.toArray(URL[]::new), currentClassLoader);
            if (includeSourceFilesInClassDiscovery) {
                UnaryOperator<ClassLoader> extendClassLoader = getSourceFilesClassLoaderExtender();
                classLoader = extendClassLoader.apply(classLoader);
            }
            Thread.currentThread().setContextClassLoader(classLoader);
            return classLoader;
        } catch (DependencyResolutionRequiredException | IOException | RuntimeException e) {
            throw new MojoExecutionException(e);
        }
    }

    /// Creates a [UnaryOperator] that extends a [ClassLoader] by adding the ability to include
    /// and compile Java source files for class discovery.
    ///
    /// This method initializes an [OptimisticInMemoryCompiler] which compiles the source files from the project's
    /// compiler source roots into the class loader.
    /// The returned operator, when applied to a class loader, updates it to include the compiled classes from
    /// the provided source folders.
    ///
    /// @return a [UnaryOperator] that adding compiled Java source files to the given [ClassLoader].
    /// @throws IOException if an I/O error occurs while attempting to compile source files or access directories.
    private UnaryOperator<ClassLoader> getSourceFilesClassLoaderExtender() throws IOException {
        Log log = getLog();
        log.info("Including source files in class discovery");
        List<Path> sourceFolders = project.getCompileSourceRoots()
                .stream()
                .map(Path::of)
                .toList();
        OptimisticInMemoryCompiler optimisticInMemoryCompiler = new OptimisticInMemoryCompiler();
        return optimisticInMemoryCompiler.optimisticCompileIntoClassLoader(log, sourceFolders);
    }

    /// Converts a processed FXML structure into its corresponding source code representation.
    /// The method generates a source code string based on the provided FXML metadata, including packages,
    /// imports, fields, methods, and class hierarchy information.
    ///
    /// @param processedFXML     the metadata of the processed FXML structure, including imports, fields, methods, root node, and class name, must not be null
    /// @param fxmlParameterized contains additional parameterization details for the FXML structure, such as root parameters, identified parameters, and interface mappings; must not be null
    /// @return a string representing the source code generated from the processed FXML structure
    private String convertFXMLToSourceCode(ProcessedFXML processedFXML, FXMLParameterized fxmlParameterized) {
        getLog().info("Generating FXML source code for %s".formatted(processedFXML.className()));
        Set<String> imports = new HashSet<>(processedFXML.imports());
        Map<String, List<String>> interfacesMap = Optional.ofNullable(fxmlParameterized.getInterfaces())
                .stream()
                .flatMap(List::stream)
                .filter(interfacesWithMethod -> interfacesWithMethod.getInterfaceName() != null)
                .peek(interfacesWithMethod -> {
                    interfacesWithMethod.setInterfaceName(
                            Utils.improveImportForParameter(imports, interfacesWithMethod.getInterfaceName())
                    );
                    interfacesWithMethod.setGenerics(
                            Objects.requireNonNullElseGet(interfacesWithMethod.getGenerics(), List::of)
                    );
                    Utils.improveImportsForParameters(interfacesWithMethod.getGenerics(), imports);
                })
                .collect(Collectors.toMap(InterfacesWithMethod::getInterfaceName, InterfacesWithMethod::getGenerics));
        Set<String> interfaceMethods = Optional.ofNullable(fxmlParameterized.getInterfaces())
                .stream()
                .flatMap(List::stream)
                .map(InterfacesWithMethod::getMethodNames)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        FXMLSourceCodeBuilder builder = new FXMLSourceCodeBuilder(getLog())
                .addPackage(packageName)
                .addImports(imports);

        if (processedFXML.root() instanceof FXMLObjectNode(_, _, Class<?> clazz, _, _, List<String> generics)) {
            builder.openClass(processedFXML.className(), clazz.getSimpleName(), generics, interfacesMap);
        } else {
            builder.openClass(processedFXML.className(), null, List.of(), interfacesMap);
        }
        builder.addResourceBundle(resourceBundleObject);

        processedFXML.fields()
                .stream()
                .sorted(Comparator.comparing(FXMLField::internal).thenComparing(FXMLField::name))
                .filter(field -> !"this".equals(field.name()))
                .reduce(
                        builder,
                        (b, field) -> b.addField(
                                field.internal(),
                                field.name(),
                                field.clazz().getSimpleName(),
                                field.generics()
                        ),
                        Utils.getFirstLambda()
                )
                .addConstructor(processedFXML.root());

        return processedFXML.methods()
                .stream()
                .sorted(Comparator.comparing(FXMLMethod::name))
                .filter(method -> !interfaceMethods.contains(method.name()))
                .reduce(builder, FXMLSourceCodeBuilder::addAbstractMethod, Utils.getFirstLambda())
                .build();
    }
}
