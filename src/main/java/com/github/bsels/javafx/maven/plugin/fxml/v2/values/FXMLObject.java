package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;

import java.util.List;
import java.util.Optional;

public record FXMLObject(
        FXMLIdentifier identifier,
        Class<?> clazz,
        Optional<String> factoryMethod,
        List<String> generics,
        List<FXMLProperty<?>> properties
) implements AbstractFXMLValue {

    public FXMLObject {
        properties = List.copyOf(properties);
    }
}
