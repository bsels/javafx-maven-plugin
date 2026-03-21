package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

public record FXMLCopy(String name) implements AbstractFXMLValue {
    public FXMLCopy {
        Objects.requireNonNull(name, "name cannot be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("name must be a valid Java identifier: %s".formatted(name));
        }
    }
}
