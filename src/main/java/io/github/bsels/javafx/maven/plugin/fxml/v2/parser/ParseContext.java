package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;

import java.util.Objects;
import java.util.Optional;

/// Represents the parsing context for an FXML document.
///
/// This record encapsulates all necessary information required for parsing and building FXML structures,
/// ensuring proper linking between parsed elements and their associated metadata.
///
/// @param structure          The parsed structure of the FXML document.
/// @param buildContext       The context used during the construction phase.
/// @param classAndIdentifier Carries information about the involved class and its identifier.
/// @param type               Specifies the type of the FXML element being processed.
/// @param factoryMethod      Represents the factory method, if any, that may be invoked.
record ParseContext(
        ParsedXMLStructure structure,
        BuildContext buildContext,
        ClassAndIdentifier classAndIdentifier,
        FXMLType type,
        Optional<FXMLFactoryMethod> factoryMethod
) {

    /// Constructor for the `ParseContext` record.
    ///
    /// The logic ensures that all required components are not `null`.
    ///
    /// @param structure          The parsed structure of the FXML document.
    /// @param buildContext       The context used during the building.
    /// @param classAndIdentifier The class and identifier details.
    /// @param type               The type of the FXML object being parsed.
    /// @param factoryMethod      The factory method associated with the FXML object.
    /// @throws NullPointerException if any of the parameters are null.
    ParseContext {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Objects.requireNonNull(classAndIdentifier, "`classAndIdentifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
    }
}
