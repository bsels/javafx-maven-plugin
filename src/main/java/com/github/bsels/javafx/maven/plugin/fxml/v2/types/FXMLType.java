package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

/// Represents a type in FXML.
///
/// This is a sealed interface that can be either an [FXMLClassType] or an [FXMLGenericType].
public sealed interface FXMLType permits FXMLClassType, FXMLGenericType {
}
