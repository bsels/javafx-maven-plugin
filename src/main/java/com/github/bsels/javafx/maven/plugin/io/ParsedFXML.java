package com.github.bsels.javafx.maven.plugin.io;

import java.util.List;
import java.util.Objects;

/// Represents a parsed FXML file structure. It consists of the import statements used in the FXML file
/// and the root element of the FXML document parsed into a [ParsedXMLStructure].
///
/// This class encapsulates the structure of an FXML file in an immutable format, providing access to
/// its import declarations and root XML hierarchy.
public record ParsedFXML(List<String> imports, ParsedXMLStructure root, String className) {

    /// Constructs an instance of the [ParsedFXML] record.
    ///
    /// @param imports   a list of import statements associated with the FXML file. The list is defensively copied to ensure immutability.
    /// @param root      the root element of the FXML file, represented as a [ParsedXMLStructure]. Must not be null.
    /// @param className the name of the Java class generated from the FXML file.
    /// @throws NullPointerException if the root element is null.
    public ParsedFXML {
        imports = List.copyOf(imports);
        Objects.requireNonNull(root);
        Objects.requireNonNull(className);
    }
}
