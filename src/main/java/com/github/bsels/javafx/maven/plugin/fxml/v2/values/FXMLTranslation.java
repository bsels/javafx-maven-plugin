package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an FXML translation.
///
/// @param translationKey The translation key.
public record FXMLTranslation(String translationKey) implements AbstractFXMLValue {

    /// Compact constructor to validate the translation key.
    ///
    /// @param translationKey The translation key.
    /// @throws NullPointerException     if `translationKey` is `null`.
    /// @throws IllegalArgumentException if `translationKey` is blank.
    public FXMLTranslation {
        Objects.requireNonNull(translationKey, "`translationKey` must not be null");
        if (translationKey.isBlank()) {
            throw new IllegalArgumentException("`translationKey` must not be blank");
        }
    }
}
