package io.github.bsels.javafx.maven.plugin.fxml.v2;

/// Represents a lazily loaded FXML document.
///
/// This class manages a single instance of an [FXMLDocument], allowing it to be initialized at a later point in time.
/// The `set` method is designed to ensure that the document can only be set once during the lifetime of an instance.
/// Once the document is loaded, it is treated as immutable within this context.
///
/// Thread-safety is ensured for setting and retrieving the document to support concurrent access.
public final class FXMLLazyLoadedDocument {

    /// Represents the FXML document associated with this instance.
    /// This document can be lazily initialized and managed through setter and getter methods.
    /// It encapsulates the parsed and structured content of an FXML file, including its root object,
    /// associated controller, script engine, and defined elements.
    ///
    /// The document is initially null and can only be set once during the object's lifecycle.
    /// Subsequent modifications to the document are not permitted.
    private FXMLDocument document;

    /// Constructs a new instance of `FXMLLazyLoadedDocument`.
    /// The [FXMLDocument] associated with this instance is initially set to null
    /// and can be lazily loaded at a later time using the provided setter method.
    public FXMLLazyLoadedDocument() {
        this.document = null;
    }

    /// Sets the FXML document for this instance if it has not already been set.
    /// This method ensures that the document is only initialized once and cannot be overwritten.
    ///
    /// This method is synchronized to ensure thread-safety.
    ///
    /// @param document The FXML document to associate with this instance. Must not be null.
    /// @throws IllegalStateException If the document has already been set.
    public synchronized void set(FXMLDocument document) {
        if (this.document != null) {
            throw new IllegalStateException("Document already loaded");
        }
        this.document = document;
    }

    /// Retrieves the FXML document if it has been loaded.
    ///
    /// @return The loaded FXML document.
    /// @throws IllegalStateException if the document has not been loaded yet.
    public FXMLDocument get() {
        if (document == null) {
            throw new IllegalStateException("Document not loaded yet");
        }
        return document;
    }
}
