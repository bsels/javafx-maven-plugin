package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.Optional;

/// Base interface for FXML objects that can contain properties and generics.
public sealed interface AbstractFXMLObject extends AbstractFXMLValue permits FXMLCollection, FXMLMap, FXMLObject {

    /// Returns the unique FXML identifier associated with this object.
    ///
    /// @return The FXML identifier
    FXMLIdentifier identifier();

    /// Returns the type of the FXML object.
    ///
    /// @return The FXML type
    FXMLType type();

    /// Returns an optional factory method associated with the FXML object.
    ///
    /// @return An [Optional] containing the factory method
    Optional<FXMLFactoryMethod> factoryMethod();
}
