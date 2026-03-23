package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML property that can have multiple values (a list).
///
/// @param name   The property name.
/// @param setter The name of the setter method, if any.
/// @param type   The property type.
/// @param value  The list of values.
public record FXMLMultipleProperties(
        String name,
        Optional<String> setter,
        Type type,
        List<AbstractFXMLValue> value
) implements FXMLObjectProperty<List<AbstractFXMLValue>> {

    /// Compact constructor to validate the property components.
    ///
    /// @param name   The property name.
    /// @param setter The name of the setter method, if any.
    /// @param type   The property type.
    /// @param value  The list of values.
    /// @throws NullPointerException if `name`, `setter`, `type`, or `value` is `null`.
    public FXMLMultipleProperties {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
