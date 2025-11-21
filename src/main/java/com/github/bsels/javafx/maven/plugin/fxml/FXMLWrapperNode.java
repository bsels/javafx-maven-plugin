package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;
import java.util.Objects;

/// Represents a wrapper node in the context of FXML processing, encapsulating a group of child nodes.
///
/// [FXMLWrapperNode] is an immutable record that serves as a container for organizing child nodes within
/// an FXML structure.
/// It extends the functionality of [FXMLParentNode], allowing it to contain multiple child nodes, and adheres to the
/// [FXMLNode] interface to align with the overall FXML node hierarchy.
///
/// This record enforces non-null constraints on its parameters to ensure the integrity and consistency of
/// the FXML structure.
public record FXMLWrapperNode(String name, List<FXMLNode> children) implements FXMLNode, FXMLParentNode {

    /// Constructs an instance of [FXMLWrapperNode], representing a named wrapper
    /// containing a list of child nodes in the context of FXML processing.
    /// Ensures that the provided name and children are not null.
    ///
    /// @param name the name of the wrapper node; must not be null
    /// @param children the list of child nodes encapsulated by the wrapper node; must not be null
    /// @throws NullPointerException if the name or children is null
    public FXMLWrapperNode {
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(children, "`children` must not be null");
    }
}
