package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Objects;

/// Grouping of class counts and their associated occurrences.
///
/// @param group   The name of the group.
/// @param count   The total number of occurrences associated with the group.
/// @param classes A list of [ClassCount] instances representing the individual class occurrences.
public record GroupedClassCount(String group, int count, List<ClassCount> classes) {

    /// Initializes a new [GroupedClassCount] record instance.
    ///
    /// @param group   The name of the group.
    /// @param count   The total number of occurrences associated with the group.
    /// @param classes A list of [ClassCount] instances representing the class occurrences.
    /// @throws NullPointerException     If `group` or `classes` is null.
    /// @throws IllegalArgumentException If `count` is negative.
    public GroupedClassCount {
        Objects.requireNonNull(group);
        Objects.requireNonNull(classes);
        classes = List.copyOf(classes);
        if (count < 0) {
            throw new IllegalArgumentException("`count` must be non-negative");
        }
    }
}
