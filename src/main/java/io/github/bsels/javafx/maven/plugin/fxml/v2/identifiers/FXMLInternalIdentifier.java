package io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

/// An internal identifier generated for FXML objects without an explicit id.
///
/// @param internalId The internal index
public record FXMLInternalIdentifier(int internalId) implements FXMLIdentifier {

    /// Initializes a new [FXMLInternalIdentifier] record instance.
    ///
    /// @param internalId The internal index
    /// @throws IllegalArgumentException If `internalId` is negative
    public FXMLInternalIdentifier {
        if (internalId < 0) {
            throw new IllegalArgumentException("`internalId` must be non-negative");
        }
    }

    /// Returns a string representation of the internal identifier.
    ///
    /// @return The string representation
    @Override
    public String toString() {
        return "$internalVariable$%03d".formatted(internalId);
    }
}
