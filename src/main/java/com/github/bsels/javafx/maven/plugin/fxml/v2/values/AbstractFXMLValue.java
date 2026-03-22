package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

/// Base interface for all FXML values.
public sealed interface AbstractFXMLValue
        permits AbstractFXMLObject, FXMLCollection, FXMLConstant, FXMLCopy, FXMLExpression, FXMLInclude,
        FXMLInlineScript, FXMLMap, FXMLMethod, FXMLObject, FXMLReference, FXMLResource, FXMLTranslation, FXMLValue {

    /// Validates that the provided identifier is a valid Java identifier.
    ///
    /// @param identifier The identifier to validate.
    /// @throws NullPointerException     if the identifier is null.
    /// @throws IllegalArgumentException if the identifier is not a valid Java identifier.
    static void validateIdentifier(String identifier)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(identifier, "Identifier must not be null");
        if (Utils.isInvalidIdentifierName(identifier)) {
            throw new IllegalArgumentException(
                    "Factory method must be a valid Java identifier: %s".formatted(identifier)
            );
        }
    }

    /// Validates the provided generic string to ensure it is not null or blank.
    ///
    /// @param generic The generic string to validate.
    /// @throws NullPointerException     if the generic string is null.
    /// @throws IllegalArgumentException if the generic string is blank.
    static void validateGeneric(String generic)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(generic, "Generic must not be null");
        if (generic.isBlank()) {
            throw new IllegalArgumentException("Generic must not be blank");
        }
    }
}
