package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.Objects;

/// Represents an immutable pair consisting of a fully qualified class name and its associated occurrence count.
///
/// This record is used to consolidate and represent data about how many times a specific class name is encountered
/// during processing within the application.
/// It ensures that the class name cannot be null upon creation.
///
/// @param fullClassName The fully qualified name of the class. Must not be null.
/// @param count         The number of occurrences for the specified class name.
record ClassCount(String fullClassName, int count) {

    /// Constructs a new `ClassCount` record instance.
    /// Ensures the specified class name is not null and the count is non-negative.
    ///
    /// @param fullClassName The full class name associated with this [ClassCount]. Must not be null.
    /// @param count         The occurrence count of the specified class name. Represents how many times this class name is encountered.
    /// @throws NullPointerException     If `fullClassName` is null.
    /// @throws IllegalArgumentException If `count` is negative.
    ClassCount {
        Objects.requireNonNull(fullClassName, "`fullClassName` must not be null");
        if (count <= 0) {
            throw new IllegalArgumentException("`count` must be non-negative");
        }
    }
}
