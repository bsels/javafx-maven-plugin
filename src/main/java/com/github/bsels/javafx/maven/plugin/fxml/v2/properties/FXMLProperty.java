package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.lang.reflect.Type;

/// Base interface for FXML properties.
///
/// @param <T> The value type of the property.
public sealed interface FXMLProperty<T>
        permits FXMLCollectionProperties, FXMLMapProperty, FXMLObjectProperty, FXMLStaticObjectProperty {
    /// Returns the name of the property.
    ///
    /// @return The property name.
    String name();

    /// Returns the type of the property.
    ///
    /// @return The property type.
    Type type();

    /// Returns the value(s) of the property.
    ///
    /// @return The property value.
    T value();
}
