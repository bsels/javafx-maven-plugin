package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents a parsed FXML document.
///
/// @param root         The root object of the document.
/// @param controller   The controller class name, if any.
/// @param scriptEngine The script engine name, if any.
/// @param imports      The list of imported classes or packages.
public record FXMLDocument(
        FXMLObject root,
        Optional<String> controller,
        Optional<String> scriptEngine,
        List<String> imports
) {
    /// Compact constructor to validate the FXML document components.
    ///
    /// @param root         The root object of the document.
    /// @param controller   The controller class name, if any.
    /// @param scriptEngine The script engine name, if any.
    /// @param imports      The list of imported classes or packages.
    /// @throws NullPointerException if any required parameter is null.
    public FXMLDocument {
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(controller, "`controller` must not be null");
        Objects.requireNonNull(scriptEngine, "`scriptEngine` must not be null");
        imports = List.copyOf(Objects.requireNonNullElseGet(imports, List::of));
    }
}
