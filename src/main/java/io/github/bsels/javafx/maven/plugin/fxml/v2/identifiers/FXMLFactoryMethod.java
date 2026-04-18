package io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;

import java.util.Objects;

/// A static factory method used in FXML with the `fx:factory` attribute.
///
/// @param clazz  The class that defines the factory method
/// @param method The name of the factory method
public record FXMLFactoryMethod(FXMLClassType clazz, String method) {

    /// Initializes a new [FXMLFactoryMethod] record instance.
    ///
    /// @param clazz  The class that defines the factory method
    /// @param method The name of the factory method
    /// @throws NullPointerException     If `clazz` or `method` is null
    /// @throws IllegalArgumentException If `method` is not a valid Java identifier
    public FXMLFactoryMethod {
        Objects.requireNonNull(clazz, "`clazz` cannot be null");
        Objects.requireNonNull(method, "`method` cannot be null");
        if (FXMLUtils.isInvalidIdentifierName(method)) {
            throw new IllegalArgumentException(
                    "`factoryMethod` must be a valid Java identifier: %s".formatted(method)
            );
        }
    }
}
