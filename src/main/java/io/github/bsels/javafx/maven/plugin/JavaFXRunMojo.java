package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.utils.CheckAndCast;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.LocationManager;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Mojo for running a JavaFX application.
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public final class JavaFXRunMojo extends BaseJavaFXMojo {

    /// Executable to use for running the application.
    @Parameter(property = "javafx.executable", defaultValue = "java", required = true)
    String executable;

    /// Whether to enable debugging mode.
    @Parameter(property = "javafx.attachDebugger", defaultValue = "false")
    boolean attachDebugger;

    /// Port used for debugging.
    @Parameter(property = "javafx.debuggerPort", defaultValue = "5005")
    int debuggerPort = 5005;

    /// Initializes a new [JavaFXRunMojo] instance with required dependencies.
    ///
    /// @param locationManager  The manager for resolving paths and JPMS modules
    /// @param toolchainManager The manager for locating the JDK toolchain
    @Inject
    public JavaFXRunMojo(LocationManager locationManager, ToolchainManager toolchainManager) {
        super(locationManager, toolchainManager);
    }

    /// Executes the mojo to run the JavaFX application.
    ///
    /// The execution follows these steps:
    /// 1. Skips execution if the `skip` parameter is set.
    /// 2. Validates that the `executable` and `baseDirectory` are specified.
    /// 3. Initializes the plugin by resolving dependencies and module paths.
    /// 4. Ensures the working directory exists.
    /// 5. Constructs the full Java execution command using [getJavaRunCommand].
    /// 6. Launches the application in a separate process and waits for it to finish.
    ///
    /// @throws MojoExecutionException If an error occurs during initialization or process execution
    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution of JavaFX run");
            return;
        }
        if (isEmpty(executable)) {
            throw new MojoExecutionException("JavaFX executable is not specified");
        }
        if (baseDirectory == null) {
            throw new MojoExecutionException("JavaFX base directory is not specified");
        }

        getLog().info("Running JavaFX application");

        init(
                Optional.of(Paths.get(executable))
                        .map(Path::getParent)
                        .map(Path::getParent)
                        .orElse(null)
        );
        handleWorkingDirectory();

        List<String> command = getJavaRunCommand();
        getLog().info("Executing command: " + String.join(" ", command));
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .inheritIO();
        builder.environment().putAll(System.getenv());

        try {
            Process process = builder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error: Could not start JavaFX application", e);
        }
    }

    /// Constructs the full command-line arguments for running the JavaFX application.
    ///
    /// The command includes:
    /// - The path to the Java executable.
    /// - Custom logging format if specified.
    /// - Debugger agent configuration if `attachDebugger` is enabled.
    /// - System properties for additional binaries.
    /// - Native access flag for `javafx.graphics`.
    /// - User-provided JVM options.
    /// - Module path and classpath configuration.
    /// - The main class (and main module if applicable).
    /// - Command-line arguments for the application.
    ///
    /// @return A list of command arguments for the [ProcessBuilder]
    private List<String> getJavaRunCommand() {
        List<String> command = new ArrayList<>(getExecutable(executable));
        if (!isEmpty(loggingFormat)) {
            command.add("-D%s=%s".formatted(SIMPLE_LOG_FORMAT_PROPERTY, loggingFormat));
        }
        if (attachDebugger) {
            getLog().info("Debugging mode enabled");
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:%d".formatted(debuggerPort));
        }
        if (!isEmpty(additionalBinaries)) {
            additionalBinaries.stream()
                    .peek(additionalBinary -> getLog().debug("Adding additional binary: %s".formatted(additionalBinary.getName())))
                    .map(additionalBinary -> "-D%s=%s".formatted(
                            additionalBinary.getMappedJavaProperty(), additionalBinary.getLocation()
                    ))
                    .forEach(command::add);
        }
        command.add(ENABLE_NATIVE_ACCESS_JAVAFX);
        if (!isEmpty(options)) {
            options.stream()
                    .gather(CheckAndCast.of(String.class))
                    .map(this::splitComplexArgumentString)
                    .flatMap(List::stream)
                    .forEach(command::add);
        }
        if (!isEmpty(modulePathElements)) {
            command.add("--module-path");
            command.add(String.join(File.pathSeparator, modulePathElements));
            command.add("--add-modules");
            command.add(createAddModulesString());
        }
        if (!isEmpty(classPathElements)) {
            command.add("--classpath");
            command.add(String.join(File.pathSeparator, classPathElements));
        }
        if (!isEmpty(mainClass)) {
            if (moduleDescriptor != null) {
                command.add("--module");
            }
            command.add(mainClass);
        }
        if (!isEmpty(commandlineArgs)) {
            command.addAll(splitComplexArgumentString(commandlineArgs));
        }
        return command;
    }
}
