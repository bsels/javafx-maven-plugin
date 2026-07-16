package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;

/// A property passed to a constructor via a `@NamedArg` annotation that has an array of values.
///
/// @param name        The property name
/// @param type        The property type
/// @param elementType The type of the elements in the array
/// @param value       The list of values in the array
public record FXMLConstructorArrayProperty(
        String name,
        FXMLType type,
        FXMLType elementType,
        List<AbstractFXMLValue> value
) implements FXMLConstructorValueProperty {

    /// Initializes a new [FXMLConstructorArrayProperty] record instance.
    ///
    /// @param name        The property name
    /// @param type        The property type
    /// @param elementType The type of the elements in the array
    /// @param value       The list of values in the array
    /// @throws NullPointerException If `name`, `type`, or `elementType` is null
    public FXMLConstructorArrayProperty {
        Objects.requireNonNull(name, "`name` cannot be null");
        Objects.requireNonNull(type, "`type` cannot be null");
        Objects.requireNonNull(elementType, "`elementType` cannot be null");
        value = List.copyOf(Objects.requireNonNullElseGet(value, List::of));
    }
}
