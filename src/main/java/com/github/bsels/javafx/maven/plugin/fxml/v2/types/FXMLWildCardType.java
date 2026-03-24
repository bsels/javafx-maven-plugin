package com.github.bsels.javafx.maven.plugin.fxml.v2.types;

/// Represents a wildcard type in FXML.
///
/// This class represents the `?` symbol in generic type parameters.
public final class FXMLWildCardType implements FXMLType {
    /// The singleton instance of [FXMLWildCardType].
    public static final FXMLWildCardType INSTANCE = new FXMLWildCardType();

    /// Private constructor to enforce a singleton pattern.
    private FXMLWildCardType() {
        // Singleton constructor
    }

    /// Returns a string representation of the wildcard type.
    ///
    /// @return The string representation, which is always `?`.
    @Override
    public String toString() {
        return "?";
    }

    /// Indicates whether some other object is "equal to" this one.
    ///
    /// @param obj The reference object with which to compare.
    /// @return `true` if this object is the same as the obj argument or if the obj
    /// argument is also an instance of [FXMLWildCardType].
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FXMLWildCardType;
    }

    /// Returns a hash code value for the object.
    ///
    /// @return A hash code value for this object.
    @Override
    public int hashCode() {
        return 0;
    }
}
