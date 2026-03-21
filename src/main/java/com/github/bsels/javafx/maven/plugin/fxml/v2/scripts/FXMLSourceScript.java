package com.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.util.Objects;

public record FXMLSourceScript(String source) implements FXMLScript {
    public FXMLSourceScript {
        Objects.requireNonNull(source, "`source` must not be null");
    }
}
