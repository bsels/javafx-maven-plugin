package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public record FXMLStaticMultipleProperties(
        String name,
        Class<?> staticClass,
        String staticSetter,
        Type type,
        List<AbstractFXMLValue> value
) implements FXMLStaticProperty<List<AbstractFXMLValue>> {

    @Override
    public Optional<String> setter() {
        return Optional.of(staticSetter);
    }
}
