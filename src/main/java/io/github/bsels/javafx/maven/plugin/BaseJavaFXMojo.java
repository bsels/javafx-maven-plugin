package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.parameters.AdditionalBinary;
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

/// Base class for JavaFX-related Maven plugin goals.
/// Provides shared functionality for resolving dependencies, managing toolchains,
/// and constructing JavaFX-specific module paths and options.
public abstract sealed class BaseJavaFXMojo extends AbstractMojo permits JavaFXJlinkMojo, JavaFXRunMojo {
    /// System property key for customizing the log format of `SimpleFormatter`.
    protected static final String SIMPLE_LOG_FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";
    /// Parameter to enable native access for the `javafx.graphics` module.
    protected static final String ENABLE_NATIVE_ACCESS_JAVAFX = "--enable-native-access=javafx.graphics";
    /// Predicate to filter `module-info.class` files.
    private static final Predicate<Path> MODULE_INFO_FILTER = path -> "module-info.class".equals(path.getFileName().toString());
    /// Predicate to detect whitespace strings.
    private static final Predicate<String> IS_WHITESPACE = Pattern.compile("\\s").asMatchPredicate();
    /// Predicate to detect whitespace characters.
    private static final Predicate<Character> IS_WHITESPACE_CHAR = c -> IS_WHITESPACE.test(String.valueOf(c));
    /// Delimiter for separating elements in strings.
    private static final char SEPARATOR = ' ';
    /// Prefix for JavaFX-related modules.
    private static final String JAVAFX_PREFIX = "javafx";
    /// Constant for empty states.
    private static final String EMPTY = "Empty";
    /// Constant for the Java executable.
    private static final String JAVA = "java";
    /// Manager for resolving paths and resources.
    protected final LocationManager locationManager;
    /// Manager for resolving toolchains.
    protected final ToolchainManager toolchainManager;
    /// List of module path elements.
    protected List<String> modulePathElements;
    /// List of classpath elements.
    protected List<String> classPathElements;
    /// Mapping of paths to their Java module descriptors.
    protected Map<String, JavaModuleDescriptor> pathElements;
    /// Module descriptor for the project.
    protected JavaModuleDescriptor moduleDescriptor;
    /// Flag to skip the plugin execution.
    @Parameter(property = "javafx.skip", defaultValue = "false")
    protected boolean skip;
    /// The Maven project.
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    /// Fully qualified name of the application's main class.
    @Parameter(property = "javafx.mainClass", required = true)
    protected String mainClass;
    /// Base directory of the Maven project.
    @Parameter(readonly = true, required = true, defaultValue = "${basedir}")
    protected Path baseDirectory;
    /// Working directory for the plugin execution.
    @Parameter(property = "javafx.workingDirectory")
    protected Path workingDirectory;
    /// Optional configuration or runtime parameters.
    @Parameter
    protected List<?> options;
    /// Command-line arguments for the JavaFX application.
    @Parameter(property = "javafx.args")
    protected String commandlineArgs;
    /// Custom logging format.
    @Parameter(property = "javafx.loggingFormat")
    protected String loggingFormat;
    /// The Maven session.
    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;
    /// List of additional binary files to include.
    @Parameter(property = "javafx.additionalBinaries")
    protected List<AdditionalBinary> additionalBinaries;

    /// Initializes a new [BaseJavaFXMojo] instance with the required managers.
    ///
    /// This constructor ensures that the [LocationManager] is not null and initializes the [ToolchainManager].
    /// It also logs a debug message indicating the start of the initialization process.
    ///
    /// @param locationManager  The manager for resolving paths and resources, used for JPMS module path resolution
    /// @param toolchainManager The manager for locating specific JDK toolchains from the Maven environment
    /// @throws NullPointerException if `locationManager` is null
    protected BaseJavaFXMojo(LocationManager locationManager, ToolchainManager toolchainManager)
            throws NullPointerException {
        this.locationManager = Objects.requireNonNull(locationManager, "`locationManager` cannot be null");
        this.toolchainManager = toolchainManager;
        getLog().debug("Initializing BaseJavaFXMojo...");
    }

    /// Determines the correct path to the Java executable to be used for execution.
    ///
    /// The selection process follows this hierarchy:
    /// 1. If the provided `executable` is an existing regular file, it is used directly.
    /// 2. If a [ToolchainManager] is available, it attempts to find the `java` tool from the Maven toolchain.
    /// 3. If no toolchain is found, it looks for `java` in the `java.home` system property.
    /// 4. On Windows, it searches for `java` in the system `PATH` using common executable extensions.
    /// 5. If all else fails, it defaults to the provided `executable` string.
    ///
    /// If the OS is Windows and the executable requires a command shell (e.g., a `.bat` or `.cmd` file),
    /// the command is wrapped with `cmd /c`.
    ///
    /// @param executable The name or path of the Java executable (e.g., "java", "java.exe", or a full path)
    /// @return A list of strings representing the command to execute (e.g., `["java"]` or `["cmd", "/c", "path/to/script.bat"]`)
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

    /// Checks whether the current operating system belongs to the Windows family.
    ///
    /// This uses the `Os.isFamily(Os.FAMILY_WINDOWS)` utility from Plexus.
    ///
    /// @return `true` if the OS is Windows; `false` otherwise
    protected final boolean isWindowsOs() {
        return Os.isFamily(Os.FAMILY_WINDOWS);
    }

    /// Checks whether the specified file name has a native Windows executable extension (`.exe` or `.com`).
    ///
    /// The comparison is case-insensitive.
    ///
    /// @param exec The file name or path to check
    /// @return `true` if it ends with a native Windows extension; `false` otherwise
    private boolean hasWindowsNativeExtension(final String exec) {
        final String lowerCase = exec.toLowerCase();
        return lowerCase.endsWith(".exe") || lowerCase.endsWith(".com");
    }

