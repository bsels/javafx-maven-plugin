package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Optional;

public record FXMLValue(
        Optional<FXMLIdentifier> identifier,
        Class<?> clazz,
        String value
) implements AbstractFXMLValue {
}
