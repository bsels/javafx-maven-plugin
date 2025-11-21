package com.github.bsels.javafx.maven.plugin;

import com.github.bsels.javafx.maven.plugin.parameters.AdditionalBinary;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Base class for JavaFX-related Maven plugin goals. Provides shared functionality for
/// resolving dependencies, managing toolchains, working directories, and constructing
/// JavaFX-specific module paths and options. It serves as a foundational class for
/// creating specialized Maven Mojo implementations for JavaFX applications.
/// Fields:
/// - MODULE_INFO_FILTER: A filter for identifying `module-info.class` files.
/// - IS_WHITESPACE: A predicate for detecting whitespace strings.
/// - IS_WHITESPACE_CHAR: A predicate for detecting whitespace characters.
/// - SEPARATOR: The character used to separate elements in paths.
/// - JAVAFX_PREFIX: The prefix used for identifying JavaFX-based modules.
/// - EMPTY: A constant representing an empty collection or string.
/// - JAVA: The name of the Java executable to be used.
/// - locationManager: An instance for managing paths and resources.
/// - toolchainManager: An instance for managing toolchains and configurations.
/// - modulePathElements: A collection of module paths required for linking or execution.
/// - classPathElements: A collection of classpath elements required for compilation or execution.
/// - pathElements: A collection of generic path elements.
/// - moduleDescriptor: The `module-info.class` descriptor for modular applications.
/// - skip: A flag to determine if the Mojo execution should be skipped.
/// - project: The Maven project associated with this Mojo.
/// - mainClass: The fully-qualified name of the application's main class.
/// - baseDirectory: The base directory of the Maven project.
/// - workingDirectory: The directory used for execution-related tasks.
/// - options: A list of additional JVM options to be passed during execution.
/// - commandlineArgs: Arguments passed to the main class during execution.
/// - session: The Maven session associated with the current build process.
public abstract sealed class BaseJavaFXMojo extends AbstractMojo permits JavaFXJlinkMojo, JavaFXRunMojo {
    /// A static predicate used to filter paths that point to the `module-info.class` file.
    /// This filter is typically used to determine whether a given path corresponds to
    /// the Java module descriptor file (`module-info.class`), which is a critical component
    /// in modular Java applications.
    /// The predicate evaluates a file path and returns `true` if the file name is exactly
    /// `module-info.class`. Otherwise, it returns `false`.
    private static final Predicate<Path> MODULE_INFO_FILTER = path -> "module-info.class".equals(path.getFileName().toString());
    /// A static Predicate implementation used to determine whether a given
    /// string matches a whitespace character pattern.
    /// This predicate is compiled from a regular expression pattern (`\\s`),
    /// which matches any whitespace character, including spaces, tabs, newlines,
    /// form feeds, and carriage returns.
    /// The primary purpose of this variable is to provide an efficient and
    /// reusable mechanism for matching strings to whitespace patterns within
    /// the context of the enclosing class functionality.
    private static final Predicate<String> IS_WHITESPACE = Pattern.compile("\\s").asMatchPredicate();
    /// A `Predicate` that determines whether a given `Character` represents a whitespace character.
    /// This is achieved by testing the character using the `IS_WHITESPACE` predicate after converting it to a `String`.
    /// The primary purpose of this constant is to serve as a functional utility for filtering or evaluating
    /// individual characters based on whether they meet the whitespace definition implemented by the `IS_WHITESPACE` predicate.
    /// This variable is immutable and thread-safe due to being declared as `private static final`.
    private static final Predicate<Character> IS_WHITESPACE_CHAR = c -> IS_WHITESPACE.test(String.valueOf(c));
    /// Represents the delimiter used for separating elements in certain string operations.
    /// Commonly utilized in operations where whitespace is treated as a separator.
    private static final char SEPARATOR = ' ';
    /// The `JAVAFX_PREFIX` is a constant string used as a prefix to identify and filter
    /// JavaFX-related modules or paths. It is commonly utilized in methods dealing with
    /// JavaFX module resolution, filtering, or validation within the build process.
    private static final String JAVAFX_PREFIX = "javafx";
    /// A constant representing the label or value "Empty".
    /// This value is used to denote or identify empty or uninitialized states
    /// or entities within the application context.
    /// The constant is declared as `private` and `static final`, indicating
    /// that it is immutable, associated with the class rather than instances,
    /// and not accessible outside the containing class.
    private static final String EMPTY = "Empty";
    /// Represents the constant string "java", typically used to denote the
    /// Java executable name or Java-related contexts within the `BaseJavaFXMojo` class.
    /// This field is defined as a static final constant, ensuring immutability and shared
    /// accessibility across instances of the class.
    private static final String JAVA = "java";
    /// Represents the system property key used to customize the log format for the `SimpleFormatter`
    /// class in the Java logging framework (`java.util.logging`).
    ///
    /// This property allows users to define a custom format string for log messages generated by
    /// the `SimpleFormatter`. When set, the value of this property overrides the default log format.
    ///
    /// The value of the property is the string "java.util.logging.SimpleFormatter.format".
    protected static final String SIMPLE_LOG_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    /// A constant string used to enable native access specifically for the `javafx.graphics` module.
    /// This value is a parameter passed to the JVM to allow native access for the JavaFX graphics module,
    /// which may be required for certain native operations or optimizations leveraged by JavaFX applications.
    protected static final String ENABLE_NATIVE_ACCESS_JAVAFX = "--enable-native-access=javafx.graphics";
    /// The locationManager serves as a reference to an instance of the LocationManager class.
    /// It is responsible for managing the resolution and location of paths and resources,
    /// specifically within the context of JavaFX builds and related setups.
    /// This field is final, ensuring that its value is assigned at construction and remains immutable
    /// throughout the lifecycle of the containing class. It plays a pivotal role in handling resource
    /// path validations, directory resolutions, and possibly other file location operations essential
    /// to the execution of the class's functionality.
    /// The LocationManager is initialized through the constructor of the BaseJavaFXMojo class,
    /// ensuring that the required dependencies and configurations are injected at instantiation.
    protected final LocationManager locationManager;
    /// Manages and resolves toolchains for the JavaFX plugin.
    /// The `ToolchainManager` is utilized by the plugin to determine compatible
    /// toolchains and provide paths to the required Java executable or other
    /// toolchain-related resources.
    /// This field is initialized via the constructor of the `BaseJavaFXMojo` class
    /// and plays a critical role in supporting cross-platform builds or resolving
    /// specific Java versions for the project's execution environment.
    /// As a protected final field, it ensures consistency and immutability across
    /// the lifecycle of the `BaseJavaFXMojo` instance, while also allowing subclasses
    /// to access the resolved toolchain configuration.
    protected final ToolchainManager toolchainManager;
    /// Represents a collection of module path elements required for the JavaFX build process.
    /// This list contains paths to the modules necessary for compiling and executing the JavaFX
    /// application. These paths typically include project dependencies and any external modules
    /// specified as part of the build configuration.
    /// The elements in this list are used during the initialization of the JavaFX plugin and are
    /// integral to ensuring the correct resolution of module paths. This is especially important
    /// in modular applications that rely on the proper configuration of `module-info.class` files
    /// and related resources.
    /// This field is initialized and populated as part of the setup process for the JavaFX
    /// plugin, after validating and resolving dependencies to ensure there are no issues
    /// in the build lifecycle.
    protected List<String> modulePathElements;
    /// Represents a list of classpath elements used in the building and runtime processes
    /// of a JavaFX project. Each element in the list is a string representing a specific
    /// path or resource that should be included in the classpath.
    /// This list typically includes directories, JAR files, or other resource locations
    /// required by the JavaFX application. It may include project-specific output directories
    /// as well as dependencies managed by the build system.
    /// This field is protected, allowing subclasses to directly access and manage the contents
    /// of the classpath elements. It is primarily initialized and populated by related configuration
    /// or initialization methods in the enclosing class, often leveraging project dependencies
    /// or manually specified paths.
    /// The order of the elements in this list may impact resolution during runtime, and care should
    /// be taken to include paths in the correct sequence.
    protected List<String> classPathElements;
    /// A mapping of module paths to their corresponding Java module descriptors.
    /// This variable helps track the relationship between specific paths used in
    /// the build process and the Java modules they represent. It assists in
    /// managing and resolving module paths within the project structure during
    /// initialization or execution.
    /// Each key in the map represents a path string, while the values are instances
    /// of `JavaModuleDescriptor`, which encapsulate information about a given
    /// module, such as its name, dependencies, and exports. This mapping is critical
    /// for proper handling of Java module-based builds, particularly those utilizing
    /// JavaFX or modular project configurations.
    protected Map<String, JavaModuleDescriptor> pathElements;
    /// Represents the module descriptor for the Java project being processed.
    /// The module descriptor provides metadata about the project's modular structure,
    /// such as its name, required modules, exported packages, and provided services. It
    /// is typically derived from the `module-info.class` file located in the project's
    /// output directory or determined from the module path elements.
    /// This variable is integral for managing and resolving module-related configuration
    /// within the JavaFX Mojo plugin, ensuring compliance with the Java Platform Module
    /// System (JPMS).
    protected JavaModuleDescriptor moduleDescriptor;
    /// Indicates whether the execution of the plugin should be skipped.
    /// If set to true, the plugin's execution will be bypassed.
    /// This parameter can be configured using the "javafx.skip" property.
    /// By default, this value is set to false.
    @Parameter(property = "javafx.skip", defaultValue = "false")
    protected boolean skip;
    /// Represents the Maven project being built by the plugin.
    /// This variable provides access to various project-related information, such as the
    /// project's dependencies, directory structure, and configuration.
    /// This field is initialized automatically by Maven and is marked as read-only to
    /// prevent unintended modifications during plugin execution. It is commonly used in
    /// Mojo implementations to gather details about the current Maven project.
    /// The `@Parameter` annotation specifies that this parameter is injected with
    /// the current Maven project (`${project}`) during the plugin's execution.
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    /// The `mainClass` specifies the fully qualified name of the JavaFX application's main class
    /// to be executed. This class should define a `public static void main(String[] args)` method
    /// that serves as the entry point for the JavaFX application.
    /// This property is required and must be set for the JavaFX plugin to function correctly.
    /// It is typically provided as a parameter in the Maven build configuration.
    @Parameter(property = "javafx.mainClass", required = true)
    protected String mainClass;
    /// Represents the base directory of the Maven project. This directory is resolved to the "basedir"
    /// property of the Maven build, typically corresponding to the root directory containing the
    /// `pom.xml` file.
    /// This variable is used as a reference point for resolving relative paths in the build process
    /// and is essential for various plugin operations.
    /// The value is immutable during execution and must be provided as it is a required parameter.
    /// Configuration:
    /// - `readonly`: Ensures the value remains constant throughout the execution.
    /// - `required`: Denotes that this parameter must be set.
    /// - `defaultValue`: Defaults to Maven's `${basedir}` property, which refers to the root project directory.
    @Parameter(readonly = true, required = true, defaultValue = "${basedir}")
    protected Path baseDirectory;
    /// Represents the working directory used by the JavaFX Maven plugin during the build process.
    /// The working directory serves as the base location for plugin-related files and operations.
    /// - This variable is configurable via the Maven property `javafx.workingDirectory`.
    /// - If not explicitly set, the working directory may default to the project's base directory.
    /// - The directory is validated and created if it does not already exist.
    /// It is typically used internally by the plugin to organize temporary files, logs, or other resources
    /// required by the JavaFX build task.
    @Parameter(property = "javafx.workingDirectory")
    protected Path workingDirectory;
    /// Represents a list of optional configuration or runtime parameters for the JavaFX Mojo.
    /// This list can include various types of objects or values required for specific build
    /// or runtime tasks. The values in this list are typically provided through configuration
    /// in the Maven POM file or passed as command-line arguments.
    /// This property is designed to accommodate a flexible and dynamic set of inputs, making it
    /// adaptable for different use cases or scenarios. The specific structure and data types
    /// within the list are dependent on the context in which the Mojo or plugin is used.
    /// This configuration is marked as a Maven parameter and can be set or overridden during
    /// the Maven build lifecycle.
    @Parameter
    protected List<?> options;
    /// Holds the command-line arguments to be passed to the JavaFX application during its execution.
    /// The arguments are defined as a single string, with multiple arguments typically separated by spaces.
    /// This variable corresponds to the `javafx.args` Maven property, allowing users to configure it in their build configuration.
    @Parameter(property = "javafx.args")
    protected String commandlineArgs;
    /// Defines the format used for logging within the JavaFX Maven plugin execution.
    /// This parameter allows users to specify a custom logging format to standardize or adapt
    /// log output according to their requirements.
    ///
    /// The property associated with this parameter is `javafx.loggingFormat`, which can be
    /// configured in the Maven POM file or passed as a command-line argument during the
    /// Maven build process.
    @Parameter(property = "javafx.loggingFormat")
    protected String loggingFormat;

