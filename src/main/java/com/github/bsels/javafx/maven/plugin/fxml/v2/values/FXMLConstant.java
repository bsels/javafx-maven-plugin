package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.lang.reflect.Type;
import java.util.Objects;

public record FXMLConstant(Class<?> clazz, String identifier, Type constantType) implements AbstractFXMLValue {
    public FXMLConstant {
        Objects.requireNonNull(clazz, "Class must not be null");
        Objects.requireNonNull(identifier, "Identifier must not be null");
        Objects.requireNonNull(constantType, "Constant type must not be null");
    }
}
