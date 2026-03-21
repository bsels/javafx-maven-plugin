package com.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

public record FXMLFileScript(Path path, Charset charset) implements FXMLScript {
    public FXMLFileScript {
        Objects.requireNonNull(path, "`path` must not be null");
        Objects.requireNonNull(charset, "`charset` must not be null");
    }
}
