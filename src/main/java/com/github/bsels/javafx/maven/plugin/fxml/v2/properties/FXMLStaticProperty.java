package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.util.Map;

public sealed interface FXMLStaticProperty<T> extends FXMLProperty<T> permits FXMLStaticMultipleProperties, FXMLStaticSingleProperty {

    String staticSetter();

    Class<?> staticClass();

    default String constructSetter(Map<Class<?>, String> typeMappings) {
        Class<?> clazz = staticClass();
        String mappedType = typeMappings.getOrDefault(clazz, clazz.getName());
        return "%s.%s".formatted(staticSetter(), mappedType);
    }
}
