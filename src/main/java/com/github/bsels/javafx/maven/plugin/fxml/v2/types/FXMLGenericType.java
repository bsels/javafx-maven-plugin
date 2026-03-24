package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// Represents a generic type in FXML with type arguments.
///
/// This record stores the base class type and its type arguments.
///
/// @param type          The base class type. Must not be `null`.
/// @param typeArguments The list of type arguments. If `null`, an empty list is used.
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

    /// Constructs an instance of `FXMLGenericType` using a base class type
    /// and a variable number of type arguments.
    ///
    /// @param type          The base class type. Must not be `null`.
    /// @param typeArguments The type arguments for the generic type. If no arguments
    ///                      are provided, an empty list is used.
    /// @throws NullPointerException if `type` is `null`.
    public FXMLGenericType(Class<?> type, FXMLType... typeArguments) {
        this(type, List.of(typeArguments));
    }
}
