package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;

import java.util.List;
import java.util.Objects;

/// Represents an FXML controller class with its associated fields and methods.
///
/// This record captures the structure of an FXML controller, including its underlying class,
/// list of fields, and list of methods.
///
/// @param controllerClass The class of the controller.
/// @param fields          The list of fields in the controller class that are relevant to FXML.
/// @param methods         The list of methods in the controller class that are relevant to FXML.
public record FXMLController(
        FXMLClassType controllerClass,
        List<FXMLControllerField> fields,
        List<FXMLControllerMethod> methods
) {

    /// Constructs a new `FXMLController` and ensures all parameters are non-null.
    ///
    /// @param controllerClass The class of the controller.
    /// @param fields          The list of fields in the controller class.
    /// @param methods         The list of methods in the controller class.
    /// @throws NullPointerException If any of the parameters are null.
    public FXMLController {
        Objects.requireNonNull(controllerClass, "`controllerClass` must not be null");
        Objects.requireNonNull(fields, "`fields` must not be null");
        Objects.requireNonNull(methods, "`methods` must not be null");
    }
}
