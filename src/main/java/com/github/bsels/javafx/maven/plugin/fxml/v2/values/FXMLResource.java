package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML resource (e.g., using %resourceKey).
///
/// @param name The resource key.
public record FXMLResource(String name) implements AbstractFXMLValue {

    /// Compact constructor to validate the resource key.
    ///
    /// @param name The resource key.
    /// @throws NullPointerException     if `name` is `null`.
    /// @throws IllegalArgumentException if `name` is blank.
    public FXMLResource {
        Objects.requireNonNull(name, "`name` must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("`name` must not be blank");
        }
    }
}
