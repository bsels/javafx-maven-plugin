package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;
import java.util.Optional;

/// Represents a property of an FXML object.
///
/// @param type       The FXML type of the property.
/// @param name       The name of the property.
/// @param methodName The optional method name associated with the property.
/// @param methodType The type of method (getter, setter, or constructor).
record ObjectProperty(FXMLType type, String name, Optional<String> methodName, MethodType methodType) {
    /// Compact constructor for [ObjectProperty].
    ///
    /// The logic ensures that all components are not `null`.
    ///
    /// @param type       The FXML type of the property.
    /// @param name       The name of the property.
    /// @param methodName The optional method name.
    /// @param methodType The type of method.
    /// @throws NullPointerException if any parameter is `null`.
    ObjectProperty {
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(methodName, "`methodName` must not be null");
        Objects.requireNonNull(methodType, "`methodType` must not be null");
    }

    /// Enumerates the types of methods associated with an object property.
    enum MethodType {
        /// A getter method.
        GETTER,
        /// A setter method.
        SETTER,
        /// A constructor parameter.
        CONSTRUCTOR
    }
}
