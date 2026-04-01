package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Represents an immutable collection of Java import statements and their associated class name mappings.
///
/// This record is used to manage and encapsulate data about import statements
/// and their corresponding class name mappings.
/// It ensures that the provided list of imports and mappings is immutable and not null.
///
/// @param imports          A list of fully qualified import statements.
/// @param inlineClassNames A map where keys represent fully qualified class names and values represent their corresponding class names in the source code.
record Imports(List<String> imports, Map<String, String> inlineClassNames) {

    /// Constructs a new `Imports` record.
    /// Ensures the specified lists and maps are non-null and immutable.
    ///
    /// @param imports          A list of import statements. Must not be null.
    /// @param inlineClassNames A map associating class names with their corresponding mappings. Must not be null.
    /// @throws NullPointerException If `imports` or `inlineClassNames` is null.
    Imports {
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(inlineClassNames, "`inlineClassNames` must not be null");
        imports = List.copyOf(imports);
        inlineClassNames = Map.copyOf(inlineClassNames);
    }
}
