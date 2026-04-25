package io.github.bsels.javafx.maven.plugin.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/// Represents a functional interface designed for operations that can throw [MojoExecutionException]
/// or [MojoFailureException].
///
/// This interface is primarily used to encapsulate code blocks that require exception handling for Maven plugin
/// execution logic.
/// Implementations of this interface are expected to define the behavior within the `run` method,
/// which may involve operations such as I/O, configuration validation,
/// or other tasks that might encounter exceptional conditions specific to Maven plugin execution.
///
/// Typical use cases for this interface include:
/// - Delegating execution of logic that requires controlled exception handling.
/// - Encapsulating operations with potential failure scenarios encountered during the execution of a Maven plugin.
public interface ThrowableRunner {
    /// Executes a specific operation that may involve Maven plugin logic.
    /// This method is the entry point for executing tasks that can potentially throw exceptions related
    /// to execution failures or Maven-specific errors.
    ///
    /// @throws MojoExecutionException if an error occurs during the execution process
    /// @throws MojoFailureException   if a failure condition specific to Maven is encountered
    void run() throws MojoExecutionException, MojoFailureException;
}
