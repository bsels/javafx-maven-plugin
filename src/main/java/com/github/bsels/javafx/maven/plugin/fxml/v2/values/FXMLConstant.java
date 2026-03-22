package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents an FXML constant (e.g., using fx:constant).
///
/// @param clazz        The class defining the constant.
/// @param identifier   The constant identifier.
/// @param constantType The type of the constant.
public record FXMLConstant(Class<?> clazz, String identifier, Type constantType) implements AbstractFXMLValue {
    /// Compact constructor to validate the constant components.
    ///
    /// @param clazz        The class defining the constant.
    /// @param identifier   The constant identifier.
    /// @param constantType The type of the constant.
    /// @throws NullPointerException if any parameter is null.
    public FXMLConstant {
        Objects.requireNonNull(clazz, "Class must not be null");
        Objects.requireNonNull(identifier, "Identifier must not be null");
        Objects.requireNonNull(constantType, "Constant type must not be null");
        AbstractFXMLValue.validateIdentifier(identifier);
    }
}
