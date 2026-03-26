package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

/// Base interface for FXML identifiers.
public sealed interface FXMLIdentifier
        permits FXMLExposedIdentifier, FXMLInternalIdentifier, FXMLNamedRootIdentifier, FXMLRootIdentifier {

    /// Returns a string representation of the FXML identifier.
    ///
    /// @return The string representation.
    @Override
    String toString();
}
