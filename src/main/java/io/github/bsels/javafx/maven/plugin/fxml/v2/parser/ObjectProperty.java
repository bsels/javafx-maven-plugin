package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;
import java.util.Optional;

/// A property of an FXML object.
///
/// @param type       The FXML type of the property
/// @param name       The name of the property
/// @param methodName The optional method name associated with the property
/// @param methodType The type of method (getter, setter, or constructor)
record ObjectProperty(FXMLType type, String name, Optional<String> methodName, MethodType methodType) {

    /// Initializes a new [ObjectProperty] record instance.
    ///
    /// @param type       The FXML type of the property
    /// @param name       The name of the property
    /// @param methodName The optional method name
    /// @param methodType The type of method
    /// @throws NullPointerException If any parameter is null
    ObjectProperty {
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(methodName, "`methodName` must not be null");
        Objects.requireNonNull(methodType, "`methodType` must not be null");
    }

    /// The types of methods associated with an object property.
    enum MethodType {
        /// Getter method.
        GETTER,
        /// Setter method.
        SETTER,
        /// Constructor parameter.
        CONSTRUCTOR
    }
}
