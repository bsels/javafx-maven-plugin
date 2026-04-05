package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

/// Represents the different parts of a generated Java source file.
enum SourcePart {
    /// The package declaration.
    PACKAGE,
    /// The import statements.
    IMPORTS,
    /// The class declaration line.
    CLASS_DECLARATION,
    /// The class fields.
    FIELDS,
    /// The beginning of the constructor.
    CONSTRUCTOR_PROLOGUE,
    /// The call to the superclass constructor.
    CONSTRUCTOR_SUPER_CALL,
    /// The end of the constructor, where property assignments and initialization occur.
    CONSTRUCTOR_EPILOGUE,
    /// The class methods.
    METHODS,
    /// Nested classes or types.
    NESTED_TYPES
}
