package io.github.bsels.javafx.maven.plugin.fxml.v2.controller;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;

/// Represents a method in an FXML controller class.
///
/// This record stores information about the method's visibility, its name, its return type,
/// and its list of parameter types.
///
/// @param visibility     The visibility level of the method.
/// @param name           The name of the method.
/// @param returnType     The FXML type representing the return type of the method.
/// @param parameterTypes The list of FXML types representing the parameter types of the method.
public record FXMLControllerMethod(
        Visibility visibility,
        String name,
        FXMLType returnType,
        List<FXMLType> parameterTypes
) {

    /// Constructs a new `FXMLControllerMethod` and ensures all parameters are non-null.
    ///
    /// @param visibility     The visibility level of the method.
    /// @param name           The name of the method.
    /// @param returnType     The return type of the method.
    /// @param parameterTypes The parameter types of the method.
    /// @throws NullPointerException If any of the parameters are null.
    public FXMLControllerMethod {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(returnType, "`returnType` must not be null");
        Objects.requireNonNull(parameterTypes, "`parameterTypes` must not be null");
    }
}
