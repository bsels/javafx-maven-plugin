package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

/// Base interface for all FXML values.
public sealed interface AbstractFXMLValue
        permits AbstractFXMLObject, FXMLCollection, FXMLConstant, FXMLCopy, FXMLExpression, FXMLInclude,
                FXMLInlineScript, FXMLLiteral, FXMLMap, FXMLMethod, FXMLObject, FXMLReference, FXMLResource,
                FXMLTranslation, FXMLValue {
}
