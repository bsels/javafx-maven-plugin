package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.Objects;
import java.util.Optional;

/// Represents a property that is passed to a constructor or factory method.
///
/// This record stores information about the property's name, its [FXMLType], and an optional default value
/// if one is specified via the [@NamedArg] annotation.
///
/// @param name         The name of the property. Must not be null.
/// @param type         The [FXMLType] of the property. Must not be null.
/// @param defaultValue An [Optional] containing the [FXMLLiteral] default value, if present. Must not be null.
public record ConstructorProperty(String name, FXMLType type, Optional<FXMLLiteral> defaultValue) {

    /// Constructs a new `ConstructorProperty` and ensures all parameters are non-null.
    ///
    /// @param name         The name of the property.
    /// @param type         The [FXMLType] of the property.
    /// @param defaultValue An [Optional] containing the [FXMLLiteral] default value.
    /// @throws NullPointerException If any of the parameters are null.
    public ConstructorProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(defaultValue, "`defaultValue` must not be null");
    }
}
