package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

public sealed interface FXMLIdentifier permits FXMLExposedIdentifier, FXMLInternalIdentifier, FXMLRootIdentifier {

    @Override
    String toString();
}
