package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.List;
import java.util.Objects;

/// Represents an FXML method call (e.g., event handlers).
///
/// @param name       The method name.
/// @param parameters The list of parameter types.
/// @param returnType The return type of the method.
public record FXMLMethod(String name, List<FXMLType> parameters, FXMLType returnType)
        implements AbstractFXMLValue {

    /// Compact constructor to validate the method signature.
    ///
    /// @param name       The method name.
    /// @param parameters The list of parameter types.
    /// @param returnType The return type of the method.
    /// @throws NullPointerException     if `name` or `returnType` is `null`.
    /// @throws IllegalArgumentException if `name` is not a valid Java identifier.
    public FXMLMethod {
        Objects.requireNonNull(name, "`name` must not be null");
        if (FXMLUtils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("`name` must be a valid Java identifier: %s".formatted(name));
        }
        Objects.requireNonNull(returnType, "`returnType` must not be null");
        parameters = List.copyOf(Objects.requireNonNullElseGet(parameters, List::of));
    }
}
