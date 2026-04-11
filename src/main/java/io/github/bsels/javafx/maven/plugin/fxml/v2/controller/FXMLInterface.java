package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;

/// An interface for FXML controller elements.
///
/// Encapsulates the type of the interface and a collection of controller methods.
///
/// @param type    The FXML type associated with this interface
/// @param methods The list of controller methods defined within this interface
public record FXMLInterface(FXMLType type, List<FXMLControllerMethod> methods) {

    /// Initializes a new [FXMLInterface] instance.
    ///
    /// @param type    The FXML type associated with this interface
    /// @param methods The list of methods associated with this interface
    /// @throws NullPointerException If `type` is null
    public FXMLInterface {
        Objects.requireNonNull(type);
        methods = List.copyOf(Objects.requireNonNullElseGet(methods, List::of));
    }
}
