package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// An FXML expression (e.g., using `${expression}`).
///
/// @param expression The expression string
public record FXMLExpression(String expression) implements AbstractFXMLValue {

    /// Initializes a new [FXMLExpression] record instance.
    ///
    /// @param expression The expression string
    /// @throws NullPointerException If `expression` is null
    public FXMLExpression {
        Objects.requireNonNull(expression, "`expression` must not be null");
    }
}
