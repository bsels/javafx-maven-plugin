package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML property that has a single value.
///
/// @param name   The property name.
/// @param setter The name of the setter method, if any.
/// @param type   The property type.
/// @param value  The property value.
public record FXMLSingleProperty(
        String name,
        Optional<String> setter,
        Type type,
        AbstractFXMLValue value
) implements FXMLObjectProperty<AbstractFXMLValue> {

    /// Compact constructor to validate the property components.
    ///
    /// @param name   The property name.
    /// @param setter The name of the setter method, if any.
    /// @param type   The property type.
    /// @param value  The property value.
    /// @throws NullPointerException if `name`, `setter`, `type`, or `value` is `null`.
    public FXMLSingleProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
