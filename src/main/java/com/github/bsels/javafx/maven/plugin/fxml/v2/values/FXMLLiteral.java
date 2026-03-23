package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML literal value as a string.
/// This class is a record that implements the [AbstractFXMLValue] interface and encapsulates a non-null string value.
/// It is primarily used to define static or constant string values within an FXML structure.
///
/// The `FXMLLiteral` ensures that the provided string value is non-null upon creation.
///
/// @param value The string value associated with this literal. Cannot be null.
public record FXMLLiteral(String value) implements AbstractFXMLValue {

    /// Compact constructor to validate the literal value.
    ///
    /// @param value The string value associated with this literal.
    /// @throws NullPointerException if `value` is `null`.
    public FXMLLiteral {
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
