package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents an object instantiated from FXML.
///
/// @param identifier    The identifier of the object.
/// @param type          The type of the object.
/// @param factoryMethod The factory method name, if any.
/// @param properties    The list of properties of the object.
public record FXMLObject(
        FXMLIdentifier identifier,
        FXMLType type,
        Optional<String> factoryMethod,
        List<FXMLProperty<?>> properties
) implements AbstractFXMLValue, AbstractFXMLObject {

    /// Compact constructor to validate the FXML object components.
    ///
    /// @param identifier    The identifier of the object.
    /// @param type          The type of the object.
    /// @param factoryMethod The factory method name, if any.
    /// @param properties    The list of properties of the object.
    /// @throws NullPointerException     if `identifier`, `type`, `factoryMethod`, or `properties` is `null`.
    /// @throws IllegalArgumentException if `factoryMethod` is not a valid Java identifier.
    public FXMLObject {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        properties = List.copyOf(Objects.requireNonNullElseGet(properties, List::of));
        factoryMethod.ifPresent(
                factoryMethodValue -> {
                    if (Utils.isInvalidIdentifierName(factoryMethodValue)) {
                        throw new IllegalArgumentException(
                                "`factoryMethod` must be a valid Java identifier: %s".formatted(factoryMethodValue)
                        );
                    }
                }
        );
    }
}
