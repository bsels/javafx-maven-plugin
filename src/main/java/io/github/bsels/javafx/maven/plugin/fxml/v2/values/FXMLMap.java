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

/// An FXML map structure.
///
/// @param identifier    The identifier of the map
/// @param type          The type of the map
/// @param keyType       The key type of the map
/// @param valueType     The value type of the map
/// @param factoryMethod The optional factory method
/// @param entries       The map entries
public record FXMLMap(
        FXMLIdentifier identifier,
        FXMLType type,
        FXMLType keyType,
        FXMLType valueType,
        Optional<FXMLFactoryMethod> factoryMethod,
        Map<FXMLLiteral, AbstractFXMLValue> entries
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Initializes a new [FXMLMap] record instance.
    ///
    /// @param identifier    The identifier of the map
    /// @param type          The type of the map
    /// @param keyType       The key type of the map
    /// @param valueType     The value type of the map
    /// @param factoryMethod The optional factory method
    /// @param entries       The collection of entries
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If `type` is not a [Map]
    public FXMLMap {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(keyType, "`keyType` must not be null");
        Objects.requireNonNull(valueType, "`valueType` must not be null");
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