    /// Represents the current Maven session.
    /// The MavenSession object provides access to various details and objects
    /// related to the Maven build lifecycle during the execution of the plugin.
    /// It includes information such as the current project, goals, settings,
    /// repository management, and execution context.
    /// This field is automatically populated by Maven and is marked as read-only.
    /// It allows the plugin to interact with the Maven build system and retrieve
    /// or manipulate relevant data during the build process.
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    /// Represents a list of additional binary files to be included as part of the JavaFX application packaging process.
    /// Each entry in the list is defined by the [AdditionalBinary] type, which contains information about the
    /// specific binary file, such as its source path and destination path within the packaged application.
    ///
    /// This field is configured via the Maven property `javafx.additionalBinaries`. Users can define additional resource
    /// files or binary dependencies required for the application, and these will be packaged alongside the application
    /// during the build process.
    @Parameter(property = "javafx.additionalBinaries")
    protected List<AdditionalBinary> additionalBinaries;

    /// Constructs a new instance of BaseJavaFXMojo with the specified location manager and toolchain manager.
    ///
    /// @param locationManager  the LocationManager instance responsible for locating and managing paths and resources.
    /// @param toolchainManager the ToolchainManager instance responsible for managing and resolving toolchains.
    protected BaseJavaFXMojo(LocationManager locationManager, ToolchainManager toolchainManager) {
        this.locationManager = Objects.requireNonNull(locationManager, "`locationManager` cannot be null");
        this.toolchainManager = toolchainManager;
        getLog().debug("Initializing BaseJavaFXMojo...");
    }

