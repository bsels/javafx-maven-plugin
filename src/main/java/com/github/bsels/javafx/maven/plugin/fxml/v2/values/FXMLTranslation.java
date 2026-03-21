package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML translation.
///
/// @param translationKey The translation key.
public record FXMLTranslation(String translationKey) implements AbstractFXMLValue {

    /// Compact constructor to validate the translation key.
    ///
    /// @param translationKey The translation key.
    /// @throws NullPointerException     if the translation key is null.
    /// @throws IllegalArgumentException if the translation key is blank.
    public FXMLTranslation {
        Objects.requireNonNull(translationKey, "translationKey cannot be null");
        if (translationKey.isBlank()) {
            throw new IllegalArgumentException("translationKey cannot be blank");
        }
    }
}
