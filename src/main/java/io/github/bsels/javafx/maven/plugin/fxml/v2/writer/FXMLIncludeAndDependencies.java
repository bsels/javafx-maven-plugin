package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;

import java.util.List;
import java.util.Objects;

/// Represents an FXML include construction and its dependencies.
///
/// @param include      The [FXMLInclude] being constructed.
/// @param dependencies The identifiers this `include` depends on.
record FXMLIncludeAndDependencies(
        FXMLInclude include,
        List<FXMLIdentifier> dependencies
) implements Constructions {

    /// Validates the include and initializes the dependency list.
    ///
    /// @param include      The [FXMLInclude] being constructed.
    /// @param dependencies The identifiers this `include` depends on.
    FXMLIncludeAndDependencies {
        Objects.requireNonNull(include, "include");
        dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
    }
}
