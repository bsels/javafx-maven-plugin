package io.github.bsels.javafx.maven.plugin.examples;

import java.util.Map;

public class MetaDataHolder {
    private Map<String, String> metaData;
    public void setMetaData(Map<String, String> map) {
        this.metaData = map;
        // This is for testing
    }
}
