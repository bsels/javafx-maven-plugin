package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

/// Features that can be present in generated FXML source code.
enum Feature {
    /// Indicates the generated class should be abstract.
    ABSTRACT_CLASS,
    /// Indicates the generated class uses a resource bundle.
    RESOURCE_BUNDLE,
    /// Indicates the generated class needs the internal string-to-URL conversion method.
    STRING_TO_URL_METHOD,
    /// Indicates the generated class needs the internal string-to-URI conversion method.
    STRING_TO_URI_METHOD,
    /// Indicates the generated class needs the internal string-to-Path conversion method.
    STRING_TO_PATH_METHOD,
    /// Indicates the generated class needs the internal string-to-File conversion method.
    STRING_TO_FILE_METHOD,
    /// Indicates that the generated class should bind its internal objects to the fields of a controller.
    BIND_CONTROLLER
}
