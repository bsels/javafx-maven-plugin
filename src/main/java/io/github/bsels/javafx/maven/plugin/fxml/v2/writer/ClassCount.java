package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.Objects;

/// Consolidates data about how many times a specific class name is encountered during processing.
///
/// @param fullClassName The fully qualified name of the class.
/// @param count         The number of occurrences for the specified class name.
record ClassCount(String fullClassName, int count) {

    /// Initializes a new [ClassCount] record instance.
    ///
    /// @param fullClassName The full class name associated with this [ClassCount].
    /// @param count         The occurrence count of the specified class name.
    /// @throws NullPointerException     If `fullClassName` is null.
    /// @throws IllegalArgumentException If `count` is negative.
    ClassCount {
        Objects.requireNonNull(fullClassName, "`fullClassName` must not be null");
        if (count <= 0) {
            throw new IllegalArgumentException("`count` must be non-negative");
        }
    }
}
