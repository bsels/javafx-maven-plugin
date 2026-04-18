package io.github.bsels.javafx.maven.plugin.fxml.v2;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unit tests for [FXMLLazyLoadedDocument].
class FXMLLazyLoadedDocumentTest {

    /// Creates a minimal [FXMLDocument] instance for use in tests.
    ///
    /// @return A valid [FXMLDocument] with a concrete root object
    private FXMLDocument createDocument() {
        return new FXMLDocument(
                "TestClass",
                new FXMLObject(FXMLRootIdentifier.INSTANCE, FXMLType.of(String.class), Optional.empty(), null),
                null,
                Optional.empty(),
                Optional.empty(),
                null,
                null
        );
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTest {

        @Test
        @DisplayName("New instance should not have a document loaded")
        void newInstanceShouldNotHaveDocumentLoaded() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();

            assertThatThrownBy(lazy::get)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Document not loaded yet");
        }
    }

    @Nested
    @DisplayName("set() tests")
    class SetTest {

        @Test
        @DisplayName("set() should store the document so get() returns it")
        void setShouldStoreDocumentSoGetReturnsIt() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            FXMLDocument document = createDocument();

            lazy.set(document);

            assertThat(lazy.get()).isSameAs(document);
        }

        @Test
        @DisplayName("set() should throw IllegalStateException when called a second time")
        void setShouldThrowWhenCalledTwice() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            lazy.set(createDocument());

            assertThatThrownBy(() -> lazy.set(createDocument()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Document already loaded");
        }
    }

    @Nested
    @DisplayName("get() tests")
    class GetTest {

        @Test
        @DisplayName("get() should return the document that was set")
        void getShouldReturnDocumentThatWasSet() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            FXMLDocument document = createDocument();
            lazy.set(document);

            assertThat(lazy.get())
                    .isSameAs(document)
                    .hasFieldOrPropertyWithValue("className", "TestClass");
        }

        @Test
        @DisplayName("get() should throw IllegalStateException when document has not been set")
        void getShouldThrowWhenDocumentNotSet() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();

            assertThatThrownBy(lazy::get)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Document not loaded yet");
        }
    }
}
