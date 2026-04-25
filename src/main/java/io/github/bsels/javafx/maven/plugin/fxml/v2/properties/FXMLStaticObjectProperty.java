package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.Objects;

/// A static (attached) FXML property that has a single value.
///
/// @param name   The property name
/// @param clazz  The class defining the static property
/// @param setter The name of the static setter method
/// @param type   The property type
/// @param value  The property value
public record FXMLStaticObjectProperty(
        String name,
        FXMLClassType clazz,
        String setter,
        FXMLType type,
        AbstractFXMLValue value
) implements FXMLProperty {

    /// Initializes a new [FXMLStaticObjectProperty] record instance.
    ///
    /// @param name   The property name
    /// @param clazz  The class defining the static property
    /// @param setter The name of the static setter method
    /// @param type   The property type
    /// @param value  The property value
    /// @throws NullPointerException If any parameter is null
    public FXMLStaticObjectProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
