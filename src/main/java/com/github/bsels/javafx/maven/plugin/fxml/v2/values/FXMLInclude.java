package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Objects;

/// Represents an FXML include (e.g., using fx:include).
///
/// @param identifier The identifier of to include.
/// @param sourceFile The source file to include.
public record FXMLInclude(FXMLIdentifier identifier, String sourceFile) implements AbstractFXMLValue {

    /// Compact constructor to validate the identifier and source file.
    ///
    /// @param identifier The identifier of the include.
    /// @param sourceFile The source file to include.
    /// @throws NullPointerException if `identifier` or `sourceFile` is `null`.
    public FXMLInclude {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(sourceFile, "`sourceFile` must not be null");
    }
}