    /// Determines the full path to the Java executable based on the provided executable name or path.
    /// The method prioritizes explicit paths, toolchain configurations, the `java.home` system property,
    /// or system environment variables (e.g., `PATH`). On Windows, if necessary, it prefixes the path with `cmd /c`.
    ///
    /// @param executable the name or path of the Java executable; this can be a custom name (e.g., "java") or an explicit path to an existing executable file.
    /// @return a list of strings representing the command to execute the Java executable.
    ///         The list typically contains a single element (the executable path), but on Windows
    ///         it may include additional commands (e.g., `cmd`, `/c`, and the executable path).
    protected List<String> getExecutable(String executable) {
        final Path executablePath = Path.of(executable);
        Path finalExecutablePath = null;
        if (Files.isRegularFile(executablePath)) {
            getLog().info("'executable' parameter is set to: " + executable);
            finalExecutablePath = executablePath.toAbsolutePath();
        } else {
            if (toolchainManager != null) {
                Toolchain toolchain = toolchainManager.getToolchainFromBuildContext("jdk", session);
                if (toolchain != null) {
                    finalExecutablePath = Paths.get(toolchain.findTool(JAVA));
                    getLog().info("Using toolchain to find Java executable: " + finalExecutablePath);
                }
            }
            if (finalExecutablePath == null) {
                String javaHome = System.getProperty("java.home");
                if (!isEmpty(javaHome)) {
                    finalExecutablePath = Paths.get(javaHome, "bin", executable);
                    getLog().info("Using java.home to find Java executable: " + finalExecutablePath);
                } else if (isWindowsOs()) {
                    String executablePathString = System.getProperty("PATH");
                    if (!isEmpty(executablePathString)) {
                        outer:
                        for (String path : executablePathString.split(File.pathSeparator)) {
                            for (String extension : getExecutableExtensions()) {
                                Path pathToExecutable = Paths.get(path, JAVA + extension);
                                if (Files.exists(pathToExecutable)) {
                                    finalExecutablePath = pathToExecutable;
                                    getLog().info("Using Path to find Java executable: " + finalExecutablePath);
                                    break outer;
                                }
                            }
                        }
                    }
                }
                if (finalExecutablePath == null) {
                    finalExecutablePath = executablePath;
                }
            }
        }

        String exec = finalExecutablePath.toString();
        if (isWindowsOs() &&
                !hasWindowsNativeExtension(executable) &&
                hasExecutableExtension(executable)) {
            String comSpec = Optional.ofNullable(System.getProperty("ComSpec"))
                    .orElse("cmd");
            return List.of(comSpec, "/c", exec);
        }
        return List.of(exec);
    }

