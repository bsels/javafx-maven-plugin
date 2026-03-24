package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

/// Represents the identifier for the root element of an FXML document.
public final class FXMLRootIdentifier implements FXMLIdentifier {
    /// Singleton instance of the FXML root identifier.
    public static final FXMLRootIdentifier INSTANCE = new FXMLRootIdentifier();

    /// Private constructor for a singleton pattern.
    private FXMLRootIdentifier() {
        // Singleton constructor
    }

    /// Returns a string representation of the root identifier.
    ///
    /// @return "this".
    @Override
    public String toString() {
        return "this";
    }

    /// Returns a hash code for the root identifier.
    ///
    /// @return 0.
    @Override
    public int hashCode() {
        return 0;
    }

    /// Compares this root identifier to another object.
    ///
    /// @param obj The object to compare with.
    /// @return `true` if the object is an instance of `FXMLRootIdentifier`, `false` otherwise.
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FXMLRootIdentifier;
    }
}
