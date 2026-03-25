package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

/// Base interface for all FXML values.
public sealed interface AbstractFXMLValue
        permits AbstractFXMLObject, FXMLCollection, FXMLConstant, FXMLCopy, FXMLExpression, FXMLInclude,
                FXMLInlineScript, FXMLLiteral, FXMLMap, FXMLMethod, FXMLObject, FXMLReference, FXMLResource,
                FXMLTranslation, FXMLValue {

    /// Validates that the provided identifier is a valid Java identifier.
    ///
    /// @param identifier The identifier to validate.
    /// @throws NullPointerException     if `identifier` is `null`.
    /// @throws IllegalArgumentException if `identifier` is not a valid Java identifier.
    static void validateIdentifier(String identifier)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        if (Utils.isInvalidIdentifierName(identifier)) {
            throw new IllegalArgumentException(
                    "`identifier` must be a valid Java identifier: %s".formatted(identifier)
            );
        }
    }

    /// Validates the provided generic string to ensure it is not null or blank.
    ///
    /// @param generic The generic string to validate.
    /// @throws NullPointerException     if `generic` is `null`.
    /// @throws IllegalArgumentException if `generic` is blank.
    static void validateGeneric(String generic)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(generic, "`generic` must not be null");
        if (generic.isBlank()) {
            throw new IllegalArgumentException("`generic` must not be blank");
        }
    }
}
