package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.Objects;

/// An FXML type representing a Java array.
///
/// @param componentType The type of the components in the array
public record FXMLArrayType(FXMLType componentType) implements FXMLType {

    /// Initializes a new [FXMLArrayType] record instance.
    ///
    /// @param componentType The type of the components in the array
    /// @throws NullPointerException If `componentType` is null
    public FXMLArrayType {
        Objects.requireNonNull(componentType, "`componentType` must not be null");
    }
}
