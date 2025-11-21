package com.github.bsels.javafx.maven.plugin.fxml;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a static property associated with an FXML element.
///
/// This class is an implementation of the `FXMLProperty` interface.
/// It is used to define a static property associated with a specific static class and a setter method within that class.
/// It encapsulates metadata about the property, including its name, the associated static class,
/// the setter method name, the property type, and its value.
///
/// Instances of this record are immutable, ensuring that the encapsulated metadata remains consistent throughout its
/// lifecycle.
///
/// Validations ensure all provided values are non-null.
public record FXMLStaticProperty(String name, Class<?> staticClass, String staticSetter, Type type, String value)
        implements FXMLProperty {

    /// Constructs an immutable [FXMLStaticProperty] record object, ensuring all provided values are not null.
    ///
    /// @param name the name of the static property; must not be null
    /// @param staticClass the class containing the static property definition; must not be null
    /// @param staticSetter the name of the setter method for the static property; must not be null
    /// @param type the type of the static property; must not be null
    /// @param value the value of the static property; must not be null
    /// @throws NullPointerException if any parameter (name, staticClass, staticSetter, type, or value) is null
    public FXMLStaticProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(staticClass, "`staticClass` must not be null");
        Objects.requireNonNull(staticSetter, "`staticSetter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
