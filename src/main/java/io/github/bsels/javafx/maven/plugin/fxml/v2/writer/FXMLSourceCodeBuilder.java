package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import org.apache.maven.plugin.logging.Log;

import java.util.Objects;

public final class FXMLSourceCodeBuilder {
    private final Log log;
    private final FXMLClassCounterHelper classCounterHelper;

    public FXMLSourceCodeBuilder(Log log) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.classCounterHelper = new FXMLClassCounterHelper();
    }
}
