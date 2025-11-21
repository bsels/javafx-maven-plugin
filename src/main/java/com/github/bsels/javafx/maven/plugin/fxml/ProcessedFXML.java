package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Represents the processed metadata of an FXML structure.
///
/// This record consolidates information about the structure of an FXML file, including imports, fields, methods,
/// the root node, and the associated class name.
/// It ensures immutability and validates that all provided properties are non-null.
public record ProcessedFXML(
        Set<String> imports,
        List<FXMLField> fields,
        List<FXMLMethod> methods,
        FXMLNode root,
        String className
) {

    /// Constructs an instance of the [ProcessedFXML] record, encapsulating metadata about an FXML structure.
    /// Ensures that all provided parameters are non-null and creates immutable copies of the collections.
    ///
    /// @param imports the set of import strings required in the FXML context; must not be null
    /// @param fields the list of FXML fields in the structure; must not be null
    /// @param methods the list of FXML methods in the structure; must not be null
    /// @param root the root [FXMLNode] representing the top-level node in the hierarchy; must not be null
    /// @param className the name of the class associated with the FXML structure; must not be null
    /// @throws NullPointerException if any of the parameters is null
    public ProcessedFXML {
        imports = Set.copyOf(Objects.requireNonNull(imports, "`imports` must not be null"));
        fields = List.copyOf(Objects.requireNonNull(fields, "`fields` must not be null"));
        methods = List.copyOf(Objects.requireNonNull(methods, "`methods` must not be null"));
        Objects.requireNonNull(root, "`root` must not be null");
        Objects.requireNonNull(className, "`className` must not be null");
    }
}
