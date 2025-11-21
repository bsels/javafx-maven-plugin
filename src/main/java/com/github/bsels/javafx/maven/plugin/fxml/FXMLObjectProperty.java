package com.github.bsels.javafx.maven.plugin.fxml;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents an object property in the context of FXML processing.
///
/// This record encapsulates details about an object's property, including its name, setter method name, type, and value.
/// It ensures immutability and guarantees that all provided values are non-null.
///
/// The [FXMLObjectProperty] class is part of an FXML model and implements the [FXMLProperty] interface,
/// indicating its role as a specific type of FXML property.
///
/// Constructor details:
/// - The `name` represents the identifier of the property.
/// - The `setter` refers to the name of the setter method associated with the property.
/// - The `type` specifies the data type of the property.
/// - The `value` defines the actual value held by the property.
///
/// All parameters in the constructor are validated to be non-null.
public record FXMLObjectProperty(String name, String setter, Type type, String value) implements FXMLProperty {

    /// Constructs an instance of [FXMLObjectProperty], representing an object property in an FXML-related context.
    /// Ensures that all provided values are not null.
    ///
    /// @param name the name of the property; must not be null
    /// @param setter the setter method name associated with the property; must not be null
    /// @param type the type of the property; must not be null
    /// @param value the value of the property; must not be null
    /// @throws NullPointerException if any of the parameters (name, setter, type, or value) is null
    public FXMLObjectProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
