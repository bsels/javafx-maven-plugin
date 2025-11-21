package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;
import java.util.Objects;

/// Represents an object node within an FXML structure.
///
/// FXMLObjectNode is a record that encapsulates metadata and relationships for an object node,
/// including its identifier, class type, properties, and child nodes.
/// It is a part of the FXML node hierarchy, implementing [FXMLNode], [FXMLIdentifiableNode], and [FXMLParentNode].
///
/// This class is immutable and ensures the integrity of its data by creating defensive copies of properties and child lists.
public record FXMLObjectNode(
        boolean internal,
        String identifier,
        Class<?> clazz,
        List<FXMLProperty> properties,
        List<FXMLNode> children,
        List<String> generics
) implements FXMLNode, FXMLIdentifiableNode, FXMLParentNode {

    /// Constructs an instance of [FXMLObjectNode], representing an object node in an FXML structure.
    /// Ensures that the provided values for identifier, clazz, properties, and children are non-null
    /// and creates immutable copies of the properties and children lists.
    ///
    /// @param internal a boolean indicating whether this node is internal
    /// @param identifier the unique identifier for the object node; must not be null
    /// @param clazz the class associated with the object node; must not be null
    /// @param properties the list of properties for the object node; must not be null
    /// @param children the list of child nodes for this object node; must not be null
    /// @param generics a list of generic types associated with the object node; must not be null
    /// @throws NullPointerException if identifier, clazz, properties, or children is null
    public FXMLObjectNode {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        properties = List.copyOf(Objects.requireNonNull(properties, "`properties` must not be null"));
        children = List.copyOf(Objects.requireNonNull(children, "`children` must not be null"));
        generics = List.copyOf(Objects.requireNonNull(generics, "`generics` must not be null"));
    }
}
