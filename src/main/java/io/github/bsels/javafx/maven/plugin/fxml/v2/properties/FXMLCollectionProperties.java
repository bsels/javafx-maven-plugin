package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;

/// Represents an FXML property that can have multiple values (a list).
///
/// @param name       The property name.
/// @param getter     The name of the getter method that returns the collection.
/// @param type       The property type.
/// @param value      The list of values.
/// @param properties The list of properties.
public record FXMLCollectionProperties(
        String name,
        String getter,
        FXMLType type,
        List<AbstractFXMLValue> value,
        List<FXMLProperty> properties
) implements FXMLProperty {

    /// Compact constructor to validate the property components.
    ///
    /// @param name       The property name.
    /// @param getter     The name of the getter method.
    /// @param type       The property type.
    /// @param value      The list of values.
    /// @param properties The list of properties.
    /// @throws NullPointerException if `name`, `getter`, `type`, or `value` is `null`.
    public FXMLCollectionProperties {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        value = List.copyOf(Objects.requireNonNullElseGet(value, List::of));
        properties = List.copyOf(Objects.requireNonNullElseGet(properties, List::of));
    }
}