    /// Determines whether the current operating system is Windows.
    ///
    /// @return true if the operating system is identified as a Windows family OS;
    ///         false otherwise.
    protected final boolean isWindowsOs() {
        return Os.isFamily(Os.FAMILY_WINDOWS);
    }

    /// Checks whether the provided file name or path ends with a known native executable extension.
    ///
    /// @param exec the file name or path to check; should not be null
    /// @return true if the file name or path has a native executable extension (e.g., ".exe" or ".com"), false otherwise
    private boolean hasWindowsNativeExtension(final String exec) {
        final String lowerCase = exec.toLowerCase();
        return lowerCase.endsWith(".exe") || lowerCase.endsWith(".com");
    }

    /// Checks whether the provided file name or path ends with a known executable extension.
    ///
    /// @param exec the file name or path to check; should not be null
    /// @return true if the file name or path has one of the executable extensions, false otherwise
    private boolean hasExecutableExtension(final String exec) {
        final String lowerCase = exec.toLowerCase();
        for (final String ext : getExecutableExtensions()) {
            if (lowerCase.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /// Retrieves the list of executable file extensions used by the operating system.
    /// If the `PATHEXT` environment variable is defined, its value is split by the path
    /// separator and converted to lowercase. If `PATHEXT` is not set, a default list
    /// containing `.bat` and `.cmd` is returned.
    ///
    /// @return a list of strings representing executable file extensions.
    private List<String> getExecutableExtensions() {
        return Optional.ofNullable(System.getProperty("PATHEXT"))
                .map(pathExt -> StringUtils.split(pathExt.toLowerCase(), File.pathSeparator))
                .map(List::of)
                .orElse(List.of(".bat", ".cmd"));
    }

    /// Initializes the JavaFX plugin by setting up module and classpath elements based on the project configuration
    /// and its dependencies. This method also validates required files like module descriptors and resolves dependency paths.
    /// If filename-based module path elements are detected, a warning is logged.
    ///
    /// @param jdkHome the path to the JDK home directory, which is used during the resolution of paths. If null, the default system JDK is used.
    /// @throws MojoExecutionException if an error occurs during initialization, such as a missing module-info file, unreachable output directory, or issues while resolving paths.
    protected final void init(Path jdkHome) throws MojoExecutionException {
        getLog().info("Initializing JavaFX plugin...");
        getLog().info("Project: " + project);
        if (project == null) {
            return;
        }
        String outputDirectory = project.getBuild().getOutputDirectory();
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            throw new MojoExecutionException("Error: Output directory does not exists");
        }

        Path moduleDescriptorPath;
        try (Stream<Path> files = Files.walk(Path.of(outputDirectory))) {
            moduleDescriptorPath = files.filter(Files::isRegularFile)
                    .filter(MODULE_INFO_FILTER)
                    .findFirst()
                    .orElseThrow(
                            () -> new MojoExecutionException("Error: module-info.class file is required")
                    );
        } catch (IOException e) {
            throw new MojoExecutionException("Error: Output directory does not exists", e);
        }

        List<Path> dependencies = getDependencies();
        getLog().info("Total dependencies: " + dependencies.size());
        ResolvePathsRequest<Path> pathsToResolve = ResolvePathsRequest.ofPaths(dependencies);
        getLog().info("Resolving paths...");
        getLog().info("Module descriptor path: " + moduleDescriptorPath);

        pathsToResolve.setMainModuleDescriptor(moduleDescriptorPath);
        if (jdkHome != null) {
            pathsToResolve.setJdkHome(jdkHome);
        }

        ResolvePathsResult<Path> resolvePathsResult;
        try {
            resolvePathsResult = locationManager.resolvePaths(pathsToResolve);
        } catch (IOException e) {
            throw new MojoExecutionException("Error: Could not resolve paths", e);
        }

        pathElements = resolvePathsResult.getPathElements()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        Map.Entry::getValue
                ));

        Map<Path, Exception> pathExceptions = resolvePathsResult.getPathExceptions();
        if (!isEmpty(pathExceptions)) {
            getLog().warn("Path exceptions:");
            getLog().warn("There are %d path exceptions. The related dependencies will be ignored.".formatted(pathExceptions.size()));
            pathExceptions.forEach((path, exception) -> {
                getLog().warn("Dependency: " + path);
                getLog().warn("Exception: " + exception.getMessage());
                Throwable cause = exception.getCause();
                while (cause != null) {
                    getLog().warn("Cause: " + cause.getMessage());
                    cause = cause.getCause();
                }
            });
        }

        moduleDescriptor = resolvePathsResult.getMainModuleDescriptor();
        boolean filenameBasedModule = resolvePathsResult.getModulepathElements()
                .values()
                .stream()
                .anyMatch(ModuleNameSource.FILENAME::equals);
        if (filenameBasedModule) {
            getLog().warn("""
                    Filename-based module path elements detected. \
                    Please don't publish this project to a public artifact repository!\
                    """);
        }
        classPathElements = resolvePathsResult.getClasspathElements()
                .stream()
                .map(Path::toString)
                .collect(Collectors.toCollection(ArrayList::new));
        modulePathElements = resolvePathsResult.getModulepathElements()
                .keySet()
                .stream()
                .map(Path::toString)
                .collect(Collectors.toCollection(ArrayList::new));

        modulePathElements.addAll(classPathElements);
        classPathElements.clear();

        getLog().info("JavaFX plugin initialized successfully.");
        getLog().debug("Classpath elements: " + String.join(", ", classPathElements));
        getLog().debug("Module path elements: " + String.join(", ", modulePathElements));
        getLog().debug("Path elements: " + pathElements);
    }

