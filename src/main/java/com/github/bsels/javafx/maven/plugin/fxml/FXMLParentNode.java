package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;

/// Represents a parent node in the FXML structure that can contain child nodes.
///
/// The [FXMLParentNode] interface extends [FXMLNode] and serves as a base for nodes that can have children.
/// This enables hierarchical organization within the FXML model, allowing the construction of complex structures.
///
/// It is a sealed interface, permitting only a defined set of types to implement it:
/// - [FXMLObjectNode]: Represents an object node with properties and child nodes.
/// - [FXMLStaticMethod]: Represents a static method invocation containing child nodes.
/// - [FXMLWrapperNode]: Represents a wrapper node encapsulating child nodes.
///
/// The interface defines methods for accessing the child nodes,
/// allowing traversal or manipulation of the node's hierarchy.
public sealed interface FXMLParentNode extends FXMLNode permits FXMLObjectNode, FXMLStaticMethod, FXMLWrapperNode {

    /// Retrieves the list of child nodes for this [FXMLParentNode].
    ///
    /// This method provides access to the direct child elements,
    /// enabling traversal or manipulation of the FXML node hierarchy.
    ///
    /// @return a list of [FXMLNode] instances representing the children of this node
    List<FXMLNode> children();
}
