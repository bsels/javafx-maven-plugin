package com.github.bsels.javafx.maven.plugin.utils;

import java.io.Serial;

/// The [InternalClassNotFoundException] is a specialized [RuntimeException] that indicates
/// the inability to locate a specific internal class during runtime. This exception
/// is used in scenarios where dynamic class loading or reflection fails to find
/// the desired internal class, typically due to issues such as incorrect class
/// paths, missing dependencies, or attempting to load classes not accessible
/// within the current runtime context.
///
/// This exception provides a mechanism to report such errors with a specific
/// detail message that describes the root cause or context of the class-loading failure.
public final class InternalClassNotFoundException extends RuntimeException {
    /// A unique identifier used during serialization and deserialization to verify
    /// that the sender and receiver of a serialized object maintain compatible classes.
    /// The value must remain consistent between versions of the class to ensure
    /// serialized objects are properly deserialized. If no such identifier is defined,
    /// the JVM will generate one automatically based on the class's details, which
    /// might lead to incompatibilities when the class evolves.
    @Serial
    private static final long serialVersionUID = 1L;

    /// Constructs a new InternalClassNotFoundException with the specified detail message.
    ///
    /// @param message the detail message describing the reason for the exception
    public InternalClassNotFoundException(String message) {
        super(message);
    }
}
