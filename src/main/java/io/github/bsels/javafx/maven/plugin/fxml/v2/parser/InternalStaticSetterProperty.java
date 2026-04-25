package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// An internal static setter property.
///
/// @param name        The name of the property
/// @param staticClass The static class containing the setter
/// @param setter      The name of the setter method
/// @param fxmlType    The FXML type of the property
record InternalStaticSetterProperty(String name, Class<?> staticClass, String setter, FXMLType fxmlType) {

    /// Initializes a new [InternalStaticSetterProperty] record instance.
    ///
    /// @param name        The name of the property
    /// @param staticClass The static class containing the setter
    /// @param setter      The name of the setter method
    /// @param fxmlType    The FXML type of the property
    /// @throws NullPointerException If any parameter is null
    InternalStaticSetterProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(staticClass, "`staticClass` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(fxmlType, "`fxmlType` must not be null");
    }
}
