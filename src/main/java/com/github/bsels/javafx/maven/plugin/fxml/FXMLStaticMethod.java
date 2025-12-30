package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.List;
import java.util.Objects;

/// Represents a static method invocation in an FXML data structure.
///
/// The FXMLStaticMethod class models a static method call as part of an FXML hierarchy.
/// It provides structural information about the method, the class it belongs to,
/// and the children nodes associated with this method invocation.
///
/// This record serves as both an FXMLNode and an FXMLParentNode, allowing it to
/// encapsulate child nodes and participate in a tree structure typical in FXML representations.
///
/// @param clazz the class associated with the static method; must not be null
/// @param method the name of the static method; must not be null
/// @param children the list of child nodes under this static method; must not be null
public record FXMLStaticMethod(
        Class<?> clazz, String method, List<FXMLNode> children
) implements FXMLNode, FXMLParentNode {

    /// Constructs an instance of [FXMLStaticMethod], representing a static method invocation
    /// in an FXML data structure.
    /// Ensures that the provided values for class, method name, and child nodes are not null.
    ///
    /// @param clazz the class associated with the static method; must not be null
    /// @param method the name of the static method; must not be null
    /// @param children the list of child nodes under this static method; must not be null
    /// @throws NullPointerException if any of the parameters (clazz, method, or children) is null
    public FXMLStaticMethod {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(method, "`method` must not be null");
        children = List.copyOf(Objects.requireNonNull(children, "`children` must not be null"));
    }
}
