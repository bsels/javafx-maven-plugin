package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

/// A sealed interface used as a container for either an [FXMLType] or a reference to another identifier.
sealed interface TypeWrapper permits FXMLTypeWrapper, ReferenceWrapper {

}
