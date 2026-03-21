package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record FXMLMultipleProperties(
        String name,
        Optional<String> setter,
        Type type,
        List<AbstractFXMLValue> value
) implements FXMLObjectProperty<List<AbstractFXMLValue>> {

    public FXMLMultipleProperties {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(setter, "`setter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
