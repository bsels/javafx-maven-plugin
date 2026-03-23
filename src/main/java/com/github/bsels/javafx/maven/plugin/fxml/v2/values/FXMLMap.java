package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML map structure, which consists of a type, an optional factory method,
/// and a collection of key-value entries.
///
/// @param identifier    The identifier of the map.
/// @param type          The type of the map.
/// @param factoryMethod The optional factory method name.
/// @param entries       A collection of key-value mappings representing the entries in the map.
public record FXMLMap(
        FXMLIdentifier identifier,
        FXMLType type,
        Optional<String> factoryMethod,
        Map<String, AbstractFXMLValue> entries
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Compact constructor to validate the map components.
    ///
    /// @param identifier    The identifier of the map.
    /// @param type          The type of the map.
    /// @param factoryMethod The optional factory method name.
    /// @param entries       The collection of entries.
    /// @throws NullPointerException if `identifier`, `type`, `factoryMethod`, or `entries` is `null`.
    public FXMLMap {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        entries = Map.copyOf(Objects.requireNonNullElseGet(entries, Map::of));
    }
}
