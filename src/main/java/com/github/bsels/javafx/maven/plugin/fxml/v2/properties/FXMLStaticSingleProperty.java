package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/// Represents a static (attached) FXML property that has a single value.
///
/// @param name         The property name.
/// @param clazz        The class defining the static property.
/// @param staticSetter The name of the static setter method.
/// @param type         The property type.
/// @param value        The property value.
public record FXMLStaticSingleProperty(
        String name,
        Class<?> clazz,
        String staticSetter,
        Type type,
        AbstractFXMLValue value
) implements FXMLStaticProperty<AbstractFXMLValue> {

    /// Compact constructor to validate the static property components.
    ///
    /// @param name         The property name.
    /// @param clazz        The class defining the static property.
    /// @param staticSetter The name of the static setter method.
    /// @param type         The property type.
    /// @param value        The property value.
    /// @throws NullPointerException if any required parameter is null.
    public FXMLStaticSingleProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(staticSetter, "`staticSetter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }

    /// Returns the class defining the static property.
    ///
    /// @return The static property class.
    @Override
    public Class<?> staticClass() {
        return clazz;
    }
}
