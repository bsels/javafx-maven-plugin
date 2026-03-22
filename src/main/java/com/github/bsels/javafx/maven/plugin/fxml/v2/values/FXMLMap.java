package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML map structure, which consists of a class type, a list of optional generics,
/// and a collection of key-value entries.
/// It also supports an optional factory method for custom instantiations.
///
/// This class is a record and serves as an implementation of both
/// [AbstractFXMLValue] and [AbstractFXMLObject].
///
/// @param identifier    The unique identifier representing this FXML map, cannot be null.
/// @param clazz         The target class type for the map; must be a subclass of [Map] and cannot be null.
/// @param factoryMethod An optional factory method that serves as a callback to create instances of this map. Cannot be null but may be empty.
/// @param generics      A list of generic type definitions associated with the map. If null, it defaults to an empty list. Each entry will be validated to ensure it is not null or blank.
/// @param entries       A collection of key-value mappings representing the entries in the map. Keys are strings, and values must be instances of [AbstractFXMLValue]. This field cannot be null, but defaults to an empty map if not provided.
public record FXMLMap(
        FXMLIdentifier identifier,
        Class<? extends Map<?, ?>> clazz,
        Optional<String> factoryMethod,
        List<String> generics,
        Map<String, AbstractFXMLValue> entries
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Constructs a new instance of FXMLMap with the provided parameters.
    /// Validates the identifier, class type, factory method, generics, and entries.
    ///
    /// @param identifier    The identifier of the FXMLMap. Cannot be null.
    /// @param clazz         The class type of the FXMLMap. Must be a subclass of Map. Cannot be null.
    /// @param factoryMethod An optional factory method to create the map. Cannot be null but can be empty.
    /// @param generics      A list of generic type definitions associated with the map. If null, an empty list will be used. Each generic value is validated.
    /// @param entries       A map containing entries for the FXMLMap, where the key is a string and the value is an instance of AbstractFXMLValue. If null, an empty map will be used.
    /// @throws NullPointerException     if any non-optional parameter is null, or if any generic or entry key/value is null.
    /// @throws IllegalArgumentException if any generic value is blank or invalid, according to validation rules.
    public FXMLMap {
        Objects.requireNonNull(identifier, "Identifier must not be null");
        Objects.requireNonNull(clazz, "Class must not be null");
        Objects.requireNonNull(factoryMethod, "Factory method must not be null");
        generics = List.copyOf(Objects.requireNonNullElseGet(generics, List::of));
        generics.forEach(AbstractFXMLValue::validateGeneric);
        entries = Map.copyOf(Objects.requireNonNullElseGet(entries, Map::of));
    }
}
