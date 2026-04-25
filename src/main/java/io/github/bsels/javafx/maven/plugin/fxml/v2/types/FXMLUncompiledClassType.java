package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.Objects;

/// An FXML type for an uncompiled class.
///
/// @param name The fully qualified name of the uncompiled class
public record FXMLUncompiledClassType(String name) implements FXMLType {

    /// Initializes a new [FXMLUncompiledClassType] record instance.
    ///
    /// @param name The name of the uncompiled class
    /// @throws NullPointerException If `name` is null
    public FXMLUncompiledClassType {
        Objects.requireNonNull(name, "`name` must not be null");
    }
}
