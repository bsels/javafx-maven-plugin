package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLFactoryMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Optional;

/// Base interface for FXML objects that can contain properties and generics.
public sealed interface AbstractFXMLObject extends AbstractFXMLValue permits FXMLCollection, FXMLMap, FXMLObject {

    /// Retrieves the unique FXML identifier associated with this object.
    ///
    /// @return The FXMLIdentifier representing the unique identifier for this object.
    FXMLIdentifier identifier();

    /// Retrieves the type of the FXML object.
    ///
    /// @return The FXMLType representing the class or generic type of the object.
    FXMLType type();

    /// Retrieves an optional factory method associated with the FXML object.
    /// The factory method, if present, is expected to be a valid Java identifier.
    ///
    /// @return An `Optional` containing the factory method if specified, or an empty `Optional` if not present.
    Optional<FXMLFactoryMethod> factoryMethod();
}
