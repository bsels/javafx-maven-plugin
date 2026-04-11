package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;

import java.util.Objects;

/// An FXML reference (e.g., using `fx:reference`).
///
/// @param name The name of the referenced object
public record FXMLReference(String name) implements AbstractFXMLValue {

    /// Initializes a new [FXMLReference] record instance.
    ///
    /// @param name The name of the referenced object
    /// @throws NullPointerException     If `name` is null
    /// @throws IllegalArgumentException If `name` is not a valid Java identifier
    public FXMLReference {
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
    }
}
