package io.github.bsels.javafx.maven.plugin.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unit tests for the `ContextClassLoaderClosable` class.
///
/// Verifies that the context [ClassLoader] is temporarily replaced and correctly restored.
class ContextClassLoaderClosableTest {

    /// Verifies that the constructor sets the context [ClassLoader] to the one returned by the extender,
    /// and that `close()` restores the original [ClassLoader].
    @Test
    void shouldReplaceAndRestoreContextClassLoader() throws MojoExecutionException, MojoFailureException {
        // Given
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader replacement = new ClassLoader(original) {};

        // When
        try (ContextClassLoaderClosable ignored = new ContextClassLoaderClosable(_ -> replacement)) {
            assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(replacement);
        }

        // Then
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    /// Verifies that a [MojoExecutionException] thrown by the extender propagates from the constructor.
    @Test
    void shouldPropagatesMojoExecutionException() {
        // Given
        MojoExecutionException exception = new MojoExecutionException("execution error");

        // When & Then
        assertThatThrownBy(() -> new ContextClassLoaderClosable(_ -> { throw exception; }))
                .isSameAs(exception);
    }

    /// Verifies that a [MojoFailureException] thrown by the extender propagates from the constructor.
    @Test
    void shouldPropagatesMojoFailureException() {
        // Given
        MojoFailureException exception = new MojoFailureException("failure error");

        // When & Then
        assertThatThrownBy(() -> new ContextClassLoaderClosable(_ -> { throw exception; }))
                .isSameAs(exception);
    }
}
