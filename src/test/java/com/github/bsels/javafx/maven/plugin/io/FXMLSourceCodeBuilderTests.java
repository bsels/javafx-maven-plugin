package com.github.bsels.javafx.maven.plugin.io;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.github.bsels.javafx.maven.plugin.utils.models.TestController;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLSourceCodeBuilderTest {

    private FXMLSourceCodeBuilder sourceCodeBuilder;

    @BeforeEach
    void setUp() {
        sourceCodeBuilder = new FXMLSourceCodeBuilder(new DefaultLog(new ConsoleLogger()));
    }

    @Nested
    @DisplayName("Constructor tests")
    class ClassConstructorTests {

        @Test
        @DisplayName("Should create builder with valid log")
        void shouldCreateBuilderWithValidLog() {
            // Given
            Log log = new DefaultLog(new ConsoleLogger());

            // When
            FXMLSourceCodeBuilder builder = new FXMLSourceCodeBuilder(log);

            // Then
            assertThat(builder)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("log", log);
        }
    }

    @Nested
    @DisplayName("Package declaration tests")
    class PackageDeclarationTests {

        @Test
        @DisplayName("Should add package declaration correctly")
        void shouldAddPackageDeclarationCorrectly() {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addPackage("com.example.test");

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("hasPackage", true);

            String sourceCode = result.openClass("TestClass", null, null, null).build();
            assertThat(sourceCode).startsWith("package com.example.test;\n\n");
        }

        @Test
        @DisplayName("Should handle null package name")
        void shouldHandleNullPackageName() {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addPackage(null);

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("hasPackage", true);
        }

        @Test
        @DisplayName("Should handle blank package name")
        void shouldHandleBlankPackageName() {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addPackage("   ");

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("hasPackage", true);
        }

        @Test
        @DisplayName("Should throw exception when package already set")
        void shouldThrowExceptionWhenPackageAlreadySet() {
            // Given
            sourceCodeBuilder.addPackage("com.example.first");

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addPackage("com.example.second"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Package has already been set or class definition has already been opened.");
        }
    }

    @Nested
    @DisplayName("Import statements tests")
    class ImportStatementsTests {

        @Test
        @DisplayName("Should add import statements in sorted order")
        void shouldAddImportStatementsInSortedOrder() {
            // Given
            Set<String> imports = Set.of(
                    "java.util.List",
                    "java.util.ArrayList",
                    "javafx.scene.control.Button"
            );

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addImports(imports);

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("hasPackage", true);

            String sourceCode = result.openClass("TestClass", null, null, null).build();
            assertThat(sourceCode)
                    .contains("import java.util.ArrayList;\n")
                    .contains("import java.util.List;\n")
                    .contains("import javafx.scene.control.Button;\n");
        }

        @Test
        @DisplayName("Should throw exception when imports already set")
        void shouldThrowExceptionWhenImportsAlreadySet() {
            // Given
            sourceCodeBuilder.openClass("TestClass", null, null, null);

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addImports(Set.of("java.util.List")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Imports have already been set.");
        }
    }

    @Nested
    @DisplayName("Class declaration tests")
    class ClassDeclarationTests {

        @Test
        @DisplayName("Should open simple class")
        void shouldOpenSimpleClass() {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.openClass("TestController", null, null, null);

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("classOpen", true)
                    .hasFieldOrPropertyWithValue("className", "TestController");

            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("public abstract class TestController")
                    .contains("@Generated(")
                    .containsPattern("date=\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\"");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should open class with parent class")
        void shouldOpenClassWithParentClass(List<String> generics) {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.openClass(
                    "TestController",
                    "BaseController",
                    generics,
                    null
            );

            // Then
            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("public abstract class TestController")
                    .contains("extends BaseController");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should open class with parent generics")
        void shouldOpenClassWithParentGenerics(Map<String, List<String>> interfaces) {
            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.openClass(
                    "TestController",
                    "BaseController",
                    List.of("String", "Integer"),
                    interfaces
            );

            // Then
            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("extends BaseController<String, Integer>");
        }

        @Test
        @DisplayName("Should open class with interfaces")
        void shouldOpenClassWithInterfaces() {
            // Given
            Map<String, List<String>> interfaces = Map.of(
                    "Initializable", List.of("URL", "ResourceBundle"),
                    "EventHandler", List.of("ActionEvent")
            );

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.openClass(
                    "TestController",
                    null,
                    null,
                    interfaces
            );

            // Then
            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("implements EventHandler<ActionEvent>, Initializable<URL, ResourceBundle>");
        }

        @Test
        @DisplayName("Should throw exception when class already open")
        void shouldThrowExceptionWhenClassAlreadyOpen() {
            // Given
            sourceCodeBuilder.openClass("FirstClass", null, null, null);

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.openClass("SecondClass", null, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class has already been opened.");
        }

        @Test
        @DisplayName("Should throw exception when class name is null")
        void shouldThrowExceptionWhenClassNameIsNull() {
            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.openClass(null, null, null, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw exception when class name is blank")
        void shouldThrowExceptionWhenClassNameIsBlank() {
            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.openClass("   ", null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Class name must not be blank.");
        }

        @Test
        void shouldThrowExceptionWhenAddingPackageAfterClassIsOpened() {
            // When & Then
            FXMLSourceCodeBuilder builder = sourceCodeBuilder.openClass("TestClass", null, null, null);
            assertThatThrownBy(() -> builder.addPackage("com.example.test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Package has already been set or class definition has already been opened.");

        }
    }

    @Nested
    @DisplayName("Resource bundle tests")
    class ResourceBundleTests {

        @Test
        @DisplayName("Should add resource bundle declaration")
        void shouldAddResourceBundleDeclaration() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addResourceBundle("ResourceBundle.getBundle(\"messages\")");

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(\"messages\");");
        }

        @Test
        @DisplayName("Should throw exception when class not opened")
        void shouldThrowExceptionWhenClassNotOpened() {
            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addResourceBundle("ResourceBundle.getBundle(\"messages\")"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class has not been opened or builder is marked as finished.");
        }
    }

    @Nested
    @DisplayName("Field declaration tests")
    class FieldDeclarationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should add private field")
        void shouldAddPrivateField(List<String> generics) {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addField(true, "myButton", "Button", generics);

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode).contains("private final Button myButton;");
        }

        @Test
        @DisplayName("Should add protected field")
        void shouldAddProtectedField() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addField(false, "myLabel", "Label", null);

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode).contains("protected final Label myLabel;");
        }

        @Test
        @DisplayName("Should add field with generics")
        void shouldAddFieldWithGenerics() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addField(
                    false,
                    "myList",
                    "List",
                    List.of("String")
            );

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode).contains("protected final List<String> myList;");
        }
    }

    @Nested
    @DisplayName("Abstract method tests")
    class AbstractMethodTests {

        @Test
        @DisplayName("Should add abstract method")
        void shouldAddAbstractMethod() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            FXMLMethod method = new FXMLMethod(
                    "handleButtonClick",
                    List.of(String.class, Integer.class),
                    void.class,
                    Map.of()
            );

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addAbstractMethod(method);

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("protected abstract void handleButtonClick(String param0, Integer param1);");
        }

        @Test
        @DisplayName("Should add abstract method with return type")
        void shouldAddAbstractMethodWithReturnType() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            FXMLMethod method = new FXMLMethod(
                    "calculateResult",
                    List.of(Double.class),
                    String.class,
                    Map.of()
            );

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addAbstractMethod(method);

            // Then
            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("protected abstract String calculateResult(Double param0);");
        }
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should add constructor with simple object node")
        void shouldAddConstructorWithSimpleObjectNode() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true,
                    "this",
                    VBox.class,
                    List.of(),
                    List.of(),
                    List.of()
            );
            Map<String, List<String>> genericTypes = Map.of();

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addConstructor(rootNode);

            // Then
            assertThat(result)
                    .isSameAs(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("hasConstructor", true);

            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("protected TestController() {")
                    .contains("super();")
                    .contains("}");
        }

        @Test
        @DisplayName("Should add constructor with value node")
        void shouldAddConstructorWithValueNode() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            FXMLValueNode rootNode = new FXMLValueNode(
                    true,
                    "this",
                    String.class,
                    "Hello World"
            );

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addConstructor(rootNode);

            // Then
            String sourceCode = result.build();
            assertThat(sourceCode)
                    .contains("protected TestController() {")
                    .contains("super();");
        }

        @Test
        @DisplayName("Should throw exception when constructor already set")
        void shouldThrowExceptionWhenConstructorAlreadySet() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            FXMLObjectNode rootNode = new FXMLObjectNode(true, "this", VBox.class, List.of(), List.of(), List.of());
            sourceCodeBuilder.addConstructor(rootNode);

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Constructor has already been set.");
        }

        @Test
        @DisplayName("Should throw exception that minimal constructor does not exists")
        void noMinimalConstructorFoundForAllNamedPatterns() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            FXMLObjectNode insetsNode = new FXMLObjectNode(
                    true,
                    "insets",
                    Insets.class,
                    List.of(
                            new FXMLConstructorProperty("topRightBottomLeft", "2.0", Double.class),
                            new FXMLConstructorProperty("top", "2.0", Double.class)
                    ),
                    List.of(),
                    List.of()
            );
            FXMLWrapperNode wrapperNode = new FXMLWrapperNode("insets", List.of(insetsNode));
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true, "this", VBox.class, List.of(), List.of(wrapperNode), List.of()
            );

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No matching constructor found for parameters: [topRightBottomLeft, top] for type class javafx.geometry.Insets");
        }
    }

    @Nested
    @DisplayName("Builder state tests")
    class BuilderStateTests {

        @Test
        @DisplayName("Should add new line")
        void shouldAddNewLine() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            FXMLSourceCodeBuilder result = sourceCodeBuilder.addLine();

            // Then
            assertThat(result).isSameAs(sourceCodeBuilder);

            String sourceCode = result.build();
            assertThat(sourceCode).contains("\n\n"); // Extra newline added
        }

        @Test
        @DisplayName("Should build source code")
        void shouldBuildSourceCode() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);

            // When
            String result = sourceCodeBuilder.build();

            // Then
            assertThat(result)
                    .isNotNull()
                    .contains("public abstract class TestController")
                    .endsWith("}\n");

            assertThat(sourceCodeBuilder)
                    .hasFieldOrPropertyWithValue("finished", true);
        }

        @Test
        @DisplayName("Should build source code only once")
        void shouldBuildSourceCodeOnlyOnce() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            String firstBuild = sourceCodeBuilder.build();

            // When
            String secondBuild = sourceCodeBuilder.build();

            // Then
            assertThat(secondBuild).isEqualTo(firstBuild);
        }

        @Test
        @DisplayName("Should throw exception when building without opening class")
        void shouldThrowExceptionWhenBuildingWithoutOpeningClass() {
            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class has not been opened.");
        }

        @Test
        @DisplayName("Should throw exception when adding to finished builder")
        void shouldThrowExceptionWhenAddingToFinishedBuilder() {
            // Given
            sourceCodeBuilder.openClass("TestController", null, null, null);
            sourceCodeBuilder.build(); // Marks as finished

            // When & Then
            assertThatThrownBy(() -> sourceCodeBuilder.addField(true, "test", "String", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class has not been opened or builder is marked as finished.");
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should build complete class with all components")
        void shouldBuildCompleteClassWithAllComponents() {
            // Given
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true,
                    "this",
                    VBox.class,
                    List.of(),
                    List.of(),
                    List.of()
            );

            FXMLMethod method = new FXMLMethod(
                    "handleAction",
                    List.of(String.class),
                    void.class,
                    Map.of()
            );

            // When
            String sourceCode = sourceCodeBuilder
                    .addPackage("com.example.test")
                    .addImports(Set.of("javafx.scene.control.Button", "javafx.scene.layout.VBox"))
                    .openClass("TestController", "VBox", null, null)
                    .addResourceBundle("ResourceBundle.getBundle(\"messages\")")
                    .addField(false, "myButton", "Button", null)
                    .addConstructor(rootNode)
                    .addAbstractMethod(method)
                    .build();

            // Then
            assertThat(sourceCode)
                    .startsWith("package com.example.test;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("import javafx.scene.layout.VBox;")
                    .contains("import javax.annotation.processing.Generated;")
                    .contains("import java.util.ResourceBundle;")
                    .contains("@Generated(")
                    .contains("public abstract class TestController")
                    .contains("extends VBox")
                    .contains("private static final ResourceBundle RESOURCE_BUNDLE")
                    .contains("protected final Button myButton;")
                    .contains("protected TestController() {")
                    .contains("super();")
                    .contains("protected abstract void handleAction(String param0);")
                    .endsWith("}\n");
        }

        @Test
        @DisplayName("Should handle complex constructor with properties")
        void shouldHandleComplexConstructorWithProperties() {
            // Given
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "text",
                    "setText",
                    String.class,
                    "Hello World"
            );

            FXMLObjectNode buttonChild = new FXMLObjectNode(
                    false,
                    "myButton",
                    Button.class,
                    List.of(property),
                    List.of(),
                    List.of()
            );

            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true,
                    "this",
                    VBox.class,
                    List.of(),
                    List.of(buttonChild),
                    List.of()
            );

            // When
            String sourceCode = sourceCodeBuilder
                    .openClass("TestController", "VBox", null, null)
                    .addField(false, "myButton", "Button", null)
                    .addConstructor(rootNode)
                    .build();

            // Then
            assertThat(sourceCode)
                    .contains("myButton = new Button();")
                    .contains("myButton.setText(\"Hello World\");")
                    .contains("super();");
        }

        @Test
        @DisplayName("Should handle border pane example")
        void borderPaneExampleValidSource() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLObjectNode insets = new FXMLObjectNode(
                        true, "insets$000", Insets.class, List.of(), List.of(), List.of()
                );
                FXMLStaticMethod borderPaneMargin = new FXMLStaticMethod(
                        BorderPane.class, "margin", List.of(insets)
                );
                FXMLWrapperNode padding = new FXMLWrapperNode("padding", List.of(insets));
                FXMLObjectNode gridPane = new FXMLObjectNode(
                        true, "gridPane$000", GridPane.class, List.of(), List.of(borderPaneMargin), List.of()
                );
                FXMLWrapperNode centerNode = new FXMLWrapperNode("center", List.of(gridPane));
                FXMLObjectNode rootNode = new FXMLObjectNode(
                        true, "this", BorderPane.class, List.of(), List.of(centerNode, padding), List.of()
                );

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", "BorderPane", null, null)
                        .addField(false, "gridPane$000", "GridPane", null)
                        .addField(false, "insets$000", "Insets", null)
                        .addConstructor(rootNode)
                        .build();

                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController
                                    extends BorderPane {
                                    protected final GridPane gridPane$000;
                                    protected final Insets insets$000;
                                
                                    protected TestController() {
                                        gridPane$000 = new GridPane();
                                        insets$000 = new Insets(0.0);
                                
                                        super();
                                
                                        this.setCenter(gridPane$000);
                                        BorderPane.setMargin(gridPane$000, insets$000);
                                        this.setPadding(insets$000);
                                    }
                                }
                                """.formatted(now));
            }
        }

        @Test
        @DisplayName("With constant in wrapping object and static method")
        void withConstantsInWrappingObject() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLConstantNode insets = new FXMLConstantNode(
                        Insets.class, "EMPTY", Insets.class
                );
                FXMLStaticMethod borderPaneMargin = new FXMLStaticMethod(
                        BorderPane.class, "margin", List.of(insets)
                );
                FXMLWrapperNode padding = new FXMLWrapperNode("padding", List.of(insets));
                FXMLObjectNode gridPane = new FXMLObjectNode(
                        true, "gridPane$000", GridPane.class, List.of(), List.of(borderPaneMargin), List.of()
                );
                FXMLWrapperNode centerNode = new FXMLWrapperNode("center", List.of(gridPane));
                FXMLObjectNode rootNode = new FXMLObjectNode(
                        true, "this", BorderPane.class, List.of(), List.of(centerNode, padding), List.of()
                );

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", "BorderPane", null, null)
                        .addField(false, "gridPane$000", "GridPane", null)
                        .addConstructor(rootNode)
                        .build();


                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController
                                    extends BorderPane {
                                    protected final GridPane gridPane$000;
                                
                                    protected TestController() {
                                        gridPane$000 = new GridPane();
                                
                                        super();
                                
                                        this.setCenter(gridPane$000);
                                        BorderPane.setMargin(gridPane$000, Insets.EMPTY);
                                        this.setPadding(Insets.EMPTY);
                                    }
                                }
                                """.formatted(now));
            }
        }

        @Test
        @DisplayName("wrapping node in wrapping node throws exception")
        void wrappingNodeInWrappingNodeThrowsException() {
            // Given
            FXMLConstantNode insets = new FXMLConstantNode(
                    Insets.class, "EMPTY", Insets.class
            );
            FXMLWrapperNode padding = new FXMLWrapperNode("padding", List.of(insets));
            FXMLWrapperNode centerNode = new FXMLWrapperNode("center", List.of(padding));
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true, "this", BorderPane.class, List.of(), List.of(centerNode, padding), List.of()
            );

            // When & Then
            sourceCodeBuilder.openClass("TestController", "BorderPane", null, null)
                    .addField(false, "gridPane$000", "GridPane", null)
                    .addField(false, "insets$000", "Insets", null);

            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unexpected child node");
        }

        @Test
        @DisplayName("wrapping node in wrapping node throws exception")
        void staticMethodInWrappingNodeThrowsException() {
            // Given
            FXMLConstantNode insets = new FXMLConstantNode(
                    Insets.class, "EMPTY", Insets.class
            );
            FXMLStaticMethod borderPaneMargin = new FXMLStaticMethod(
                    BorderPane.class, "margin", List.of(insets)
            );
            FXMLWrapperNode padding = new FXMLWrapperNode("padding", List.of(insets));
            FXMLWrapperNode centerNode = new FXMLWrapperNode("center", List.of(borderPaneMargin));
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true, "this", BorderPane.class, List.of(), List.of(centerNode, padding), List.of()
            );

            // When & Then
            sourceCodeBuilder.openClass("TestController", "BorderPane", null, null)
                    .addField(false, "gridPane$000", "GridPane", null)
                    .addField(false, "insets$000", "Insets", null);

            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unexpected child node");
        }

        @Test
        @DisplayName("Non observable list as getter and unknown static method, valid")
        void nonObservableListAsGetterAndUnknownStaticMethod() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLStaticMethod unknown = new FXMLStaticMethod(
                        TestController.class, "unknown", List.of()
                );
                FXMLConstantNode insets = new FXMLConstantNode(
                        TestPseudoClass.class, "TRUE", PseudoClass.class
                );
                FXMLWrapperNode padding = new FXMLWrapperNode("pseudoClassStates", List.of(insets));
                FXMLObjectNode rootNode = new FXMLObjectNode(
                        true, "this", BorderPane.class, List.of(), List.of(padding, unknown), List.of()
                );

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", "BorderPane", null, null)
                        .addConstructor(rootNode)
                        .build();

                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController
                                    extends BorderPane {
                                
                                    protected TestController() {
                                
                                        super();
                                
                                        this.getPseudoClassStates().add(TestPseudoClass.TRUE);
                                    }
                                }
                                """.formatted(now));
            }
        }

        @Test
        @DisplayName("Valid grid pane")
        void gridPaneValidSource() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLStaticMethod columnIndex = new FXMLStaticMethod(
                        GridPane.class, "columnIndex", List.of(new FXMLValueNode(true, "index0", int.class, "0"))
                );
                FXMLObjectNode buttonNode = new FXMLObjectNode(
                        true, "internal", Button.class, List.of(), List.of(columnIndex), List.of()
                );
                FXMLObjectNode rootNode = new FXMLObjectNode(
                        true, "this", GridPane.class, List.of(), List.of(buttonNode), List.of()
                );

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", "GridPane", null, null)
                        .addConstructor(rootNode)
                        .build();

                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController
                                    extends GridPane {
                                
                                    protected TestController() {
                                        internal = new Button();
                                        index0 = 0;
                                
                                        super();
                                
                                        this.getChildren().add(internal);
                                    }
                                }
                                """.formatted(now));
            }
        }

        @Test
        @DisplayName("Static method as static method argument, throws exception")
        void invalidStaticMethodAsStaticMethodArgumentThrowsException() {
            // Given
            FXMLStaticMethod internalStaticMethod = new FXMLStaticMethod(GridPane.class, "columnIndex", List.of());
            FXMLStaticMethod columnIndex = new FXMLStaticMethod(
                    GridPane.class, "columnIndex",
                    List.of(internalStaticMethod)
            );
            FXMLObjectNode buttonNode = new FXMLObjectNode(
                    true, "internal", Button.class, List.of(), List.of(columnIndex), List.of()
            );
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true, "this", GridPane.class, List.of(), List.of(buttonNode), List.of()
            );

            // When & Then
            sourceCodeBuilder.openClass("TestController", "GridPane", null, null);
            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unexpected static method node: %s".formatted(internalStaticMethod));
        }

        @Test
        @DisplayName("Wrapper node as static method argument, throws exception")
        void invalidWrappedNodeAsStaticMethodArgumentThrowsException() {
            // Given
            FXMLWrapperNode wrapperNode = new FXMLWrapperNode("columnIndex", List.of());
            FXMLStaticMethod columnIndex = new FXMLStaticMethod(
                    GridPane.class, "columnIndex",
                    List.of(wrapperNode)
            );
            FXMLObjectNode buttonNode = new FXMLObjectNode(
                    true, "internal", Button.class, List.of(), List.of(columnIndex), List.of()
            );
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    true, "this", GridPane.class, List.of(), List.of(buttonNode), List.of()
            );

            // When & Then
            sourceCodeBuilder.openClass("TestController", "GridPane", null, null);
            assertThatThrownBy(() -> sourceCodeBuilder.addConstructor(rootNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unexpected static method node: %s".formatted(wrapperNode));
        }

        @Test
        @DisplayName("Wrapper node as root")
        void wrapperNodeAsRoot() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLWrapperNode wrapperNode = new FXMLWrapperNode("wrapper", List.of());

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", null, null, null)
                        .addConstructor(wrapperNode)
                        .build();

                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController {
                                
                                    protected TestController() {
                                
                                        super();
                                
                                    }
                                }
                                """.formatted(now));
            }
        }

        @Test
        @DisplayName("Object node with special children to ignore")
        void objectNodeSpecialChildren() {
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);
                // Given
                FXMLObjectNode buttonRoot = new FXMLObjectNode(
                        true,
                        "this",
                        Button.class,
                        List.of(),
                        List.of(
                                new FXMLConstantNode(Insets.class, "EMPTY", Insets.class),
                                new FXMLObjectNode(
                                        true, "insets$000", Insets.class, List.of(), List.of(
                                        new FXMLStaticMethod(Integer.class, "parseInt", List.of())
                                ), List.of())
                        ),
                        List.of()
                );

                // When
                String sourceCode = sourceCodeBuilder
                        .openClass("TestController", "Button", null, null)
                        .addField(false, "insets$000", "Insets", null)
                        .addConstructor(buttonRoot)
                        .build();

                // Then
                assertThat(sourceCode)
                        .isEqualToIgnoringNewLines("""
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class TestController
                                    extends Button {
                                    protected final Insets insets$000;
                                
                                    protected TestController() {
                                        insets$000 = new Insets(0.0);
                                
                                        super();
                                
                                    }
                                }
                                """.formatted(now));
            }
        }
    }

    public static class TestPseudoClass extends PseudoClass {
        public static final PseudoClass TRUE = new TestPseudoClass();

        @Override
        public String getPseudoClassName() {
            return "test";
        }
    }
}
