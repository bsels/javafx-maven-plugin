package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Objects;

/// Represents an FXML copy (e.g., using fx:copy).
///
/// @param identifier The identifier for the copy.
/// @param name       The name of the object to copy.
public record FXMLCopy(FXMLIdentifier identifier, String name) implements AbstractFXMLValue {
    /// Compact constructor to validate the copy identifier and source name.
    ///
    /// @param identifier The identifier for the copy.
    /// @param name       The name of the object to copy.
    /// @throws NullPointerException     if `identifier` or `name` is `null`.
    /// @throws IllegalArgumentException if `name` is not a valid Java identifier.
    public FXMLCopy {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
    }
}
