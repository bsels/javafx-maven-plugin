package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;

/// An FXML property that has an array of values.
///
/// @param name        The property name
/// @param setter      The name of the setter method
/// @param type        The property type
/// @param elementType The type of the elements in the array
/// @param value       The list of values in the array
public record FXMLArrayProperty(
        String name,
        String setter,
        FXMLType type,
        FXMLType elementType,
        List<AbstractFXMLValue> value
) implements FXMLProperty {

    /// Initializes a new [FXMLArrayProperty] record instance.
    ///
    /// @param name        The property name
    /// @param setter      The name of the setter method
    /// @param type        The property type
    /// @param elementType The type of the elements in the array
    /// @param value       The list of values in the array
    /// @throws NullPointerException If `name`, `setter`, `type`, or `elementType` is null
    public FXMLArrayProperty {
        Objects.requireNonNull(name, "`name` cannot be null");
        Objects.requireNonNull(setter, "`setter` cannot be null");
        Objects.requireNonNull(type, "`type` cannot be null");
        Objects.requireNonNull(elementType, "`elementType` cannot be null");
        value = List.copyOf(Objects.requireNonNullElseGet(value, List::of));
    }
}
