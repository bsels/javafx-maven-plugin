package com.github.bsels.javafx.maven.plugin.fxml;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a constant node in the FXML data structure.
/// This record is a direct implementation of the [FXMLNode] interface, which describes elements of an FXML model.
///
/// The [FXMLConstantNode] stores metadata about a constant, including its associated class, identifier, and type.
///
/// @param clazz              the class associated with the constant must not be null
/// @param constantIdentifier the identifier for the constant must not be null
/// @param constantType       the type of the constant must not be null
public record FXMLConstantNode(Class<?> clazz, String constantIdentifier, Type constantType) implements FXMLNode {

    /// Constructs an instance of [FXMLConstantNode].
    ///
    /// @param clazz              the class associated with the constant must not be null
    /// @param constantIdentifier the identifier for the constant must not be null
    /// @param constantType       the type of the constant must not be null
    /// @throws NullPointerException if any of the parameters are null
    public FXMLConstantNode {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(constantIdentifier, "`constantIdentifier` must not be null");
        Objects.requireNonNull(constantType, "`constantType` must not be null");
    }
}
