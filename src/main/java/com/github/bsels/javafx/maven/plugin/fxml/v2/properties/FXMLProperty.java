package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.lang.reflect.Type;

public sealed interface FXMLProperty<T> permits FXMLObjectProperty, FXMLStaticProperty {
    String name();

    Type type();

    T value();
}
