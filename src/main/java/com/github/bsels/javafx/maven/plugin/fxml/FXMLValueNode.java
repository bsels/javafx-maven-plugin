package com.github.bsels.javafx.maven.plugin.fxml;

import java.util.Objects;

/// Represents a leaf node in an FXML data structure, holding a specific value.
///
/// The [FXMLValueNode] serves as an immutable representation of a value-bearing node.
/// This record implements both [FXMLNode] and [FXMLIdentifiableNode] interfaces,
/// making it uniquely identifiable within the FXML structure.
/// It encapsulates metadata about its type, identifier, and the value it holds,
/// ensuring all provided inputs are non-null.
///
/// This class is part of the sealed hierarchy of [FXMLNode] and is primarily used in modeling FXML constructs,
/// enabling structured representation and interaction with FXML elements in JavaFX.
///
/// @param internal a boolean indicating whether the [FXMLValueNode] is internal
/// @param identifier the identifier of the [FXMLValueNode]; must not be null
/// @param clazz the class metadata associated with the value; must not be null
/// @param value the value held by this [FXMLValueNode]; must not be null
public record FXMLValueNode(boolean internal, String identifier, Class<?> clazz, String value)
        implements FXMLNode, FXMLIdentifiableNode {

    /// Constructs an instance of [FXMLValueNode], representing a leaf node that holds
    /// a specific value in an FXML data structure.
    /// Ensures that the provided values are not null.
    ///
    /// @param internal a boolean indicating whether the [FXMLValueNode] is internal
    /// @param identifier the identifier of the [FXMLValueNode]; must not be null
    /// @param clazz the class metadata associated with the value; must not be null
    /// @param value the value held by this [FXMLValueNode]; must not be null
    /// @throws NullPointerException if any of the parameters (identifier, clazz, or value) is null
    public FXMLValueNode {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
    }
}