    /// Retrieves the classpath elements required for the compilation process.
    /// This includes the output directory path, system paths of project dependencies,
    /// and sorted artifact file paths. Duplicates are removed, and the result provides
    /// a distinct list of paths in the form of a `List<Path>`.
    ///
    /// @return a list of distinct `Path` objects representing the compile-time classpath elements.
    protected final List<Path> getDependencies() {
        Stream<Path> outputDirectoryStream = Optional.ofNullable(project.getBuild())
                .map(Build::getOutputDirectory)
                .map(Path::of)
                .stream();
        Stream<Path> dependencyPathStream = project.getDependencies()
                .stream()
                .map(Dependency::getSystemPath)
                .filter(Predicate.not(this::isEmpty))
                .map(Paths::get);
        Stream<Path> artifactsPathStream = project.getArtifacts()
                .stream()
                .sorted(this::compareArtifact)
                .map(Artifact::getFile)
                .map(File::toPath);
        return Stream.concat(outputDirectoryStream, Stream.concat(dependencyPathStream, artifactsPathStream))
                .distinct()
                .toList();
    }

    /// Compares two Artifact objects to determine their relative ordering.
    /// The primary comparison is based on their natural ordering as defined
    /// by the `compareTo` method of the Artifact class. If the artifacts
    /// are considered equal by their natural ordering, a secondary comparison
    /// is made based on the presence of a classifier.
    ///
    /// @param a the first Artifact object to compare
    /// @param b the second Artifact object to compare
    /// @return a negative integer, zero, or a positive integer as the first
    ///         Artifact is less than, equal to, or greater than the second,
    ///         respectively. If the natural ordering is equal, the result will
    ///         depend on the comparison of the classifiers.
    private int compareArtifact(Artifact a, Artifact b) {
        int result = a.compareTo(b);
        if (result == 0) {
            result = Boolean.compare(a.hasClassifier(), b.hasClassifier());
        }
        return result;
    }

