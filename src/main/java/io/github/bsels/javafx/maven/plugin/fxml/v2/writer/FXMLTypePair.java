package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// Represents a pairing of two [FXMLType] instances,
/// typically used to associate a primary type with its corresponding interface type.
///
/// This class ensures that neither the primary [FXMLType] nor the interface [FXMLType] is null during instantiation.
///
/// @param type          The primary [FXMLType] instance.
/// @param interfaceType The [FXMLType] instance representing the associated interface type.
record FXMLTypePair(FXMLType type, FXMLType interfaceType) {

    /// Constructs an `FXMLTypePair` record instance, ensuring that neither the `type` nor the `interfaceType` is null.
    ///
    /// @param type          The primary instance of [FXMLType].
    /// @param interfaceType The [FXMLType] instance representing the associated interface type.
    /// @throws NullPointerException If `type` or `interfaceType` is null.
    FXMLTypePair {
        Objects.requireNonNull(type, "`type` cannot be null");
        Objects.requireNonNull(interfaceType, "`interfaceType` cannot be null");
    }
}
