package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML include (e.g., using fx:include).
///
/// @param sourceFile The source file to include.
public record FXMLInclude(String sourceFile) implements AbstractFXMLValue {

    /// Compact constructor to validate the source file.
    ///
    /// @param sourceFile The source file to include.
    /// @throws NullPointerException if the source file is null.
    public FXMLInclude {
        Objects.requireNonNull(sourceFile, "`sourceFile` cannot be null");
    }
}