    /// Ensures that the working directory is properly set and exists.
    ///
    ///   - If the working directory is not already set, it defaults to the base directory.
    ///   - If the working directory does not exist, it attempts to create the necessary directories.
    ///   - Debug logs are generated when creating the working directory.
    ///
    /// @throws MojoExecutionException if the working directory could not be created due to an I/O error
    protected final void handleWorkingDirectory() throws MojoExecutionException {
        if (workingDirectory == null) {
            workingDirectory = baseDirectory;
        }

        if (!Files.exists(workingDirectory)) {
            getLog().debug("Making working directory '" + workingDirectory.toAbsolutePath() + "'.");
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not make working directory: '" + workingDirectory.toAbsolutePath() + "'", e);
            }
        }
    }

    /// Checks if the provided collection is either null or empty.
    ///
    /// @param collection the collection to check; it can be null
    /// @return `true` if the collection is null or empty, otherwise `false`
    protected final boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /// Checks whether the provided string is either null or empty.
    ///
    /// @param string the string to check; it can be null
    /// @return `true` if the string is null or empty; `false` otherwise
    protected final boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /// Checks whether the provided map is either null or empty.
    ///
    /// @param map the map to check; it can be null
    /// @return `true` if the map is null or empty; `false` otherwise
    protected final boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /// Splits a complex string of arguments into individual components, respecting quoted
    /// substrings and preserving the content within quotes.
    ///
    /// @param argumentString the input string containing the arguments to be split. It may
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             include whitespace and quoted substrings.
    /// @return a list of strings where each element represents a distinct argument. Quoted
    ///         substrings are treated as single arguments, and leading/trailing whitespace
    ///         is removed.
    protected final List<String> splitComplexArgumentString(String argumentString) {
        argumentString = argumentString.strip();
        List<String> splitArguments = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        char expectedSeparator = SEPARATOR;
        for (int i = 0; i < argumentString.length(); i++) {
            char item = argumentString.charAt(i);
            if (item == expectedSeparator || (expectedSeparator == SEPARATOR && IS_WHITESPACE_CHAR.test(item))) {
                if (expectedSeparator == '"' || expectedSeparator == '\'') {
                    stringBuilder.append(item);
                    expectedSeparator = SEPARATOR;
                } else if (!stringBuilder.isEmpty()) {
                    splitArguments.add(stringBuilder.toString());
                    stringBuilder.delete(0, stringBuilder.length());
                }
            } else {
                if (expectedSeparator == SEPARATOR && (item == '"' || item == '\'')) {
                    expectedSeparator = item;
                }
                stringBuilder.append(item);
            }
        }
        splitArguments.add(stringBuilder.toString());
        return splitArguments.stream()
                .filter(Predicate.not(String::isEmpty))
                .toList();
    }

    /// Creates a string that lists the JavaFX modules to be added.
    /// If a module descriptor exists, its name is returned. Otherwise, the method
    /// filters the path elements to include only non-null module names starting with
    /// the JavaFX prefix and excludes modules ending with "Empty". These module names
    /// are then concatenated into a single, comma-separated string.
    ///
    /// @return a comma-separated string of module names to be added, or the name of the
    ///         module descriptor if it exists; an empty string if no valid modules are found
    protected final String createAddModulesString() {
        if (moduleDescriptor != null) {
            return moduleDescriptor.name();
        }
        return pathElements.values()
                .stream()
                .filter(Objects::nonNull)
                .map(JavaModuleDescriptor::name)
                .filter(Objects::nonNull)
                .filter(module -> module.startsWith(JAVAFX_PREFIX) && !module.endsWith(EMPTY))
                .collect(Collectors.joining(","));
    }
}
