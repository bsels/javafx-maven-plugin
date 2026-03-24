package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.Objects;

public record FXMLConstructorProperty(
        String name,
        FXMLType type,
        AbstractFXMLValue value
) implements FXMLProperty<AbstractFXMLValue> {

    public FXMLConstructorProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
