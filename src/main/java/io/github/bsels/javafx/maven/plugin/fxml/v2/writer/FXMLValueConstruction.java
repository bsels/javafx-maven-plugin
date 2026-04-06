package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;

import java.util.List;
import java.util.Objects;

/// Represents an FXML value construction and its dependencies.
///
/// @param value        The [FXMLValue] being constructed.
/// @param dependencies The identifiers this value construction depends on.
record FXMLValueConstruction(
        FXMLValue value,
        List<FXMLIdentifier> dependencies
) implements Constructions {

    /// Validates the value and initializes the dependency list.
    ///
    /// @param value        The [FXMLValue] being constructed.
    /// @param dependencies The identifiers this value construction depends on.
    FXMLValueConstruction {
        Objects.requireNonNull(value, "value");
        dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
    }
}
