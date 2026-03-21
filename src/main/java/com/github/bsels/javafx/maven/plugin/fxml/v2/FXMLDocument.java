package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record FXMLDocument(
        FXMLObject root,
        Optional<String> controller,
        Optional<String> scriptEngine,
        List<String> imports
) {
    public FXMLDocument {
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(controller, "`controller` must not be null");
        Objects.requireNonNull(scriptEngine, "`scriptEngine` must not be null");
        imports = List.copyOf(Objects.requireNonNullElseGet(imports, List::of));
    }
}