    /// Checks whether the specified file name has any of the system's defined executable extensions.
    ///
    /// It compares the file name against the list of extensions retrieved via [getExecutableExtensions].
    ///
    /// @param exec The file name or path to check
    /// @return `true` if it has an executable extension; `false` otherwise
    private boolean hasExecutableExtension(final String exec) {
        final String lowerCase = exec.toLowerCase();
        for (final String ext : getExecutableExtensions()) {
            if (lowerCase.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /// Returns the list of valid executable file extensions for the current environment.
    ///
    /// On most systems, this reads the `PATHEXT` environment variable.
    /// If `PATHEXT` is not defined, it defaults to `.bat` and `.cmd`.
    ///
    /// @return A list of lowercase executable extensions, including the leading dot
    private List<String> getExecutableExtensions() {
        return Optional.ofNullable(System.getProperty("PATHEXT"))
                .map(pathExt -> StringUtils.split(pathExt.toLowerCase(), File.pathSeparator))
                .map(List::of)
                .orElse(List.of(".bat", ".cmd"));
    }

    /// Initializes the JavaFX plugin by resolving module paths and dependencies.
    ///
    /// This method performs the following steps:
    /// 1. Verifies the existence of the project output directory.
    /// 2. Scans the output directory for a `module-info.class` file to identify the main module.
    /// 3. Collects all project dependencies (artifacts, system dependencies, and output directories).
    /// 4. Resolves the paths for these dependencies using the [LocationManager],
    ///    distinguishing between module path and classpath elements based on the main module's descriptor.
    /// 5. Populates the `modulePathElements`, `classPathElements`, and `pathElements` maps.
    /// 6. Logs warnings for any path resolution exceptions or filename-based automatic modules.
    ///
    /// @param jdkHome The path to the JDK home directory, used for resolving system modules. Can be `null`.
    /// @throws MojoExecutionException If the output directory or `module-info.class` is missing, or if path resolution fails.
    protected final void init(Path jdkHome) throws MojoExecutionException {
        getLog().info("Initializing JavaFX plugin...");
        getLog().info("Project: " + project);
        if (project == null) {
            return;
        }
        String outputDirectory = project.getBuild().getOutputDirectory();
        if (outputDirectory == null || outputDirectory.isEmpty()) {
            throw new MojoExecutionException("Error: Output directory does not exist");
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
            throw new MojoExecutionException("Error: Output directory does not exist", e);
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
            getLog().warn("There are %d path exceptions. The related dependencies will be ignored.".formatted(
                    pathExceptions.size()));
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

    /// Collects and returns all dependencies required for the Java execution.
    ///
    /// This includes:
    /// - The project's build output directory.
    /// - Any system-scoped dependencies that have a defined path.
    /// - All project artifacts, sorted to ensure consistent ordering.
    ///
    /// The result is a list of distinct [Path] objects.
    ///
    /// @return A list of unique paths representing the project's dependencies and output
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

    /// Compares two Maven artifacts to determine their relative order in the dependency list.
    ///
    /// The comparison first uses the default `Artifact.compareTo(Artifact)` logic.
    /// If the artifacts are equal, according to that logic,
    /// it breaks the tie by comparing whether they have classifiers.
    ///
    /// @param a The first artifact to compare
    /// @param b The second artifact to compare
    /// @return A negative integer, zero, or a positive integer as the first artifact is less than, equal to, or greater than the second
    private int compareArtifact(Artifact a, Artifact b) {
        int result = a.compareTo(b);
        if (result == 0) {
            result = Boolean.compare(a.hasClassifier(), b.hasClassifier());
        }
        return result;
    }

    /// Ensures that the working directory for the execution is properly set and created.
    ///
    /// If `workingDirectory` is not explicitly configured, it defaults to the project's `baseDirectory`.
    /// If the directory does not exist on disk, this method attempts to create it,
    /// including any necessary parent directories.
    ///
    /// @throws MojoExecutionException If the directory does not exist and cannot be created
    protected final void handleWorkingDirectory() throws MojoExecutionException {
        if (workingDirectory == null) {
            workingDirectory = baseDirectory;
        }

        if (!Files.exists(workingDirectory)) {
            getLog().debug("Making working directory '" + workingDirectory.toAbsolutePath() + "'.");
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Could not make working directory: '" + workingDirectory.toAbsolutePath() + "'",
                        e
                );
            }
        }
    }

    /// Checks if the collection is null or empty.
    ///
    /// @param collection The collection to check
    /// @return `true` if null or empty; `false` otherwise
    protected final boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /// Checks if the string is null or empty.
    ///
    /// @param string The string to check
    /// @return `true` if null or empty; `false` otherwise
    protected final boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /// Checks if the map is null or empty.
    ///
    /// @param map The map to check
    /// @return `true` if null or empty; `false` otherwise
    protected final boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /// Splits a command-line argument string into a list of individual arguments,
    /// correctly handling quoted sections (both single and double quotes).
    ///
    /// This method preserves characters inside quotes and treats whitespace as the delimiter outside quoted segments.
    ///
    /// @param argumentString The raw string of command-line arguments to split
    /// @return A list of individual argument strings, with empty strings filtered out
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

    /// Constructs a string suitable for the `--add-modules` JVM option.
    ///
    /// If a main `moduleDescriptor` is available, its name is returned.
    /// Otherwise, it scans all resolved `pathElements` and collects the names of modules that start with the `javafx`
    /// prefix and do not have an "Empty" state.
    ///
    /// @return A comma-separated string of module names to be added at runtime
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
