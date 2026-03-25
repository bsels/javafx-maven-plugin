package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.Objects;

/// A property that is passed to a constructor via a `@NamedArg` annotation.
///
/// @param name  The name of the property as specified in the `@NamedArg` annotation.
/// @param type  The type of the property.
/// @param value The value of the property.
public record FXMLConstructorProperty(
        String name,
        FXMLType type,
        AbstractFXMLValue value
) implements FXMLProperty {

    /// Creates a new `FXMLConstructorProperty` instance.
    ///
    /// @param name  The name of the property as specified in the `@NamedArg` annotation.
    /// @param type  The type of the property.
    /// @param value The value of the property.
    /// @throws NullPointerException if `name`, `type`, or `value` is null.
    public FXMLConstructorProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
