package com.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import java.util.Optional;

public sealed interface FXMLObjectProperty<T> extends FXMLProperty<T> permits FXMLMultipleProperties, FXMLSingleProperty {

    Optional<String> setter();
}
