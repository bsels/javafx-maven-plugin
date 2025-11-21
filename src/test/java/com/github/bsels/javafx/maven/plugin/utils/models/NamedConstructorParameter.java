package com.github.bsels.javafx.maven.plugin.utils.models;

import javafx.beans.NamedArg;

public class NamedConstructorParameter {

    public NamedConstructorParameter(@NamedArg("test") String test) {
    }

    public NamedConstructorParameter(@NamedArg("test") int test) {
    }
}
