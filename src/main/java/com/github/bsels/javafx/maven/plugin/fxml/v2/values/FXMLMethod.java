package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Represents an FXML method call (e.g., event handlers).
///
/// @param name          The method name.
/// @param parameters    The list of parameter types.
/// @param returnType    The return type of the method.
/// @param namedGenerics A map of generic type names to their actual types.
public record FXMLMethod(String name, List<Type> parameters, Type returnType, Map<String, String> namedGenerics)
        implements AbstractFXMLValue {

    /// Compact constructor to validate the method signature and generic types.
    ///
    /// @param name          The method name.
    /// @param parameters    The list of parameter types.
    /// @param returnType    The return type of the method.
    /// @param namedGenerics A map of generic type names to their actual types.
    /// @throws NullPointerException     if name, returnType or any element of parameters or namedGenerics is null.
    /// @throws IllegalArgumentException if the name is not a valid Java identifier.
    public FXMLMethod {
        Objects.requireNonNull(name, "name cannot be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("name must be a valid Java identifier: %s".formatted(name));
        }
        Objects.requireNonNull(returnType, "returnType cannot be null");
        parameters = List.copyOf(Objects.requireNonNullElseGet(parameters, List::of));
        namedGenerics = Map.copyOf(Objects.requireNonNullElseGet(namedGenerics, Map::of));

        for (Type parameter : parameters) {
            Objects.requireNonNull(parameter, "parameter cannot be null");
        }

        for (Map.Entry<String, String> entry : namedGenerics.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "namedGenerics key cannot be null");
            Objects.requireNonNull(entry.getValue(), "namedGenerics value cannot be null");
        }
    }
}
