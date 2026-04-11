package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.Objects;
import java.util.Optional;

/// A property passed to a constructor or factory method.
///
/// Stores information about the property's name, its [FXMLType], and an optional default value
/// if one is specified via the [@NamedArg] annotation.
///
/// @param name         The name of the property
/// @param type         The [FXMLType] of the property
/// @param defaultValue An [Optional] containing the [FXMLLiteral] default value, if present
public record ConstructorProperty(String name, FXMLType type, Optional<FXMLLiteral> defaultValue) {

    /// Initializes a new [ConstructorProperty] instance.
    ///
    /// @param name         The name of the property
    /// @param type         The [FXMLType] of the property
    /// @param defaultValue An [Optional] containing the [FXMLLiteral] default value
    /// @throws NullPointerException If any parameter is null
    public ConstructorProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(defaultValue, "`defaultValue` must not be null");
    }
}
