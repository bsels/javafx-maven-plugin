package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

/// Base interface for FXML properties.
public sealed interface FXMLProperty
        permits FXMLCollectionProperties, FXMLConstructorProperty, FXMLMapProperty, FXMLObjectProperty,
                FXMLStaticObjectProperty {
    /// Returns the name of the property.
    ///
    /// @return The property name.
    String name();

    /// Returns the type of the property.
    ///
    /// @return The property type.
    FXMLType type();
}
