package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;
import java.util.Objects;

/// Represents a field definition in an FXML-related context with details about its associated class, name,
/// and whether it is internal.
///
/// This record is immutable and ensures non-null values for its properties.
/// It is primarily used to store metadata about a field in the context
/// of JavaFX FXML processing and can be associated with FXML-related constructs.
public record FXMLField(Class<?> clazz, String name, boolean internal, List<String> generics) {

    /// Constructs an immutable [FXMLField] record object, ensuring the provided values are not null.
    ///
    /// @param clazz the class metadata associated with the field; must not be null
    /// @param name the name of the field; must not be null
    /// @param internal a boolean indicating whether the field is internal
    /// @param generics a list of generic types associated with the field; must not be null
    /// @throws NullPointerException if `clazz` or `name` is null
    public FXMLField {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        generics = List.copyOf(Objects.requireNonNull(generics, "`generics` must not be null"));
    }
}
