package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// A field in an FXML controller class.
///
/// @param visibility The visibility level of the field
/// @param name       The name of the field
/// @param type       The FXML type associated with the field
public record FXMLControllerField(Visibility visibility, String name, FXMLType type) {

    /// Initializes a new [FXMLControllerField] instance.
    ///
    /// @param visibility The visibility level of the field
    /// @param name       The name of the field
    /// @param type       The FXML type associated with the field
    /// @throws NullPointerException If any parameter is null
    public FXMLControllerField {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
    }
}
