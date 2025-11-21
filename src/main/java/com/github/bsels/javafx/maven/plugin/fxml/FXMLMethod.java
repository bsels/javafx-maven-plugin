package com.github.bsels.javafx.maven.plugin.fxml;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Represents a method definition in the context of FXML processing.
/// This record encapsulates details about the name, parameter types, and return type of the method.
///
/// The class is designed to provide metadata about methods used in JavaFX FXML-related constructs.
/// It ensures immutability and guarantees that all provided values are non-null.
///
/// The parameter list is securely wrapped in an immutable copy,
/// ensuring thread safety and preserving the integrity of the data.
public record FXMLMethod(String name, List<Type> parameters, Type returnType, Map<String, String> namedGenerics) {

    /// Constructs an instance of [FXMLMethod], representing a method definition in an FXML-related context.
    /// Ensures that the provided values for name, parameters, and returnType are non-null
    /// and wraps the parameter list in an immutable copy for safe usage.
    ///
    /// @param name the name of the method; must not be null
    /// @param parameters the list of parameter types for the method; must not be null
    /// @param returnType the return type of the method; must not be null
    /// @param namedGenerics a map of generic type names to their resolved types; can be null or empty
    /// @throws NullPointerException if any of the parameters (name, parameters, or returnType) is null
    public FXMLMethod {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(parameters, "`parameters` must not be null");
        parameters = List.copyOf(parameters);
        Objects.requireNonNull(returnType, "`returnType` must not be null");
        namedGenerics = Map.copyOf(Objects.requireNonNull(namedGenerics, "`namedGenerics` must not be null"));
    }
}
