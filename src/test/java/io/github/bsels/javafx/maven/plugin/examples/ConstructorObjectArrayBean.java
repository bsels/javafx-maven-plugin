package io.github.bsels.javafx.maven.plugin.examples;

import javafx.beans.NamedArg;

public class ConstructorObjectArrayBean {
    private final Object[] values;

    public ConstructorObjectArrayBean(@NamedArg("values") Object[] values) {
        this.values = values;
    }

    public Object[] getValues() {
        return values;
    }
}
