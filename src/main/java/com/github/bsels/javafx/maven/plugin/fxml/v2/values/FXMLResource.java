package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML resource (e.g., using %resourceKey).
///
/// @param name The resource key.
public record FXMLResource(String name) implements AbstractFXMLValue {

    /// Compact constructor to validate the resource key.
    ///
    /// @param name The resource key.
    /// @throws NullPointerException     if the name is null.
    /// @throws IllegalArgumentException if the name is blank.
    public FXMLResource {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
