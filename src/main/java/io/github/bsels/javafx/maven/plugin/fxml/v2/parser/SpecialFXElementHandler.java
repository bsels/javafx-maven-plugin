package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;

import java.util.Optional;

/// Functional interface for handling special FXML elements during parsing.
///
/// Implementations provide custom processing logic for specific FXML elements,
/// allowing for the generation of corresponding values or actions.
@FunctionalInterface
interface SpecialFXElementHandler {
    /// Processes a special FXML element.
    ///
    /// @param instance     The [FXMLDocumentParser] instance
    /// @param structure    The [ParsedXMLStructure] representing the FXML element
    /// @param buildContext The [BuildContext] for the current document
    /// @return An [Optional] containing the generated [AbstractFXMLValue]
    Optional<? extends AbstractFXMLValue> handle(
            FXMLDocumentParser instance,
            ParsedXMLStructure structure,
            BuildContext buildContext
    );
}
