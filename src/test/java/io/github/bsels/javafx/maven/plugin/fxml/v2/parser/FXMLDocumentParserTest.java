package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.TestHelpers;
import io.github.bsels.javafx.maven.plugin.examples.MetaDataHolder;
import io.github.bsels.javafx.maven.plugin.examples.Unsettable;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLFileScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLResource;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLTranslation;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<java.util.Map, org.assertj.core.api.MapAssert<FXMLLiteral, AbstractFXMLValue>> MAP_VALUES_ASSERT_FACTORY = InstanceOfAssertFactories.map(
            FXMLLiteral.class,
            AbstractFXMLValue.class
    );
    public static final FXMLClassType STRING_TYPE = new FXMLClassType(String.class);

    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser classUnderTest;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        classUnderTest = new FXMLDocumentParser(log, StandardCharsets.UTF_8);
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

    private ParsedFXML readFXML(String fxml) throws MojoExecutionException {
        Path fxmlFile = TestHelpers.getTestResourcePath(fxml);
        return fxmlReader.readFXML(fxmlFile);
    }

    private Path getRootPath() {
        return TestHelpers.getTestResourcePath("/examples").getParent();
    }

    @Nested
    class ParseValueStringTest {
        private Method parseValueString;
        private BuildContext buildContext;

        @BeforeEach
        void setUp() throws Exception {
            parseValueString = FXMLDocumentParser.class.getDeclaredMethod(
                    "parseValueString",
                    String.class,
                    FXMLType.class,
                    BuildContext.class
            );
            parseValueString.setAccessible(true);
            buildContext = new BuildContext(List.of(), "/base");
        }

        @Test
        void shouldParseTranslation() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "%myKey", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLTranslation("myKey"));
        }

        @Test
        void shouldParseLocationWithRelativePath() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "@my_image.png", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLResource("/base/my_image.png"));
        }

        @Test
        void shouldParseLocationWithAbsolutePath() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "@/other/image.png", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLResource("/other/image.png"));
        }

        @Test
        void shouldParseMethodReference() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "#myMethod", FXMLType.of(F.class), buildContext);
            assertThat(result)
                    .hasFieldOrPropertyWithValue("name", "myMethod")
                    .hasFieldOrPropertyWithValue("parameters", List.of(STRING_TYPE))
                    .hasFieldOrPropertyWithValue("returnType", STRING_TYPE);
        }

        @Test
        void shouldParseReference() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "$myId", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLReference("myId"));
        }

        @Test
        void shouldParseExpression() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "${myExpr}", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLExpression("myExpr"));
        }

        @Test
        void shouldThrowErrorOnUnclosedExpression() {
            assertThatThrownBy(() -> parseValueString.invoke(
                    classUnderTest,
                    "${unclosedExpr",
                    STRING_TYPE,
                    buildContext
            ))
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("`name` must be a valid Java identifier: {unclosedExpr");
        }

        @Test
        void shouldParseEscaped() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "\\$notRef", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLLiteral("$notRef"));
        }

        @Test
        void shouldParseEventHandler() throws Exception {
            Object result = parseValueString.invoke(
                    classUnderTest,
                    "myCode();",
                    FXMLType.of(EventHandler.class),
                    buildContext
            );
            assertThat(result)
                    .isEqualTo(new FXMLInlineScript("myCode();"));
        }

        @Test
        void shouldParseLiteral() throws Exception {
            Object result = parseValueString.invoke(classUnderTest, "myValue", STRING_TYPE, buildContext);
            assertThat(result)
                    .isEqualTo(new FXMLLiteral("myValue"));
        }

        @FunctionalInterface
        interface F {
            String apply(String value);
        }
    }

    @Nested
    class ExamplesTest {

        @Test
        void inMemoryScript() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/InMemoryScript.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

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
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Click Me!"))
                    );
        }

        @Test
        void externalScript() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/ExternalScript.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

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
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Click Me!"))
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {"ImplicitDefault", "ExplicitDefault"})
        void implicitExplicitDefault(String className) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/%s.fxml".formatted(className));

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", className)
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
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
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
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
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Button 2"))
                    );
        }

        @Test
        void myHashMap() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/MyHashMap.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "MyHashMap")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
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
                                    List.of(STRING_TYPE, STRING_TYPE)
                            )
                    )
                    .satisfies(
                            map -> assertThat(map.entries())
                                    .hasSize(3)
                                    .containsEntry(new FXMLLiteral("foo"), new FXMLLiteral("123"))
                                    .containsEntry(new FXMLLiteral("bar"), new FXMLLiteral("456"))
                                    .satisfies(
                                            entries -> assertThat(entries.get(new FXMLLiteral("test")))
                                                    .isInstanceOf(FXMLValue.class)
                                                    .hasFieldOrPropertyWithValue("value", "Dummy")
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            STRING_TYPE
                                                    )
                                    )
                    );
        }

        @Test
        void mapWithReferences() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/MapWithReferences.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    .isNotNull()
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLMap.class)
                    .extracting(FXMLMap.class::cast)
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLClassType(HashMap.class)
                    )
                    .satisfies(
                            map -> assertThat(map.entries())
                                    .hasSize(2)
                                    .containsEntry(new FXMLLiteral("refEntry"), new FXMLReference("myButton"))
                                    .satisfies(
                                            entries -> assertThat(entries.get(new FXMLLiteral("copyEntry")))
                                                    .isInstanceOf(FXMLCopy.class)
                                                    .extracting(FXMLCopy.class::cast)
                                                    .hasFieldOrPropertyWithValue("source.name", "myButton")
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
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "ObservableListDefinition")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
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
                                            new FXMLGenericType(ObservableList.class, STRING_TYPE)
                                    )
                                    .hasFieldOrPropertyWithValue(
                                            "factoryMethod",
                                            Optional.of(new FXMLFactoryMethod(
                                                    new FXMLClassType(FXCollections.class),
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
                                                            STRING_TYPE
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
                                                            STRING_TYPE
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
                                                            STRING_TYPE
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
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "ColorDefinitions")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .satisfies(
                            doc -> assertThat(doc.root())
                                    .isInstanceOf(FXMLObject.class)
                                    .extracting(FXMLObject.class::cast)
                                    .isNotNull()
                                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Object.class))
                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                    .hasFieldOrPropertyWithValue("properties", List.of()),
                            doc -> assertThat(doc.definitions())
                                    .hasSize(4)
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
                                                    .hasFieldOrPropertyWithValue("value", "#f0f0f0"),
                                            fourth -> assertThat(fourth)
                                                    .isInstanceOf(FXMLObject.class)
                                                    .extracting(FXMLObject.class::cast)
                                                    .hasFieldOrPropertyWithValue(
                                                            "identifier",
                                                            new FXMLExposedIdentifier("values")
                                                    )
                                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Color.class))
                                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                                    .hasSize(3)
                                                    .hasOnlyElementsOfType(FXMLConstructorProperty.class)
                                                    .extracting(FXMLConstructorProperty.class::cast)
                                                    .satisfiesExactlyInAnyOrder(
                                                            firstProp -> assertThat(firstProp)
                                                                    .hasFieldOrPropertyWithValue("name", "red")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .extracting(FXMLConstructorProperty::value)
                                                                    .isInstanceOf(FXMLValue.class)
                                                                    .extracting(FXMLValue.class::cast)
                                                                    .satisfies(
                                                                            value -> assertThat(value.identifier())
                                                                                    .containsInstanceOf(
                                                                                            FXMLInternalIdentifier.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(Double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue("value", "0.67"),
                                                            secondProp -> assertThat(secondProp)
                                                                    .hasFieldOrPropertyWithValue("name", "green")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .extracting(FXMLConstructorProperty::value)
                                                                    .isInstanceOf(FXMLValue.class)
                                                                    .extracting(FXMLValue.class::cast)
                                                                    .satisfies(
                                                                            value -> assertThat(value.identifier())
                                                                                    .containsInstanceOf(
                                                                                            FXMLInternalIdentifier.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(Double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue("value", "0.67"),
                                                            thirdProp -> assertThat(thirdProp)
                                                                    .hasFieldOrPropertyWithValue("name", "blue")
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(double.class)
                                                                    )
                                                                    .extracting(FXMLConstructorProperty::value)
                                                                    .isInstanceOf(FXMLValue.class)
                                                                    .extracting(FXMLValue.class::cast)
                                                                    .satisfies(
                                                                            value -> assertThat(value.identifier())
                                                                                    .containsInstanceOf(
                                                                                            FXMLInternalIdentifier.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            new FXMLClassType(Double.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue("value", "0.0")
                                                    )
                                    )
                    );
        }

        @Test
        void fxInclude() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/FXInclude.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "FXInclude")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
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
                    .hasSize(2)
                    .hasOnlyElementsOfType(FXMLInclude.class)
                    .extracting(FXMLInclude.class::cast)
                    .allSatisfy(
                            element -> assertThat(element)
                                    .hasFieldOrPropertyWithValue("sourceFile", "/examples/ExplicitDefault.fxml")
                                    .hasFieldOrPropertyWithValue("resources", Optional.empty())
                                    .hasFieldOrPropertyWithValue("charset", StandardCharsets.UTF_8)
                                    .extracting(FXMLInclude::identifier)
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    );
        }

        @ParameterizedTest
        @ValueSource(strings = {"FXRootNoId", "FXRootWithId", "NoFXRootNoId", "NoFXRootWithId"})
        void root(String className) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/%s.fxml".formatted(className));

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples/", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", className)
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
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

        @Test
        void myButtonWithConstants() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/MyButtonWithConstants.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "MyButtonWithConstants")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "minHeight")
                    .hasFieldOrPropertyWithValue("setter", "setMinHeight")
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(double.class))
                    .extracting("value")
                    .isInstanceOf(FXMLConstant.class)
                    .extracting(FXMLConstant.class::cast)
                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(Double.class))
                    .hasFieldOrPropertyWithValue("identifier", "NEGATIVE_INFINITY")
                    .hasFieldOrPropertyWithValue("constantType", new FXMLClassType(double.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"GridPaneStaticPropertyAttribute", "GridPaneStaticPropertyElement"})
        void gridPaneStaticProperties(String className) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/%s.fxml".formatted(className));

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", className)
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(GridPane.class))
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
                    // Validate children's values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
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
                    .first()
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Label.class))
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(3)
                    .satisfiesExactlyInAnyOrder(
                            first -> assertThat(first)
                                    .isInstanceOf(FXMLObjectProperty.class)
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("My Label")),
                            second -> assertThat(second)
                                    .isInstanceOf(FXMLStaticObjectProperty.class)
                                    .hasFieldOrPropertyWithValue("name", "rowIndex")
                                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(GridPane.class))
                                    .hasFieldOrPropertyWithValue("setter", "setRowIndex")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("0")),
                            third -> assertThat(third)
                                    .isInstanceOf(FXMLStaticObjectProperty.class)
                                    .hasFieldOrPropertyWithValue("name", "columnIndex")
                                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(GridPane.class))
                                    .hasFieldOrPropertyWithValue("setter", "setColumnIndex")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("0"))
                    );
        }

        @Test
        void gridPaneStaticPropertiesExplicitValue() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/GridPaneStaticPropertyElementExplicitValue.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "GridPaneStaticPropertyElementExplicitValue")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(GridPane.class))
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
                    // Validate children's values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
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
                    .first()
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Label.class))
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(3)
                    .satisfiesExactlyInAnyOrder(
                            first -> assertThat(first)
                                    .isInstanceOf(FXMLObjectProperty.class)
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("My Label")),
                            second -> assertThat(second)
                                    .isInstanceOf(FXMLStaticObjectProperty.class)
                                    .extracting(FXMLStaticObjectProperty.class::cast)
                                    .hasFieldOrPropertyWithValue("name", "rowIndex")
                                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(GridPane.class))
                                    .hasFieldOrPropertyWithValue("setter", "setRowIndex")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .extracting(FXMLStaticObjectProperty::value)
                                    .isInstanceOf(FXMLValue.class)
                                    .extracting(FXMLValue.class::cast)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .hasFieldOrPropertyWithValue("value", "0")
                                    .satisfies(
                                            staticProperty -> assertThat(staticProperty.identifier())
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    ),
                            third -> assertThat(third)
                                    .isInstanceOf(FXMLStaticObjectProperty.class)
                                    .extracting(FXMLStaticObjectProperty.class::cast)
                                    .hasFieldOrPropertyWithValue("name", "columnIndex")
                                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(GridPane.class))
                                    .hasFieldOrPropertyWithValue("setter", "setColumnIndex")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .extracting(FXMLStaticObjectProperty::value)
                                    .isInstanceOf(FXMLValue.class)
                                    .extracting(FXMLValue.class::cast)
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Integer.class))
                                    .hasFieldOrPropertyWithValue("value", "0")
                                    .satisfies(
                                            staticProperty -> assertThat(staticProperty.identifier())
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    )
                    );
        }

        @Test
        void locationResolution() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/LocationResolution.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "LocationResolution")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(ImageView.class))
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .hasFieldOrPropertyWithValue("name", "image")
                    .hasFieldOrPropertyWithValue("setter", "setImage")
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Image.class))
                    .extracting(FXMLObjectProperty::value)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Image.class))
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .satisfies(
                            image -> assertThat(image.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLConstructorProperty.class)
                    .extracting(FXMLConstructorProperty.class::cast)
                    .hasFieldOrPropertyWithValue("name", "url")
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("value", new FXMLResource("/examples/my_image.png"));
        }

        @Test
        void mapProperties() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/NodeProperties.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "NodeProperties")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
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
                    .hasSize(2)
                    .hasOnlyElementsOfType(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .satisfiesExactly(
                            first -> assertThat(first)
                                    .satisfies(
                                            object -> assertThat(object.identifier())
                                                    .isInstanceOf(FXMLInternalIdentifier.class)
                                    )
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                    .hasSize(1)
                                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                                    .extracting(FXMLObjectProperty.class::cast)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", new FXMLLiteral("Ignored")),
                            second -> assertThat(second)
                                    .satisfies(
                                            object -> assertThat(object.identifier())
                                                    .isInstanceOf(FXMLInternalIdentifier.class)
                                    )
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                                    .hasSize(2)
                                    .satisfiesExactly(
                                            first -> assertThat(first)
                                                    .isInstanceOf(FXMLObjectProperty.class)
                                                    .extracting(FXMLObjectProperty.class::cast)
                                                    .hasFieldOrPropertyWithValue("name", "text")
                                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            STRING_TYPE
                                                    )
                                                    .hasFieldOrPropertyWithValue(
                                                            "value",
                                                            new FXMLLiteral("Properties")
                                                    ),
                                            secondProperty -> assertThat(secondProperty)
                                                    .isInstanceOf(FXMLMapProperty.class)
                                                    .extracting(FXMLMapProperty.class::cast)
                                                    .hasFieldOrPropertyWithValue("name", "properties")
                                                    .hasFieldOrPropertyWithValue("getter", "getProperties")
                                                    .hasFieldOrPropertyWithValue(
                                                            "type",
                                                            new FXMLGenericType(
                                                                    ObservableMap.class,
                                                                    new FXMLClassType(Object.class),
                                                                    new FXMLClassType(Object.class)
                                                            )
                                                    )
                                                    .hasFieldOrPropertyWithValue(
                                                            "keyType",
                                                            FXMLType.OBJECT
                                                    )
                                                    .hasFieldOrPropertyWithValue(
                                                            "valueType",
                                                            FXMLType.OBJECT
                                                    )
                                                    .extracting(FXMLMapProperty::value, MAP_VALUES_ASSERT_FACTORY)
                                                    .hasSize(3)
                                                    .containsEntry(
                                                            new FXMLLiteral("attribute"),
                                                            new FXMLLiteral("Attribute")
                                                    )
                                                    .containsEntry(
                                                            new FXMLLiteral("element"),
                                                            new FXMLLiteral("Element")
                                                    )
                                                    .hasEntrySatisfying(
                                                            new FXMLLiteral("elementValue"), value -> assertThat(value)
                                                                    .isInstanceOf(FXMLValue.class)
                                                                    .extracting(FXMLValue.class::cast)
                                                                    .satisfies(
                                                                            fxmlValue -> assertThat(fxmlValue.identifier())
                                                                                    .containsInstanceOf(
                                                                                            FXMLInternalIdentifier.class)
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "type",
                                                                            STRING_TYPE
                                                                    )
                                                                    .hasFieldOrPropertyWithValue(
                                                                            "value",
                                                                            "ElementValue"
                                                                    )
                                                    )
                                    )
                    );
        }

        @Test
        void styling() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/Styling.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "Styling")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .hasOnlyElementsOfType(FXMLCollectionProperties.class)
                    .extracting(FXMLCollectionProperties.class::cast)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "styleClass")
                    .hasFieldOrPropertyWithValue("getter", "getStyleClass")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, STRING_TYPE)
                    )
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .first()
                    .isEqualTo(new FXMLLiteral("single-style-class"));
        }

        @Test
        void setListIgnoredAsAttribute() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/SetListIgnoredAsAttribute.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "SetListIgnoredAsAttribute")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(TableView.class, STRING_TYPE)
                    )
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .isEmpty();
        }

        @Test
        void setList() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/SetList.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "SetList")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(TableView.class, STRING_TYPE)
                    )
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "items")
                    .hasFieldOrPropertyWithValue("setter", "setItems")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, STRING_TYPE)
                    )
                    .extracting(FXMLObjectProperty::value)
                    .isInstanceOf(FXMLCollection.class)
                    .extracting(FXMLCollection.class::cast)
                    .satisfies(
                            collection -> assertThat(collection.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(ObservableList.class, STRING_TYPE)
                    )
                    .hasFieldOrPropertyWithValue(
                            "factoryMethod", Optional.of(
                                    new FXMLFactoryMethod(new FXMLClassType(FXCollections.class), "observableArrayList")
                            )
                    )
                    .extracting(FXMLCollection::values, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(3)
                    .hasOnlyElementsOfType(FXMLValue.class)
                    .extracting(FXMLValue.class::cast)
                    .satisfiesExactly(
                            first -> assertThat(first)
                                    .satisfies(
                                            value -> assertThat(value.identifier())
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    )
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", "Item 0"),
                            second -> assertThat(second)
                                    .satisfies(
                                            value -> assertThat(value.identifier())
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    )
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", "Item 1"),
                            third -> assertThat(third)
                                    .satisfies(
                                            value -> assertThat(value.identifier())
                                                    .containsInstanceOf(FXMLInternalIdentifier.class)
                                    )
                                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                                    .hasFieldOrPropertyWithValue("value", "Item 2")
                    );
        }

        @Test
        void setMap() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/SetMap.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "SetMap")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(MetaDataHolder.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "metaData")
                    .hasFieldOrPropertyWithValue("setter", "setMetaData")
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(
                                    Map.class,
                                    STRING_TYPE,
                                    STRING_TYPE
                            )
                    )
                    .extracting(FXMLObjectProperty::value)
                    .isInstanceOf(FXMLMap.class)
                    .extracting(FXMLMap.class::cast)
                    .satisfies(
                            collection -> assertThat(collection.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    .hasFieldOrPropertyWithValue(
                            "type",
                            new FXMLGenericType(
                                    HashMap.class,
                                    STRING_TYPE,
                                    STRING_TYPE
                            )
                    )
                    .hasFieldOrPropertyWithValue("keyType", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("valueType", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .extracting(FXMLMap::entries, MAP_VALUES_ASSERT_FACTORY)
                    .hasSize(1)
                    .containsEntry(new FXMLLiteral("item"), new FXMLLiteral("Data"));
        }

        @Test
        void unsettableProperty() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/UnsettableProperty.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML, "/examples", getRootPath());

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "UnsettableProperty")
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.empty())
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Unsettable.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .isEmpty();
        }
    }

    @Nested
    class InvalidExampleTest {

        @Test
        void doubleFxRoot() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/DoubleFxRoot.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("fx:root must be the root element of the FXML document");
        }

        @Test
        void notAnObjectRoot() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/NotAnObjectRoot.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Root object must be an instance of object, collection, or map, but was Double");
        }

        @Test
        void fxDefineRoot() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/FXDefineRoot.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Root object must be an instance of object, collection, or map, but was fx:define");
        }

        @Test
        void incompleteFXCopy() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/IncompleteFXCopy.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`source` attribute is required for fx:copy");
        }

        @Test
        void incompleteFXInclude() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/IncompleteFXInclude.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`source` attribute is required for fx:include");
        }

        @Test
        void incompleteFXRoot() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/IncompleteFXRoot.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fx:root must have a 'type' attribute");
        }

        @Test
        void incompleteFXReference() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/IncompleteFXReference.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`source` attribute is required for fx:reference");
        }

        @ParameterizedTest
        @ValueSource(strings = {"HashMapKeyButNoValue", "HashMapKeyButMultipleValue"})
        void invalidMap(String file) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/%s.fxml".formatted(file));

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Map entry element `test` must have exactly one child element representing the value");
        }

        @ParameterizedTest
        @ValueSource(strings = {"GridPaneStaticPropertyElementMultipleValues", "GridPaneStaticPropertyElementNoValue"})
        void noOrMultipleStaticPropertyValues(String file) throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/%s.fxml".formatted(file));

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Expected single value for the static property");
        }

        @Test
        void doubleValueStyling() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/invalid/DoubleValueStyling.fxml");

            // Act and Assert
            assertThatThrownBy(() -> classUnderTest.parse(parsedFXML, "/examples/invalid/", getRootPath()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Multiple values found for property: style");
        }
    }
}
