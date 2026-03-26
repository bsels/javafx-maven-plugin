package com.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import com.github.bsels.javafx.maven.plugin.TestHelpers;
import com.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLFileScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import com.github.bsels.javafx.maven.plugin.io.FXMLReader;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.OptionalAssert;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FXMLDocumentParserTest {
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<List, ListAssert<FXMLProperty>> PROPERTIES_ASSERT_FACTORY = InstanceOfAssertFactories.list(
            FXMLProperty.class);
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<List, ListAssert<AbstractFXMLValue>> LIST_VALUE_ASSERT_FACTORY = InstanceOfAssertFactories.list(
            AbstractFXMLValue.class);
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<Optional, OptionalAssert<FXMLIdentifier>> OPTIONAL_IDENTIFIER_ASSERT_FACTORY = InstanceOfAssertFactories.optional(
            FXMLIdentifier.class);

    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser classUnderTest;
    private MockedStatic<Charset> mockedCharset;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        classUnderTest = new FXMLDocumentParser(log);
        originalJavaHome = System.getProperty("java.home");
        System.setProperty("java.home", "/java/home");
        assertThat(fxmlReader.toString())
                .isNotNull()
                .startsWith("FXMLReader[log=");
        mockedCharset = Mockito.mockStatic(Charset.class, Mockito.CALLS_REAL_METHODS);
        mockedCharset.when(Charset::defaultCharset)
                .thenReturn(StandardCharsets.UTF_8);
    }

    @AfterEach
    void tearDown() {
        if (originalJavaHome != null) {
            System.setProperty("java.home", originalJavaHome);
        } else {
            System.clearProperty("java.home");
        }
        mockedCharset.close();
    }

    private ParsedFXML readFXML(String fxml) throws MojoExecutionException {
        Path fxmlFile = TestHelpers.getTestResourcePath(fxml);
        return fxmlReader.readFXML(fxmlFile);
    }

    @Nested
    class ExamplesTest {

        @Test
        void inMemoryScript() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/InMemoryScript.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "InMemoryScript")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.of("javascript"))
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .satisfies(
                            doc -> assertThat(doc.scripts())
                                    .hasSize(1)
                                    .satisfiesExactly(
                                            script -> assertThat(script)
                                                    .isInstanceOf(FXMLSourceScript.class)
                                                    .hasFieldOrPropertyWithValue(
                                                            "source", """
                                                                    function handleButtonAction(event) {
                                                                        java.lang.System.out.println('You clicked me!');
                                                                    }
                                                                    """
                                                    )
                                    ),
                            doc -> assertThat(doc.imports())
                                    .hasSize(2)
                                    .containsExactly("javafx.scene.control.*", "javafx.scene.layout.*")
                    )
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(VBox.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLCollectionProperties.class)
                    .extracting(FXMLCollectionProperties.class::cast)
                    .hasFieldOrPropertyWithValue("name", "children")
                    .hasFieldOrPropertyWithValue("getter", "getChildren")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, new FXMLClassType(Node.class))
                    )
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    // Validate root property values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                    .satisfies(
                            object -> assertThat(object.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    // Validate button properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(2)
                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .satisfiesExactlyInAnyOrder(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("name", "onAction")
                                    .hasFieldOrPropertyWithValue("setter", "setOnAction")
                                    .hasFieldOrPropertyWithValue(
                                            "type",
                                            new FXMLGenericType(
                                                    EventHandler.class,
                                                    new FXMLClassType(ActionEvent.class)
                                            )
                                    )
                                    .hasFieldOrPropertyWithValue(
                                            "value",
                                            new FXMLInlineScript("handleButtonAction(event);")
                                    ),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Click Me!"))
                    );
        }

        @Test
        void externalScript() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/ExternalScript.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "ExternalScript")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.of("javascript"))
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .satisfies(
                            doc -> assertThat(doc.scripts())
                                    .hasSize(1)
                                    .satisfiesExactly(
                                            script -> assertThat(script)
                                                    .isInstanceOf(FXMLFileScript.class)
                                                    .hasFieldOrPropertyWithValue("path", "/examples/example.js")
                                                    .hasFieldOrPropertyWithValue("charset", StandardCharsets.UTF_8)
                                    ),
                            doc -> assertThat(doc.imports())
                                    .hasSize(2)
                                    .containsExactly("javafx.scene.control.Button", "javafx.scene.layout.VBox")
                    )
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(VBox.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLCollectionProperties.class)
                    .extracting(FXMLCollectionProperties.class::cast)
                    .hasFieldOrPropertyWithValue("name", "children")
                    .hasFieldOrPropertyWithValue("getter", "getChildren")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, new FXMLClassType(Node.class))
                    )
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    // Validate root property values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                    .satisfies(
                            object -> assertThat(object.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    // Validate button properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(2)
                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .satisfiesExactlyInAnyOrder(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("name", "onAction")
                                    .hasFieldOrPropertyWithValue("setter", "setOnAction")
                                    .hasFieldOrPropertyWithValue(
                                            "type",
                                            new FXMLGenericType(
                                                    EventHandler.class,
                                                    new FXMLClassType(ActionEvent.class)
                                            )
                                    )
                                    .hasFieldOrPropertyWithValue(
                                            "value",
                                            new FXMLInlineScript("handleButtonAction(event);")
                                    ),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Click Me!"))
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ImplicitDefault", "ExplicitDefault"})
        void implicitExplicitDefault(String className) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/%s.fxml".formatted(className));

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", className)
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
                            doc -> assertThat(doc.imports())
                                    .hasSize(3)
                                    .containsExactly(
                                            "javafx.scene.control.Button",
                                            "javafx.scene.control.Separator",
                                            "javafx.scene.layout.VBox"
                                    )
                    )
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(VBox.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLCollectionProperties.class)
                    .extracting(FXMLCollectionProperties.class::cast)
                    .hasFieldOrPropertyWithValue("name", "children")
                    .hasFieldOrPropertyWithValue("getter", "getChildren")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, new FXMLClassType(Node.class))
                    )
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    // Validate children values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(3)
                    .hasOnlyElementsOfType(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .allSatisfy(
                            object -> assertThat(object.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    .allSatisfy(
                            object -> assertThat(object.factoryMethod())
                                    .isNotPresent()
                    )
                    .satisfiesExactly(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                    .hasSize(1)
                                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                                    .extracting(FXMLObjectProperty.class::cast)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Button 1")),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Separator.class))
                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                    .hasSize(1)
                                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                                    .extracting(FXMLObjectProperty.class::cast)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "orientation")
                                    .hasFieldOrPropertyWithValue("setter", "setOrientation")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Orientation.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("VERTICAL")),
                            third -> assertThat(third)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                    .hasSize(1)
                                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                                    .extracting(FXMLObjectProperty.class::cast)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Button 2"))
                    );
        }

        @Test
        void myHashMap() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/MyHashMap.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "MyHashMap")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
                            doc -> assertThat(doc.imports())
                                    .hasSize(2)
                                    .containsExactly("java.lang.String", "java.util.HashMap")
                    )
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLMap.class)
                    .extracting(FXMLMap.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(
                                    HashMap.class,
                                    List.of(new FXMLClassType(String.class), new FXMLClassType(String.class))
                            )
                    )
                    .satisfies(
                            map -> assertThat(map.entries())
                                    .hasSize(3)
                                    .containsEntry("foo", new FXMLLiteral("123"))
                                    .containsEntry("bar", new FXMLLiteral("456"))
                                    .satisfies(
                                            entries -> assertThat(entries.get("test"))
                                                    .isInstanceOf(FXMLValue.class)
                                                    .hasFieldOrPropertyWithValue("value", "Dummy")
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            new FXMLClassType(String.class)
                                                    )
                                    )
                    );
        }

        @Test
        void mapWithReferences() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/MapWithReferences.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    .isNotNull()
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLMap.class)
                    .extracting(FXMLMap.class::cast)
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(
                                    HashMap.class,
                                    List.of(new FXMLClassType(Object.class), new FXMLClassType(Object.class))
                            )
                    )
                    .satisfies(
                            map -> assertThat(map.entries())
                                    .hasSize(2)
                                    .containsEntry("refEntry", new FXMLReference("myButton"))
                                    .satisfies(
                                            entries -> assertThat(entries.get("copyEntry"))
                                                    .isInstanceOf(FXMLCopy.class)
                                                    .extracting(FXMLCopy.class::cast)
                                                    .hasFieldOrPropertyWithValue("name", "myButton")
                                                    .extracting(FXMLCopy::identifier)
                                                    .isInstanceOf(FXMLInternalIdentifier.class)
                                    )
                    );
        }

        @Test
        void observableListDefinition() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/ObservableListDefinition.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "ObservableListDefinition")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
                            doc -> assertThat(doc.imports())
                                    .hasSize(3)
                                    .containsExactly(
                                            "javafx.collections.FXCollections",
                                            "java.lang.Object",
                                            "java.lang.String"
                                    ),
                            doc -> assertThat(doc.root())
                                    .isInstanceOf(FXMLObject.class)
                                    .extracting(FXMLObject.class::cast)
                                    .isNotNull()
                                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Object.class))
                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                    .hasFieldOrPropertyWithValue("properties", List.of()),
                            doc -> assertThat(doc.definitions())
                                    .hasSize(1)
                                    .first()
                                    .isInstanceOf(FXMLCollection.class)
                                    .extracting(FXMLCollection.class::cast)
                                    .hasFieldOrPropertyWithValue("identifier", new FXMLExposedIdentifier("myList"))
                                    .hasFieldOrPropertyWithValue(
                                            "type",
                                            new FXMLGenericType(ObservableList.class, new FXMLClassType(String.class))
                                    )
                                    .hasFieldOrPropertyWithValue(
                                            "factoryMethod",
                                            Optional.of(new FXMLFactoryMethod(
                                                    FXCollections.class,
                                                    "observableArrayList"
                                            ))
                                    )
                                    .extracting(FXMLCollection::values, LIST_VALUE_ASSERT_FACTORY)
                                    .hasSize(3)
                                    .hasOnlyElementsOfType(FXMLValue.class)
                                    .extracting(FXMLValue.class::cast)
                                    .satisfiesExactly(
                                            first -> assertThat(first)
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            new FXMLClassType(String.class)
                                                    )
                                                    .hasFieldOrPropertyWithValue("value", "A")
                                                    .extracting(
                                                            FXMLValue::identifier,
                                                            OPTIONAL_IDENTIFIER_ASSERT_FACTORY
                                                    )
                                                    .containsInstanceOf(FXMLInternalIdentifier.class),
                                            second -> assertThat(second)
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            new FXMLClassType(String.class)
                                                    )
                                                    .hasFieldOrPropertyWithValue("value", "B")
                                                    .extracting(
                                                            FXMLValue::identifier,
                                                            OPTIONAL_IDENTIFIER_ASSERT_FACTORY
                                                    )
                                                    .containsInstanceOf(FXMLInternalIdentifier.class),
                                            third -> assertThat(third)
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            new FXMLClassType(String.class)
                                                    )
                                                    .hasFieldOrPropertyWithValue("value", "C")
                                                    .extracting(
                                                            FXMLValue::identifier,
                                                            OPTIONAL_IDENTIFIER_ASSERT_FACTORY
                                                    )
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    )
                    );
        }

        @Test
        void colorDefinitions() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/ColorDefinitions.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "ColorDefinitions")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
                            doc -> assertThat(doc.imports())
                                    .hasSize(2)
                                    .containsExactly("javafx.scene.paint.Color", "java.lang.Object"),
                            doc -> assertThat(doc.root())
                                    .isInstanceOf(FXMLObject.class)
                                    .extracting(FXMLObject.class::cast)
                                    .isNotNull()
                                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Object.class))
                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                    .hasFieldOrPropertyWithValue("properties", List.of()),
                            doc -> assertThat(doc.definitions())
                                    .hasSize(3)
                                    .satisfiesExactly(
                                            first -> assertThat(first)
                                                    .isInstanceOf(FXMLObject.class)
                                                    .extracting(FXMLObject.class::cast)
                                                    .hasFieldOrPropertyWithValue(
                                                            "identifier",
                                                            new FXMLExposedIdentifier("attributes")
                                                    )
                                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Color.class))
                                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                                    .hasSize(3)
                                                    .hasOnlyElementsOfType(FXMLConstructorProperty.class)
                                                    .satisfiesExactlyInAnyOrder(
                                                            firstProp -> assertThat(firstProp)
                                                                    .hasFieldOrPropertyWithValue("name", "red")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("1.0")
                                                                    ),
                                                            secondProp -> assertThat(secondProp)
                                                                    .hasFieldOrPropertyWithValue("name", "green")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("0.5")
                                                                    ),
                                                            thirdProp -> assertThat(thirdProp)
                                                                    .hasFieldOrPropertyWithValue("name", "blue")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("0.01")
                                                                    )
                                                    ),
                                            second -> assertThat(second)
                                                    .isInstanceOf(FXMLObject.class)
                                                    .extracting(FXMLObject.class::cast)
                                                    .hasFieldOrPropertyWithValue(
                                                            "identifier",
                                                            new FXMLExposedIdentifier("elements")
                                                    )
                                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Color.class))
                                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                                    .hasSize(3)
                                                    .hasOnlyElementsOfType(FXMLConstructorProperty.class)
                                                    .satisfiesExactlyInAnyOrder(
                                                            firstProp -> assertThat(firstProp)
                                                                    .hasFieldOrPropertyWithValue("name", "red")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("0.5")
                                                                    ),
                                                            secondProp -> assertThat(secondProp)
                                                                    .hasFieldOrPropertyWithValue("name", "green")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("1.0")
                                                                    ),
                                                            thirdProp -> assertThat(thirdProp)
                                                                    .hasFieldOrPropertyWithValue("name", "blue")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            new FXMLLiteral("0.5")
                                                                    )
                                                    ),
                                            third -> assertThat(third)
                                                    .isInstanceOf(FXMLValue.class)
                                                    .extracting(FXMLValue.class::cast)
                                                    .hasFieldOrPropertyWithValue(
                                                            "identifier",
                                                            Optional.of(new FXMLExposedIdentifier("valueOfMethod"))
                                                    )
                                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Color.class))
                                                    .hasFieldOrPropertyWithValue("value", "#f0f0f0")
                                    )
                    );
        }

        @Test
        void fxInclude() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/FXInclude.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "FXInclude")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("imports", List.of("javafx.scene.layout.VBox"))
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(VBox.class))
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .hasOnlyElementsOfType(FXMLCollectionProperties.class)
                    .first(InstanceOfAssertFactories.type(FXMLCollectionProperties.class))
                    .hasFieldOrPropertyWithValue("name", "children")
                    .hasFieldOrPropertyWithValue("getter", "getChildren")
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, new FXMLClassType(Node.class))
                    )
                    // Validate children values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
                    .hasOnlyElementsOfType(FXMLInclude.class)
                    .first(InstanceOfAssertFactories.type(FXMLInclude.class))
                    .hasFieldOrPropertyWithValue("sourceFile", "/examples/ExplicitDefault.fxml")
                    .hasFieldOrPropertyWithValue("resources", Optional.empty())
                    .hasFieldOrPropertyWithValue("charset", StandardCharsets.UTF_8)
                    .extracting(FXMLInclude::identifier)
                    .isInstanceOf(FXMLInternalIdentifier.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"FXRootNoId", "FXRootWithId", "NoFXRootNoId", "NoFXRootWithId"})
        void root(String className) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/%s.fxml".formatted(className));

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples/");

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", className)
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .hasFieldOrPropertyWithValue("imports", List.of("javafx.scene.layout.BorderPane"))
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(BorderPane.class))
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue(
                            "identifier", switch (className) {
                                case "FXRootNoId", "NoFXRootNoId" -> FXMLRootIdentifier.INSTANCE;
                                case "FXRootWithId", "NoFXRootWithId" -> new FXMLNamedRootIdentifier("myRoot");
                                default -> throw new IllegalArgumentException("Invalid test case: " + className);
                            }
                    );
        }
    }
}
