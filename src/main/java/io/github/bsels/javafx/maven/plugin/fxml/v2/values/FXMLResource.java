package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// An FXML resource (e.g., using `@resourcePath`).
///
/// @param path The relative path to the resource
public record FXMLResource(String path) implements AbstractFXMLValue {

    /// Initializes a new [FXMLResource] record instance.
    ///
    /// @param path The relative path to the resource
    /// @throws NullPointerException     If `path` is null
    /// @throws IllegalArgumentException If `path` is blank
    public FXMLResource {
        Objects.requireNonNull(path, "`path` must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("`path` must not be blank");
        }
    }
}
