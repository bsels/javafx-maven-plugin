package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

/// Represents an FXML expression (e.g., using ${expression}).
///
/// @param expression The expression string.
public record FXMLExpression(String expression) {
    /// Compact constructor to validate the expression.
    ///
    /// @param expression The expression string.
    /// @throws NullPointerException if the expression is null.
    public FXMLExpression {
        java.util.Objects.requireNonNull(expression, "`expression` must not be null");
    }
}
