package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.Objects;

/// An FXML property that has a single value.
///
/// @param name   The property name
/// @param setter The name of the setter method
/// @param type   The property type
/// @param value  The property value
public record FXMLObjectProperty(
        String name,
        String setter,
        FXMLType type,
        AbstractFXMLValue value
) implements FXMLProperty {

    /// Initializes a new [FXMLObjectProperty] record instance.
    ///
    /// @param name   The property name
    /// @param setter The name of the setter method
    /// @param type   The property type
    /// @param value  The property value
    /// @throws NullPointerException If `name`, `setter`, `type`, or `value` is null
    public FXMLObjectProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
