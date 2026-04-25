package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// An FXML translation.
///
/// @param translationKey The translation key
public record FXMLTranslation(String translationKey) implements AbstractFXMLValue {

    /// Initializes a new [FXMLTranslation] record instance.
    ///
    /// @param translationKey The translation key
    /// @throws NullPointerException     If `translationKey` is null
    /// @throws IllegalArgumentException If `translationKey` is blank
    public FXMLTranslation {
        Objects.requireNonNull(translationKey, "`translationKey` must not be null");
        if (translationKey.isBlank()) {
            throw new IllegalArgumentException("`translationKey` must not be blank");
        }
    }
}
