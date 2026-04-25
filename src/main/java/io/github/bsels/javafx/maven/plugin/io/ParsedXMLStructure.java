package io.github.bsels.javafx.maven.plugin.io;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/// An XML structure representing an element in an FXML file.
///
/// @param name       The XML element name
/// @param properties The map of attribute names to values
/// @param children   The list of child elements
/// @param comments   The list of comments associated with this element
/// @param textValue  The combined text content of the element
public record ParsedXMLStructure(
        String name,
        Map<String, String> properties,
        List<ParsedXMLStructure> children,
        List<String> comments,
        Optional<String> textValue
) {

    /// Initializes a new [ParsedXMLStructure] record instance.
    ///
    /// @param name       The XML element name
    /// @param properties The attribute properties
    /// @param children   The child elements
    /// @param comments   The associated comments
    /// @param textValue  The combined text content
    /// @throws NullPointerException If `name`, `properties`, `children`, `comments`, or `textValue` is null
    public ParsedXMLStructure {
        Objects.requireNonNull(name, "`name` must not be null");
        properties = Map.copyOf(Objects.requireNonNull(properties, "`properties` must not be null"));
        children = List.copyOf(Objects.requireNonNull(children, "`children` must not be null"));
        comments = List.copyOf(Objects.requireNonNull(comments, "`comments` must not be null"));
        Objects.requireNonNull(textValue, "`textValue` must not be null");
    }
}
