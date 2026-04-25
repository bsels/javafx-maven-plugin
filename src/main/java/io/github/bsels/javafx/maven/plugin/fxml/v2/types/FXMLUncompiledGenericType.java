package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// An FXML generic type for an uncompiled class.
///
/// @param name          The name of the uncompiled generic type
/// @param typeArguments The list of generic type arguments
public record FXMLUncompiledGenericType(String name, List<FXMLType> typeArguments)
        implements FXMLType {

    /// Initializes a new [FXMLUncompiledGenericType] record instance.
    ///
    /// @param name          The name of the uncompiled generic type
    /// @param typeArguments The list of generic type arguments
    /// @throws NullPointerException If `name` is null
    public FXMLUncompiledGenericType {
        Objects.requireNonNull(name, "`name` must not be null");
        typeArguments = List.copyOf(Objects.requireNonNullElseGet(typeArguments, List::of));
    }

    /// Initializes a new [FXMLUncompiledGenericType] record instance.
    ///
    /// @param name          The name of the uncompiled generic type
    /// @param typeArguments The list of generic type arguments
    /// @throws NullPointerException If `name` or `typeArguments` is null
    public FXMLUncompiledGenericType(String name, FXMLType... typeArguments) {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(typeArguments, "`typeArguments` must not be null");
        this(name, List.of(typeArguments));
    }
}
