package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;

import java.util.Objects;
import java.util.Optional;

/// Parsing context for an FXML document.
/// Encapsulates information required for parsing and building FXML structures.
///
/// @param structure          The parsed XML structure
/// @param buildContext       The build context
/// @param classAndIdentifier Information about the class and its identifier
/// @param type               Type of the FXML element being processed
/// @param factoryMethod      Optional factory method
record ParseContext(
        ParsedXMLStructure structure,
        BuildContext buildContext,
        ClassAndIdentifier classAndIdentifier,
        FXMLType type,
        Optional<FXMLFactoryMethod> factoryMethod
) {

    /// Initializes a new [ParseContext] record instance.
    ///
    /// @param structure          The parsed XML structure
    /// @param buildContext       The build context
    /// @param classAndIdentifier The class and identifier details
    /// @param type               The type of the FXML object
    /// @param factoryMethod      The factory method
    /// @throws NullPointerException If any parameter is null
    ParseContext {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Objects.requireNonNull(classAndIdentifier, "`classAndIdentifier` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
    }
}
