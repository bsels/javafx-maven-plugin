package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.util.List;
import java.util.Optional;

/// Base interface for FXML objects that can contain properties and generics.
public sealed interface AbstractFXMLObject extends AbstractFXMLValue permits FXMLCollection, FXMLMap, FXMLObject {

    /// Retrieves the unique FXML identifier associated with this object.
    ///
    /// @return The FXMLIdentifier representing the unique identifier for this object.
    FXMLIdentifier identifier();

    /// Retrieves the class type associated with the FXML object.
    ///
    /// @return The `Class<?>` representing the class type of this FXML object.
    Class<?> clazz();

    /// Retrieves an optional factory method name associated with the FXML object.
    /// The factory method, if present, is expected to be a valid Java identifier.
    ///
    /// @return An `Optional` containing the factory method name if specified, or an empty `Optional` if not present.
    Optional<String> factoryMethod();

    /// Retrieves a list of generic type names used within the FXML object.
    ///
    /// @return A list of strings representing the names of generic types, or an empty list if no generics are defined.
    List<String> generics();
}
