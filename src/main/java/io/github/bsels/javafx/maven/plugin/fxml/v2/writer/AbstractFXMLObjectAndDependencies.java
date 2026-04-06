package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;

import java.util.List;
import java.util.Objects;

/// Represents an FXML object construction and its dependencies.
///
/// @param object                The [AbstractFXMLObject] being constructed.
/// @param constructorProperties The properties passed to the constructor.
/// @param dependencies          The identifiers this object depends on.
record AbstractFXMLObjectAndDependencies(
        AbstractFXMLObject object,
        List<FXMLConstructorProperty> constructorProperties,
        List<FXMLIdentifier> dependencies
) implements Constructions {

    /// Validates the object and initializes the lists.
    ///
    /// @param object                The [AbstractFXMLObject] being constructed.
    /// @param constructorProperties The properties passed to the constructor.
    /// @param dependencies          The identifiers this object depends on.
    AbstractFXMLObjectAndDependencies {
        Objects.requireNonNull(object, "object");
        constructorProperties = List.copyOf(Objects.requireNonNullElseGet(constructorProperties, List::of));
        dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
    }
}
