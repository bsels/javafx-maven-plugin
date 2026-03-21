package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

/// Represents an FXML reference (e.g., using fx:reference).
///
/// @param name The name of the referenced object.
public record FXMLReference(String name) implements AbstractFXMLValue {

    /// Compact constructor to validate the reference name.
    ///
    /// @param name The name of the referenced object.
    /// @throws NullPointerException     if the name is null.
    /// @throws IllegalArgumentException if the name is not a valid Java identifier.
    public FXMLReference {
        Objects.requireNonNull(name, "name cannot be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("name must be a valid Java identifier: %s".formatted(name));
        }
    }
}
