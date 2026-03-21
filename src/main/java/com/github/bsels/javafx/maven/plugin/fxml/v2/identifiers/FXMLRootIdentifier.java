package com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

public final class FXMLRootIdentifier implements FXMLIdentifier {
    public static final FXMLRootIdentifier INSTANCE = new FXMLRootIdentifier();

    private FXMLRootIdentifier() {
        // Singleton constructor
    }

    @Override
    public String toString() {
        return "root";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FXMLRootIdentifier;
    }
}
