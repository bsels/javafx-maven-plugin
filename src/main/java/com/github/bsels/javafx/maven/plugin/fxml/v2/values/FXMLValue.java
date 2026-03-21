package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Optional;

/// Represents a simple FXML value.
///
/// @param identifier The identifier of the value, if any.
/// @param clazz      The class of the value.
/// @param value      The value string.
public record FXMLValue(
        Optional<FXMLIdentifier> identifier,
        Class<?> clazz,
        String value
) implements AbstractFXMLValue {
    /// Compact constructor to validate the FXML value.
    ///
    /// @param identifier The identifier of the value, if any.
    /// @param clazz      The class of the value.
    /// @param value      The value string.
    /// @throws NullPointerException if any parameter is null.
    public FXMLValue {
        java.util.Objects.requireNonNull(identifier, "`identifier` must not be null");
        java.util.Objects.requireNonNull(clazz, "`clazz` must not be null");
        java.util.Objects.requireNonNull(value, "`value` must not be null");
    }
}
