package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// Represents a field in an FXML controller class.
///
/// This record stores information about the field's visibility, its name, and its FXML type.
///
/// @param visibility The visibility level of the field.
/// @param name       The name of the field.
/// @param type       The FXML type associated with the field.
public record FXMLControllerField(Visibility visibility, String name, FXMLType type) {

    /// Constructs a new `FXMLControllerField` and ensures all parameters are non-null.
    ///
    /// @param visibility The visibility level of the field.
    /// @param name       The name of the field.
    /// @param type       The FXML type associated with the field.
    /// @throws NullPointerException If any of the parameters are null.
    public FXMLControllerField {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
    }
}
