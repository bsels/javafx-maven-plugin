package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.util.Map;

/// Base interface for static (attached) FXML properties.
///
/// @param <T> The value type.
public sealed interface FXMLStaticProperty<T> extends FXMLProperty<T> permits FXMLStaticMultipleProperties, FXMLStaticSingleProperty {

    /// Returns the name of the static setter method.
    ///
    /// @return The static setter name.
    String staticSetter();

    /// Returns the class that defines the static property.
    ///
    /// @return The static property class.
    Class<?> staticClass();

    /// Constructs the setter string for the static property.
    ///
    /// @param typeMappings Map of class types to their custom name mappings.
    /// @return The constructed setter string.
    default String constructSetter(Map<Class<?>, String> typeMappings) {
        Class<?> clazz = staticClass();
        String mappedType = typeMappings.getOrDefault(clazz, clazz.getName());
        return "%s.%s".formatted(staticSetter(), mappedType);
    }
}
