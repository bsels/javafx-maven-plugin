package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.Objects;

/// An FXML type representing a Java class.
///
/// @param clazz The Java class
public record FXMLClassType(Class<?> clazz) implements FXMLType {

    /// Initializes a new [FXMLClassType] record instance.
    ///
    /// @param clazz The Java class
    /// @throws NullPointerException If `clazz` is null
    public FXMLClassType {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
    }
}
