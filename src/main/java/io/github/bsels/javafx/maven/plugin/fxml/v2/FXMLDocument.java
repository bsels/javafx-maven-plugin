package io.github.bsels.javafx.maven.plugin.fxml.v2;

import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Represents a parsed FXML document.
///
/// @param className    The name of the class associated with this document.
/// @param root         The root object of the document.
/// @param controller   The controller class name, if any.
/// @param scriptEngine The script engine name, if any.
/// @param imports      The list of imported classes or packages.
/// @param definitions  The list of objects defined within this object.
/// @param scripts      The list of scripts associated with this object.
public record FXMLDocument(
        String className,
        AbstractFXMLObject root,
        Optional<FXMLController> controller,
        Optional<String> scriptEngine,
        List<String> imports,
        List<AbstractFXMLValue> definitions,
        List<FXMLScript> scripts
) {
    /// Compact constructor to validate the FXML document components.
    ///
    /// @param className    The name of the class associated with this document.
    /// @param root         The root object of the document.
    /// @param controller   The controller class name, if any.
    /// @param scriptEngine The script engine name, if any.
    /// @param imports      The list of imported classes or packages.
    /// @param definitions  The list of objects defined within this object.
    /// @param scripts      The list of scripts associated with this object.
    /// @throws NullPointerException if `className`, `root`, `controller`, or `scriptEngine` is `null`.
    public FXMLDocument {
        Objects.requireNonNull(className, "`className` must not be null");
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(controller, "`controller` must not be null");
        Objects.requireNonNull(scriptEngine, "`scriptEngine` must not be null");
        imports = List.copyOf(Objects.requireNonNullElseGet(imports, List::of));
        definitions = List.copyOf(Objects.requireNonNullElseGet(definitions, List::of));
        scripts = List.copyOf(Objects.requireNonNullElseGet(scripts, List::of));
    }
}
