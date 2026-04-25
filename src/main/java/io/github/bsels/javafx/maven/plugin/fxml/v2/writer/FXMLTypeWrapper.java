package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Objects;

/// A wrapper for an [FXMLType].
///
/// @param type The [FXMLType] being wrapped.
public record FXMLTypeWrapper(FXMLType type) implements TypeWrapper {

    /// Constructs an `FXMLTypeWrapper` and ensures the type is not null.
    ///
    /// @param type The [FXMLType] being wrapped.
    public FXMLTypeWrapper {
        Objects.requireNonNull(type, "`type` must not be null");
    }
}
