package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

/// Represents a type in FXML.
///
/// This is a sealed interface that can be either an [FXMLClassType], an [FXMLGenericType],
/// an [FXMLUncompiledClassType], an [FXMLUncompiledGenericType], or an [FXMLWildCardType].
public sealed interface FXMLType
        permits FXMLClassType, FXMLGenericType, FXMLUncompiledClassType, FXMLUncompiledGenericType, FXMLWildCardType {
}
