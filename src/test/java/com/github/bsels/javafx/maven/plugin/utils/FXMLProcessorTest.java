package com.github.bsels.javafx.maven.plugin.utils;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLController;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.github.bsels.javafx.maven.plugin.fxml.ProcessedFXML;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerField;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.Visibility;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import com.github.bsels.javafx.maven.plugin.utils.models.NamedConstructorParameter;
import com.github.bsels.javafx.maven.plugin.utils.models.TestController;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLProcessorTest {
    private FXMLProcessor fxmlProcessor;

    @BeforeEach
    void setUp() {
        fxmlProcessor = new FXMLProcessor(new DefaultLog(new ConsoleLogger()));
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create processor with valid log")
        void shouldCreateProcessorWithValidLog() {
            // Given
            Log log = new DefaultLog(new ConsoleLogger());

            // When
            FXMLProcessor processor = new FXMLProcessor(log);

            // Then
            assertThat(processor)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("log", log);
        }

        @Test
        @DisplayName("Should throw NullPointerException when log is null")
        void shouldThrowNullPointerExceptionWhenLogIsNull() {
            // When & Then
            assertThatThrownBy(() -> new FXMLProcessor(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`log` must not be null");
        }
    }

    @Nested
    @DisplayName("Process method tests")
    class ProcessMethodTests {

        @Test
        @DisplayName("Should process simple FXML with basic structure")
        void shouldProcessSimpleFxmlWithBasicStructure() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Button",
                    Map.of("text", "Click me"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Button"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "TestController");

            assertThat(result.imports())
                    .contains("javafx.scene.control.Button");

            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class);

            assertThat(result.fields())
                    .hasSize(1);

            assertThat(result.methods())
                    .isEmpty();
        }

        @Test
        @DisplayName("Should process FXML with fx:id attributes")
        void shouldProcessFxmlWithFxIdAttributes() {
            // Given
            ParsedXMLStructure childStructure = new ParsedXMLStructure(
                    "Label",
                    Map.of("fx:id", "myLabel", "text", "Hello"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(childStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Label", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fields())
                    .hasSize(2)
                    .anyMatch(field -> "myLabel".equals(field.name()) && !field.internal())
                    .anyMatch(field -> "$internalVariable$000".equals(field.name()) && field.internal());
        }

        @Test
        @DisplayName("Should process FXML with fx:constant")
        void shouldProcessFxmlWithFxConstant() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Double",
                    Map.of("fx:constant", "MAX_VALUE"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.lang.Double"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLConstantNode.class);

            FXMLConstantNode constantNode = (FXMLConstantNode) result.root();
            assertThat(constantNode)
                    .hasFieldOrPropertyWithValue("clazz", Double.class)
                    .hasFieldOrPropertyWithValue("constantIdentifier", "MAX_VALUE")
                    .hasFieldOrPropertyWithValue("constantType", double.class);
        }

        @Test
        @DisplayName("Should process FXML with fx:value")
        void shouldProcessFxmlWithFxValue() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "String",
                    Map.of("fx:value", "Hello World"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLValueNode.class);

            FXMLValueNode valueNode = (FXMLValueNode) result.root();
            assertThat(valueNode)
                    .hasFieldOrPropertyWithValue("value", "Hello World")
                    .hasFieldOrPropertyWithValue("clazz", String.class)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "$internalVariable$000");
        }

        @Test
        @DisplayName("Should process FXML with method references")
        void shouldProcessFxmlWithMethodReferences() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Button",
                    Map.of("onAction", "#handleButtonClick"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Button"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.methods())
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "handleButtonClick");
        }

        @Test
        @DisplayName("Should process FXML with static methods")
        void shouldProcessFxmlWithStaticMethods() {
            // Given
            ParsedXMLStructure staticMethodStructure = new ParsedXMLStructure(
                    "GridPane.columnIndex",
                    Map.of(),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "GridPane",
                    Map.of(),
                    List.of(staticMethodStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.GridPane"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class);

            FXMLObjectNode objectNode = (FXMLObjectNode) result.root();
            assertThat(objectNode.children())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("Should process FXML with static methods")
        void shouldProcessFxmlWithStaticProperty() {
            // Given
            ParsedXMLStructure label = new ParsedXMLStructure(
                    "Label",
                    Map.of("GridPane.columnIndex", "0"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "GridPane",
                    Map.of(),
                    List.of(label)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Label", "javafx.scene.layout.GridPane"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class);

            FXMLObjectNode objectNode = (FXMLObjectNode) result.root();
            assertThat(objectNode.children())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("Should handle empty imports list")
        void shouldHandleEmptyImportsList() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Object",
                    Map.of(),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of(),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.imports())
                    .isNotNull();

            assertThat(result.className())
                    .isEqualTo("TestController");
        }

        @Test
        void shouldHandleFont() {
            // Given
            ParsedXMLStructure font = new ParsedXMLStructure(
                    "Font",
                    Map.of("size", "12", "name", "Arial"),
                    List.of()
            );
            ParsedXMLStructure fontStructure = new ParsedXMLStructure(
                    "font",
                    Map.of(),
                    List.of(font)
            );
            ParsedXMLStructure insets0 = new ParsedXMLStructure(
                    "Insets",
                    Map.of(),
                    List.of()
            );
            ParsedXMLStructure insets0Structure = new ParsedXMLStructure(
                    "insets",
                    Map.of(),
                    List.of(insets0)
            );
            ParsedXMLStructure insets1 = new ParsedXMLStructure(
                    "Insets",
                    Map.of("top", "0.0"),
                    List.of()
            );
            ParsedXMLStructure insets1Structure = new ParsedXMLStructure(
                    "insets",
                    Map.of(),
                    List.of(insets1)
            );
            ParsedXMLStructure label1 = new ParsedXMLStructure(
                    "Label",
                    Map.of("text", "Hello", "fx:id", "label1"),
                    List.of(fontStructure, insets0Structure)
            );
            ParsedXMLStructure label2 = new ParsedXMLStructure(
                    "Label",
                    Map.of("text", "world", "fx:id", "label2"),
                    List.of(fontStructure, insets1Structure)
            );
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(label1, label2)
            );
            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Label", "javafx.scene.layout.VBox", "javafx.scene.text.Font", "javafx.geometry.Insets"),
                    rootStructure,
                    "TestController"
            );


            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.imports())
                    .isNotNull();

            assertThat(result.className())
                    .isEqualTo("TestController");
            System.out.println(result.root());
        }

        @Test
        @DisplayName("Should deduplicate identical nodes")
        void shouldNotDeduplicateIdenticalLabelNodes() {
            // Given
            ParsedXMLStructure duplicateChild1 = new ParsedXMLStructure(
                    "Label",
                    Map.of("text", "Same Text"),
                    List.of()
            );

            ParsedXMLStructure duplicateChild2 = new ParsedXMLStructure(
                    "Label",
                    Map.of("text", "Same Text"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(duplicateChild1, duplicateChild2)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Label", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result)
                    .isNotNull();
        }

        @Test
        void withMethodReference() {
            // Given
            ParsedXMLStructure button = new ParsedXMLStructure(
                    "Button",
                    Map.of("onAction", "#handleButtonClick"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(button)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Button", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(
                            processedFXML -> assertThat(processedFXML.fields())
                                    .isNotEmpty()
                                    .hasSize(2)
                                    .satisfiesExactly(
                                            first -> assertThat(first)
                                                    .hasFieldOrPropertyWithValue("name", "$internalVariable$000")
                                                    .hasFieldOrPropertyWithValue("internal", true)
                                                    .hasFieldOrPropertyWithValue("clazz", VBox.class),
                                            second -> assertThat(second)
                                                    .hasFieldOrPropertyWithValue("name", "$internalVariable$001")
                                                    .hasFieldOrPropertyWithValue("internal", true)
                                                    .hasFieldOrPropertyWithValue("clazz", Button.class)
                                    )
                    )
                    .satisfies(
                            processedFXML -> assertThat(processedFXML.methods())
                                    .isNotEmpty()
                                    .hasSize(1)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "handleButtonClick")
                                    .hasFieldOrPropertyWithValue("returnType", void.class)
                                    .satisfies(
                                            method -> assertThat(method.parameters())
                                                    .isNotEmpty()
                                                    .hasSize(1)
                                                    .containsExactly(ActionEvent.class)
                                    )
                    );

        }

        @Test
        void parameterizedMethodWithParameterizedType() {
            // Given
            ParsedXMLStructure column = new ParsedXMLStructure(
                    "TableColumn",
                    Map.of("onEditCancel", "#editCancel"),
                    List.of(),
                    List.of("generic 0: java.lang.Object", "generic 1: java.lang.Object")
            );

            ParsedXMLStructure tableView = new ParsedXMLStructure(
                    "TableView",
                    Map.of(),
                    List.of(new ParsedXMLStructure(
                            "columns",
                            Map.of(),
                            List.of(column)
                    )),
                    List.of("generic 0: java.lang.Object")
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(tableView)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.TableColumn", "javafx.scene.control.TableView", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result)
                    .isNotNull()
                    .satisfies(
                            processedFXML -> assertThat(processedFXML.methods())
                                    .isNotEmpty()
                                    .hasSize(1)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "editCancel")
                                    .hasFieldOrPropertyWithValue("returnType", void.class)
                                    .satisfies(
                                            method -> assertThat(method.parameters())
                                                    .isNotEmpty()
                                                    .hasSize(1)
                                                    .satisfiesExactly(
                                                            parameter -> assertThat(parameter)
                                                                    .isInstanceOf(ParameterizedType.class)
                                                                    .extracting(ParameterizedType.class::cast)
                                                                    .extracting(ParameterizedType::getRawType)
                                                                    .isEqualTo(TableColumn.CellEditEvent.class)
                                                    )
                                    )
                    );
        }
    }

    @Nested
    @DisplayName("Edge cases and error handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle complex nested structures")
        void shouldHandleComplexNestedStructures() {
            // Given
            ParsedXMLStructure nestedChild = new ParsedXMLStructure(
                    "Button",
                    Map.of("fx:id", "nestedButton", "text", "Nested"),
                    List.of()
            );

            ParsedXMLStructure middleChild = new ParsedXMLStructure(
                    "HBox",
                    Map.of("spacing", "10"),
                    List.of(nestedChild)
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of("fx:id", "rootVBox"),
                    List.of(middleChild)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Button", "javafx.scene.layout.VBox", "javafx.scene.layout.HBox"),
                    rootStructure,
                    "ComplexController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fields())
                    .hasSizeGreaterThan(1);

            assertThat(result.imports())
                    .contains("javafx.scene.control.Button",
                            "javafx.scene.layout.VBox",
                            "javafx.scene.layout.HBox");
        }

        @Test
        @DisplayName("Should process wrapper nodes correctly")
        void shouldProcessWrapperNodesCorrectly() {
            // Given
            ParsedXMLStructure wrapperChild = new ParsedXMLStructure(
                    "UnknownElement",
                    Map.of("someProperty", "value"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(wrapperChild)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("className", "WrapperController");
        }

        @Test
        void nonExistingConstantThrowRuntimeException() {

            // Given
            ParsedXMLStructure wrapperChild = new ParsedXMLStructure(
                    "String",
                    Map.of("fx:constant", "UNKNOWN_CONSTANT"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(wrapperChild)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(NoSuchFieldException.class)
                    .hasRootCauseMessage("UNKNOWN_CONSTANT");
        }

        @Test
        void nonStaticConstantForFXConstantThrowRuntimeException() {

            // Given
            ParsedXMLStructure wrapperChild = new ParsedXMLStructure(
                    "TestController",
                    Map.of("fx:constant", "hash"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(wrapperChild)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of(
                            "javafx.scene.layout.VBox",
                            "com.github.bsels.javafx.maven.plugin.utils.models.TestController"
                    ),
                    rootStructure,
                    "WrapperController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(NoSuchFieldException.class)
                    .hasRootCauseMessage("Not a static field: hash");
        }

        @Test
        void fxRootWithoutTypeConvertThrowIllegalArgumentException() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of(),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fx:root must have a type attribute");
        }

        @Test
        void fxRootWithTypeCorrectlyConvertedNoPropertiesLeft() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue("children", List.of());
        }

        @Test
        void styleClassAsPropertyValid() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox", "styleClass", "my-class"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("children", List.of())
                    .satisfies(
                            root -> assertThat(root.properties())
                                    .isNotEmpty()
                                    .hasSize(1)
                                    .first()
                                    .hasFieldOrPropertyWithValue("name", "styleClass")
                                    .hasFieldOrPropertyWithValue("value", "my-class")
                                    .hasFieldOrPropertyWithValue("type", String.class)
                                    .hasFieldOrPropertyWithValue("setter", "getStyleClass().add")
                    );
        }

        @Test
        void styleClassAsElementValid() {
            // Given
            ParsedXMLStructure styleClassStructure = new ParsedXMLStructure(
                    "styleClass",
                    Map.of(),
                    List.of(
                            new ParsedXMLStructure("String", Map.of("fx:value", "my-class"), List.of()),
                            new ParsedXMLStructure("String", Map.of("fx:value", "my-class"), List.of()),
                            new ParsedXMLStructure("String", Map.of("fx:value", "other-class"), List.of()),
                            new ParsedXMLStructure("Integer", Map.of("fx:value", "0"), List.of())
                    )
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox"),
                    List.of(styleClassStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .satisfies(
                            root -> assertThat(root.children())
                                    .isNotEmpty()
                                    .hasSize(1)
                                    .first()
                                    .isInstanceOf(FXMLWrapperNode.class)
                                    .extracting(FXMLWrapperNode.class::cast)
                                    .satisfies(
                                            wrapper -> assertThat(wrapper.children())
                                                    .satisfiesExactly(
                                                            child -> assertThat(child)
                                                                    .isInstanceOf(FXMLValueNode.class)
                                                                    .hasFieldOrPropertyWithValue("value", "my-class")
                                                                    .hasFieldOrPropertyWithValue("clazz", String.class),
                                                            child -> assertThat(child)
                                                                    .isInstanceOf(FXMLValueNode.class)
                                                                    .hasFieldOrPropertyWithValue("value", "my-class")
                                                                    .hasFieldOrPropertyWithValue("clazz", String.class),
                                                            child -> assertThat(child)
                                                                    .isInstanceOf(FXMLValueNode.class)
                                                                    .hasFieldOrPropertyWithValue("value", "other-class")
                                                                    .hasFieldOrPropertyWithValue("clazz", String.class),
                                                            child -> assertThat(child)
                                                                    .isInstanceOf(FXMLValueNode.class)
                                                                    .hasFieldOrPropertyWithValue("value", "0")
                                                                    .hasFieldOrPropertyWithValue("clazz", Integer.class)
                                                    )
                                    )
                    );
        }

        @Test
        void doubleSetterIgnoreProperty() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "TestController", "field123", "value"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("com.github.bsels.javafx.maven.plugin.utils.models.TestController"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", TestController.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue("children", List.of());
        }

        @Test
        void unknownStaticPropertySkipped() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox", "TestController.unknown", "value"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("com.github.bsels.javafx.maven.plugin.utils.models.TestController", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue("children", List.of());
        }

        @Test
        void doubleStaticPropertySkipped() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox", "TestController.staticField123", "value"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("com.github.bsels.javafx.maven.plugin.utils.models.TestController", "javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue("children", List.of());
        }

        @Test
        void unknownPropertySkipped() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "fx:root",
                    Map.of("type", "VBox", "unknownProperty", "value"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "this")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .hasFieldOrPropertyWithValue("children", List.of());
        }

        @Test
        void sameNamedConstructorParameterDifferentTypeSkipped() {
            // Given
            ParsedXMLStructure testControllerStructure = new ParsedXMLStructure(
                    "NamedConstructorParameter",
                    Map.of("test", "123"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(testControllerStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", "com.github.bsels.javafx.maven.plugin.utils.models.NamedConstructorParameter"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.root())
                    .isInstanceOf(FXMLObjectNode.class)
                    .extracting(FXMLObjectNode.class::cast)
                    .hasFieldOrPropertyWithValue("internal", true)
                    .hasFieldOrPropertyWithValue("identifier", "$internalVariable$000")
                    .hasFieldOrPropertyWithValue("clazz", VBox.class)
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    .satisfies(
                            root -> assertThat(root.children())
                                    .isNotEmpty()
                                    .hasSize(1)
                                    .first()
                                    .isInstanceOf(FXMLObjectNode.class)
                                    .hasFieldOrPropertyWithValue("internal", true)
                                    .hasFieldOrPropertyWithValue("identifier", "$internalVariable$001")
                                    .hasFieldOrPropertyWithValue("clazz", NamedConstructorParameter.class)
                                    .hasFieldOrPropertyWithValue("properties", List.of())
                                    .hasFieldOrPropertyWithValue("children", List.of())
                    );
        }

        @Test
        void specialParametrizedMethods() {
            // Given
            ParsedXMLStructure testControllerStructure = new ParsedXMLStructure(
                    "ParameterizedMethods",
                    Map.of("handler", "#myHandler", "parameterizedMethod", "#parameterizedMethod"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(testControllerStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", "com.github.bsels.javafx.maven.plugin.utils.models.ParameterizedMethods"),
                    rootStructure,
                    "WrapperController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);
            assertThat(result.methods())
                    .isNotEmpty()
                    .hasSize(2)
                    .satisfiesExactlyInAnyOrder(
                            method -> assertThat(method)
                                    .hasFieldOrPropertyWithValue("name", "myHandler")
                                    .hasFieldOrPropertyWithValue("returnType", void.class)
                                    .hasFieldOrPropertyWithValue("parameters", List.of()),
                            method -> assertThat(method)
                                    .hasFieldOrPropertyWithValue("name", "parameterizedMethod")
                                    .returns("R", m -> m.returnType().getTypeName())
                                    .hasFieldOrPropertyWithValue("parameters", List.of(Object.class))
                    );
        }

        @Test
        void nonFunctionalInterfaceThrowUnsupportedOperationException() {
            // Given
            ParsedXMLStructure testControllerStructure = new ParsedXMLStructure(
                    "ParameterizedMethods",
                    Map.of("notFunctionalInterface", "#invalid"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(testControllerStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", "com.github.bsels.javafx.maven.plugin.utils.models.ParameterizedMethods"),
                    rootStructure,
                    "WrapperController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Functional interface expected, but got %s".formatted(String.class));
        }

        @Test
        void nonParameterizedFunctionalInterfaceThrowUnsupportedOperationException() {
            // Given
            ParsedXMLStructure testControllerStructure = new ParsedXMLStructure(
                    "ParameterizedMethods",
                    Map.of("notParameterizedFunctionalInterface", "#invalid"),
                    List.of()
            );

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of(testControllerStructure)
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", "com.github.bsels.javafx.maven.plugin.utils.models.ParameterizedMethods"),
                    rootStructure,
                    "WrapperController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Functional interface expected, but got %s".formatted("java.util.ArrayList<java.lang.String>"));
        }
    }

    @Nested
    @DisplayName("Integration with logging")
    class LoggingIntegrationTests {

        @Test
        @DisplayName("Should log debug messages during processing")
        void shouldLogDebugMessagesDuringProcessing() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Button",
                    Map.of("text", "Test Button"),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.control.Button"),
                    rootStructure,
                    "LoggingTestController"
            );

            // When
            fxmlProcessor.process(parsedFXML);
        }
    }

    @Nested
    @DisplayName("Controller tests")
    class ControllerTests {

        @Test
        @DisplayName("Should process FXML without controller")
        void shouldProcessFxmlWithoutController() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of(),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox"),
                    rootStructure,
                    "NoControllerTest"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fxmlController())
                    .isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when controller is abstract")
        void shouldThrowExceptionWhenControllerIsAbstract() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of("fx:controller", "java.util.List"), // List is an abstract interface
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", "java.util.List"),
                    rootStructure,
                    "AbstractControllerTest"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Controller class 'java.util.List' is abstract");
        }

        @Test
        @DisplayName("Should process controller with instanceFields() of different visibility")
        void shouldProcessControllerWithFieldsOfDifferentVisibility() {
            // Given
            class TestControllerWithFields {
                public static int ignoredStaticField = 0;
                public final int ignoredFinalField = 0;

                public String publicField;
                protected long protectedField;
                int packagePrivateField;
                private boolean privateField;
            }

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of("fx:controller", TestControllerWithFields.class.getName()),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", TestControllerWithFields.class.getName()),
                    rootStructure,
                    "FieldVisibilityTest"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fxmlController())
                    .isPresent()
                    .get()
                    .extracting(FXMLController::instanceFields)
                    .asInstanceOf(InstanceOfAssertFactories.collection(ControllerField.class))
                    .hasSize(4)
                    .satisfiesExactlyInAnyOrder(
                            field -> assertThat(field)
                                    .hasFieldOrPropertyWithValue("name", "publicField")
                                    .hasFieldOrPropertyWithValue("type", String.class)
                                    .hasFieldOrPropertyWithValue("visibility", Visibility.PUBLIC),
                            field -> assertThat(field)
                                    .hasFieldOrPropertyWithValue("name", "protectedField")
                                    .hasFieldOrPropertyWithValue("type", long.class)
                                    .hasFieldOrPropertyWithValue("visibility", Visibility.PROTECTED),
                            field -> assertThat(field)
                                    .hasFieldOrPropertyWithValue("name", "packagePrivateField")
                                    .hasFieldOrPropertyWithValue("type", int.class)
                                    .hasFieldOrPropertyWithValue("visibility", Visibility.PACKAGE_PRIVATE),
                            field -> assertThat(field)
                                    .hasFieldOrPropertyWithValue("name", "privateField")
                                    .hasFieldOrPropertyWithValue("type", boolean.class)
                                    .hasFieldOrPropertyWithValue("visibility", Visibility.PRIVATE)
                    );
        }

        @Test
        @DisplayName("Should process controller with methods of different visibility")
        void shouldProcessControllerWithMethodsOfDifferentVisibility() throws NoSuchMethodException {
            // Given
            class TestControllerWithMethods {
                public int publicMethod(String data) {
                    return data.length();
                }

                protected void protectedMethod() {
                }

                long packagePrivateMethod(long x, long y) {
                    return x + y;
                }

                private void privateMethod() {
                }
            }

            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "VBox",
                    Map.of("fx:controller", TestControllerWithMethods.class.getName()),
                    List.of()
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("javafx.scene.layout.VBox", TestControllerWithMethods.class.getName()),
                    rootStructure,
                    "MethodVisibilityTest"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fxmlController())
                    .isPresent()
                    .get()
                    .extracting(FXMLController::instanceMethods)
                    .asInstanceOf(InstanceOfAssertFactories.collection(ControllerMethod.class))
                    .hasSize(16)
                    .containsExactlyInAnyOrder(
                            new ControllerMethod(Visibility.PROTECTED, "protectedMethod", void.class, List.of()),
                            new ControllerMethod(Visibility.PACKAGE_PRIVATE, "packagePrivateMethod", long.class, List.of(long.class, long.class)),
                            new ControllerMethod(Visibility.PRIVATE, "privateMethod", void.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC, "publicMethod", int.class, List.of(String.class)),
                            new ControllerMethod(Visibility.PROTECTED, "finalize", void.class, List.of()),
                            new ControllerMethod(Visibility.PRIVATE, "wait0", void.class, List.of(long.class)),
                            new ControllerMethod(Visibility.PUBLIC, "equals", boolean.class, List.of(Object.class)),
                            new ControllerMethod(Visibility.PUBLIC, "toString", String.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC,"hashCode", int.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC, "getClass", Object.class.getMethod("getClass").getGenericReturnType(), List.of()),
                            new ControllerMethod(Visibility.PROTECTED, "clone", Object.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC, "notify", void.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC, "notifyAll", void.class, List.of()),
                            new ControllerMethod(Visibility.PUBLIC, "wait", void.class, List.of(long.class)),
                            new ControllerMethod(Visibility.PUBLIC, "wait", void.class, List.of(long.class, int.class)),
                            new ControllerMethod(Visibility.PUBLIC, "wait", void.class, List.of())
                    );
        }
    }

    @Nested
    @DisplayName("Generics parsing tests")
    class GenericsParsingTests {

        @Test
        @DisplayName("Should process simple nested generics")
        void shouldProcessSimpleNestedGenerics() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<java.lang.String>")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fields())
                    .first()
                    .satisfies(field -> assertThat(field.generics())
                            .containsExactly("List<String>"));
        }

        @Test
        @DisplayName("Should process multiple nested generics")
        void shouldProcessMultipleNestedGenerics() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Map",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>", "generic 1: java.lang.String")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.Map", "java.lang.String", "java.util.List", "java.lang.Integer"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fields())
                    .first()
                    .satisfies(field -> assertThat(field.generics())
                            .containsExactly("Map<String, List<Integer>>", "String"));
        }

        @Test
        @DisplayName("Should process multiple generic indices")
        void shouldProcessMultipleGenericIndices() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Map",
                    Map.of(),
                    List.of(),
                    List.of("generic 1: java.lang.Integer", "generic 0: java.lang.String")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.Map", "java.lang.String", "java.lang.Integer"),
                    rootStructure,
                    "TestController"
            );

            // When
            ProcessedFXML result = fxmlProcessor.process(parsedFXML);

            // Then
            assertThat(result.fields())
                    .first()
                    .satisfies(field -> assertThat(field.generics())
                            .containsExactly("String", "Integer"));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic definition has unbalanced brackets (too many opening)")
        void shouldThrowExceptionWhenBracketsUnbalancedTooManyOpening() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<<java.lang.String>")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic definition has unbalanced brackets (extra opening)")
        void shouldThrowExceptionWhenBracketsUnbalancedOpening() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<java.lang.String<>>")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic definition has unbalanced brackets (extra closing)")
        void shouldThrowExceptionWhenBracketsUnbalancedClosing() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<java.lang.String>>")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic definition has unbalanced brackets (missing closing)")
        void shouldThrowExceptionWhenBracketsUnbalancedMissingClosing() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<java.lang.String")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic definition has closing bracket before opening")
        void shouldThrowExceptionWhenClosingBeforeOpening() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "List",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.List<java.lang.String>>")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.List", "java.lang.String"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when generic indices are not sequential")
        void shouldThrowExceptionWhenIndicesNotSequential() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Map",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.lang.String", "generic 2: java.lang.Integer")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.Map", "java.lang.String", "java.lang.Integer"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Generic type indices are not sequential after sort: 2");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when nested generic definition is invalid")
        void shouldThrowExceptionWhenNestedDefinitionInvalid() {
            // Given
            ParsedXMLStructure rootStructure = new ParsedXMLStructure(
                    "Map",
                    Map.of(),
                    List.of(),
                    List.of("generic 0: java.util.Map<java.lang.String,,java.lang.Integer>", "generic 1: java.lang.String")
            );

            ParsedFXML parsedFXML = new ParsedFXML(
                    List.of("java.util.Map", "java.lang.String", "java.lang.Integer"),
                    rootStructure,
                    "TestController"
            );

            // When & Then
            assertThatThrownBy(() -> fxmlProcessor.process(parsedFXML))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid generic definition");
        }
    }
}
