package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

public record FXMLMapProperty(
        String name,
        String getter,
        Type type,
        Map<String, AbstractFXMLValue> value
) implements FXMLProperty<Map<String, AbstractFXMLValue>> {
    public FXMLMapProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(getter, "`getter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        value = Map.copyOf(Objects.requireNonNullElseGet(value, Map::of));
    }
}
