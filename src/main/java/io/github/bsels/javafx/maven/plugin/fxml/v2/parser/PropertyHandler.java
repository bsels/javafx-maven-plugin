package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;

import java.util.Optional;

/// Functional interface for property parsing logic.
///
/// @param <T> The type of the value to handle
@FunctionalInterface
interface PropertyHandler<T> {
    /// Applies the property handling logic.
    ///
    /// @param buildContext The build context
    /// @param property     The object property
    /// @param value        The value to apply
    /// @return An [Optional] containing the [FXMLProperty] if successful
    Optional<FXMLProperty> apply(BuildContext buildContext, ObjectProperty property, T value);
}
