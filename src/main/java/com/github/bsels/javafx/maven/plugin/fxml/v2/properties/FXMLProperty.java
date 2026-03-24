package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

/// Base interface for FXML properties.
///
/// @param <T> The value type of the property.
public sealed interface FXMLProperty<T>
        permits FXMLCollectionProperties, FXMLConstructorProperty, FXMLMapProperty, FXMLObjectProperty, FXMLStaticObjectProperty {
    /// Returns the name of the property.
    ///
    /// @return The property name.
    String name();

    /// Returns the type of the property.
    ///
    /// @return The property type.
    FXMLType type();

    /// Returns the value(s) of the property.
    ///
    /// @return The property value.
    T value();
}
