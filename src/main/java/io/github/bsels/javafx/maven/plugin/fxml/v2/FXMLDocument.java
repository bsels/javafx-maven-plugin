package io.github.bsels.javafx.maven.plugin.fxml.v2;

import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// A parsed FXML document.
///
/// @param className    The name of the class associated with this document
/// @param root         The root object of the document
/// @param interfaces   The list of interfaces implemented by the class
/// @param controller   The controller class, if any
/// @param scriptEngine The script engine name, if any
/// @param definitions  The list of objects defined within this object
/// @param scripts      The list of scripts associated with this object
public record FXMLDocument(
        String className,
        AbstractFXMLObject root,
        List<FXMLInterface> interfaces,
        Optional<FXMLController> controller,
        Optional<String> scriptEngine,
        List<AbstractFXMLValue> definitions,
        List<FXMLScript> scripts
) {

    /// Initializes a new [FXMLDocument] record instance.
    ///
    /// @param className    The name of the class associated with this document
    /// @param root         The root object of the document
    /// @param interfaces   The list of interfaces implemented by the class
    /// @param controller   The controller class, if any
    /// @param scriptEngine The script engine name, if any
    /// @param definitions  The list of objects defined within this object
    /// @param scripts      The list of scripts associated with this object
    /// @throws NullPointerException If `className`, `root`, `controller`, or `scriptEngine` is null
    public FXMLDocument {
        Objects.requireNonNull(className, "`className` must not be null");
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(controller, "`controller` must not be null");
        Objects.requireNonNull(scriptEngine, "`scriptEngine` must not be null");
        interfaces = List.copyOf(Objects.requireNonNullElseGet(interfaces, List::of));
        definitions = List.copyOf(Objects.requireNonNullElseGet(definitions, List::of));
        scripts = List.copyOf(Objects.requireNonNullElseGet(scripts, List::of));
    }
}
