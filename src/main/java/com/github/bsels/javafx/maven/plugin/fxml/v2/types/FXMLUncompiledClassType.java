package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.Objects;

/// Represents an uncompiled class type in FXML.
///
/// This record is used to define a class type specified by its name, which may not be resolved or compiled.
///
/// @param name The name of the uncompiled class type. Must not be `null`.
public record FXMLUncompiledClassType(String name) implements FXMLType {

    /// Compact constructor for the `FXMLUncompiledClassType` record.
    ///
    /// @param name The name of the uncompiled class type. Must not be null.
    /// @throws NullPointerException if `name` is null.
    public FXMLUncompiledClassType {
        Objects.requireNonNull(name, "`name` must not be null");
    }
}
