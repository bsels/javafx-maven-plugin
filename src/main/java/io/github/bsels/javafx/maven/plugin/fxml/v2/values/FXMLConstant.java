package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// An FXML constant (e.g., using `fx:constant`).
///
/// @param clazz        The class defining the constant
/// @param identifier   The constant identifier
/// @param constantType The type of the constant
public record FXMLConstant(FXMLClassType clazz, String identifier, FXMLType constantType) implements AbstractFXMLValue {

    /// Initializes a new [FXMLConstant] record instance.
    ///
    /// @param clazz        The class defining the constant
    /// @param identifier   The constant identifier
    /// @param constantType The type of the constant
    /// @throws NullPointerException     If `clazz`, `identifier`, or `constantType` is null
    /// @throws IllegalArgumentException If `identifier` is not a valid Java identifier
    public FXMLConstant {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(constantType, "`constantType` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(identifier)) {
            throw new IllegalArgumentException("`identifier` must be a valid Java identifier: %s".formatted(identifier));
        }
    }
}
