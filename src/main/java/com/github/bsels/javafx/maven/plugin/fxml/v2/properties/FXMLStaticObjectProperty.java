package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a static (attached) FXML property that has a single value.
///
/// @param name   The property name.
/// @param clazz  The class defining the static property.
/// @param setter The name of the static setter method.
/// @param type   The property type.
/// @param value  The property value.
public record FXMLStaticObjectProperty(
        String name,
        Class<?> clazz,
        String setter,
        Type type,
        AbstractFXMLValue value
) implements FXMLProperty<AbstractFXMLValue> {

    /// Compact constructor to validate the static property components.
    ///
    /// @param name   The property name.
    /// @param clazz  The class defining the static property.
    /// @param setter The name of the static setter method.
    /// @param type   The property type.
    /// @param value  The property value.
    /// @throws NullPointerException if `name`, `clazz`, `setter`, `type`, or `value` is `null`.
    public FXMLStaticObjectProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
