package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;

import java.util.Optional;

/// Represents a functional interface designed to handle and process special FXML elements encountered during
/// the parsing and building of an FXML document.
/// The interface defines a single abstract method intended to handle custom processing logic for specific FXML
/// elements, allowing for the generation of corresponding values or actions.
///
/// Implementations of this interface provide a mechanism to extend or customize the default behavior of
/// FXML document parsing.
///
/// The handler receives relevant contextual information including the document parser instance,
/// the segment of the FXML document to be processed, and the build context,
/// enabling comprehensive and flexible handling of special FXML elements.
///
/// This interface follows the contract of a functional interface and can be implemented using a lambda expression
/// or method reference.
@FunctionalInterface
interface SpecialFXElementHandler {
    /// Handles processing of a special FXML element based on the provided inputs.
    ///
    /// @param instance     The [FXMLDocumentParser] instance responsible for parsing the document.
    /// @param structure    The [ParsedXMLStructure] representing the current segment of the FXML document.
    /// @param buildContext The [BuildContext] providing contextual information during the building process.
    /// @return An [Optional] containing an [AbstractFXMLValue] if the processing completes successfully, or an empty [Optional] if no value could be generated.
    Optional<? extends AbstractFXMLValue> handle(
            FXMLDocumentParser instance,
            ParsedXMLStructure structure,
            BuildContext buildContext
    );
}
