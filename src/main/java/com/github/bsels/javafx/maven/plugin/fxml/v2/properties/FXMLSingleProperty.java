package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public record FXMLSingleProperty(
        String name,
        Optional<String> setter,
        Type type,
        AbstractFXMLValue value
) implements FXMLObjectProperty<AbstractFXMLValue> {

    public FXMLSingleProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
