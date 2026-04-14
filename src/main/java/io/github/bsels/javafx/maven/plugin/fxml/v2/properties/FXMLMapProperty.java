package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.Map;
import java.util.Objects;

/// An FXML property that can have multiple values (a map).
///
/// @param name          The property name
/// @param getter        The name of the getter method that returns the map
/// @param type        The property type
/// @param keyType     The key type
/// @param valueType   The value type
/// @param value       The map of values
public record FXMLMapProperty(
        String name,
        String getter,
        FXMLType type,
        FXMLType keyType,
        FXMLType valueType,
        Map<FXMLLiteral, AbstractFXMLValue> value
) implements FXMLProperty {

    /// Initializes a new [FXMLMapProperty] record instance.
    ///
    /// @param name        The property name
    /// @param getter      The name of the getter method
    /// @param type        The property type
    /// @param keyType     The key type
    /// @param valueType   The value type
    /// @param value       The map of values
    /// @throws NullPointerException If `name`, `getter`, `type`, `keyType`, or `valueType` is null
    public FXMLMapProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(keyType, "`keyType` must not be null");
        Objects.requireNonNull(valueType, "`valueType` must not be null");
        value = Map.copyOf(Objects.requireNonNullElseGet(value, Map::of));
    }
}
