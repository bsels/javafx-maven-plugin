package io.github.bsels.javafx.maven.plugin.io;

import io.github.bsels.javafx.maven.plugin.TestHelpers;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.OptionalAssert;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FXMLReaderTests {

    @TempDir
    Path tempDir;

    private String originalJavaHome;
    private FXMLReader fxmlReader;

    @BeforeEach
    void setUp() {
        fxmlReader = new FXMLReader(new DefaultLog(new ConsoleLogger()));
        originalJavaHome = System.getProperty("java.home");
        System.setProperty("java.home", "/java/home");
        assertThat(fxmlReader.toString())
                .isNotNull()
                .startsWith("FXMLReader[log=");
    }

    @AfterEach
    void tearDown() {
        if (originalJavaHome != null) {
            System.setProperty("java.home", originalJavaHome);
        } else {
            System.clearProperty("java.home");
        }
    }

    @Nested
    class ReadFXMLTest {

        @Test
        void withSimpleValidFXML_shouldParseCorrectly() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.control.Button?>
                    <?import javafx.scene.layout.VBox?>
                    
                    <VBox xmlns="http://javafx.com/javafx/17.0.12">
                        <Button text="Click me" id="myButton"/>
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("TestView.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.className()).isEqualTo("TestView");
            assertThat(result.imports()).containsExactly(
                    "javafx.scene.control.Button",
                    "javafx.scene.layout.VBox"
            );

            ParsedXMLStructure root = result.root();
            assertThat(root.name()).isEqualTo("VBox");
            assertThat(root.properties()).isEmpty();
            assertThat(root.children()).hasSize(1);

            ParsedXMLStructure button = root.children().getFirst();
            assertThat(button.name()).isEqualTo("Button");
            assertThat(button.properties())
                    .containsEntry("text", "Click me")
                    .containsEntry("id", "myButton");
            assertThat(button.children()).isEmpty();
        }

        @Test
        void withNoImports_shouldReturnEmptyImportsList() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <VBox xmlns="http://javafx.com/javafx/17.0.12">
                        <Button text="No imports"/>
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("NoImports.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result.imports()).isEmpty();
            assertThat(result.className()).isEqualTo("NoImports");
            assertThat(result.root().name()).isEqualTo("VBox");
        }

        @Test
        void withMultipleImports_shouldParseAllImports() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.control.*?>
                    <?import javafx.scene.layout.VBox?>
                    <?import javafx.geometry.Insets?>
                    <?import java.util.List?>
                    
                    <VBox xmlns="http://javafx.com/javafx/17.0.12"/>
                    """;

            Path fxmlFile = tempDir.resolve("MultipleImports.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result.imports()).containsExactly(
                    "javafx.scene.control.*",
                    "javafx.scene.layout.VBox",
                    "javafx.geometry.Insets",
                    "java.util.List"
            );
        }

        @Test
        void withNestedElements_shouldParseHierarchy() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.control.Label?>
                    <?import javafx.scene.layout.VBox?>
                    
                    <VBox>
                        <VBox id="inner">
                            <Label text="Nested Label"/>
                        </VBox>
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("Nested.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            ParsedXMLStructure root = result.root();
            assertThat(root.name()).isEqualTo("VBox");
            assertThat(root.children()).hasSize(1);

            ParsedXMLStructure innerVBox = root.children().get(0);
            assertThat(innerVBox.name()).isEqualTo("VBox");
            assertThat(innerVBox.properties()).containsEntry("id", "inner");
            assertThat(innerVBox.children()).hasSize(1);

            ParsedXMLStructure label = innerVBox.children().get(0);
            assertThat(label.name()).isEqualTo("Label");
            assertThat(label.properties()).containsEntry("text", "Nested Label");
            assertThat(label.children()).isEmpty();
        }

        @Test
        void withComplexFileName_shouldDeriveClassNameCorrectly() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <VBox xmlns="http://javafx.com/javafx/17.0.12"/>
                    """;

            Path fxmlFile = tempDir.resolve("My-Complex.File_Name.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result.className()).isEqualTo("My_Complex_File_Name");
        }

        @Test
        void withImportsAfterContent_shouldIgnoreImportsAfterContent() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.layout.VBox?>
                    
                    <VBox xmlns="http://javafx.com/javafx/17.0.12"/>
                    
                    <?import javafx.scene.control.Button?>
                    """;

            Path fxmlFile = tempDir.resolve("ImportsAfter.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result.imports()).containsExactly("javafx.scene.layout.VBox");
        }

        @Test
        void withEmptyLines_shouldIgnoreEmptyLines() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    
                    <?import javafx.scene.layout.VBox?>
                    
                    
                    <?import javafx.scene.control.Button?>
                    
                    
                    <VBox xmlns="http://javafx.com/javafx/17.0.12"/>
                    """;

            Path fxmlFile = tempDir.resolve("EmptyLines.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result.imports()).containsExactly(
                    "javafx.scene.layout.VBox",
                    "javafx.scene.control.Button"
            );
        }

        @Test
        void withNonExistentFile_shouldThrowMojoExecutionException() {
            // Arrange
            Path nonExistentFile = tempDir.resolve("NonExistent.fxml");

            // Act & Assert
            assertThatThrownBy(() -> fxmlReader.readFXML(nonExistentFile))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("Unable to read FXML file")
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void withMalformedXML_shouldThrowRuntimeException() throws Exception {
            // Arrange
            String malformedContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <VBox>
                        <Button text="Unclosed button"
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("Malformed.fxml");
            Files.writeString(fxmlFile, malformedContent, StandardCharsets.UTF_8);

            // Act & Assert
            assertThatThrownBy(() -> fxmlReader.readFXML(fxmlFile))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void withElementsWithMultipleAttributes_shouldParseAllAttributes() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.control.Button?>
                    
                    <Button text="Multi Attr" 
                            id="multiButton" 
                            prefWidth="100.0" 
                            prefHeight="50.0" 
                            disable="false"/>
                    """;

            Path fxmlFile = tempDir.resolve("MultiAttr.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            ParsedXMLStructure root = result.root();
            assertThat(root.properties())
                    .containsEntry("text", "Multi Attr")
                    .containsEntry("id", "multiButton")
                    .containsEntry("prefWidth", "100.0")
                    .containsEntry("prefHeight", "50.0")
                    .containsEntry("disable", "false");
        }

        @Test
        void withNamespaces_DropNamespaces() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.layout.VBox?>
                    
                    <VBox xmlns="http://javafx.com/javafx/17.0.12" 
                          xmlns:fx="http://javafx.com/fxml/1">
                        <fx:define>
                            <fx:String fx:id="testString">Test Value</fx:String>
                        </fx:define>
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("Namespaces.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            ParsedXMLStructure root = result.root();
            assertThat(root.properties())
                    .isEmpty();

            assertThat(root.children()).hasSize(1);
            ParsedXMLStructure define = root.children().getFirst();
            assertThat(define.name()).isEqualTo("fx:define");
        }

        @Test
        void shouldLogImportsAndConversionInfo() throws Exception {
            // Arrange
            String fxmlContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.scene.control.Button?>
                    <?import javafx.scene.layout.VBox?>
                    
                    <VBox>
                        <Button text="Test"/>
                    </VBox>
                    """;

            Path fxmlFile = tempDir.resolve("LogTest.fxml");
            Files.writeString(fxmlFile, fxmlContent, StandardCharsets.UTF_8);

            // Act
            fxmlReader.readFXML(fxmlFile);
        }

        @Test
        void nonExistingFile_ThrowsMojoExecutionException() {
            try (MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
                // Arrange
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(Stream.empty());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenThrow(new IOException("Test exception"));

                // Act & Assert
                assertThatThrownBy(() -> fxmlReader.readFXML(Path.of("non-existing-file")))
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessage("Unable to read FXML file")
                        .hasCauseInstanceOf(IOException.class)
                        .cause()
                        .hasMessage("Test exception");
            }
        }
    }

    @Nested
    class ExamplesTest {

        @SuppressWarnings("rawtypes") // Suppress raw type warning because of the assertion factory
        private static final InstanceOfAssertFactory<List, ListAssert<ParsedXMLStructure>> PARSED_XML_STRUCTURE_LIST_ASSERT_FACTORY =
                InstanceOfAssertFactories.list(ParsedXMLStructure.class);
        @SuppressWarnings("rawtypes") // Suppress raw type warning because of the assertion factory
        private static final InstanceOfAssertFactory<Map, MapAssert<String, String>> STRING_STRING_MAP_ASSERT_FACTORY =
                InstanceOfAssertFactories.map(String.class, String.class);
        @SuppressWarnings("rawtypes") // Suppress raw type warning because of the assertion factory
        private static final InstanceOfAssertFactory<Optional, OptionalAssert<String>> STRING_OPTIONAL_ASSERT_FACTORY =
                InstanceOfAssertFactories.optional(String.class);

        @Test
        void inMemoryScript_ReturnExpectedIncludingPlainText() throws MojoExecutionException {
            // Prepare
            Path fxmlFile = TestHelpers.getTestResourcePath("/examples/InMemoryScript.fxml");

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("scriptNamespace", Optional.of("javascript"))
                    .hasFieldOrPropertyWithValue("className", "InMemoryScript")
                    .satisfies(
                            parsedFXML -> assertThat(parsedFXML.imports())
                                    .hasSize(2)
                                    .containsExactlyInAnyOrder("javafx.scene.control.*", "javafx.scene.layout.*")
                    )
                    // Verify the root node
                    .extracting(ParsedFXML::root)
                    .hasFieldOrPropertyWithValue("name", "VBox")
                    .hasFieldOrPropertyWithValue("properties", Map.of())
                    .hasFieldOrPropertyWithValue("comments", List.of())
                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                    // Verify the children
                    .extracting(ParsedXMLStructure::children, PARSED_XML_STRUCTURE_LIST_ASSERT_FACTORY)
                    .hasSize(2)
                    .satisfiesExactly(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("name", FXMLConstants.FX_SCRIPT_ELEMENT)
                                    .hasFieldOrPropertyWithValue("properties", Map.of())
                                    .hasFieldOrPropertyWithValue("children", List.of())
                                    .hasFieldOrPropertyWithValue("comments", List.of())
                                    .extracting(ParsedXMLStructure::textValue, STRING_OPTIONAL_ASSERT_FACTORY)
                                    .isPresent()
                                    .get(InstanceOfAssertFactories.STRING)
                                    .isEqualTo("""
                                            function handleButtonAction(event) {
                                                java.lang.System.out.println('You clicked me!');
                                            }
                                            """),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("name", "children")
                                    .hasFieldOrPropertyWithValue("properties", Map.of())
                                    .hasFieldOrPropertyWithValue("comments", List.of())
                                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                                    .extracting(ParsedXMLStructure::children, PARSED_XML_STRUCTURE_LIST_ASSERT_FACTORY)
                                    .hasSize(1)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "Button")
                                    .hasFieldOrPropertyWithValue("children", List.of())
                                    .hasFieldOrPropertyWithValue("comments", List.of())
                                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                                    .extracting(ParsedXMLStructure::properties, STRING_STRING_MAP_ASSERT_FACTORY)
                                    .hasSize(2)
                                    .containsEntry("text", "Click Me!")
                                    .containsEntry("onAction", "handleButtonAction(event);")
                    );
        }

        @Test
        void externalScript_ReturnExpectedNonPlainText() throws MojoExecutionException {
            // Prepare
            Path fxmlFile = TestHelpers.getTestResourcePath("/examples/ExternalScript.fxml");

            // Act
            ParsedFXML result = fxmlReader.readFXML(fxmlFile);

            // Assert
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("scriptNamespace", Optional.of("javascript"))
                    .hasFieldOrPropertyWithValue("className", "ExternalScript")
                    .satisfies(
                            parsedFXML -> assertThat(parsedFXML.imports())
                                    .hasSize(2)
                                    .containsExactlyInAnyOrder("javafx.scene.control.Button", "javafx.scene.layout.VBox")
                    )
                    // Verify the root node
                    .extracting(ParsedFXML::root)
                    .hasFieldOrPropertyWithValue("name", "VBox")
                    .hasFieldOrPropertyWithValue("properties", Map.of())
                    .hasFieldOrPropertyWithValue("comments", List.of())
                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                    // Verify the children
                    .extracting(ParsedXMLStructure::children, PARSED_XML_STRUCTURE_LIST_ASSERT_FACTORY)
                    .hasSize(2)
                    .satisfiesExactly(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("name", FXMLConstants.FX_SCRIPT_ELEMENT)
                                    .hasFieldOrPropertyWithValue("children", List.of())
                                    .hasFieldOrPropertyWithValue("comments", List.of())
                                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                                    .extracting(ParsedXMLStructure::properties, STRING_STRING_MAP_ASSERT_FACTORY)
                                    .hasSize(2)
                                    .containsEntry("charset", "utf8")
                                    .containsEntry("source", "example.js"),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("name", "Button")
                                    .hasFieldOrPropertyWithValue("children", List.of())
                                    .hasFieldOrPropertyWithValue("comments", List.of())
                                    .returns(Optional.empty(), ParsedXMLStructure::textValue)
                                    .extracting(ParsedXMLStructure::properties, STRING_STRING_MAP_ASSERT_FACTORY)
                                    .hasSize(2)
                                    .containsEntry("text", "Click Me!")
                                    .containsEntry("onAction", "handleButtonAction(event);")
                    );
        }
    }
}
