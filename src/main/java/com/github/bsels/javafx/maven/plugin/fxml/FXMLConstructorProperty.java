package com.github.bsels.javafx.maven.plugin.fxml;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a property defined in the constructor of an FXML element.
/// This property specifies a key-value pair along with the type of the property.
/// It implements the [FXMLProperty] interface, allowing it to describe properties that can be used within the FXML
/// context.
public record FXMLConstructorProperty(String name, String value, Type type) implements FXMLProperty {

    /// Constructs an instance of [FXMLConstructorProperty] with the specified name, value, and type.
    ///
    /// @param name the name of the property must not be null
    /// @param value the value of the property must not be null
    /// @param type the type of the property must not be null
    /// @throws NullPointerException if any of the provided arguments is null
    public FXMLConstructorProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
    }
}
