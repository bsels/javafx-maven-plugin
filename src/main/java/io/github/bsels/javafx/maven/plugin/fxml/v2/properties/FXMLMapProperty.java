package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.Map;
import java.util.Objects;

/// Represents an FXML property that can have multiple values (a map).
///
/// @param name          The property name.
/// @param getter        The name of the getter method that returns the map.
/// @param type          The property type.
/// @param rawKeyClass   The raw key class.
/// @param rawValueClass The raw value class.
/// @param value         The map of values.
public record FXMLMapProperty(
        String name,
        String getter,
        FXMLType type,
        FXMLClassType rawKeyClass,
        FXMLClassType rawValueClass,
        Map<FXMLLiteral, AbstractFXMLValue> value
) implements FXMLProperty {

    /// Compact constructor to validate the property components.
    ///
    /// @param name          The property name.
    /// @param getter        The name of the getter method.
    /// @param type          The property type.
    /// @param rawKeyClass   The raw key class.
    /// @param rawValueClass The raw value class.
    /// @param value         The map of values.
    /// @throws NullPointerException if `name`, `getter`, `type`, `rawKeyClass`, or `rawValueClass` is `null`.
    public FXMLMapProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(rawKeyClass, "`rawKeyClass` must not be null");
        Objects.requireNonNull(rawValueClass, "`rawValueClass` must not be null");
        value = Map.copyOf(Objects.requireNonNullElseGet(value, Map::of));
    }
}
