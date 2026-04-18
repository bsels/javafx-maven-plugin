package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// An FXML property that can have multiple values (a list).
///
/// @param name               The property name
/// @param getter             The name of the getter method that returns the collection
/// @param type               The property type
/// @param elementType        The element type
/// @param value              The list of values
/// @param onChangeListener   The name of the method to call when the collection changes
public record FXMLCollectionProperties(
        String name,
        String getter,
        FXMLType type,
        FXMLType elementType,
        List<AbstractFXMLValue> value,
        Optional<String> onChangeListener
) implements FXMLProperty {

    /// Initializes a new [FXMLCollectionProperties] record instance.
    ///
    /// @param name               The property name
    /// @param getter             The name of the getter method
    /// @param type               The property type
    /// @param elementType        The element type
    /// @param value              The list of values
    /// @param onChangeListener   The name of the method to call when the collection changes
    /// @throws NullPointerException If `name`, `getter`, `type`, or `elementType` is null
    public FXMLCollectionProperties {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(elementType, "`elementType` must not be null");
        value = List.copyOf(Objects.requireNonNullElseGet(value, List::of));
        onChangeListener = Objects.requireNonNullElse(onChangeListener, Optional.empty());
    }
}