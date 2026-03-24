package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.Objects;

/// Represents an FXML property that has a single value.
///
/// @param name   The property name.
/// @param setter The name of the getter method, if any.
/// @param type   The property type.
/// @param value  The property value.
public record FXMLObjectProperty(
        String name,
        String setter,
        FXMLType type,
        AbstractFXMLValue value
) implements FXMLProperty<AbstractFXMLValue> {

    /// Compact constructor to validate the property components.
    ///
    /// @param name   The property name.
    /// @param setter The name of the getter method, if any.
    /// @param type   The property type.
    /// @param value  The property value.
    /// @throws NullPointerException if `name`, `getter`, `type`, or `value` is `null`.
    public FXMLObjectProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
