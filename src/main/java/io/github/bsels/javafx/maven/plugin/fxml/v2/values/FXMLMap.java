package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML map structure, which consists of a type, an optional factory method,
/// and a collection of key-value entries.
///
/// @param identifier    The identifier of the map.
/// @param type          The type of the map.
/// @param rawKeyClass   The raw key class of the map.
/// @param rawValueClass The raw value class of the map.
/// @param factoryMethod The optional factory method.
/// @param entries       A collection of key-value mappings representing the entries in the map.
public record FXMLMap(
        FXMLIdentifier identifier,
        FXMLType type,
        FXMLClassType rawKeyClass,
        FXMLClassType rawValueClass,
        Optional<FXMLFactoryMethod> factoryMethod,
        Map<FXMLLiteral, AbstractFXMLValue> entries
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Compact constructor to validate the map components.
    ///
    /// @param identifier    The identifier of the map.
    /// @param type          The type of the map.
    /// @param rawKeyClass   The raw key class of the map.
    /// @param rawValueClass The raw value class of the map.
    /// @param factoryMethod The optional factory method name.
    /// @param entries       The collection of entries.
    /// @throws NullPointerException if `identifier`, `type`, `rawKeyClass`, `rawValueClass`, or `factoryMethod` is `null`.
    public FXMLMap {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(rawKeyClass, "`rawKeyClass` must not be null");
        Objects.requireNonNull(rawValueClass, "`rawValueClass` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        entries = Map.copyOf(Objects.requireNonNullElseGet(entries, Map::of));
        switch (type) {
            case FXMLClassType(Class<?> clazz) -> {
                if (!Map.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("`type` must be a Map: %s".formatted(clazz));
                }
            }
            case FXMLGenericType(Class<?> clazz, _) -> {
                if (!Map.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("`type` must be a Map: %s".formatted(clazz));
                }
            }
            case FXMLUncompiledClassType _, FXMLUncompiledGenericType _, FXMLWildcardType _ -> {
                // The type is not yet compiled or available in the current classloader or is a wildcard;
                // map assignability cannot be verified at this point.
            }
        }
    }
}
