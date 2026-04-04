package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

/// Represents features that can be present in a generated FXML source code.
enum Feature {
    /// Indicates that the generated class should be abstract.
    ABSTRACT_CLASS,
    /// Indicates that the generated class uses a resource bundle.
    RESOURCE_BUNDLE,
    /// Indicates that the generated class needs the internal string-to-URL conversion method.
    STRING_TO_URL_METHOD,
    /// Indicates that the generated class needs the internal string-to-URI conversion method.
    STRING_TO_URI_METHOD,
    /// Indicates that the generated class needs the internal string-to-Path conversion method.
    STRING_TO_PATH_METHOD,
    /// Indicates that the generated class needs the internal string-to-File conversion method.
    STRING_TO_FILE_METHOD
}
