package io.github.bsels.javafx.maven.plugin.examples;

public class ReadOnlyArrayBean {
    private final String[] values = new String[0];

    public String[] getValues() {
        return values;
    }
}
