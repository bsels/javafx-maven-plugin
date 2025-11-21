package com.github.bsels.javafx.maven.plugin.fxml;

/// Represents an identifiable node in an FXML data structure.
///
/// This sealed interface extends the [FXMLNode] interface and serves as a parent for specific types of identifiable
/// nodes in an FXML model.
///
/// An identifiable node is characterized by its unique identifier, distinguishing it within an FXML context.
/// Implementations of this interface include:
/// - [FXMLObjectNode]: Represents an object node containing properties and child nodes.
/// - [FXMLValueNode]: Represents a leaf node that holds a specific value.
///
/// This abstraction facilitates the management and representation of node structures within FXML processing,
/// supporting identification and interaction with nodes that carry meaningful information.
public sealed interface FXMLIdentifiableNode extends FXMLNode permits FXMLObjectNode, FXMLValueNode {

    /// Indicates whether the current FXML identifiable node is internal.
    ///
    /// @return true if the node is considered internal; false otherwise
    boolean internal();

    /// Retrieves the unique identifier of this FXML node.
    ///
    /// @return a non-null string representing the unique identifier of the node
    String identifier();

    /// Retrieves the class metadata associated with this FXML node.
    ///
    /// @return the class object representing the type of this node
    Class<?> clazz();
}
