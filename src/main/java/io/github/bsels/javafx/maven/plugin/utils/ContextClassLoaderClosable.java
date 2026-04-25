package io.github.bsels.javafx.maven.plugin.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/// Handles the temporary modification of a thread's context [ClassLoader] and ensures that it is restored
/// to its original state when the operation is complete.
/// This class implements [AutoCloseable],
/// allowing its usage in try-with-resources blocks for proper resource management.
///
/// The primary purpose of this class is to facilitate safe and temporary changes to the context [ClassLoader]
/// of the current thread,
/// which is critical in scenarios involving custom class loading or execution within modified runtime environments.
public class ContextClassLoaderClosable implements AutoCloseable {
    /// Represents the thread associated with the current instance.
    /// This field is initialized with the thread that is active at the time of the enclosing object's creation.
    ///
    /// It is used to capture and operate on the original context of the thread, such as its [ClassLoader].
    /// The thread context can be temporarily modified and later restored to its original state,
    /// ensuring that the changes do not affect other operations or threads.
    ///
    /// This field is immutable and should not be modified directly to maintain thread-safety and consistency.
    private final Thread thread;
    /// Represents the original context [ClassLoader] of a thread, which is stored for later restoration.
    /// This field is immutable and is initialized at the time of object creation with the current thread's context
    /// [ClassLoader].
    /// It is used to manage temporary changes to the context [ClassLoader] during the lifetime of the enclosing
    /// object.
    ///
    /// The `classLoader` is critical for ensuring that the thread's context [ClassLoader] is reverted to its
    /// original state after modifications, preventing potential conflicts or issues related to class loading in
    /// multithreaded or complex runtime environments.
    private final ClassLoader classLoader;

    /// Constructs a new `ContextClassLoaderClosable`, temporarily replacing the context [ClassLoader]
    /// of the current thread with the one obtained by applying the given `classLoaderExtender`.
    ///
    /// @param classLoaderExtender a functional interface that takes the current thread's context [ClassLoader] as input and returns a modified [ClassLoader] to be temporarily set. The provided operation may throw a [MojoExecutionException] or [MojoFailureException].
    /// @throws MojoExecutionException if there is an error during the execution of the `classLoaderExtender`.
    /// @throws MojoFailureException   if the `classLoaderExtender` fails in a recoverable manner.
    public ContextClassLoaderClosable(ThrowableUnaryOperator<ClassLoader> classLoaderExtender)
            throws MojoExecutionException, MojoFailureException {
        Thread thread = Thread.currentThread();
        classLoader = thread.getContextClassLoader();
        this.thread = thread;
        super();
        thread.setContextClassLoader(classLoaderExtender.apply(classLoader));
    }

    /// Restores the original context [ClassLoader] of the thread.
    /// This method sets the thread's context [ClassLoader] back to the [ClassLoader] that was active when
    /// the enclosing object was created.
    ///
    /// It ensures that the thread's context [ClassLoader] is reverted to its original state,
    /// preventing potential [ClassLoader]-related issues after the custom [ClassLoader] modifications are applied
    /// during the object's lifetime.
    ///
    /// This method complies with the [AutoCloseable#close()] contract, providing a mechanism to clean up resources
    /// or revert changes when the object is no longer needed.
    @Override
    public void close() {
        thread.setContextClassLoader(classLoader);
    }
}
