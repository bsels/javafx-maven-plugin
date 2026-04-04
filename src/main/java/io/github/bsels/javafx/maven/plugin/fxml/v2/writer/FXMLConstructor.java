package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Objects;

/// Represents an FXML constructor, which is a collection of [ConstructorProperty] objects.
///
/// This record encapsulates information about the properties required to instantiate a class via its constructor
/// or a factory method, as defined in FXML and identified by [@NamedArg] annotations.
///
/// @param properties The list of [ConstructorProperty] required by the constructor. Must not be null.
record FXMLConstructor(List<ConstructorProperty> properties) {

    /// Constructs a new `FXMLConstructor` and ensures the properties list is not null and immutable.
    ///
    /// @param properties The list of [ConstructorProperty] required.
    /// @throws NullPointerException If the properties list is null.
    public FXMLConstructor {
        Objects.requireNonNull(properties, "`properties` must not be null");
        properties = List.copyOf(properties);
    }
}
