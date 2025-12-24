package com.github.bsels.javafx.maven.plugin.fxml.introspect;

/// Represents various levels of visibility within a given context.
///
/// The `Visibility` enum defines four distinct levels of access control: `PUBLIC`, `PRIVATE`, `PROTECTED`,
/// and `PACKAGE`.
/// These levels are commonly used to define the accessibility of classes, methods, and fields within a program,
/// adhering to the principles of encapsulation and controlled access in object-oriented programming.
public enum Visibility {
    /// Represents the `PUBLIC` visibility level within the `Visibility` enumeration.
    ///
    /// The `PUBLIC` value indicates that a member is accessible from any other class or package.
    /// This visibility level is the most permissive and allows unrestricted access to the member.
    PUBLIC,
    /// Represents the `PRIVATE` visibility level within the `Visibility` enumeration.
    ///
    /// The `PRIVATE` value indicates that a member is accessible only within its own class.
    /// This visibility level is commonly used to enforce encapsulation and restrict member access entirely from outside
    /// its defining class.
    PRIVATE,
    /// Represents the `PROTECTED` visibility level within the `Visibility` enumeration.
    ///
    /// The `PROTECTED` value indicates that a member is accessible within its own package and by subclasses.
    /// This level of visibility is commonly used in object-oriented programming to allow controlled access to fields
    /// and methods.
    PROTECTED,
    /// Represents the package-private visibility level within the `Visibility` enumeration.
    ///
    /// The `PACKAGE` value indicates that a member is visible only within its own package
    /// and cannot be accessed from classes in other packages.
    /// It is a commonly used visibility modifier in Java to restrict access for encapsulation purposes.
    PACKAGE
}
