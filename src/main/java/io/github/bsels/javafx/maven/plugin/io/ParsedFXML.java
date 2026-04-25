package io.github.bsels.javafx.maven.plugin.io;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// Results of parsing an FXML file.
///
/// @param scriptNamespace The script language name
/// @param imports         The list of import statements
/// @param root            The root XML structure
/// @param className       The derived Java class name
public record ParsedFXML(
        Optional<String> scriptNamespace,
        List<String> imports,
        ParsedXMLStructure root,
        String className
) {

    /// Initializes a new [ParsedFXML] record instance.
    ///
    /// @param scriptNamespace The script language name
    /// @param imports         The list of import statements
    /// @param root            The root XML structure
    /// @param className       The derived Java class name
    /// @throws NullPointerException If `root`, `className`, or `scriptNamespace` is null
    public ParsedFXML {
        Objects.requireNonNull(scriptNamespace, "`scriptNamespace` must not be null");
        imports = List.copyOf(Objects.requireNonNullElseGet(imports, List::of));
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(className, "`className` must not be null");
    }
}
