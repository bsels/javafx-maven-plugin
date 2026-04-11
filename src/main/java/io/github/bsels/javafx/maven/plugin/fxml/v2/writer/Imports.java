package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// A collection of Java import statements and their associated class name mappings.
///
/// @param imports          A list of fully qualified import statements
/// @param inlineClassNames A map from fully qualified class names to their names in the source code
record Imports(List<String> imports, Map<String, String> inlineClassNames) {

    /// Initializes a new [Imports] record instance.
    ///
    /// @param imports          A list of import statements
    /// @param inlineClassNames A map associating class names with their mappings
    /// @throws NullPointerException If `imports` or `inlineClassNames` is null
    Imports {
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(inlineClassNames, "`inlineClassNames` must not be null");
        imports = List.copyOf(imports);
        inlineClassNames = Map.copyOf(inlineClassNames);
    }
}
