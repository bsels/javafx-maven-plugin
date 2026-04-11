package io.github.bsels.javafx.maven.plugin.fxml.v2;

/// A lazily loaded FXML document.
/// Manages a single [FXMLDocument] instance that can be initialized once.
public final class FXMLLazyLoadedDocument {

    /// The FXML document associated with this instance.
    private FXMLDocument document;

    /// Initializes a new [FXMLLazyLoadedDocument] instance.
    public FXMLLazyLoadedDocument() {
        this.document = null;
    }

    /// Sets the FXML document for this instance.
    ///
    /// @param document The [FXMLDocument] to associate
    /// @throws IllegalStateException If the document has already been set
    public synchronized void set(FXMLDocument document) {
        if (this.document != null) {
            throw new IllegalStateException("Document already loaded");
        }
        this.document = document;
    }

    /// Retrieves the FXML document.
    ///
    /// @return The loaded [FXMLDocument]
    /// @throws IllegalStateException If the document has not been loaded yet
    public FXMLDocument get() {
        if (document == null) {
            throw new IllegalStateException("Document not loaded yet");
        }
        return document;
    }
}
