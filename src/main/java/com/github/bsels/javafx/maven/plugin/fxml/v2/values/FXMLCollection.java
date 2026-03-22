package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Constructs an immutable `FXMLCollection` object representing a list of FXML abstract values,
/// with additional metadata like identifier, class type, optional factory method, and generic type.
///
/// @param identifier    The unique identifier associated with this FXML collection.
/// @param clazz         The class type of the values stored in this collection.
/// @param factoryMethod An optional factory method name used for instantiation. Must be a valid Java identifier if present.
/// @param generics      The generic type arguments.
/// @param values        The collection of AbstractFXMLValue elements. If null, an empty collection is used instead.
public record FXMLCollection(
        FXMLIdentifier identifier,
        Class<? extends Collection<?>> clazz,
        Optional<String> factoryMethod,
        List<String> generics,
        List<AbstractFXMLValue> values
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Constructs an immutable `FXMLCollection` object representing a collection of FXML abstract values,
    /// with additional metadata like identifier, class type, optional factory method, and generic type.
    ///
    /// @param identifier    The unique identifier associated with this FXML collection.
    /// @param clazz         The class type of the values stored in this collection.
    /// @param factoryMethod An optional factory method name used for instantiation. Must be a valid Java identifier if present.
    /// @param generics      The generic type arguments.
    /// @param values        The collection of AbstractFXMLValue elements. If null, an empty collection is used instead.
    /// @throws NullPointerException     If `identifier`, `clazz`, `factoryMethod`, `generic`, or `values` is null.
    /// @throws IllegalArgumentException If `factoryMethod` is not a valid Java identifier or if `generic` is blank.
    public FXMLCollection {
        Objects.requireNonNull(identifier, "Identifier must not be null");
        Objects.requireNonNull(clazz, "Class must not be null");
        Objects.requireNonNull(factoryMethod, "Factory method must not be null");
        generics = List.copyOf(Objects.requireNonNullElseGet(generics, List::of));
        values = List.copyOf(Objects.requireNonNullElseGet(values, List::of));

        factoryMethod.ifPresent(AbstractFXMLValue::validateIdentifier);
        generics.forEach(AbstractFXMLValue::validateGeneric);
    }
}
