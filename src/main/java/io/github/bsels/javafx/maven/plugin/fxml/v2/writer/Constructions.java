package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.List;

/// Represents an FXML construction step that may have dependencies.
sealed interface Constructions
        permits AbstractFXMLObjectAndDependencies, FXMLCopyAndDependencies, FXMLIncludeAndDependencies,
                FXMLValueConstruction {
    /// Returns the identifiers this construction depends on.
    ///
    /// @return A list of [FXMLIdentifier] dependencies.
    List<FXMLIdentifier> dependencies();
}
