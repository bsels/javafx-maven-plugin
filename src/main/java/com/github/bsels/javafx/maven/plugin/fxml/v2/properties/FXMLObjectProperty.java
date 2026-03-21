package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.util.Optional;

/// Base interface for standard FXML properties.
///
/// @param <T> The value type.
public sealed interface FXMLObjectProperty<T> extends FXMLProperty<T> permits FXMLMultipleProperties, FXMLSingleProperty {

    /// Returns the name of the setter method, if any.
    ///
    /// @return The setter name.
    Optional<String> setter();
}
