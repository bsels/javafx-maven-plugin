package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents an object instantiated from FXML.
///
/// @param identifier    The identifier of the object.
/// @param clazz         The class of the object.
/// @param factoryMethod The factory method name, if any.
/// @param generics      The generic type arguments.
/// @param properties    The list of properties of the object.
/// @param definitions   The list of objects defined within this object.
/// @param scripts       The list of scripts associated with this object.
public record FXMLObject(
        FXMLIdentifier identifier,
        Class<?> clazz,
        Optional<String> factoryMethod,
        List<String> generics,
        List<FXMLProperty<?>> properties,
        List<FXMLObject> definitions,
        List<FXMLScript> scripts
) implements AbstractFXMLValue {

    /// Compact constructor to validate the FXML object components.
    ///
    /// @param identifier    The identifier of the object.
    /// @param clazz         The class of the object.
    /// @param factoryMethod The factory method name, if any.
    /// @param generics      The generic type arguments.
    /// @param properties    The list of properties of the object.
    /// @param definitions   The list of objects defined within this object.
    /// @param scripts       The list of scripts associated with this object.
    /// @throws NullPointerException if any required parameter is null.
    public FXMLObject {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        generics = List.copyOf(Objects.requireNonNullElseGet(generics, List::of));
        properties = List.copyOf(Objects.requireNonNullElseGet(properties, List::of));
        definitions = List.copyOf(Objects.requireNonNullElseGet(definitions, List::of));
        scripts = List.copyOf(Objects.requireNonNullElseGet(scripts, List::of));
    }
}
