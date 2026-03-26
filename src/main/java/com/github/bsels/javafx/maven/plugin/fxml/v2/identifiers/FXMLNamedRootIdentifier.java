package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;

import java.util.Objects;

/// Represents an FXML identifier for a named root node.
///
/// @param name The name of the root node used for mapping it to the controller.
public record FXMLNamedRootIdentifier(String name) implements FXMLIdentifier {

    /// Creates a new `FXMLNamedRootIdentifier`.
    ///
    /// @param name The name of the root node.
    /// @throws NullPointerException If `name` is `null`.
    /// @throws IllegalArgumentException If `name` is not a valid Java identifier.
    public FXMLNamedRootIdentifier {
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
    }

    /// Returns the string representation of the root node.
    ///
    /// @return The string "this".
    @Override
    public String toString() {
        // Intentionally return "this" to avoid ambiguity with the root node.
        // The name is only used for mapping to the controller.
        return "this";
    }
}
