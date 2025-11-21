package com.github.bsels.javafx.maven.plugin.fxml;

/// Represents a property within the context of FXML.
///
/// This sealed interface serves as the common parent for various types of properties that can be used
/// to define FXML elements, such as constructor properties, object properties, and static properties.
///
/// Implementations of this interface include:
/// - [FXMLConstructorProperty]: Represents a property defined in the constructor of an FXML element.
/// - [FXMLObjectProperty]: Represents an object property with a specific setter in the context of FXML processing.
/// - [FXMLStaticProperty]: Represents a static property associated with a specific static class and method.
///
/// FXMLProperty provides an abstraction to describe different types of configurable attributes
/// or parameters used in FXML structures, enabling seamless integration and description of FXML metadata.
public sealed interface FXMLProperty permits FXMLConstructorProperty, FXMLObjectProperty, FXMLStaticProperty {
    /// Returns the name of the property.
    ///
    /// @return the name of the property as a non-null string
    String name();
}
