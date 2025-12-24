package com.github.bsels.javafx.maven.plugin.fxml.introspect;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/// Represents a method in a controller with its visibility, name, return type, and parameter types.
///
/// This class encapsulates the properties of a method, including its visibility, name, return type,
/// and a list of parameter types.
/// It also enforces non-null constraints on these parameters to ensure the validity of the represented method.
///
/// @param visibility     the visibility level of the method; must not be null
/// @param name           the name of the method; must not be null
/// @param returnType     the return type of the method; must not be null
/// @param parameterTypes the list of parameter types for the method; must not be null, but can be empty
public record ControllerMethod(Visibility visibility, String name, Type returnType, List<Type> parameterTypes) {

    /// Constructs a new instance of `ControllerMethod`.
    ///
    /// @param visibility     the visibility level of the method; must not be null
    /// @param name           the name of the method; must not be null
    /// @param returnType     the return type of the method; must not be null
    /// @param parameterTypes the list of parameter types for the method; must not be null, but can be empty
    /// @throws NullPointerException if any of the parameters except `parameterTypes` are null, or if `parameterTypes` is null when not defaulted to an empty list
    public ControllerMethod {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(returnType, "`returnType` must not be null");
        parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "`parameterTypes` must not be null"));
    }
}
