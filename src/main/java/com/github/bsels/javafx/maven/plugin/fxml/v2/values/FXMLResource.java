package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

public record FXMLResource(String name) implements AbstractFXMLValue {

    public FXMLResource {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
