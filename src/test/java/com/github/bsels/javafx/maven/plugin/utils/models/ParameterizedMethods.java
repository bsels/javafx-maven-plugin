package com.github.bsels.javafx.maven.plugin.utils.models;

import java.util.ArrayList;
import java.util.function.Function;

public class ParameterizedMethods {

    public <S, T extends Number> void setParameterizedMethod(Function<? super S, T> input) {
    }

    public void setHandler(Runnable handler) {
    }

    public void setNotParameterizedFunctionalInterface(ArrayList<String> input) {
    }

    public void setNotFunctionalInterface(String input) {
    }
}
