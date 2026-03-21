package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FXMLMethod(String name, List<Type> parameters, Type returnType, Map<String, String> namedGenerics)
        implements AbstractFXMLValue {

    public FXMLMethod {
        Objects.requireNonNull(name, "name cannot be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("name must be a valid Java identifier: %s".formatted(name));
        }
        Objects.requireNonNull(returnType, "returnType cannot be null");
        parameters = List.copyOf(Objects.requireNonNullElseGet(parameters, List::of));
        namedGenerics = Map.copyOf(Objects.requireNonNullElseGet(namedGenerics, Map::of));

        for (Type parameter : parameters) {
            Objects.requireNonNull(parameter, "parameter cannot be null");
        }

        for (Map.Entry<String, String> entry : namedGenerics.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "namedGenerics key cannot be null");
            Objects.requireNonNull(entry.getValue(), "namedGenerics value cannot be null");
        }
    }
}
