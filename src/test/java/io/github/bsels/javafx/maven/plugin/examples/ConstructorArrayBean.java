package io.github.bsels.javafx.maven.plugin.examples;

import javafx.beans.NamedArg;

public class ConstructorArrayBean {
    private final String[] values;

    public ConstructorArrayBean(@NamedArg("values") String[] values) {
        this.values = values;
    }

    public String[] getValues() {
        return values;
    }
}
