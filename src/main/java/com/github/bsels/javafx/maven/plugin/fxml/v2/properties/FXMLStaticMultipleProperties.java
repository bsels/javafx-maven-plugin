package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.lang.reflect.Type;
import java.util.List;

/// Represents a static (attached) FXML property that can have multiple values.
///
/// @param name         The property name.
/// @param staticClass  The class defining the static property.
/// @param staticSetter The name of the static setter method.
/// @param type         The property type.
/// @param value        The list of values.
public record FXMLStaticMultipleProperties(
        String name,
        Class<?> staticClass,
        String staticSetter,
        Type type,
        List<AbstractFXMLValue> value
) implements FXMLStaticProperty<List<AbstractFXMLValue>> {

}
