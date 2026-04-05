package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Objects;

/// Represents an FXML copy (e.g., using fx:copy).
///
/// @param identifier The identifier for the copy.
/// @param source     The source of the object to copy.
public record FXMLCopy(FXMLIdentifier identifier, FXMLExposedIdentifier source) implements AbstractFXMLValue {
    /// Compact constructor to validate the copy identifier and source.
    ///
    /// @param identifier The identifier for the copy.
    /// @param source     The source of the object to copy.
    /// @throws NullPointerException if `identifier` or `source` is `null`.
    public FXMLCopy {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(source, "`source` must not be null");
    }
}
