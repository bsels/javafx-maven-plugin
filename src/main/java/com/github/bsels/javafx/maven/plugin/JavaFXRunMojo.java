package com.github.bsels.javafx.maven.plugin;

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

/// Executes a Maven goal to run a JavaFX application.
/// This class extends the functionality of the base JavaFX Mojo, providing the capability
/// to execute a JavaFX application during a Maven build lifecycle. The goal is configured
/// to execute during the `process-classes` lifecycle phase and requires runtime dependency
/// resolution.
/// The class is responsible for validating configurations, constructing the execution
/// command, and launching the JavaFX application using the specified executable. It
/// ensures that the working directory is properly set and utilizes system environment
/// variables during process execution. The goal execution process is logged at various
/// stages to provide feedback on its operation.
/// Key operations include:
/// - Verifying mandatory configurations such as the JavaFX executable and base directory.
/// - Preparing the execution environment, including resolving required paths.
/// - Constructing the command for launching the JavaFX application.
/// - Launching and managing the execution of the process.
/// - Handling execution-related exceptions.
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
public final class JavaFXRunMojo extends BaseJavaFXMojo {

    /// Specifies the executable to be used for running the JavaFX application.
    /// This variable holds the name or path of the executable (e.g., "java").
    /// It is a required field and defaults to "java" if not explicitly configured.
    /// The value for this variable can be defined in the Maven build configuration
    /// using the property `javafx.executable`.
    /// This property is used in constructing the command for executing the JavaFX application.
    @Parameter(property = "javafx.executable", defaultValue = "java", required = true)
    String executable;

    /// Indicates whether debugging mode is enabled for the JavaFX application.
    /// This variable is a Maven parameter that can be configured using the `javafx.debug` property.
    /// When set to `true`, it is expected to enable detailed debugging information
    /// or related behavior to assist with diagnosing issues during application runtime.
    /// The default value is `false`, and this parameter is mandatory.
    @Parameter(property = "javafx.attachDebugger", defaultValue = "false")
    boolean attachDebugger;

    /// Represents the port used for debugging purposes when running a JavaFX application.
    /// This allows developers to attach a debugger to the application by connecting to
    /// the specified port. By default, the port is set to 5005 but can be overridden via
    /// the `javafx.debuggerPort` property.
    /// The debugger port is useful for diagnosing and resolving issues during the
    /// development and testing phases of JavaFX applications.
    /// Property: `javafx.debuggerPort`
    /// Default Value: `5005`
    @Parameter(property = "javafx.debuggerPort", defaultValue = "5005")
    int debuggerPort = 5005;

    /// Constructs a new instance of `JavaFXRunMojo`.
    ///
    /// @param locationManager  the `LocationManager` instance responsible for locating and managing paths and resources.
    /// @param toolchainManager the `ToolchainManager` instance responsible for managing and resolving toolchains.
    @Inject
    public JavaFXRunMojo(LocationManager locationManager, ToolchainManager toolchainManager) {
        super(locationManager, toolchainManager);
    }

    /// Executes the main logic for running a JavaFX application using Maven.
    /// This method performs several tasks:
    /// 1. Skips execution if the "skip" flag is set to true.
    /// 2. Validates that mandatory configurations, such as the JavaFX executable
    ///    and base directory, are properly defined. Throws a [MojoExecutionException]
    ///    if any required configuration is missing.
    /// 3. Initializes the environment and resolves necessary paths by calling the `init` method.
    /// 4. Configures the working directory using the `handleWorkingDirectory` method,
    ///    ensuring that the directory exists.
    /// 5. Constructs the command to execute the JavaFX application by invoking `getCommand`.
    /// 6. Starts the constructed command using a [ProcessBuilder], inheriting the current
    ///    I/O streams and configuring system environment variables.
    /// 7. Waits for the process to complete and handles any exceptions that may occur
    ///    during the execution.
    /// Log messages are generated to provide feedback throughout the execution process,
    /// detailing the status of key actions, configurations, and errors if they occur.
    ///
    /// @throws MojoExecutionException if there is an error during validation, initialization, handling of the working directory, or execution of the process.
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

    /// Generates and returns a command list for executing a JavaFX application.
    /// The command includes the executable, optional arguments, module path elements,
    /// classpath elements, the main class, and any additional command-line arguments.
    /// The method constructs the list by conditionally appending various components
    /// based on their availability and relevance. If optional arguments, module path
    /// elements, or command-line arguments are non-empty, they are processed and added
    /// accordingly.
    ///
    /// @return a list of strings representing the complete command to execute the JavaFX application.
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
