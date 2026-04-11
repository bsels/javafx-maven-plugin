package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

/// The parts of a generated Java source file.
enum SourcePart {
    /// Package declaration.
    PACKAGE,
    /// Import statements.
    IMPORTS,
    /// Class declaration line.
    CLASS_DECLARATION,
    /// Class fields.
    FIELDS,
    /// Beginning of the constructor.
    CONSTRUCTOR_PROLOGUE,
    /// Call to the superclass constructor.
    CONSTRUCTOR_SUPER_CALL,
    /// End of the constructor, where property assignments and initialization occur.
    CONSTRUCTOR_EPILOGUE,
    /// Class methods.
    METHODS,
    /// Nested classes or types.
    NESTED_TYPES
}
