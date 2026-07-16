package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

/// Base interface for FXML properties that can be passed to a constructor.
public sealed interface FXMLConstructorValueProperty extends FXMLProperty
        permits FXMLConstructorArrayProperty, FXMLConstructorProperty {
}
