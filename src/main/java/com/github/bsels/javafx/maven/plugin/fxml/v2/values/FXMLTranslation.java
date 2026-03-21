package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

public record FXMLTranslation(String translationKey) implements AbstractFXMLValue {

    public FXMLTranslation {
        Objects.requireNonNull(translationKey, "translationKey cannot be null");
        if (translationKey.isBlank()) {
            throw new IllegalArgumentException("translationKey cannot be blank");
        }
    }
}
