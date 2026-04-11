package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// An object instantiated from FXML.
///
/// @param identifier    The identifier of the object
/// @param type          The type of the object
/// @param factoryMethod The optional factory method
/// @param properties    The list of properties
public record FXMLObject(
        FXMLIdentifier identifier,
        FXMLType type,
        Optional<FXMLFactoryMethod> factoryMethod,
        List<FXMLProperty> properties
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Initializes a new [FXMLObject] record instance.
    ///
    /// @param identifier    The identifier of the object
    /// @param type          The type of the object
    /// @param factoryMethod The optional factory method
    /// @param properties    The list of properties
    /// @throws NullPointerException If any parameter is null
    public FXMLObject {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        properties = List.copyOf(Objects.requireNonNullElseGet(properties, List::of));
    }
}
