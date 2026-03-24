package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLFactoryMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildCardType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents a collection of FXML values.
///
/// @param identifier    The unique identifier associated with this FXML collection.
/// @param type          The class type of the values stored in this collection. It may be an [FXMLClassType], [FXMLGenericType], or [FXMLUncompiledGenericType].
/// @param factoryMethod An optional factory method used for instantiation.
/// @param values        The collection of values.
public record FXMLCollection(
        FXMLIdentifier identifier,
        FXMLType type,
        Optional<FXMLFactoryMethod> factoryMethod,
        List<AbstractFXMLValue> values
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Compact constructor to validate the identifier, type, factory method, and values.
    ///
    /// @param identifier    The unique identifier associated with this FXML collection.
    /// @param type          The class type of the values stored in this collection.
    /// @param factoryMethod An optional factory method used for instantiation.
    /// @param values        The collection of values.
    /// @throws NullPointerException if `identifier`, `type`, `factoryMethod`, or `values` is `null`.
    public FXMLCollection {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        values = List.copyOf(Objects.requireNonNullElseGet(values, List::of));
        switch (type) {
            case FXMLClassType(Class<?> clazz) -> {
                if (!Collection.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("`type` must be a Collection: %s".formatted(clazz));
                }
            }
            case FXMLGenericType(Class<?> clazz, _) -> {
                if (!Collection.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("`type` must be a Collection: %s".formatted(clazz));
                }
            }
            case FXMLUncompiledClassType _, FXMLUncompiledGenericType _, FXMLWildCardType _ -> {
                // The type is not yet compiled or available in the current classloader or is a wildcard;
                // collection assignability cannot be verified at this point.
            }
        }
    }
}
