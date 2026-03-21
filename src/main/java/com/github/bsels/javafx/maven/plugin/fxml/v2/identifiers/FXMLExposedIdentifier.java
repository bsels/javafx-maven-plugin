package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import com.github.bsels.javafx.maven.plugin.fxml.v2.Utils;

import java.util.Objects;

public record FXMLExposedIdentifier(String name) implements FXMLIdentifier {

    public FXMLExposedIdentifier {
        Objects.requireNonNull(name, "Name must not be null");
        if (Utils.isInvalidIdentifierName(name)) {
            throw new IllegalArgumentException("Name must be a valid Java identifier: %s".formatted(name));
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
