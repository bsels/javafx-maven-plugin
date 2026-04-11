package io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;

import java.util.Objects;

/// An identifier explicitly named in FXML (e.g., using `fx:id`).
///
/// @param name The name of the identifier
public record FXMLExposedIdentifier(String name) implements FXMLIdentifier {

    /// Initializes a new [FXMLExposedIdentifier] record instance.
    ///
    /// @param name The name of the identifier
    /// @throws NullPointerException     If `name` is null
    /// @throws IllegalArgumentException If `name` is not a valid Java identifier
    public FXMLExposedIdentifier {
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
    }

    /// Returns the name of the identifier.
    ///
    /// @return The name
    @Override
    public String toString() {
        return name;
    }
}
