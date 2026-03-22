package com.github.bsels.javafx.maven.plugin.io;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Represents a parsed XML structure including the name, attributes, children, and comments of an XML element.
/// This class is designed to provide an immutable representation of an XML element hierarchy.
///
/// @param name       the name of the XML element
/// @param properties the attributes of the XML element represented as a map of key-value pairs
/// @param children   the nested child elements of this XML element represented as a list of [ParsedXMLStructure]
/// @param comments   the comments related to the element
public record ParsedXMLStructure(
        String name,
        Map<String, String> properties,
        List<ParsedXMLStructure> children,
        List<String> comments
) {

    /// Constructs an instance of [ParsedXMLStructure].
    ///
    /// @param name       the name of the XML element
    /// @param properties the attributes of the XML element represented as a map of key-value pairs
    /// @param children   the nested child elements of this XML element represented as a list of [ParsedXMLStructure]
    /// @param comments   the comments related to the element
    public ParsedXMLStructure {
        name = Objects.requireNonNull(name, "`name` must not be null");
        properties = Map.copyOf(properties);
        children = List.copyOf(children);
        comments = List.copyOf(comments);
    }

    /// Constructs an instance of [ParsedXMLStructure] with the specified name, properties,
    /// and child elements, setting comments as an empty list by default.
    ///
    /// @param name       the name of the XML element
    /// @param properties the attributes of the XML element represented as a map of key-value pairs
    /// @param children   the nested child elements of this XML element represented as a list of ParsedXMLStructure
    public ParsedXMLStructure(String name, Map<String, String> properties, List<ParsedXMLStructure> children) {
        this(name, properties, children, List.of());
    }

    /// Retrieves the text value of this XML element.
    ///
    /// This method is intended to be called on elements that do not contain child elements.
    /// If the element has children, an [IllegalStateException] is thrown.
    ///
    /// @return the text value of the element as a [String] if the element does not have children
    /// @throws IllegalStateException if the element contains child elements
    @JsonIgnore
    public String getTextValue() throws IllegalStateException {
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot get text value of an element with children");
        }
        return ""; // TODO
    }
}
