package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

public record FXMLInclude(String sourceFile) implements AbstractFXMLValue {

    public FXMLInclude {
        Objects.requireNonNull(sourceFile, "`sourceFile` cannot be null");
    }
}
