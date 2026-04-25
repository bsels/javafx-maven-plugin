package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Objects;

/// An FXML constructor, consisting of a collection of [ConstructorProperty] objects.
///
/// Encapsulates information about the properties required to instantiate a class via its constructor
/// or a factory method, as defined in FXML and identified by [@NamedArg] annotations.
///
/// @param properties The list of [ConstructorProperty] required by the constructor
record FXMLConstructor(List<ConstructorProperty> properties) {

    /// Initializes a new [FXMLConstructor] instance.
    ///
    /// @param properties The list of [ConstructorProperty] required
    /// @throws NullPointerException If `properties` is null
    public FXMLConstructor {
        Objects.requireNonNull(properties, "`properties` must not be null");
        properties = List.copyOf(properties);
    }
}
