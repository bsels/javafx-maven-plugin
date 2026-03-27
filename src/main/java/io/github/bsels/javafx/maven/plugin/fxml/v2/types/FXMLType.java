package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// Represents a type in FXML.
///
/// This is a sealed interface that can be either an [FXMLClassType], an [FXMLGenericType],
/// an [FXMLUncompiledClassType], an [FXMLUncompiledGenericType], or an [FXMLWildcardType].
public sealed interface FXMLType
        permits FXMLClassType, FXMLGenericType, FXMLUncompiledClassType, FXMLUncompiledGenericType, FXMLWildcardType {

    /// Creates an instance of `FXMLType` representing a class type.
    ///
    /// The returned instance is of type `FXMLClassType`, encapsulating the specified class.
    ///
    /// @param clazz The class type to be encapsulated. Must not be `null`.
    /// @return An `FXMLType` instance representing the given class type.
    /// @throws NullPointerException if `clazz` is `null`.
    static FXMLType of(Class<?> clazz) {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return new FXMLClassType(clazz);
    }

    /// Constructs an instance of `FXMLType` based on the provided class type and its generic type arguments.
    /// If the list of generic types is empty, a basic `FXMLClassType` is returned.
    /// Otherwise, an `FXMLGenericType` is created using the base class and the provided generic arguments.
    ///
    /// @param clazz        The class type to be encapsulated. Must not be `null`.
    /// @param genericTypes The list of generic type arguments. Must not be `null`.
    /// @return An instance of `FXMLType`, either an `FXMLClassType` or `FXMLGenericType`.
    /// @throws NullPointerException if `clazz` or `genericTypes` is `null`.
    static FXMLType of(Class<?> clazz, List<FXMLType> genericTypes) {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(genericTypes, "`genericTypes` must not be null");
        if (genericTypes.isEmpty()) {
            return new FXMLClassType(clazz);
        }
        return new FXMLGenericType(clazz, genericTypes);
    }

    /// Creates an instance of `FXMLType` based on the provided uncompiled type name and its generic type arguments.
    /// If the list of generic types is empty, an `FXMLUncompiledClassType` is returned.
    /// Otherwise, an `FXMLUncompiledGenericType` is created using the base type name and the provided generic arguments.
    ///
    /// @param uncompiledType The name of the uncompiled type. Must not be `null`.
    /// @param genericTypes   The list of generic type arguments. Must not be `null`.
    /// @return An instance of `FXMLType`, either an `FXMLUncompiledClassType` or `FXMLUncompiledGenericType`.
    /// @throws NullPointerException if `uncompiledType` or `genericTypes` is `null`.
    static FXMLType of(String uncompiledType, List<FXMLType> genericTypes) {
        Objects.requireNonNull(uncompiledType, "`uncompiledType` must not be null");
        Objects.requireNonNull(genericTypes, "`genericTypes` must not be null");
        if (genericTypes.isEmpty()) {
            return new FXMLUncompiledClassType(uncompiledType);
        }
        return new FXMLUncompiledGenericType(uncompiledType, genericTypes);
    }

    /// Returns a singleton instance representing the wildcard type in FXML.
    ///
    /// This method provides an FXMLType instance that represents the `?` symbol in generic type parameters.
    ///
    /// @return An instance of FXMLWildCardType representing the wildcard type.
    static FXMLType wildcard() {
        return FXMLWildcardType.INSTANCE;
    }
}
