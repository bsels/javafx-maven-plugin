package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;

/// A method in an FXML controller class.
///
/// @param visibility     The visibility level of the method
/// @param name           The name of the method
/// @param isAbstract     Whether the method is abstract
/// @param returnType     The return type of the method
/// @param parameterTypes The list of parameter types of the method
public record FXMLControllerMethod(
        Visibility visibility,
        String name,
        boolean isAbstract,
        FXMLType returnType,
        List<FXMLType> parameterTypes
) {

    /// Initializes a new [FXMLControllerMethod] instance.
    ///
    /// @param visibility     The visibility level of the method
    /// @param name           The name of the method
    /// @param isAbstract     Whether the method is abstract
    /// @param returnType     The return type of the method
    /// @param parameterTypes The parameter types of the method
    /// @throws NullPointerException If any parameter is null
    public FXMLControllerMethod {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(returnType, "`returnType` must not be null");
        Objects.requireNonNull(parameterTypes, "`parameterTypes` must not be null");
    }
}
