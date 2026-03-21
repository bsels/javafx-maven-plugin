package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

public record FXMLInternalIdentifier(int internalId) implements FXMLIdentifier {

    public FXMLInternalIdentifier {
        if (internalId < 0) {
            throw new IllegalArgumentException("Internal identifier must be non-negative");
        }
    }

    @Override
    public String toString() {
        return "$internalVariable$%03d".formatted(internalId);
    }
}
