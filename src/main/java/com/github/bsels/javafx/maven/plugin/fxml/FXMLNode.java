package com.github.bsels.javafx.maven.plugin.fxml;

/// Represents the base type for all nodes in an FXML hierarchical structure.
///
/// The FXMLNode interface is a sealed interface that defines the common contract for
/// elements in an FXML model. It serves as the root of a hierarchy where nodes have
/// different roles and specializations depending on their type.
///
/// Implementing types:
/// - [FXMLConstantNode]: Represents a constant value in the FXML data structure.
/// - [FXMLIdentifiableNode]: Represents nodes with unique identifiers.
/// - [FXMLObjectNode]: Represents an object node in the hierarchy.
/// - [FXMLParentNode]: Represents nodes capable of containing child nodes.
/// - [FXMLStaticMethod]: Represents a static method call in the structure.
/// - [FXMLValueNode]: Represents a node containing a single value.
/// - [FXMLWrapperNode]: Represents a wrapper node encapsulating other nodes.
///
/// This structure enables the representation and manipulation of FXML models, supporting
/// various elements such as constants, objects, methods, and hierarchical relationships.
public sealed interface FXMLNode
        permits FXMLConstantNode, FXMLIdentifiableNode, FXMLObjectNode, FXMLParentNode, FXMLStaticMethod, FXMLValueNode,
        FXMLWrapperNode {
}
