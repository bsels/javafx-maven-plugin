package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

/// Represents an FXML property that can have multiple values (a map).
///
/// @param name   The property name.
/// @param getter The name of the getter method that returns the map.
/// @param type   The property type.
/// @param value  The map of values.
public record FXMLMapProperty(
        String name,
        String getter,
        Type type,
        Map<String, AbstractFXMLValue> value
) implements FXMLProperty<Map<String, AbstractFXMLValue>> {

    /// Compact constructor to validate the property components.
    ///
    /// @param name   The property name.
    /// @param getter The name of the getter method.
    /// @param type   The property type.
    /// @param value  The map of values.
    /// @throws NullPointerException if `name`, `getter`, or `type` is `null`.
    public FXMLMapProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        value = Map.copyOf(Objects.requireNonNullElseGet(value, Map::of));
    }
}
