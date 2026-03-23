package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML expression (e.g., using ${expression}).
///
/// @param expression The expression string.
public record FXMLExpression(String expression) implements AbstractFXMLValue {
    /// Compact constructor to validate the expression.
    ///
    /// @param expression The expression string.
    /// @throws NullPointerException if `expression` is `null`.
    public FXMLExpression {
        Objects.requireNonNull(expression, "`expression` must not be null");
    }
}
