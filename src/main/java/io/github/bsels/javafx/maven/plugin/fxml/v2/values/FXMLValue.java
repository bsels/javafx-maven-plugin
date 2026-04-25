package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;
import java.util.Optional;

/// A simple FXML value.
///
/// @param identifier The identifier of the value, if any
/// @param type       The type of the value
/// @param value      The value string
public record FXMLValue(
        Optional<FXMLIdentifier> identifier,
        FXMLType type,
        String value
) implements AbstractFXMLValue {

    /// Initializes a new [FXMLValue] record instance.
    ///
    /// @param identifier The identifier of the value, if any
    /// @param type       The type of the value
    /// @param value      The value string
    /// @throws NullPointerException If `identifier`, `type`, or `value` is null
    public FXMLValue {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
