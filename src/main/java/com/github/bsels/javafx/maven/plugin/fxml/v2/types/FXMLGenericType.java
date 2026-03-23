package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// Represents a generic type in FXML with type arguments.
///
/// This record stores the base class type and its type arguments.
///
/// @param type          The base class type.
/// @param typeArguments The list of type arguments.
public record FXMLGenericType(Class<?> type, List<FXMLType> typeArguments) implements FXMLType {

    /// Compact constructor to validate the generic type components.
    ///
    /// @param type          The base class type.
    /// @param typeArguments The list of type arguments.
    /// @throws NullPointerException if `type` is `null`.
    public FXMLGenericType {
        Objects.requireNonNull(type, "`type` must not be null");
        typeArguments = List.copyOf(Objects.requireNonNullElseGet(typeArguments, List::of));
    }
}
