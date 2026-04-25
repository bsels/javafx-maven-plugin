package io.github.bsels.javafx.maven.plugin.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/// Represents an operation that accepts a single input argument and returns a result,
/// potentially throwing [MojoExecutionException] or [MojoFailureException].
///
/// This is a functional interface designed for use in contexts where operations may produce checked exceptions.
/// It is primarily intended to facilitate operations that deal with exceptions encountered during Maven plugin
/// execution by abstracting the exception-handling logic.
///
/// @param <T> the type of the input and output of the operation
public interface ThrowableUnaryOperator<T> {
    /// Applies the operation defined by this method to the given input.
    ///
    /// @param t the input value to be processed
    /// @return the result of applying the operation to the input value
    /// @throws MojoExecutionException if an error occurs during the execution of the operation
    /// @throws MojoFailureException   if the operation fails in a recoverable manner
    T apply(T t) throws MojoExecutionException, MojoFailureException;
}
