package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

/// Base interface for all FXML values.
public sealed interface AbstractFXMLValue permits FXMLConstant, FXMLCopy, FXMLExpression, FXMLInclude, FXMLInlineScript, FXMLMethod, FXMLObject, FXMLReference, FXMLResource, FXMLTranslation, FXMLValue {
}
