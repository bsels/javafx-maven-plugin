package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Objects;

/// Represents an immutable grouping of class counts and their associated occurrences.
///
/// This record consolidates data pertaining to a group and its associated class counts,
/// ensuring immutability and enforcing constraints on the group name, count, and class list.
///
/// @param group   The name of the group. Must not be null.
/// @param count   The total number of occurrences associated with the group. Must be non-negative.
/// @param classes A list of [ClassCount] instances representing the individual class occurrences in this group. Must not be null.
public record GroupedClassCount(String group, int count, List<ClassCount> classes) {

    /// Constructs a new `GroupedClassCount` record instance.
    /// Ensures the specified group name and classes list are not null, and the count is non-negative.
    /// The classes list is defensively copied to maintain immutability.
    ///
    /// @param group   The name of the group. Must not be null.
    /// @param count   The total number of occurrences associated with the group. Must be non-negative.
    /// @param classes A list of [ClassCount] instances representing the class occurrences in this group. Must not be null.
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
