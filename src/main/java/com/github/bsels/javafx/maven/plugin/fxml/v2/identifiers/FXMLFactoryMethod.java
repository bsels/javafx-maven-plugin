package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;

import java.util.Objects;

/// Represents a static factory method used in FXML with the `fx:factory` attribute.
///
/// A factory method is a static method that returns an instance of the class that defines it.
/// It is used to create objects that cannot be instantiated via a default constructor.
///
/// @param clazz  The class that defines the factory method.
/// @param method The name of the factory method.
public record FXMLFactoryMethod(Class<?> clazz, String method) {
    /// Constructs an [FXMLFactoryMethod] instance.
    ///
    /// The constructor validates that both [#clazz] and [#method] are non-null and that
    /// [#method] is a valid Java identifier.
    ///
    /// @param clazz  The class that defines the factory method.
    /// @param method The name of the factory method.
    /// @throws NullPointerException     if [#clazz] or [#method] is null.
    /// @throws IllegalArgumentException if [#method] is not a valid Java identifier.
    public FXMLFactoryMethod {
        Objects.requireNonNull(clazz, "`clazz` cannot be null");
        Objects.requireNonNull(method, "`method` cannot be null");
        if (FXMLUtils.isInvalidIdentifierName(method)) {
            throw new IllegalArgumentException(
                    "`factoryMethod` must be a valid Java identifier: %s".formatted(method)
            );
        }
    }
}
