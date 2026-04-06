package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;

import java.util.List;
import java.util.Objects;

/// Represents an FXML copy construction and its dependencies.
///
/// @param copy         The [FXMLCopy] being constructed.
/// @param dependencies The identifiers this copy depends on.
record FXMLCopyAndDependencies(
        FXMLCopy copy,
        List<FXMLIdentifier> dependencies
) implements Constructions {

    /// Validates the copy and initializes the dependency list.
    ///
    /// @param copy         The [FXMLCopy] being constructed.
    /// @param dependencies The identifiers this copy depends on.
    FXMLCopyAndDependencies {
        Objects.requireNonNull(copy, "copy");
        dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
    }
}
