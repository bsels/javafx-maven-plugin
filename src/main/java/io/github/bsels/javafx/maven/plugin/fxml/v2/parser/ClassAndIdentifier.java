package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Objects;

/// A Java class and its associated FXML identifier.
///
/// @param clazz      The class type
/// @param identifier The FXML identifier
record ClassAndIdentifier(Class<?> clazz, FXMLIdentifier identifier) {

    /// Initializes a new [ClassAndIdentifier] record instance.
    ///
    /// @param clazz      The class type
    /// @param identifier The FXML identifier
    /// @throws NullPointerException If `clazz` or `identifier` is null
    ClassAndIdentifier {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(identifier, "`identifier` must not be null");
    }
}
