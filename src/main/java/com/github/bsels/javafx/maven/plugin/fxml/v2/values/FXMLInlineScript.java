package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

public record FXMLInlineScript(String script) implements AbstractFXMLValue {
    public FXMLInlineScript {
        Objects.requireNonNull(script, "script cannot be null");
        if (script.isBlank()) {
            throw new IllegalArgumentException("script cannot be blank");
        }
    }
}
