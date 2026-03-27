package io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;

import java.util.Objects;

/// Represents an identifier explicitly named in FXML (e.g., using fx:id).
///
/// @param name The name of the identifier.
public record FXMLExposedIdentifier(String name) implements FXMLIdentifier {

    /// Compact constructor to validate the identifier name.
    ///
    /// @param name The name of the identifier.
    /// @throws NullPointerException     if `name` is null.
    /// @throws IllegalArgumentException if `name` is not a valid Java identifier.
    public FXMLExposedIdentifier {
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
    }

    /// Returns the name of the identifier.
    ///
    /// @return The name.
    @Override
    public String toString() {
        return name;
    }
}
