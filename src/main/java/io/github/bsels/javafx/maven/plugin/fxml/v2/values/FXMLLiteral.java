package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// An FXML literal value as a string.
///
/// @param value The string value associated with this literal
public record FXMLLiteral(String value) implements AbstractFXMLValue {

    /// Initializes a new [FXMLLiteral] record instance.
    ///
    /// @param value The string value associated with this literal
    /// @throws NullPointerException If `value` is null
    public FXMLLiteral {
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
