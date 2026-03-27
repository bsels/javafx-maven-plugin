package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML resource (e.g., using @resourcePath).
///
/// @param path The relative path to the resource according to the resources root (single /).
public record FXMLResource(String path) implements AbstractFXMLValue {

    /// Compact constructor to validate the resource path.
    ///
    /// @param path The relative path to the resource according to the resources root (single /).
    /// @throws NullPointerException     if `path` is `null`.
    /// @throws IllegalArgumentException if `path` is blank.
    public FXMLResource {
        Objects.requireNonNull(path, "`path` must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("`path` must not be blank");
        }
    }
}
