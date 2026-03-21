package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

public sealed interface AbstractFXMLValue permits FXMLConstant, FXMLCopy, FXMLInclude, FXMLInlineScript, FXMLMethod, FXMLObject, FXMLReference, FXMLResource, FXMLTranslation, FXMLValue {
}
