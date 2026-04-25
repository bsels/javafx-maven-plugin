package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// A type in FXML.
public sealed interface FXMLType
        permits FXMLClassType, FXMLGenericType, FXMLUncompiledClassType, FXMLUncompiledGenericType, FXMLWildcardType {

    /// An [FXMLType] representing the `Object` class.
    FXMLType OBJECT = new FXMLClassType(Object.class);

    /// Creates an [FXMLType] representing a class type.
    ///
    /// @param clazz The class type to be encapsulated
    /// @return An [FXMLType] instance
    /// @throws NullPointerException If `clazz` is null
    static FXMLType of(Class<?> clazz) {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return new FXMLClassType(clazz);
    }

    /// Creates an [FXMLType] based on the specified class and generic arguments.
    ///
    /// @param clazz        The class type
    /// @param genericTypes The list of generic type arguments
    /// @return An [FXMLType] instance
    /// @throws NullPointerException If `clazz` or `genericTypes` is null
    static FXMLType of(Class<?> clazz, List<FXMLType> genericTypes) {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(genericTypes, "`genericTypes` must not be null");
        if (genericTypes.isEmpty()) {
            return new FXMLClassType(clazz);
        }
        return new FXMLGenericType(clazz, genericTypes);
    }

    /// Creates an [FXMLType] for an uncompiled class and its generic arguments.
    ///
    /// @param uncompiledType The name of the uncompiled type
    /// @param genericTypes   The list of generic type arguments
    /// @return An [FXMLType] instance
    /// @throws NullPointerException If `uncompiledType` or `genericTypes` is null
    static FXMLType of(String uncompiledType, List<FXMLType> genericTypes) {
        Objects.requireNonNull(uncompiledType, "`uncompiledType` must not be null");
        Objects.requireNonNull(genericTypes, "`genericTypes` must not be null");
        if (genericTypes.isEmpty()) {
            return new FXMLUncompiledClassType(uncompiledType);
        }
        return new FXMLUncompiledGenericType(uncompiledType, genericTypes);
    }

    /// Returns the wildcard type singleton instance.
    ///
    /// @return An [FXMLWildcardType] instance
    static FXMLType wildcard() {
        return FXMLWildcardType.INSTANCE;
    }
}
