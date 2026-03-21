package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public record FXMLStaticSingleProperty(
        String name,
        Class<?> clazz,
        String staticSetter,
        Type type,
        AbstractFXMLValue value
) implements FXMLStaticProperty<AbstractFXMLValue> {

    public FXMLStaticSingleProperty {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(staticSetter, "`staticSetter` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }

    @Override
    public Optional<String> setter() {
        return Optional.of(staticSetter);
    }
}
