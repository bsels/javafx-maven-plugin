package io.github.bsels.javafx.maven.plugin.io;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/// Represents a parsed XML structure including the name, attributes, children, and comments of an XML element.
/// This class is designed to provide an immutable representation of an XML element hierarchy.
///
/// @param name       the name of the XML element
/// @param properties the attributes of the XML element represented as a map of key-value pairs
/// @param children   the nested child elements of this XML element represented as a list of [ParsedXMLStructure]
/// @param comments   the comments related to the element
/// @param textValue  the plain text value of this XML element, if it contains no child elements
public record ParsedXMLStructure(
        String name,
        Map<String, String> properties,
        List<ParsedXMLStructure> children,
        List<String> comments,
        Optional<String> textValue
) {

    /// Constructs an instance of [ParsedXMLStructure].
    ///
    /// @param name       the name of the XML element
    /// @param properties the attributes of the XML element represented as a map of key-value pairs
    /// @param children   the nested child elements of this XML element represented as a list of [ParsedXMLStructure]
    /// @param comments   the comments related to the element
    /// @param textValue  the plain text value of this XML element, if it contains no child elements
    public ParsedXMLStructure {
        name = Objects.requireNonNull(name, "`name` must not be null");
        properties = Map.copyOf(properties);
        children = List.copyOf(children);
        comments = List.copyOf(comments);
        Objects.requireNonNull(textValue, "`textValue` must not be null");
    }

    /// Constructs an instance of ParsedXMLStructure with the specified name, properties, child elements,
    /// and associated comments.
    /// The text value of the XML element is set to an empty Optional by default.
    ///
    /// @param name       the name of the XML element
    /// @param properties the attributes of the XML element represented as a map of key-value pairs
    /// @param children   the nested child elements of this XML element represented as a list of ParsedXMLStructure
    /// @param comments   the comments related to the element
    public ParsedXMLStructure(
            String name,
            Map<String, String> properties,
            List<ParsedXMLStructure> children,
            List<String> comments
    ) {
        this(name, properties, children, comments, Optional.empty());
    }

    /// Constructs an instance of [ParsedXMLStructure] with the specified name, properties,
    /// and child elements, setting comments as an empty list and text value as empty by default.
    ///
    /// @param name       the name of the XML element
    /// @param properties the attributes of the XML element represented as a map of key-value pairs
    /// @param children   the nested child elements of this XML element represented as a list of `ParsedXMLStructure`
    public ParsedXMLStructure(String name, Map<String, String> properties, List<ParsedXMLStructure> children) {
        this(name, properties, children, List.of(), Optional.empty());
    }
}
