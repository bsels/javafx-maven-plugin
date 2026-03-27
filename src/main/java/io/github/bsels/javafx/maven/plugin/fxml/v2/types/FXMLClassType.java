package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.Objects;

/// Represents a simple class type in FXML.
///
/// This record stores the underlying class of the FXML type.
///
/// @param clazz The class type. Must not be `null`.
public record FXMLClassType(Class<?> clazz) implements FXMLType {

    /// Compact constructor to validate the class type.
    ///
    /// @param clazz The class type.
    /// @throws NullPointerException if `clazz` is `null`.
    public FXMLClassType {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
    }
}
