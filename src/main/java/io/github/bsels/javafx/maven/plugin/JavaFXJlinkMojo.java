package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.parameters.AdditionalBinary;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.languages.java.jpms.LocationManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// Mojo for generating a JavaFX runtime image using the `jlink` tool.
/// Handles output directories, launcher scripts, and optional zipping of the runtime image.
@Mojo(name = "jlink", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public final class JavaFXJlinkMojo extends BaseJavaFXMojo {
    /// Predicate to match Java logging configuration for `SimpleFormatter`.
    private static final Predicate<String> LOG_FORMAT_REGEX = Pattern.compile(
            "^#?\\s*java.util.logging.SimpleFormatter.format=.*$"
    ).asMatchPredicate();
    /// Variable for all command-line arguments in launcher scripts.
    private static final String COMMAND_ARGS_VAR = "$@";
    /// Prefix for UNIX VM options in launcher scripts.
    private static final String JLINK_VM_OPTIONS = "JLINK_VM_OPTIONS=";
    /// Prefix for Windows VM options in launcher scripts.
    private static final String WIN_JLINK_VM_OPTIONS = "set JLINK_VM_OPTIONS=";
    /// Valid compression options for the `jlink` tool.
    private static final Set<String> COMPRESS_OPTIONS = Stream.concat(
            IntStream.rangeClosed(0, 2).mapToObj(String::valueOf),
            IntStream.rangeClosed(0, 9).mapToObj(index -> String.format("zip-%d", index))
    ).collect(Collectors.toSet());
    /// Default name for the binaries directory.
    private static final String BIN_DIRECTORY = "bin";
    /// Default name for the configuration directory.
    private static final String CONF_DIRECTORY = "conf";
    /// Name of the logging properties file.
    private static final String LOGGING_PROPERTIES_FILE = "logging.properties";
    /// File extension for ZIP archives.
    private static final String ZIP_EXTENSION = ".zip";
    /// Key for the `SimpleFormatter` format property.
    private static final String JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT = "java.util.logging.SimpleFormatter.format=%s";
    /// Utility for creating ZIP archives.
    private final Archiver zipArchiver;
    /// Project build output directory.
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    Path buildDirectory;
    /// Name of the launcher to create.
    @Parameter(property = "javafx.launcher")
    String launcher;
    /// Path to the `jlink` executable.
    @Parameter(property = "javafx.jlinkExecutable", defaultValue = "jlink")
    String jlinkExecutable;
    /// Name of the generated ZIP file.
    @Parameter(property = "javafx.jlinkZipName")
    String jlinkZipName;
    /// Name of the generated runtime image directory.
    @Parameter(property = "javafx.jlinkImageName", defaultValue = "image")
    String jlinkImageName;
    /// Path to the JavaFX jmods directory.
    @Parameter(property = "javafx.jmodsPath")
    String jmodsPath;
    /// Whether to strip debug information.
    @Parameter(property = "javafx.stripDebug", defaultValue = "false")
    boolean stripDebug;
    /// Whether to strip Java debug attributes.
    @Parameter(property = "javafx.stripJavaDebugAttributes", defaultValue = "false")
    boolean stripJavaDebugAttributes;
    /// Compression level for the runtime image.
    @Parameter(property = "javafx.compress", defaultValue = "0")
    String compress;
    /// Whether to remove the `includes` directory.
    @Parameter(property = "javafx.noHeaderFiles", defaultValue = "false")
    boolean noHeaderFiles;
    /// Whether to remove the `man` directory.
    @Parameter(property = "javafx.noManPages", defaultValue = "false")
    boolean noManPages;
    /// Whether to include the `--bind-services` option.
    @Parameter(property = "javafx.bindServices", defaultValue = "false")
    boolean bindServices;
    /// Whether to ignore signing information.
    @Parameter(property = "javafx.ignoreSigningInformation", defaultValue = "false")
    boolean ignoreSigningInformation;
    /// Whether to enable verbose output for `jlink`.
    @Parameter(property = "javafx.jlinkVerbose", defaultValue = "false")
    boolean jlinkVerbose;

    /// Initializes a new [JavaFXJlinkMojo] instance.
    ///
    /// @param locationManager  The manager for paths and resources
    /// @param toolchainManager The manager for toolchains
    /// @param zipArchiver      The archiver for ZIP files
    @Inject
    public JavaFXJlinkMojo(
            LocationManager locationManager,
            ToolchainManager toolchainManager,
            @Named("zip")
            Archiver zipArchiver
    ) {
        super(locationManager, toolchainManager);
        this.zipArchiver = zipArchiver;
    }

    /// Executes the `jlink` mojo to create a JavaFX runtime image.
    ///
    /// @throws MojoExecutionException If an error occurs during execution
    /// @throws MojoFailureException   If the linking process fails
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution of JavaFX jlink");
            return;
        }
        if (isEmpty(jlinkExecutable)) {
            throw new MojoExecutionException("JavaFX jlink executable is not specified");
        }
        if (baseDirectory == null) {
            throw new MojoExecutionException("JavaFX base directory is not specified");
        }

        getLog().info("Linking JavaFX application");
        init(
                Optional.of(Paths.get(jlinkExecutable))
                        .map(Path::getParent)
                        .map(Path::getParent)
                        .orElse(null)
        );
        handleWorkingDirectory();

        List<String> command = getJLinkCommand();
        getLog().info("Executing command: " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .inheritIO();
        builder.environment().putAll(System.getenv());

        try {
            Process process = builder.start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                getLog().error("Error: Could not link JavaFX application, exit code: %d".formatted(errorCode));
                throw new MojoFailureException("Error: Could not link JavaFX application");
            }
        } catch (InterruptedException | IOException e) {
            throw new MojoExecutionException("Error: Could not link JavaFX application", e);
        }

        copyAdditionalBinariesToBinaryFolder();
        patchLauncherScripts();
        patchLoggingFormat();
        zipApplication();
    }

    /// Copies additional binaries to the runtime image's bin directory.
    ///
    /// @throws MojoExecutionException If an I/O error occurs during copying
    private void copyAdditionalBinariesToBinaryFolder() throws MojoExecutionException {
        if (isEmpty(additionalBinaries)) {
            return;
        }
        getLog().info("Copying additional binaries to binary folder");
        Path binDirectory = buildDirectory.resolve(jlinkImageName, BIN_DIRECTORY);
        for (AdditionalBinary additionalBinary : additionalBinaries) {
            Path inputPath = additionalBinary.getLocation();
            Path outputPath = binDirectory.resolve(inputPath.getFileName());
            try {
                getLog().info("Copying additional binary: %s to %s".formatted(inputPath.toAbsolutePath(), outputPath));
                Files.copy(
                        inputPath,
                        outputPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                );
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to copy `%s` to `%s`".formatted(inputPath, outputPath), e);
            }
        }
    }

    /// Constructs the `jlink` execution command.
    ///
    /// @return A list of command arguments
    /// @throws MojoExecutionException If a required configuration is missing
    private List<String> getJLinkCommand() throws MojoExecutionException {
        List<String> command = new ArrayList<>(getExecutable(jlinkExecutable));
        if (!isEmpty(modulePathElements)) {
            command.add("--module-path");
            String modules = String.join(File.pathSeparator, modulePathElements);
            if (!isEmpty(jmodsPath)) {
                getLog().debug("Using custom jmods path: " + jmodsPath);
                modules = jmodsPath + File.pathSeparator + modules;
            }
            command.add(modules);
            command.add("--add-modules");
            if (moduleDescriptor != null) {
                command.add(createAddModulesString());
            } else {
                throw new MojoExecutionException("jlink requires a module descriptor");
            }
        }
        command.add("--output");
        Path outputDirectory = buildDirectory.resolve(jlinkImageName);
        getLog().debug("Using output directory: " + outputDirectory.toAbsolutePath());
        if (Files.exists(outputDirectory)) {
            cleanupOutputDirectory(outputDirectory);
        }
        command.add(outputDirectory.toAbsolutePath().toString());

        if (stripDebug) {
            command.add("--strip-debug");
        }
        if (stripJavaDebugAttributes) {
            command.add("--strip-java-debug-attributes");
        }
        if (!isEmpty(compress)) {
            if (!COMPRESS_OPTIONS.contains(compress)) {
                getLog().warn("Invalid compression option: " + compress);
                getLog().warn("Valid compression options are: " + String.join(", ", COMPRESS_OPTIONS));
                throw new MojoExecutionException("Invalid compression option: " + compress);
            }
            command.add("--compress=" + compress);
        }
        if (noHeaderFiles) {
            command.add("--no-header-files");
        }
        if (noManPages) {
            command.add("--no-man-pages");
        }
        if (bindServices) {
            command.add("--bind-services");
        }
        if (ignoreSigningInformation) {
            command.add("--ignore-signing-information");
        }
        if (jlinkVerbose) {
            command.add("--verbose");
        }

        if (!isEmpty(launcher)) {
            command.add("--launcher");
            String moduleMainClass;
            if (mainClass.contains("/")) {
                moduleMainClass = mainClass;
            } else {
                moduleMainClass = moduleDescriptor.name() + "/" + mainClass;
            }
            command.add(launcher + "=" + moduleMainClass);
        }
        return command;
    }

    /// Deletes the specified output directory and its contents.
    ///
    /// @param outputDirectory The directory to clean up
    /// @throws MojoExecutionException If deletion fails
    private void cleanupOutputDirectory(Path outputDirectory) throws MojoExecutionException {
        try (Stream<Path> filesToRemove = Files.walk(outputDirectory)) {
            filesToRemove.sorted(Comparator.reverseOrder())
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (RuntimeException | IOException e) {
            Throwable exception = e;
            if (exception instanceof RuntimeException) {
                exception = exception.getCause();
            }
            throw new MojoExecutionException("Error: Could not remove existing output directory", exception);
        }
    }

    /// Patches launcher scripts to include custom JVM options and arguments.
    ///
    /// @throws MojoExecutionException If patching fails
    private void patchLauncherScripts() throws MojoExecutionException {
        if (isEmpty(launcher)) {
            return;
        }
        getLog().info("Creating launcher");
        getLog().info("Patching launcher scripts");
        patchLauncherScript(launcher);
        if (isWindowsOs()) {
            patchLauncherScript(launcher + ".bat");
        }
        getLog().info("Launcher scripts patched successfully");
    }

    /// Updates the specified launcher script.
    ///
    /// @param launcher The name of the launcher script file
    /// @throws MojoExecutionException If an I/O error occurs
    private void patchLauncherScript(String launcher) throws MojoExecutionException {
        Path launcherScript = buildDirectory.resolve(jlinkImageName, BIN_DIRECTORY, launcher);
        if (!Files.exists(launcherScript)) {
            getLog().warn("Launcher script does not exist: " + launcherScript.toAbsolutePath());
            return;
        }

        Stream<String> optionsStream = Stream.of(ENABLE_NATIVE_ACCESS_JAVAFX);
        if (!isEmpty(additionalBinaries)) {
            optionsStream = Stream.concat(
                    optionsStream,
                    additionalBinaries.stream()
                            .map(this::extractPropertyForBinary)
            );
            getLog().debug("Added %d additional binaries".formatted(additionalBinaries.size()));
        } else {
            getLog().debug("No additional binaries specified for launcher script");
        }
        if (!isEmpty(options)) {
            optionsStream = Stream.concat(
                    optionsStream,
                    options.stream()
                            .gather(CheckAndCast.of(String.class))
            );
            getLog().debug("Add JVM options to launcher script");
        } else {
            getLog().debug("No JVM options specified for launcher script");
        }
        String optionsString = optionsStream.collect(Collectors.joining(" "));
        getLog().debug("Launcher script options: " + optionsString);
        Function<String, String> handleJvmOptions = line -> handleJvmOption(line, optionsString);

        Function<String, String> handleCommandlineArgs;
        if (!isEmpty(commandlineArgs)) {
            getLog().debug("Add commandline args to launcher script: " + String.join(" ", commandlineArgs));
            handleCommandlineArgs = this::handleCommandLineArgs;
        } else {
            getLog().debug("No commandline args specified for launcher script");
            handleCommandlineArgs = Function.identity();
        }

        List<String> updatedScript;
        try (Stream<String> lines = Files.lines(launcherScript)) {
            updatedScript = lines.map(handleJvmOptions)
                    .map(handleCommandlineArgs)
                    .toList();
            Files.write(launcherScript, updatedScript);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to patch launcher script", e);
        }
    }

    /// Extracts a formatted file path property for an [AdditionalBinary].
    ///
    /// @param additionalBinary The binary object
    /// @return A formatted property string
    private String extractPropertyForBinary(AdditionalBinary additionalBinary) {
        getLog().debug("Adding %s to additional binaries".formatted(additionalBinary.getName()));
        Path fileName = additionalBinary.getLocation().getFileName();
        return "-D%s=./%s".formatted(additionalBinary.getMappedJavaProperty(), fileName);
    }

    /// Updates the logging configuration file with the desired log format.
    ///
    /// @throws MojoExecutionException If an I/O error occurs
    private void patchLoggingFormat() throws MojoExecutionException {
        if (isEmpty(loggingFormat)) {
            return;
        }
        getLog().info("Patching logging format in `conf/logging.properties` configuration file");
        Path loggingPropertiesFile = buildDirectory.resolve(jlinkImageName, CONF_DIRECTORY, LOGGING_PROPERTIES_FILE);
        if (!Files.exists(loggingPropertiesFile)) {
            getLog().warn("Logging format configuration file does not exist: %s".formatted(
                    loggingPropertiesFile.toAbsolutePath()
            ));
            return;
        }

        try (Stream<String> lines = Files.lines(loggingPropertiesFile)) {
            List<String> updateConfiguration = lines.map(this::replaceLoggingFormat)
                    .collect(Collectors.toCollection(ArrayList::new));
            boolean noneMatchFormat = updateConfiguration.stream()
                    .noneMatch(LOG_FORMAT_REGEX);
            if (noneMatchFormat) {
                updateConfiguration.addLast(JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT.formatted(loggingFormat));
            }
            Files.write(loggingPropertiesFile, updateConfiguration);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to patch configuration logging properties", e);
        }
    }

    /// Replaces the logging format in a line if it matches the pattern.
    ///
    /// @param line The input line
    /// @return The updated line
    private String replaceLoggingFormat(String line) {
        if (LOG_FORMAT_REGEX.test(line)) {
            return JAVA_UTIL_LOGGING_SIMPLE_FORMATTER_FORMAT.formatted(loggingFormat);
        }
        return line;
    }

    /// Substitutes command-line argument placeholders in a script line.
    ///
    /// @param line The input line
    /// @return The updated line
    private String handleCommandLineArgs(String line) {
        if (line.stripTrailing().endsWith(COMMAND_ARGS_VAR)) {
            return line.replace(COMMAND_ARGS_VAR, commandlineArgs + " " + COMMAND_ARGS_VAR);
        }
        return line;
    }

    /// Appends custom options to a JVM option line.
    ///
    /// @param line          The original line
    /// @param optionsString The options to append
    /// @return The updated line
    private String handleJvmOption(String line, String optionsString) {
        boolean unixOptionsLine = JLINK_VM_OPTIONS.equals(line);
        boolean winOptionsLine = WIN_JLINK_VM_OPTIONS.equals(line);

        if (unixOptionsLine || winOptionsLine) {
            String lineWrapper = unixOptionsLine ? "'" : "";
            return line + lineWrapper + optionsString + lineWrapper;
        }
        return line;
    }

    /// Creates a ZIP archive of the application.
    ///
    /// @throws MojoExecutionException If an error occurs during zipping
    private void zipApplication() throws MojoExecutionException {
        if (isEmpty(jlinkZipName)) {
            return;
        }
        getLog().info("Zipping application");
        Path applicationZip = createApplicationZipArchive();
        Optional.ofNullable(project)
                .map(MavenProject::getArtifact)
                .ifPresent(artifact -> artifact.setFile(applicationZip.toFile()));
        getLog().info("Application zipped successfully");
    }

    /// Creates a ZIP archive of the JavaFX runtime image.
    ///
    /// @return The path to the created ZIP archive
    /// @throws MojoExecutionException If an I/O error occurs
    private Path createApplicationZipArchive() throws MojoExecutionException {
        Path imageDirectory = buildDirectory.resolve(jlinkImageName);
        zipArchiver.addFileSet(
                DefaultFileSet.fileSet(imageDirectory.toFile())
                        .prefixed("")
                        .includeExclude(null, null)
                        .includeEmptyDirs(true)
        );
        Path resultArchive = buildDirectory.resolve(jlinkZipName + ZIP_EXTENSION);
        zipArchiver.setDestFile(resultArchive.toFile());
        try {
            zipArchiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create ZIP archive", e);
        }
        return resultArchive;
    }

}
