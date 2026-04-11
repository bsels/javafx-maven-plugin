package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// A collection of FXML values.
///
/// @param identifier    The unique identifier associated with this FXML collection
/// @param type          The type of the collection
/// @param factoryMethod An optional factory method
/// @param values        The list of values
public record FXMLCollection(
        FXMLIdentifier identifier,
        FXMLType type,
        Optional<FXMLFactoryMethod> factoryMethod,
        List<AbstractFXMLValue> values
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Initializes a new [FXMLCollection] record instance.
    ///
    /// @param identifier    The unique identifier associated with this FXML collection
    /// @param type          The type of the collection
    /// @param factoryMethod An optional factory method
    /// @param values        The list of values
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If `type` is not a [Collection]
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
            case FXMLUncompiledClassType _, FXMLUncompiledGenericType _, FXMLWildcardType _ -> {
                // The type is not yet compiled or available in the current classloader or is a wildcard;
                // collection assignability cannot be verified at this point.
            }
        }
    }
}
