package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

/// Represents an FXML copy (e.g., using fx:copy).
///
/// @param name The name of the object to copy.
public record FXMLCopy(String name) implements AbstractFXMLValue {
    /// Compact constructor to validate the copy source name.
    ///
    /// @param name The name of the object to copy.
    /// @throws NullPointerException     if the name is null.
    /// @throws IllegalArgumentException if the name is not a valid Java identifier.
    public FXMLCopy {
        Objects.requireNonNull(name, "name cannot be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("name must be a valid Java identifier: %s".formatted(name));
        }
    }
}
