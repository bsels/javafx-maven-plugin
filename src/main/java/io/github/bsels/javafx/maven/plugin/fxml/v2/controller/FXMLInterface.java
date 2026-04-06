package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;

/// Represents an interface for FXML controller elements.
///
/// This record encapsulates the type of the interface and a collection of controller methods.
/// It provides a structural abstraction for FXML controller introspection and processing.
///
/// @param type    The FXML type associated with this interface. Must not be null.
/// @param methods The list of controller methods defined within this interface. If null, it defaults to an empty list.
public record FXMLInterface(FXMLType type, List<FXMLControllerMethod> methods) {

    /// Constructs a new `FXMLInterface` instance while enforcing non-null constraints on its parameters.
    ///
    /// @param type    The FXML type associated with this interface. Must not be null.
    /// @param methods The list of methods associated with this interface. If the provided value is null, it will default to an empty list.
    /// @throws NullPointerException If `type` is null.
    public FXMLInterface {
        Objects.requireNonNull(type);
        methods = List.copyOf(Objects.requireNonNullElseGet(methods, List::of));
    }
}
