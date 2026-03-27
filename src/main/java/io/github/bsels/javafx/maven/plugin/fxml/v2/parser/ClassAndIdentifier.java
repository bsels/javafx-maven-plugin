package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.Objects;

/// Holds a class and its associated FXML identifier.
///
/// This record is used to associate a Java class with its identifier in the FXML document.
///
/// @param clazz      The class type.
/// @param identifier The FXML identifier.
record ClassAndIdentifier(Class<?> clazz, FXMLIdentifier identifier) {
    /// Compact constructor to validate the class and identifier.
    ///
    /// The logic ensures that both the `clazz` and `identifier` are not `null`.
    ///
    /// @param clazz      The class type.
    /// @param identifier The FXML identifier.
    /// @throws NullPointerException if `clazz` or `identifier` is `null`.
    ClassAndIdentifier {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(identifier, "`identifier` must not be null");
    }
}
