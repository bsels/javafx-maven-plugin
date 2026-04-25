package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// An FXML type representing a Java generic type.
///
/// @param type          The raw class type
/// @param typeArguments The list of FXML generic type arguments
public record FXMLGenericType(Class<?> type, List<FXMLType> typeArguments) implements FXMLType {

    /// Initializes a new [FXMLGenericType] record instance.
    ///
    /// @param type          The raw class type
    /// @param typeArguments The list of generic type arguments
    /// @throws NullPointerException     If `type` or `typeArguments` is null
    /// @throws IllegalArgumentException If `typeArguments` is empty
    public FXMLGenericType {
        Objects.requireNonNull(type, "`type` must not be null");
        typeArguments = List.copyOf(Objects.requireNonNullElseGet(typeArguments, List::of));
        if (typeArguments.isEmpty()) {
            throw new IllegalArgumentException("`typeArguments` must not be empty");
        }
    }

    /// Constructs an instance of `FXMLGenericType` using a base class type and a variable number of type arguments.
    ///
    /// @param type          The base class type. Must not be `null`.
    /// @param typeArguments The type arguments for the generic type. If no arguments are provided, an empty list is used.
    /// @throws NullPointerException if `type` or `typeArguments` is `null`.
    /// @throws IllegalArgumentException if `typeArguments` is empty
    public FXMLGenericType(Class<?> type, FXMLType... typeArguments) {
        this(type, List.of(typeArguments));
    }
}
