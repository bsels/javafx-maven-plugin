package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.TestHelpers;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLResource;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLTranslation;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class FXMLSourceCodeBuilderTest {
    private static final ZonedDateTime ZONED_DATE_TIME_NOW_MOCK = ZonedDateTime.of(
            2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );

    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser documentParser;
    private FXMLSourceCodeBuilder classUnderTest;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        documentParser = new FXMLDocumentParser(log, StandardCharsets.UTF_8);
        originalJavaHome = System.getProperty("java.home");
        System.setProperty("java.home", "/java/home");
        assertThat(fxmlReader.toString())
                .isNotNull()
                .startsWith("FXMLReader[log=");

        MockedStatic<ZonedDateTime> zonedDateTimeMock = Mockito.mockStatic(ZonedDateTime.class);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(ZoneOffset.UTC)).thenReturn(ZONED_DATE_TIME_NOW_MOCK);
        classUnderTest = new FXMLSourceCodeBuilder(log, "org.example.Translations.RESOURCE_BUNDLE", true);
        zonedDateTimeMock.close();
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

    private FXMLDocument parse(String fxml) throws MojoExecutionException {
        ParsedFXML parsedFXML = readFXML(fxml);
        return documentParser.parse(parsedFXML, "/examples", getRootPath());
    }

    private Path getRootPath() {
        return TestHelpers.getTestResourcePath("/examples").getParent();
    }

    @Nested
    class GenerateSourceCodeTest {

        /// Verifies that `ColorDefinitions.fxml` generates a class extending `Object` with four `Color` fields
        /// and constructor assignments using both direct and variable-based construction.
        @Test
        void colorDefinitions() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/ColorDefinitions.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.paint.Color;")
                    .contains("public class ColorDefinitions extends Object {")
                    .contains("protected final Color attributes;")
                    .contains("protected final Color elements;")
                    .contains("protected final Color valueOfMethod;")
                    .contains("protected final Color values;")
                    .contains("attributes = new Color(1.0, 0.5, 0.01, 1);")
                    .contains("elements = new Color(0.5, 1.0, 0.5, 1);")
                    .contains("valueOfMethod = Color.valueOf(\"#f0f0f0\");")
                    .contains("super();");
        }

        /// Verifies that `SetList.fxml` generates a class extending `TableView<String>` with an observable list
        /// populated with three string items.
        @Test
        void setList() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/SetList.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.collections.FXCollections;")
                    .contains("import javafx.collections.ObservableList;")
                    .contains("public class SetList extends TableView<String> {")
                    .contains("ObservableList<String> $internalVariable$000 = FXCollections.observableArrayList();")
                    .contains("this.setItems($internalVariable$000);")
                    .contains("String $internalVariable$001 = \"Item 0\";")
                    .contains("String $internalVariable$002 = \"Item 1\";")
                    .contains("String $internalVariable$003 = \"Item 2\";")
                    .contains("$internalVariable$000.add($internalVariable$001);")
                    .contains("$internalVariable$000.add($internalVariable$002);")
                    .contains("$internalVariable$000.add($internalVariable$003);");
        }

        /// Verifies that `MapWithReferences.fxml` generates a class extending `HashMap` with a named `Button`
        /// field and map entries for both a copy and a reference.
        @Test
        void mapWithReferences() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/MapWithReferences.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import java.util.HashMap;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("public class MapWithReferences extends HashMap {")
                    .contains("protected final Button myButton;")
                    .contains("Button $$myButton = new Button();")
                    .contains("myButton = $$myButton;")
                    .contains("myButton.setText(\"Original\");")
                    .contains("this.put(\"refEntry\", myButton)")
                    .contains("this.put(\"copyEntry\",");
        }

        /// Verifies that `GridPaneStaticPropertyElement.fxml` generates a class extending `GridPane`
        /// with a `Label` child and static property calls using element syntax.
        @Test
        void gridPaneStaticPropertyElement() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/GridPaneStaticPropertyElement.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.GridPane;")
                    .contains("import javafx.scene.control.Label;")
                    .contains("public class GridPaneStaticPropertyElement extends GridPane {")
                    .contains("Label $internalVariable$000 = new Label();")
                    .contains("this.getChildren().add($internalVariable$000);")
                    .contains("$internalVariable$000.setText(\"My Label\");")
                    .contains("GridPane.setRowIndex($internalVariable$000, 0);")
                    .contains("GridPane.setColumnIndex($internalVariable$000, 0);");
        }

        /// Verifies that `GridPaneStaticPropertyAttribute.fxml` generates a class extending `GridPane`
        /// with a `Label` child and static property calls using attribute syntax.
        @Test
        void gridPaneStaticPropertyAttribute() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/GridPaneStaticPropertyAttribute.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.GridPane;")
                    .contains("import javafx.scene.control.Label;")
                    .contains("public class GridPaneStaticPropertyAttribute extends GridPane {")
                    .contains("Label $internalVariable$000 = new Label();")
                    .contains("this.getChildren().add($internalVariable$000);")
                    .contains("$internalVariable$000.setText(\"My Label\");")
                    .contains("GridPane.setColumnIndex($internalVariable$000, 0);")
                    .contains("GridPane.setRowIndex($internalVariable$000, 0);");
        }

        /// Verifies that `GridPaneStaticPropertyElementExplicitValue.fxml` generates a class extending `GridPane`
        /// with explicit `Integer` variables for row and column indices.
        @Test
        void gridPaneStaticPropertyElementExplicitValue() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/GridPaneStaticPropertyElementExplicitValue.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.GridPane;")
                    .contains("import javafx.scene.control.Label;")
                    .contains("public class GridPaneStaticPropertyElementExplicitValue extends GridPane {")
                    .contains("Label $internalVariable$000 = new Label();")
                    .contains("Integer $internalVariable$001 = 0;")
                    .contains("Integer $internalVariable$002 = 0;")
                    .contains("GridPane.setRowIndex($internalVariable$000, $internalVariable$001);")
                    .contains("GridPane.setColumnIndex($internalVariable$000, $internalVariable$002);");
        }

        /// Verifies that `MyButtonWithConstants.fxml` generates a class extending `Button`
        /// that sets a constant field value via `Double.NEGATIVE_INFINITY`.
        @Test
        void myButtonWithConstants() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/MyButtonWithConstants.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("public class MyButtonWithConstants extends Button {")
                    .contains("this.setMinHeight(Double.NEGATIVE_INFINITY);");
        }

        /// Verifies that `MyHashMap.fxml` generates a class extending `HashMap<String, String>`
        /// with three map entries put into the map.
        @Test
        void myHashMap() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/MyHashMap.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import java.util.HashMap;")
                    .contains("public class MyHashMap extends HashMap<String, String> {")
                    .contains("this.put(\"foo\", \"123\");")
                    .contains("this.put(\"bar\", \"456\");")
                    .contains("this.put(\"test\",");
        }

        /// Verifies that `NodeProperties.fxml` generates a class extending `VBox` with two `Button` children
        /// and property assignments including `getProperties().put(...)`.
        @Test
        void nodeProperties() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/NodeProperties.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.VBox;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("public class NodeProperties extends VBox {")
                    .contains("Button $internalVariable$000 = new Button();")
                    .contains("Button $internalVariable$001 = new Button();")
                    .contains("this.getChildren().add($internalVariable$000);")
                    .contains("this.getChildren().add($internalVariable$001);")
                    .contains("$internalVariable$000.setText(\"Ignored\");")
                    .contains("$internalVariable$001.setText(\"Properties\");")
                    .contains("$internalVariable$001.getProperties().put(");
        }

        /// Verifies that `FXInclude.fxml` generates a class extending `VBox` with two nested `ExplicitDefault`
        /// instances and a private static nested class for the included document.
        @Test
        void fxInclude() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/FXInclude.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.VBox;")
                    .contains("public class FXInclude extends VBox {")
                    .contains("FXInclude.ExplicitDefault $internalVariable$000 = new FXInclude.ExplicitDefault();")
                    .contains("FXInclude.ExplicitDefault $internalVariable$001 = new FXInclude.ExplicitDefault();")
                    .contains("this.getChildren().add($internalVariable$000);")
                    .contains("this.getChildren().add($internalVariable$001);")
                    .contains("private static class ExplicitDefault extends VBox {")
                    .contains("$internalVariable$000.setText(\"Button 1\");")
                    .contains("$internalVariable$002.setText(\"Button 2\");");
        }

        /// Verifies that `ButtonWithNoControllerAction.fxml` generates an abstract class extending `Button`
        /// with an abstract event handler method and a `setOnAction` call using a method reference.
        @Test
        void buttonWithNoControllerAction() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/ButtonWithNoControllerAction.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("import javafx.event.ActionEvent;")
                    .contains("public abstract class ButtonWithNoControllerAction extends Button {")
                    .contains("this.setOnAction(this::onButtonClick);")
                    .contains("this.setText(\"Click here...\");")
                    .contains("protected abstract void onButtonClick(ActionEvent param0);");
        }

        /// Verifies that `ButtonWithControllerAction.fxml` generates a class extending `Button`
        /// with a controller field, bind method, and `setOnAction` delegation.
        @Test
        void buttonWithControllerAction() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/ButtonWithControllerAction.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("public class ButtonWithControllerAction extends Button {")
                    .contains("private final TestController $internalField$controller$;")
                    .contains("$internalField$controller$ = new TestController();")
                    .contains("this.setOnAction(this::onButtonClick);")
                    .contains("this.setText(\"Click here...\");")
                    .contains("$bindController$();")
                    .contains("private void $bindController$()");
        }

        /// Verifies that `FXIncludeNestedController.fxml` generates a class extending `VBox`
        /// with exposed fields for the nested controller and its controller instance.
        @Test
        void fxIncludeNestedController() throws MojoExecutionException {
            String sourceCode = classUnderTest.generateSourceCode(parse("/examples/FXIncludeNestedController.fxml"), "com.example");
            assertThat(sourceCode)
                    .contains("package com.example;")
                    .contains("import javafx.scene.layout.VBox;")
                    .contains("public class FXIncludeNestedController extends VBox {")
                    .contains("protected final FXIncludeNestedController.ButtonWithControllerAction myButton;")
                    .contains("protected final TestController myButtonController;")
                    .contains("myButton = new FXIncludeNestedController.ButtonWithControllerAction();")
                    .contains("this.getChildren().add(myButton);")
                    .contains("myButtonController = myButton.$internalField$controller$;");
        }
    }

    /// Tests for the private `generateSourceCode(SourceCodeGeneratorContext, String, boolean)` method
    /// accessed via reflection.
    @Nested
    class GenerateSourceCodePrivateTest {

        /// Helper to invoke the private `generateSourceCode` method via reflection.
        ///
        /// @param instance       The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context        The [SourceCodeGeneratorContext] to pass.
        /// @param className      The class name to pass.
        /// @param isRootDocument Whether the document is the root document.
        /// @return The generated source code string.
        /// @throws Exception if reflection fails or the method throws.
        private String invokeGenerateSourceCode(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                String className,
                boolean isRootDocument
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "generateSourceCode",
                    SourceCodeGeneratorContext.class,
                    String.class,
                    boolean.class
            );
            method.setAccessible(true);
            return (String) method.invoke(instance, context, className, isRootDocument);
        }

        /// Creates a minimal [SourceCodeGeneratorContext] with empty imports and no package.
        ///
        /// @return A new [SourceCodeGeneratorContext] with empty state.
        private SourceCodeGeneratorContext minimalContext() {
            return new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()),
                    "",
                    Map.of(),
                    null
            );
        }

        /// Verifies that when `isRootDocument` is true, the package and imports sections are included
        /// in the output.
        @Test
        void rootDocumentIncludesPackageAndImports() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.PACKAGE).append("package com.example;\n");
            context.sourceCode(SourcePart.IMPORTS).append("import java.util.List;\n");
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("package com.example;\n")
                    .contains("import java.util.List;\n");
        }

        /// Verifies that when `isRootDocument` is false, the package and imports sections are omitted
        /// from the output.
        @Test
        void nonRootDocumentOmitsPackageAndImports() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.PACKAGE).append("package com.example;\n");
            context.sourceCode(SourcePart.IMPORTS).append("import java.util.List;\n");
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", false);

            assertThat(result)
                    .doesNotContain("package com.example;\n")
                    .doesNotContain("import java.util.List;\n");
        }

        /// Verifies that when `isRootDocument` is true and `addGeneratedAnnotation` is true,
        /// the `@Generated` annotation is included in the output.
        @Test
        void rootDocumentWithGeneratedAnnotationIncludesAnnotation() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains("@javax.annotation.processing.Generated");
        }

        /// Verifies that when `isRootDocument` is false, the `@Generated` annotation is not included
        /// even if `addGeneratedAnnotation` is true on the builder.
        @Test
        void nonRootDocumentOmitsGeneratedAnnotation() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", false);

            assertThat(result).doesNotContain("@javax.annotation.processing.Generated");
        }

        /// Verifies that when `addGeneratedAnnotation` is false on the builder, the `@Generated`
        /// annotation is not included even for a root document.
        @Test
        void rootDocumentWithoutGeneratedAnnotationOmitsAnnotation() throws Exception {
            DefaultLog log = new DefaultLog(new ConsoleLogger());
            FXMLSourceCodeBuilder builderWithoutAnnotation = new FXMLSourceCodeBuilder(log, "", false);
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(builderWithoutAnnotation, context, "MyClass", true);

            assertThat(result).doesNotContain("@javax.annotation.processing.Generated");
        }

        /// Verifies that the class declaration and constructor are always present in the output.
        @Test
        void outputAlwaysContainsClassDeclarationAndConstructor() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("public class MyClass {")
                    .contains("public MyClass()");
        }

        /// Verifies that when the `ABSTRACT_CLASS` feature is present, the constructor uses
        /// the `protected` modifier instead of `public`.
        @Test
        void abstractClassFeatureGeneratesProtectedConstructor() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public abstract class MyClass");
            context.addFeature(Feature.ABSTRACT_CLASS);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("protected MyClass()")
                    .doesNotContain("public MyClass()");
        }

        /// Verifies that without the `ABSTRACT_CLASS` feature, the constructor uses the `public` modifier.
        @Test
        void withoutAbstractClassFeatureGeneratesPublicConstructor() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains("public MyClass()");
        }

        /// Verifies that when the `RESOURCE_BUNDLE` feature is present, the resource bundle field
        /// is included in the output.
        @Test
        void resourceBundleFeatureGeneratesResourceBundleField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()),
                    "com.example.MyBundle.BUNDLE",
                    Map.of(),
                    null
            );
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.addFeature(Feature.RESOURCE_BUNDLE);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("private static final java.util.ResourceBundle")
                    .contains("com.example.MyBundle.BUNDLE");
        }

        /// Verifies that without the `RESOURCE_BUNDLE` feature, no resource bundle field is generated.
        @Test
        void withoutResourceBundleFeatureOmitsResourceBundleField() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).doesNotContain("java.util.ResourceBundle");
        }

        /// Verifies that when the `BIND_CONTROLLER` feature is present, the bind controller method
        /// call is appended to the constructor epilogue and the bind method is added to the methods section.
        @Test
        void bindControllerFeatureGeneratesBindMethodAndConstructorCall() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.CONTROLLER_FIELDS).append("this.field = field;\n");
            context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).append("controller.init();\n");
            context.addFeature(Feature.BIND_CONTROLLER);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("$bindController$()")
                    .contains("private void $bindController$()");
        }

        /// Verifies that without the `BIND_CONTROLLER` feature, no bind method is generated.
        @Test
        void withoutBindControllerFeatureOmitsBindMethod() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).doesNotContain("$bindController$()");
        }

        /// Verifies that when the `STRING_TO_URL_METHOD` feature is present, the string-to-URL
        /// conversion method is included in the output.
        @Test
        void stringToUrlFeatureGeneratesStringToUrlMethod() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.addFeature(Feature.STRING_TO_URL_METHOD);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains(FXMLSourceCodeBuilder.INTERNAL_STRING_TO_URL_METHOD);
        }

        /// Verifies that when the `STRING_TO_URI_METHOD` feature is present, the string-to-URI
        /// conversion method is included in the output.
        @Test
        void stringToUriFeatureGeneratesStringToUriMethod() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.addFeature(Feature.STRING_TO_URI_METHOD);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains(FXMLSourceCodeBuilder.INTERNAL_STRING_TO_URI_METHOD);
        }

        /// Verifies that when the `STRING_TO_PATH_METHOD` feature is present, the string-to-Path
        /// conversion method is included in the output.
        @Test
        void stringToPathFeatureGeneratesStringToPathMethod() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.addFeature(Feature.STRING_TO_PATH_METHOD);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains(FXMLSourceCodeBuilder.INTERNAL_STRING_TO_PATH_METHOD);
        }

        /// Verifies that when the `STRING_TO_FILE_METHOD` feature is present, the string-to-File
        /// conversion method is included in the output.
        @Test
        void stringToFileFeatureGeneratesStringToFileMethod() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.addFeature(Feature.STRING_TO_FILE_METHOD);

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains(FXMLSourceCodeBuilder.INTERNAL_STRING_TO_FILE_METHOD);
        }

        /// Verifies that when `isRootDocument` is true, nested types are indented and the class
        /// is closed with a single `}`.
        @Test
        void rootDocumentPlacesNestedTypesInsideClass() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.NESTED_TYPES).append("public static class Inner {}\n");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("    public static class Inner {}")
                    .endsWith("}\n");
        }

        /// Verifies that when `isRootDocument` is false, nested types are appended after the class
        /// closing brace without indentation.
        @Test
        void nonRootDocumentPlacesNestedTypesAfterClassClosingBrace() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.NESTED_TYPES).append("public static class Inner {}\n");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", false);

            assertThat(result)
                    .contains("}\n\n")
                    .contains("public static class Inner {}");
        }

        /// Verifies that fields defined in the context are included and indented in the output.
        @Test
        void fieldsAreIncludedAndIndented() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.FIELDS).append("private String myField;\n");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains("    private String myField;");
        }

        /// Verifies that constructor prologue, super call, and epilogue content are included
        /// and indented inside the constructor body.
        @Test
        void constructorPartsAreIncludedAndIndented() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).append("// prologue\n");
            context.sourceCode(SourcePart.CONSTRUCTOR_SUPER_CALL).append("super();\n");
            context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).append("this.x = 1;\n");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result)
                    .contains("        // prologue")
                    .contains("        super();")
                    .contains("        this.x = 1;");
        }

        /// Verifies that methods defined in the context are included and indented in the output.
        @Test
        void methodsAreIncludedAndIndented() throws Exception {
            SourceCodeGeneratorContext context = minimalContext();
            context.sourceCode(SourcePart.CLASS_DECLARATION).append("public class MyClass");
            context.sourceCode(SourcePart.METHODS).append("public void doSomething() {}\n");

            String result = invokeGenerateSourceCode(classUnderTest, context, "MyClass", true);

            assertThat(result).contains("    public void doSomething() {}");
        }
    }

    /// Tests for the private `addImports(SourceCodeGeneratorContext)` method accessed via reflection.
    @Nested
    class AddImportsPrivateTest {

        /// Helper to invoke the private `addImports` method via reflection.
        ///
        /// @param instance The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context  The [SourceCodeGeneratorContext] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddImports(FXMLSourceCodeBuilder instance, SourceCodeGeneratorContext context)
                throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addImports",
                    SourceCodeGeneratorContext.class
            );
            method.setAccessible(true);
            method.invoke(instance, context);
        }

        /// Verifies that non-primitive imports are written as sorted `import X;` lines followed by a blank line.
        @Test
        void nonPrimitiveImportsAreSortedAndFormatted() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of("java.util.Map", "java.util.List"), Map.of()),
                    "",
                    Map.of(),
                    null
            );

            invokeAddImports(classUnderTest, context);

            String imports = context.sourceCode(SourcePart.IMPORTS).toString();
            assertThat(imports)
                    .isEqualTo("import java.util.List;\nimport java.util.Map;\n\n");
        }

        /// Verifies that primitive type names are filtered out and not written as import statements.
        @Test
        void primitiveTypesAreFilteredOut() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of("int", "java.util.List", "boolean"), Map.of()),
                    "",
                    Map.of(),
                    null
            );

            invokeAddImports(classUnderTest, context);

            String imports = context.sourceCode(SourcePart.IMPORTS).toString();
            assertThat(imports)
                    .contains("import java.util.List;")
                    .doesNotContain("import int;")
                    .doesNotContain("import boolean;");
        }

        /// Verifies that when the import list is empty, only the trailing blank line is written.
        @Test
        void emptyImportListWritesOnlyTrailingNewline() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()),
                    "",
                    Map.of(),
                    null
            );

            invokeAddImports(classUnderTest, context);

            String imports = context.sourceCode(SourcePart.IMPORTS).toString();
            assertThat(imports).isEqualTo("\n");
        }

        /// Verifies that imports are written in lexicographic order regardless of the order they were provided.
        @Test
        void importsAreWrittenInLexicographicOrder() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of("java.util.Set", "java.util.ArrayList", "java.util.Map"), Map.of()),
                    "",
                    Map.of(),
                    null
            );

            invokeAddImports(classUnderTest, context);

            String imports = context.sourceCode(SourcePart.IMPORTS).toString();
            int posArrayList = imports.indexOf("import java.util.ArrayList;");
            int posMap = imports.indexOf("import java.util.Map;");
            int posSet = imports.indexOf("import java.util.Set;");
            assertThat(posArrayList).isLessThan(posMap);
            assertThat(posMap).isLessThan(posSet);
        }
    }

    /// Tests for the private `addControllerInitializeMethod(SourceCodeGeneratorContext, FXMLDocument)` method
    /// accessed via reflection.
    @Nested
    class AddControllerInitializeMethodPrivateTest {

        /// Helper to invoke the private `addControllerInitializeMethod` method via reflection.
        ///
        /// @param instance The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context  The [SourceCodeGeneratorContext] to pass.
        /// @param document The [FXMLDocument] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddControllerInitializeMethod(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                FXMLDocument document
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addControllerInitializeMethod",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class
            );
            method.setAccessible(true);
            method.invoke(instance, context, document);
        }

        /// Creates a minimal [FXMLDocument] with no controller.
        ///
        /// @return A [FXMLDocument] with an empty root object and no controller.
        private FXMLDocument documentWithoutController() {
            return new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Creates a minimal [FXMLDocument] with a controller that has the given methods.
        ///
        /// @param methods The list of controller methods to include.
        /// @return A [FXMLDocument] with a controller containing the specified methods.
        private FXMLDocument documentWithController(List<FXMLControllerMethod> methods) {
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class),
                    List.of(),
                    methods
            );
            return new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.of(controller),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Verifies that when the document has no controller, the method returns without modifying the context.
        @Test
        void noControllerLeavesContextUnchanged() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithoutController();

            invokeAddControllerInitializeMethod(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString()).isEmpty();
            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that when the controller has no `initialize` method, the context is not modified.
        @Test
        void controllerWithNoInitializeMethodLeavesContextUnchanged() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod otherMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "doSomething", false,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLDocument document = documentWithController(List.of(otherMethod));

            invokeAddControllerInitializeMethod(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString()).isEmpty();
            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that a zero-parameter `initialize` method causes `BIND_CONTROLLER` to be added
        /// and `CONTROLLER_INITIALIZATION` to be populated.
        @Test
        void zeroParamInitializeMethodAddsBindControllerFeature() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod initMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "initialize", false,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLDocument document = documentWithController(List.of(initMethod));

            invokeAddControllerInitializeMethod(classUnderTest, context, document);

            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
            assertThat(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString()).isNotEmpty();
        }

        /// Verifies that a two-parameter `initialize(URL, ResourceBundle)` method is preferred over
        /// a zero-parameter one, and that `BIND_CONTROLLER` is added.
        @Test
        void twoParamInitializeMethodIsPreferredAndAddsBindControllerFeature() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod zeroParam = new FXMLControllerMethod(
                    Visibility.PUBLIC, "initialize", false,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLControllerMethod twoParam = new FXMLControllerMethod(
                    Visibility.PUBLIC, "initialize", false,
                    new FXMLClassType(void.class),
                    List.of(new FXMLClassType(java.net.URL.class),
                            new FXMLClassType(java.util.ResourceBundle.class))
            );
            FXMLDocument document = documentWithController(List.of(zeroParam, twoParam));

            invokeAddControllerInitializeMethod(classUnderTest, context, document);

            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
            assertThat(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString()).isNotEmpty();
        }

        /// Verifies that an `initialize` method with a wrong parameter count is ignored.
        @Test
        void initializeMethodWithWrongParamCountIsIgnored() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod oneParam = new FXMLControllerMethod(
                    Visibility.PUBLIC, "initialize", false,
                    new FXMLClassType(void.class),
                    List.of(new FXMLClassType(String.class))
            );
            FXMLDocument document = documentWithController(List.of(oneParam));

            invokeAddControllerInitializeMethod(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString()).isEmpty();
            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }
    }

    /// Tests for the private `addPackageLine(SourceCodeGeneratorContext)` method accessed via reflection.
    @Nested
    class AddPackageLinePrivateTest {

        /// Helper to invoke the private `addPackageLine` method via reflection.
        ///
        /// @param instance The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context  The [SourceCodeGeneratorContext] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddPackageLine(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addPackageLine",
                    SourceCodeGeneratorContext.class
            );
            method.setAccessible(true);
            method.invoke(instance, context);
        }

        /// Verifies that when a package name is present, the package declaration is written
        /// in the form `"package X;\n\n"`.
        @Test
        void withPackageNameWritesPackageDeclaration() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), "com.example"
            );

            invokeAddPackageLine(classUnderTest, context);

            assertThat(context.sourceCode(SourcePart.PACKAGE).toString())
                    .isEqualTo("package com.example;\n\n");
        }

        /// Verifies that when no package name is present (null), the PACKAGE source part remains empty.
        @Test
        void withoutPackageNameLeavesPackagePartEmpty() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            invokeAddPackageLine(classUnderTest, context);

            assertThat(context.sourceCode(SourcePart.PACKAGE).toString()).isEmpty();
        }

        /// Verifies that the package declaration uses the exact package name provided.
        @Test
        void packageDeclarationContainsExactPackageName() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), "org.example.sub"
            );

            invokeAddPackageLine(classUnderTest, context);

            assertThat(context.sourceCode(SourcePart.PACKAGE).toString())
                    .startsWith("package org.example.sub;");
        }

        /// Verifies that calling `addPackageLine` twice appends the declaration twice.
        @Test
        void calledTwiceAppendsTwice() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), "com.example"
            );

            invokeAddPackageLine(classUnderTest, context);
            invokeAddPackageLine(classUnderTest, context);

            assertThat(context.sourceCode(SourcePart.PACKAGE).toString())
                    .isEqualTo("package com.example;\n\npackage com.example;\n\n");
        }
    }

    /// Tests for the private `addFields(SourceCodeGeneratorContext, FXMLDocument)` method accessed via reflection.
    @Nested
    class AddFieldsPrivateTest {

        /// Helper to invoke the private `addFields(SourceCodeGeneratorContext, FXMLDocument)` method via reflection.
        ///
        /// @param instance  The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context   The [SourceCodeGeneratorContext] to pass.
        /// @param document  The [FXMLDocument] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddFields(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                FXMLDocument document
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addFields",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class
            );
            method.setAccessible(true);
            method.invoke(instance, context, document);
        }

        /// Creates a minimal [FXMLDocument] with no controller and a root object with no identifier.
        ///
        /// @return A [FXMLDocument] with an internal root and no controller.
        private FXMLDocument documentWithNoControllerAndInternalRoot() {
            return new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Creates a minimal [FXMLDocument] with no controller and a root object with an exposed identifier.
        ///
        /// @param name The fx:id name for the root object.
        /// @return A [FXMLDocument] with an exposed root identifier and no controller.
        private FXMLDocument documentWithExposedRoot(String name) {
            return new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLExposedIdentifier(name), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Creates a minimal [FXMLDocument] with a controller of the given class and an internal root.
        ///
        /// @param controllerClass The class to use as the controller type.
        /// @return A [FXMLDocument] with a controller and no exposed identifiers.
        private FXMLDocument documentWithController(Class<?> controllerClass) {
            FXMLController controller = new FXMLController(
                    new FXMLClassType(controllerClass),
                    List.of(),
                    List.of()
            );
            return new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.of(controller),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Verifies that a document with no controller and no exposed identifiers produces only
        /// a trailing newline in the FIELDS source part.
        @Test
        void noControllerAndNoIdentifiersProducesOnlyTrailingNewline() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithNoControllerAndInternalRoot();

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that a document with a controller produces a private final field for the controller
        /// using the internal controller field name.
        @Test
        void withControllerProducesControllerField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithController(Object.class);

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString())
                    .contains("private final")
                    .contains(FXMLSourceCodeBuilder.INTERNAL_CONTROLLER_FIELD);
        }

        /// Verifies that a root object with an exposed identifier produces a corresponding field declaration.
        @Test
        void exposedRootIdentifierProducesField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithExposedRoot("myButton");

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString())
                    .contains("myButton");
        }

        /// Verifies that multiple exposed identifiers produce fields sorted lexicographically.
        @Test
        void multipleExposedIdentifiersProduceSortedFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject aFieldObject = new FXMLObject(new FXMLExposedIdentifier("aField"),
                    new FXMLClassType(Object.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLExposedIdentifier("zField"), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(aFieldObject),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields.indexOf("aField")).isLessThan(fields.indexOf("zField"));
        }

        /// Verifies that the FIELDS source part always ends with a trailing newline.
        @Test
        void fieldsAlwaysEndsWithTrailingNewline() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithController(Object.class);

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).endsWith("\n");
        }

        /// Verifies that a root [FXMLObject] with an [FXMLInternalIdentifier] produces no field.
        @Test
        void internalIdentifierOnRootObjectProducesNoField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithNoControllerAndInternalRoot();

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that a root [FXMLObject] with a [FXMLRootIdentifier] produces no field.
        @Test
        void rootIdentifierOnRootObjectProducesNoField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that a root [FXMLObject] with a [FXMLNamedRootIdentifier] produces no field.
        @Test
        void namedRootIdentifierOnRootObjectProducesNoField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLNamedRootIdentifier("myRoot"), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that a [FXMLCollection] root with an exposed identifier produces a field,
        /// and that nested children with exposed identifiers also produce fields.
        @Test
        void collectionWithExposedIdentifierAndNestedChildrenProducesFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject nestedChild = new FXMLObject(
                    new FXMLExposedIdentifier("childItem"),
                    new FXMLClassType(Object.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLCollection(
                            new FXMLExposedIdentifier("myList"),
                            new FXMLClassType(java.util.ArrayList.class),
                            Optional.empty(),
                            List.of(nestedChild)
                    ),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields).contains("myList");
            assertThat(fields).contains("childItem");
        }

        /// Verifies that a [FXMLCopy] with an exposed identifier produces a field using the source type.
        @Test
        void copyWithExposedIdentifierProducesField() throws Exception {
            FXMLClassType sourceType = new FXMLClassType(Object.class);
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of("sourceObj", sourceType), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new FXMLCopy(new FXMLExposedIdentifier("myCopy"), new FXMLExposedIdentifier("sourceObj"))),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).contains("myCopy");
        }

        /// Verifies that a [FXMLInclude] with an exposed identifier produces a field for the included document class,
        /// and an additional field for the included document's controller when present.
        @Test
        void includeWithExposedIdentifierProducesFieldAndControllerField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLController includedController = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of()
            );
            FXMLDocument includedDocument = new FXMLDocument(
                    "IncludedClass",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.of(includedController),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            lazyDoc.set(includedDocument);
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new FXMLInclude(
                            new FXMLExposedIdentifier("myInclude"),
                            "Included.fxml",
                            StandardCharsets.UTF_8,
                            Optional.empty(),
                            lazyDoc
                    )),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields).contains("myInclude");
            assertThat(fields).contains("myIncludeController");
        }

        /// Verifies that a [FXMLInclude] without a controller in the included document produces only
        /// the include field, not a controller field.
        @Test
        void includeWithoutControllerProducesOnlyIncludeField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument includedDocument = new FXMLDocument(
                    "IncludedClass",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            lazyDoc.set(includedDocument);
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new FXMLInclude(
                            new FXMLExposedIdentifier("myInclude"),
                            "Included.fxml",
                            StandardCharsets.UTF_8,
                            Optional.empty(),
                            lazyDoc
                    )),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields).contains("myInclude");
            assertThat(fields).doesNotContain("myIncludeController");
        }

        /// Verifies that a [FXMLMap] with an exposed identifier produces a field,
        /// and that nested entry values with exposed identifiers also produce fields.
        @Test
        void mapWithExposedIdentifierAndNestedEntriesProducesFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject entryValue = new FXMLObject(
                    new FXMLExposedIdentifier("entryVal"),
                    new FXMLClassType(Object.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLMap(
                            new FXMLExposedIdentifier("myMap"),
                            new FXMLClassType(java.util.HashMap.class),
                            new FXMLClassType(String.class),
                            new FXMLClassType(Object.class),
                            Optional.empty(),
                            Map.of(new FXMLLiteral("key"), entryValue)
                    ),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields).contains("myMap");
            assertThat(fields).contains("entryVal");
        }

        /// Verifies that a [FXMLObject] with a nested property containing an exposed identifier
        /// produces fields for both the object and the nested value.
        @Test
        void objectWithNestedPropertyProducesFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject nestedValue = new FXMLObject(
                    new FXMLExposedIdentifier("nestedObj"),
                    new FXMLClassType(Object.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "prop", "setProp", new FXMLClassType(Object.class), nestedValue
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(
                            new FXMLExposedIdentifier("parentObj"),
                            new FXMLClassType(Object.class),
                            Optional.empty(),
                            List.of(property)
                    ),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            String fields = context.sourceCode(SourcePart.FIELDS).toString();
            assertThat(fields).contains("parentObj");
            assertThat(fields).contains("nestedObj");
        }

        /// Verifies that a [FXMLValue] with an exposed identifier produces a field.
        @Test
        void fxmlValueWithExposedIdentifierProducesField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new FXMLValue(
                            Optional.of(new FXMLExposedIdentifier("myValue")),
                            new FXMLClassType(String.class),
                            "hello"
                    )),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).contains("myValue");
        }

        /// Verifies that a [FXMLValue] without an identifier produces no field.
        @Test
        void fxmlValueWithoutIdentifierProducesNoField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(new FXMLValue(
                            Optional.empty(),
                            new FXMLClassType(String.class),
                            "hello"
                    )),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that no-op value types ([FXMLConstant], [FXMLReference], [FXMLLiteral],
        /// [FXMLExpression], [FXMLInlineScript], [FXMLResource], [FXMLTranslation]) produce no fields.
        @Test
        void noOpValueTypesProduceNoFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(
                            new FXMLConstant(new FXMLClassType(Object.class), "CONST", new FXMLClassType(Object.class)),
                            new FXMLReference("someRef"),
                            new FXMLLiteral("lit"),
                            new FXMLExpression("expr"),
                            new FXMLInlineScript("script"),
                            new FXMLResource("res.png"),
                            new FXMLTranslation("key")
                    ),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).isEqualTo("\n");
        }

        /// Verifies that definitions are processed and exposed identifiers within them produce fields.
        @Test
        void definitionsWithExposedIdentifiersProduceFields() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject definitionObj = new FXMLObject(
                    new FXMLExposedIdentifier("defField"),
                    new FXMLClassType(Object.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyClass",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(definitionObj),
                    List.of()
            );

            invokeAddFields(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.FIELDS).toString()).contains("defField");
        }
    }

    /// Tests for the private `identifierToField(SourceCodeGeneratorContext, FXMLIdentifier, FXMLType)` method
    /// accessed via reflection.
    @Nested
    class IdentifierToFieldPrivateTest {

        /// Helper to invoke the private `identifierToField` method via reflection.
        ///
        /// @param instance   The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context    The [SourceCodeGeneratorContext] to pass.
        /// @param identifier The [io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier] to pass.
        /// @param type       The [io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType] to pass.
        /// @return The resulting stream as a list of strings.
        /// @throws Exception if reflection fails or the method throws.
        @SuppressWarnings("unchecked")
        private List<String> invokeIdentifierToField(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier identifier,
                io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType type
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "identifierToField",
                    SourceCodeGeneratorContext.class,
                    io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier.class,
                    io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType.class
            );
            method.setAccessible(true);
            return ((java.util.stream.Stream<String>) method.invoke(instance, context, identifier, type)).toList();
        }

        /// Verifies that an [FXMLExposedIdentifier] produces a `protected final` field declaration.
        @Test
        void exposedIdentifierProducesProtectedFinalField() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            List<String> result = invokeIdentifierToField(
                    classUnderTest, context,
                    new FXMLExposedIdentifier("myField"),
                    new FXMLClassType(String.class)
            );

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo("protected final java.lang.String myField;\n");
        }

        /// Verifies that an [FXMLInternalIdentifier] produces an empty stream.
        @Test
        void internalIdentifierProducesEmptyStream() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            List<String> result = invokeIdentifierToField(
                    classUnderTest, context,
                    new FXMLInternalIdentifier(0),
                    new FXMLClassType(Object.class)
            );

            assertThat(result).isEmpty();
        }

        /// Verifies that an [FXMLNamedRootIdentifier] produces an empty stream.
        @Test
        void namedRootIdentifierProducesEmptyStream() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            List<String> result = invokeIdentifierToField(
                    classUnderTest, context,
                    new FXMLNamedRootIdentifier("root"),
                    new FXMLClassType(Object.class)
            );

            assertThat(result).isEmpty();
        }

        /// Verifies that an [FXMLRootIdentifier] produces an empty stream.
        @Test
        void rootIdentifierProducesEmptyStream() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            List<String> result = invokeIdentifierToField(
                    classUnderTest, context,
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(Object.class)
            );

            assertThat(result).isEmpty();
        }

        /// Verifies that an [FXMLExposedIdentifier] with an uncompiled type produces the uncompiled class name.
        @Test
        void exposedIdentifierWithUncompiledTypeProducesUncompiledClassName() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );

            List<String> result = invokeIdentifierToField(
                    classUnderTest, context,
                    new FXMLExposedIdentifier("included"),
                    new FXMLUncompiledClassType("com.example.IncludedView")
            );

            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo("protected final com.example.IncludedView included;\n");
        }
    }

    /// Tests for the private `addClassDeclaration(SourceCodeGeneratorContext, FXMLDocument, boolean)` method
    /// accessed via reflection.
    @Nested
    class AddClassDeclarationPrivateTest {

        /// Helper to invoke the private `addClassDeclaration` method via reflection.
        ///
        /// @param instance       The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context        The [SourceCodeGeneratorContext] to pass.
        /// @param document       The [FXMLDocument] to pass.
        /// @param isRootDocument Whether this is the root document.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddClassDeclaration(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                FXMLDocument document,
                boolean isRootDocument
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addClassDeclaration",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class,
                    boolean.class
            );
            method.setAccessible(true);
            method.invoke(instance, context, document, isRootDocument);
        }

        /// Builds a minimal [FXMLDocument] with the given interfaces and no controller.
        ///
        /// @param interfaces The list of [FXMLInterface] instances to include.
        /// @return A minimal [FXMLDocument].
        private FXMLDocument documentWithInterfaces(List<FXMLInterface> interfaces) {
            return new FXMLDocument(
                    "MyView",
                    new FXMLObject(new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                            Optional.empty(), List.of()),
                    interfaces,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
        }

        /// Verifies that a root document without abstract features produces `public class X extends Y`.
        @Test
        void rootDocumentProducesPublicClass() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithInterfaces(List.of());

            invokeAddClassDeclaration(classUnderTest, context, document, true);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).startsWith("public class MyView extends");
            assertThat(declaration).doesNotContain("abstract");
        }

        /// Verifies that a root document with [Feature#ABSTRACT_CLASS] produces `public abstract class X extends Y`.
        @Test
        void rootDocumentWithAbstractFeatureProducesAbstractClass() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            context.addFeature(Feature.ABSTRACT_CLASS);
            FXMLDocument document = documentWithInterfaces(List.of());

            invokeAddClassDeclaration(classUnderTest, context, document, true);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).startsWith("public abstract class MyView extends");
        }

        /// Verifies that a root document with an abstract interface method produces `public abstract class X extends Y`.
        @Test
        void rootDocumentWithAbstractInterfaceMethodProducesAbstractClass() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod abstractMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "doSomething", true,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLInterface fxmlInterface = new FXMLInterface(
                    new FXMLClassType(Runnable.class), List.of(abstractMethod)
            );
            FXMLDocument document = documentWithInterfaces(List.of(fxmlInterface));

            invokeAddClassDeclaration(classUnderTest, context, document, true);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).contains("abstract");
        }

        /// Verifies that a root document with interfaces produces an `implements` clause.
        @Test
        void rootDocumentWithInterfacesProducesImplementsClause() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInterface fxmlInterface = new FXMLInterface(
                    new FXMLClassType(Runnable.class), List.of()
            );
            FXMLDocument document = documentWithInterfaces(List.of(fxmlInterface));

            invokeAddClassDeclaration(classUnderTest, context, document, true);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).contains("implements");
            assertThat(declaration).contains("java.lang.Runnable");
        }

        /// Verifies that a root document with multiple interfaces lists all of them separated by commas.
        @Test
        void rootDocumentWithMultipleInterfacesListsAll() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInterface iface1 = new FXMLInterface(new FXMLClassType(Runnable.class), List.of());
            FXMLInterface iface2 = new FXMLInterface(new FXMLClassType(java.io.Serializable.class), List.of());
            FXMLDocument document = documentWithInterfaces(List.of(iface1, iface2));

            invokeAddClassDeclaration(classUnderTest, context, document, true);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).contains("java.lang.Runnable");
            assertThat(declaration).contains("java.io.Serializable");
            assertThat(declaration).doesNotEndWith(", ");
        }

        /// Verifies that a non-root document produces `private static class X extends Y`.
        @Test
        void nonRootDocumentProducesPrivateStaticClass() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = documentWithInterfaces(List.of());

            invokeAddClassDeclaration(classUnderTest, context, document, false);

            String declaration = context.sourceCode(SourcePart.CLASS_DECLARATION).toString();
            assertThat(declaration).startsWith("private static class MyView extends");
        }

        /// Verifies that a non-root document with [Feature#ABSTRACT_CLASS] throws [UnsupportedOperationException].
        @Test
        void nonRootDocumentWithAbstractFeatureThrows() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            context.addFeature(Feature.ABSTRACT_CLASS);
            FXMLDocument document = documentWithInterfaces(List.of());

            assertThatThrownBy(() -> invokeAddClassDeclaration(classUnderTest, context, document, false))
                    .cause()
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Abstract inner classes");
        }

        /// Verifies that a non-root document with an abstract interface method throws [UnsupportedOperationException].
        @Test
        void nonRootDocumentWithAbstractInterfaceMethodThrows() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod abstractMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "doSomething", true,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLInterface fxmlInterface = new FXMLInterface(
                    new FXMLClassType(Runnable.class), List.of(abstractMethod)
            );
            FXMLDocument document = documentWithInterfaces(List.of(fxmlInterface));

            assertThatThrownBy(() -> invokeAddClassDeclaration(classUnderTest, context, document, false))
                    .cause()
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    /// Tests for the private `addMethods(FXMLDocument, SourceCodeGeneratorContext)` method
    /// accessed via reflection.
    @Nested
    class AddMethodsPrivateTest {

        /// Helper to invoke the private `addMethods(FXMLDocument, SourceCodeGeneratorContext)` method via reflection.
        ///
        /// @param instance The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param document The [FXMLDocument] to pass.
        /// @param context  The [SourceCodeGeneratorContext] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddMethods(
                FXMLSourceCodeBuilder instance,
                FXMLDocument document,
                SourceCodeGeneratorContext context
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addMethods",
                    FXMLDocument.class,
                    SourceCodeGeneratorContext.class
            );
            method.setAccessible(true);
            method.invoke(instance, document, context);
        }

        /// Builds a minimal [FXMLDocument] with the given root object, controller, and definitions.
        ///
        /// @param root        The root [AbstractFXMLObject].
        /// @param controller  The optional [FXMLController].
        /// @param definitions The list of definition values.
        /// @return A minimal [FXMLDocument].
        private FXMLDocument buildDocument(
                io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject root,
                Optional<FXMLController> controller,
                List<io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue> definitions
        ) {
            return new FXMLDocument(
                    "MyView",
                    root,
                    List.of(),
                    controller,
                    Optional.empty(),
                    definitions,
                    List.of()
            );
        }

        /// Wraps an [AbstractFXMLValue] as a property of a root [FXMLObject].
        ///
        /// @param value The value to wrap.
        /// @return An [FXMLObject] containing the value as a property.
        private FXMLObject wrapInRoot(io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue value) {
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "onAction", "setOnAction", new FXMLClassType(Object.class), value
            );
            return new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(property)
            );
        }

        /// Verifies that a document with no [FXMLMethod] nodes produces only a trailing newline in METHODS.
        @Test
        void noMethodsProducesOnlyTrailingNewline() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of()
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).isEqualTo("\n");
        }

        /// Verifies that a [FXMLMethod] with no matching controller method produces an abstract method
        /// and adds the [Feature#ABSTRACT_CLASS] feature.
        @Test
        void fxmlMethodWithNoControllerProducesAbstractMethod() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod("onAction", List.of(), new FXMLClassType(void.class));
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("abstract");
            assertThat(methods).contains("onAction");
            assertThat(context.hasFeature(Feature.ABSTRACT_CLASS)).isTrue();
        }

        /// Verifies that a [FXMLMethod] with a matching PUBLIC controller method produces a direct call body.
        @Test
        void fxmlMethodWithPublicControllerMethodProducesDirectCall() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod controllerMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "onAction", false,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of(controllerMethod)
            );
            FXMLMethod fxmlMethod = new FXMLMethod("onAction", List.of(), new FXMLClassType(void.class));
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.of(controller), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("onAction");
            assertThat(methods).doesNotContain("abstract");
            assertThat(methods).contains("$internalField$controller$.onAction()");
        }

        /// Verifies that a [FXMLMethod] with a matching PRIVATE controller method produces a reflection call body.
        @Test
        void fxmlMethodWithPrivateControllerMethodProducesReflectionCall() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod controllerMethod = new FXMLControllerMethod(
                    Visibility.PRIVATE, "onAction", false,
                    new FXMLClassType(void.class), List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of(controllerMethod)
            );
            FXMLMethod fxmlMethod = new FXMLMethod("onAction", List.of(), new FXMLClassType(void.class));
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.of(controller), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("getDeclaredMethod");
            assertThat(methods).contains("setAccessible");
            assertThat(methods).doesNotContain("abstract");
        }

        /// Verifies that a duplicate [FXMLMethod] (same reference) is only rendered once via `singleCreation`.
        @Test
        void duplicateFxmlMethodRenderedOnlyOnce() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod("onAction", List.of(), new FXMLClassType(void.class));
            FXMLCollection collection = new FXMLCollection(
                    new FXMLInternalIdentifier(0), new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(), List.of(fxmlMethod, fxmlMethod)
            );
            FXMLDocument document = buildDocument(collection, Optional.empty(), List.of(fxmlMethod));

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            long occurrences = methods.chars().filter(c -> c == '{').count();
            // abstract method has no body, so count "onAction" occurrences instead
            assertThat(methods.split("onAction", -1).length - 1).isEqualTo(1);
        }

        /// Verifies that [FXMLCollection] children are recursed into for method generation.
        @Test
        void fxmlCollectionChildrenAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod("handleClick", List.of(), new FXMLClassType(void.class));
            FXMLCollection collection = new FXMLCollection(
                    new FXMLInternalIdentifier(0), new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(), List.of(fxmlMethod)
            );
            FXMLDocument document = buildDocument(collection, Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("handleClick");
        }

        /// Verifies that [FXMLMap] entry values are recursed into for method generation.
        @Test
        void fxmlMapEntriesAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod("handleMap", List.of(), new FXMLClassType(void.class));
            FXMLLiteral key = new FXMLLiteral("key");
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLInternalIdentifier(0), new FXMLClassType(java.util.HashMap.class),
                    new FXMLClassType(String.class), new FXMLClassType(Object.class),
                    Optional.empty(),
                    Map.of(key, fxmlMethod)
            );
            FXMLDocument document = buildDocument(fxmlMap, Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("handleMap");
        }

        /// Verifies that [FXMLValue] as a root property child produces no method output.
        @Test
        void fxmlValueProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLValue value = new FXMLValue(Optional.empty(), new FXMLClassType(String.class), "hello");
            FXMLDocument document = buildDocument(wrapInRoot(value), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLConstant] as a root property child produces no method output.
        @Test
        void fxmlConstantProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLConstant constant = new FXMLConstant(
                    new FXMLClassType(Object.class), "CONST", new FXMLClassType(Object.class)
            );
            FXMLDocument document = buildDocument(wrapInRoot(constant), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLCopy] as a root property child produces no method output.
        @Test
        void fxmlCopyProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLCopy copy = new FXMLCopy(
                    new FXMLInternalIdentifier(1), new FXMLExposedIdentifier("source")
            );
            FXMLDocument document = buildDocument(wrapInRoot(copy), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLExpression] as a root property child produces no method output.
        @Test
        void fxmlExpressionProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLExpression expression = new FXMLExpression("someExpr");
            FXMLDocument document = buildDocument(wrapInRoot(expression), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLInclude] as a root property child produces no method output.
        @Test
        void fxmlIncludeProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInclude include = new FXMLInclude(
                    new FXMLInternalIdentifier(1),
                    "other.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    new FXMLLazyLoadedDocument()
            );
            FXMLDocument document = buildDocument(wrapInRoot(include), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLInlineScript] as a root property child produces no method output.
        @Test
        void fxmlInlineScriptProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInlineScript script = new FXMLInlineScript("var x = 1;");
            FXMLDocument document = buildDocument(wrapInRoot(script), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLLiteral] as a root property child produces no method output.
        @Test
        void fxmlLiteralProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLLiteral literal = new FXMLLiteral("someText");
            FXMLDocument document = buildDocument(wrapInRoot(literal), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLReference] as a root property child produces no method output.
        @Test
        void fxmlReferenceProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLReference reference = new FXMLReference("someRef");
            FXMLDocument document = buildDocument(wrapInRoot(reference), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLResource] as a root property child produces no method output.
        @Test
        void fxmlResourceProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLResource resource = new FXMLResource("icon.png");
            FXMLDocument document = buildDocument(wrapInRoot(resource), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLTranslation] as a root property child produces no method output.
        @Test
        void fxmlTranslationProducesNoMethods() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLTranslation translation = new FXMLTranslation("label.key");
            FXMLDocument document = buildDocument(wrapInRoot(translation), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            assertThat(context.sourceCode(SourcePart.METHODS).toString()).isEqualTo("\n");
        }

        /// Verifies that [FXMLMethod] nodes in the definitions list are also processed.
        @Test
        void definitionsListIsAlsoProcessed() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of()
            );
            FXMLMethod defMethod = new FXMLMethod("onDefinition", List.of(), new FXMLClassType(void.class));
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of(defMethod));

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("onDefinition");
        }

        /// Verifies that a [FXMLMethod] with parameters renders the parameter list correctly.
        @Test
        void fxmlMethodWithParametersRendersParameterList() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod(
                    "onEvent",
                    List.of(new FXMLClassType(javafx.event.ActionEvent.class)),
                    new FXMLClassType(void.class)
            );
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("javafx.event.ActionEvent param0");
        }

        /// Verifies that a [FXMLMethod] with a non-void return type renders the return type correctly.
        @Test
        void fxmlMethodWithNonVoidReturnTypeRendersReturnType() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLMethod fxmlMethod = new FXMLMethod(
                    "getValue",
                    List.of(),
                    new FXMLClassType(String.class)
            );
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.empty(), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("java.lang.String");
            assertThat(methods).contains("getValue");
        }

        /// Verifies that a [FXMLMethod] with a matching PUBLIC controller method with a non-void return type
        /// produces a direct call body with a `return` statement.
        @Test
        void fxmlMethodWithPublicControllerMethodAndNonVoidReturnProducesReturnStatement() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLControllerMethod controllerMethod = new FXMLControllerMethod(
                    Visibility.PUBLIC, "getValue", false,
                    new FXMLClassType(String.class), List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of(controllerMethod)
            );
            FXMLMethod fxmlMethod = new FXMLMethod("getValue", List.of(), new FXMLClassType(String.class));
            FXMLDocument document = buildDocument(wrapInRoot(fxmlMethod), Optional.of(controller), List.of());

            invokeAddMethods(classUnderTest, document, context);

            String methods = context.sourceCode(SourcePart.METHODS).toString();
            assertThat(methods).contains("return $internalField$controller$.getValue()");
        }
    }

    /// Tests for the private `addConstructorPrologue(SourceCodeGeneratorContext, FXMLDocument)` method
    /// accessed via reflection.
    @Nested
    class AddConstructorProloguePrivateTest {

        /// Helper to invoke the private `addConstructorPrologue(SourceCodeGeneratorContext, FXMLDocument)` method
        /// via reflection.
        ///
        /// @param instance The [FXMLSourceCodeBuilder] instance to invoke the method on.
        /// @param context  The [SourceCodeGeneratorContext] to pass.
        /// @param document The [FXMLDocument] to pass.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddConstructorPrologue(
                FXMLSourceCodeBuilder instance,
                SourceCodeGeneratorContext context,
                FXMLDocument document
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addConstructorPrologue",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class
            );
            method.setAccessible(true);
            method.invoke(instance, context, document);
        }

        /// Builds a minimal [FXMLDocument] with the given root object, optional controller, and definitions.
        ///
        /// @param root        The root [io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject].
        /// @param controller  The optional [FXMLController].
        /// @param definitions The list of definition values.
        /// @return A minimal [FXMLDocument].
        private FXMLDocument buildDocument(
                io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject root,
                Optional<FXMLController> controller,
                List<io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue> definitions
        ) {
            return new FXMLDocument(
                    "MyView",
                    root,
                    List.of(),
                    controller,
                    Optional.empty(),
                    definitions,
                    List.of()
            );
        }

        /// Builds a minimal [FXMLObject] root with no properties.
        ///
        /// @param identifier The identifier to use for the root object.
        /// @return A minimal [FXMLObject].
        private FXMLObject buildRoot(
                io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier identifier
        ) {
            return new FXMLObject(identifier, new FXMLClassType(Object.class), Optional.empty(), List.of());
        }

        /// Verifies that with no controller and a simple root, `CONSTRUCTOR_PROLOGUE` is empty
        /// and `CONSTRUCTOR_SUPER_CALL` contains `super()`.
        @Test
        void noControllerProducesEmptyPrologueAndSuperCall() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLInternalIdentifier(0)), Optional.empty(), List.of()
            );

            invokeAddConstructorPrologue(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString()).isEmpty();
            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_SUPER_CALL).toString()).isEqualTo("super();");
        }

        /// Verifies that when a controller is present, `CONSTRUCTOR_PROLOGUE` starts with the controller
        /// field initialization line.
        @Test
        void controllerPresentWritesControllerInitToPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLInternalIdentifier(0)), Optional.of(controller), List.of()
            );

            invokeAddConstructorPrologue(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString())
                    .startsWith(FXMLSourceCodeBuilder.INTERNAL_CONTROLLER_FIELD + " = new ");
        }

        /// Verifies that an [FXMLObject] with an [FXMLInternalIdentifier] produces a local variable declaration
        /// with the type prefix in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlObjectWithInternalIdentifierProducesLocalVarWithTypePrefix() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject root = buildRoot(new FXMLInternalIdentifier(0));
            FXMLObject child = new FXMLObject(
                    new FXMLInternalIdentifier(1), new FXMLClassType(String.class),
                    Optional.empty(), List.of()
            );
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "child", "setChild", new FXMLClassType(Object.class), child
            );
            FXMLObject rootWithChild = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(property)
            );
            FXMLDocument document = buildDocument(rootWithChild, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("java.lang.String");
            assertThat(prologue).contains(" = new java.lang.String();\n");
        }

        /// Verifies that an [FXMLObject] with an [FXMLExposedIdentifier] that is not a dependency
        /// produces a simple `name = new Type();` assignment in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlObjectWithExposedIdentifierNonDependencyProducesSimpleAssignment() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject child = new FXMLObject(
                    new FXMLExposedIdentifier("myButton"), new FXMLClassType(String.class),
                    Optional.empty(), List.of()
            );
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "child", "setChild", new FXMLClassType(Object.class), child
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(property)
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("myButton = new java.lang.String();\n");
            assertThat(prologue).doesNotContain("$$");
        }

        /// Verifies that an [FXMLObject] with an [FXMLExposedIdentifier] that IS a constructor dependency
        /// produces a `$$`-prefixed local variable and a subsequent bind line in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlObjectWithExposedIdentifierAsDependencyProducesPrefixedVarAndBindLine() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", new java.util.HashMap<>(), null
            );
            FXMLExposedIdentifier depId = new FXMLExposedIdentifier("x");
            FXMLValue dep = new FXMLValue(
                    Optional.of(depId), new FXMLClassType(double.class), "1.0"
            );
            FXMLConstructorProperty constructorPropX = new FXMLConstructorProperty(
                    "x", new FXMLClassType(double.class), new FXMLReference(depId.name())
            );
            FXMLConstructorProperty constructorPropY = new FXMLConstructorProperty(
                    "y", new FXMLClassType(double.class), new FXMLLiteral("0.0")
            );
            FXMLObject consumer = new FXMLObject(
                    new FXMLExposedIdentifier("myPoint"), new FXMLClassType(javafx.geometry.Point2D.class),
                    Optional.empty(), List.of(constructorPropX, constructorPropY)
            );
            FXMLObjectProperty depProp = new FXMLObjectProperty(
                    "dep", "setDep", new FXMLClassType(Object.class), dep
            );
            FXMLObjectProperty consumerProp = new FXMLObjectProperty(
                    "consumer", "setConsumer", new FXMLClassType(Object.class), consumer
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(depProp, consumerProp)
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("$$x");
            assertThat(prologue).contains("x = $$x;\n");
        }

        /// Verifies that an [FXMLCopy] produces a `name = new SourceType($$source);` line in
        /// `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlCopyProducesCopyConstructorCallInPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLExposedIdentifier sourceId = new FXMLExposedIdentifier("original");
            FXMLObject source = new FXMLObject(
                    sourceId, new FXMLClassType(String.class), Optional.empty(), List.of()
            );
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), sourceId);
            FXMLObjectProperty sourceProp = new FXMLObjectProperty(
                    "source", "setSource", new FXMLClassType(Object.class), source
            );
            FXMLObjectProperty copyProp = new FXMLObjectProperty(
                    "copy", "setCopy", new FXMLClassType(Object.class), copy
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(sourceProp, copyProp)
            );
            FXMLDocument document = buildDocument(
                    root, Optional.empty(),
                    List.of()
            );
            // identifierToTypeMap must be mutable for FXMLCopy type resolution
            context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "",
                    new java.util.HashMap<>(Map.of(sourceId.name(), new FXMLClassType(String.class))),
                    null
            );

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("myCopy = new java.lang.String($$original);\n");
        }

        /// Verifies that an [FXMLInclude] produces a `name = new IncludedClass();` line in
        /// `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlIncludeProducesNewIncludedClassCallInPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            FXMLDocument includedDoc = new FXMLDocument(
                    "IncludedView", buildRoot(new FXMLInternalIdentifier(0)),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            lazyDoc.set(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("myInclude"),
                    "included.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazyDoc
            );
            FXMLObjectProperty includeProp = new FXMLObjectProperty(
                    "include", "setInclude", new FXMLClassType(Object.class), include
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(includeProp)
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("myInclude = new IncludedView();\n");
        }

        /// Verifies that an [FXMLValue] with an identifier produces a literal assignment in
        /// `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlValueWithIdentifierProducesLiteralAssignmentInPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLValue fxmlValue = new FXMLValue(
                    Optional.of(new FXMLExposedIdentifier("myVal")),
                    new FXMLClassType(String.class),
                    "hello"
            );
            FXMLObjectProperty valueProp = new FXMLObjectProperty(
                    "val", "setVal", new FXMLClassType(Object.class), fxmlValue
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(valueProp)
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("myVal = ");
            assertThat(prologue).contains("hello");
        }

        /// Verifies that an [FXMLValue] without an identifier produces nothing in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlValueWithoutIdentifierProducesNothingInPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLValue fxmlValue = new FXMLValue(
                    Optional.empty(),
                    new FXMLClassType(String.class),
                    "hello"
            );
            FXMLObjectProperty valueProp = new FXMLObjectProperty(
                    "val", "setVal", new FXMLClassType(Object.class), fxmlValue
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(valueProp)
            );
            FXMLDocument document = buildDocument(root, Optional.empty(), List.of());

            invokeAddConstructorPrologue(classUnderTest, context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString()).isEmpty();
        }

        /// Verifies that objects in the definitions list are also processed and appear in
        /// `CONSTRUCTOR_PROLOGUE`.
        @Test
        void definitionsListObjectsAreAlsoProcessedInPrologue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject defObject = new FXMLObject(
                    new FXMLExposedIdentifier("defItem"), new FXMLClassType(String.class),
                    Optional.empty(), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLInternalIdentifier(0)), Optional.empty(), List.of(defObject)
            );

            invokeAddConstructorPrologue(classUnderTest, context, document);

            String prologue = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(prologue).contains("defItem = new java.lang.String();\n");
        }
    }

    /// Tests for the private `findConstructions(AbstractFXMLValue, SourceCodeGeneratorContext)` method
    /// accessed via reflection through `addConstructorPrologue`.
    @Nested
    class FindConstructionsPrivateTest {

        /// Helper to build a minimal [FXMLDocument] with the given root.
        ///
        /// @param root The root object.
        /// @return A minimal [FXMLDocument].
        private FXMLDocument buildDocument(FXMLObject root) {
            return new FXMLDocument(
                    "MyView", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
        }

        /// Helper to invoke `addConstructorPrologue` and return the prologue string.
        ///
        /// @param document The document to process.
        /// @return The generated prologue string.
        /// @throws Exception if reflection fails.
        private String invokePrologue(FXMLDocument document) throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addConstructorPrologue", SourceCodeGeneratorContext.class, FXMLDocument.class
            );
            method.setAccessible(true);
            method.invoke(classUnderTest, context, document);
            return context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
        }

        /// Verifies that an [FXMLCollection] nested inside an [FXMLObject] property produces a
        /// `new ArrayList()` construction in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlCollectionNestedInPropertyProducesCollectionConstruction() throws Exception {
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "items", "setItems", new FXMLClassType(Object.class), collection
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).contains("myList = new java.util.ArrayList();");
        }

        /// Verifies that an [FXMLCollection] nested in a property with a nested [FXMLObject] child
        /// also constructs the child in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlCollectionWithNestedChildConstructsChild() throws Exception {
            FXMLObject child = new FXMLObject(
                    new FXMLExposedIdentifier("item"),
                    new FXMLClassType(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(child)
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "items", "setItems", new FXMLClassType(Object.class), collection
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).contains("item = new java.lang.String();");
        }

        /// Verifies that an [FXMLMap] nested inside an [FXMLObject] property produces a map construction
        /// in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlMapNestedInPropertyProducesMapConstruction() throws Exception {
            FXMLMap map = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLClassType(java.util.HashMap.class),
                    new FXMLClassType(String.class),
                    new FXMLClassType(String.class),
                    Optional.empty(),
                    Map.of()
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "data", "setData", new FXMLClassType(Object.class), map
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).contains("myMap = new java.util.HashMap();");
        }

        /// Verifies that an [FXMLMap] nested in a property with a nested [FXMLObject] entry value
        /// also constructs the entry in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlMapWithNestedEntryValueConstructsEntry() throws Exception {
            FXMLObject entryValue = new FXMLObject(
                    new FXMLExposedIdentifier("entryObj"),
                    new FXMLClassType(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLMap map = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLClassType(java.util.HashMap.class),
                    new FXMLClassType(String.class),
                    new FXMLClassType(Object.class),
                    Optional.empty(),
                    Map.of(new FXMLLiteral("key"), entryValue)
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "data", "setData", new FXMLClassType(Object.class), map
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).contains("entryObj = new java.lang.String();");
        }

        /// Verifies that no-op value types ([FXMLMethod], [FXMLConstant], [FXMLExpression],
        /// [FXMLInlineScript], [FXMLLiteral], [FXMLResource], [FXMLTranslation]) nested inside an
        /// [FXMLObject] property produce no construction lines in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void noOpValueTypesNestedInObjectPropertyProduceNoConstruction() throws Exception {
            FXMLMethod fxmlMethod = new FXMLMethod("onClick", List.of(), new FXMLClassType(void.class));
            FXMLObjectProperty methodProp = new FXMLObjectProperty(
                    "onAction", "setOnAction", new FXMLClassType(Object.class), fxmlMethod
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(methodProp)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).isEmpty();
        }

        /// Verifies that an [FXMLObject] with a factory method nested inside a root property produces
        /// a `Type.method(...)` call instead of `new Type(...)` in `CONSTRUCTOR_PROLOGUE`.
        @Test
        void fxmlObjectWithFactoryMethodProducesFactoryCallInPrologue() throws Exception {
            FXMLFactoryMethod factoryMethod = new FXMLFactoryMethod(
                    new FXMLClassType(java.util.Collections.class), "emptyList"
            );
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("myObj"),
                    new FXMLClassType(java.util.List.class),
                    Optional.of(factoryMethod),
                    List.of()
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "items", "setItems", new FXMLClassType(Object.class), obj
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root);

            String prologue = invokePrologue(document);

            assertThat(prologue).contains("java.util.Collections.emptyList(");
        }
    }

    /// Tests for the private `resolveConstructionOrder(List)` method accessed via reflection.
    @Nested
    class ResolveConstructionOrderPrivateTest {

        /// Verifies that a cyclic dependency between two [FXMLObject] nodes throws
        /// [IllegalArgumentException] when `addConstructorPrologue` is called.
        @Test
        void cyclicDependencyThrowsIllegalArgumentException() {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", new java.util.HashMap<>(), null
            );
            FXMLExposedIdentifier idA = new FXMLExposedIdentifier("objA");
            FXMLExposedIdentifier idB = new FXMLExposedIdentifier("objB");
            // objA depends on objB via constructor property
            FXMLConstructorProperty propA = new FXMLConstructorProperty(
                    "x", new FXMLClassType(double.class), new FXMLReference(idB.name())
            );
            // objB depends on objA via constructor property
            FXMLConstructorProperty propB = new FXMLConstructorProperty(
                    "x", new FXMLClassType(double.class), new FXMLReference(idA.name())
            );
            FXMLObject objA = new FXMLObject(
                    idA, new FXMLClassType(javafx.geometry.Point2D.class),
                    Optional.empty(), List.of(propA)
            );
            FXMLObject objB = new FXMLObject(
                    idB, new FXMLClassType(javafx.geometry.Point2D.class),
                    Optional.empty(), List.of(propB)
            );
            FXMLObjectProperty propObjA = new FXMLObjectProperty(
                    "a", "setA", new FXMLClassType(Object.class), objA
            );
            FXMLObjectProperty propObjB = new FXMLObjectProperty(
                    "b", "setB", new FXMLClassType(Object.class), objB
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of(propObjA, propObjB)
            );
            FXMLDocument document = new FXMLDocument(
                    "MyView", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );

            Method method;
            try {
                method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                        "addConstructorPrologue", SourceCodeGeneratorContext.class, FXMLDocument.class
                );
                method.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            final Method finalMethod = method;

            assertThatThrownBy(() -> finalMethod.invoke(classUnderTest, context, document))
                    .cause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cyclic dependencies");
        }
    }

    /// Tests for the private `findIdentifierForValue(AbstractFXMLValue)` method accessed via reflection.
    @Nested
    class FindIdentifierForValuePrivateTest {

        /// Helper to invoke the private `findIdentifierForValue` method via reflection.
        ///
        /// @param value The value to find the identifier for.
        /// @return The [Optional] result.
        /// @throws Exception if reflection fails.
        @SuppressWarnings("unchecked")
        private Optional<Object> invokeFindIdentifierForValue(Object value) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "findIdentifierForValue",
                    io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue.class
            );
            method.setAccessible(true);
            return (Optional<Object>) method.invoke(classUnderTest, value);
        }

        /// Verifies that an [FXMLObject] returns its identifier.
        @Test
        void fxmlObjectReturnsItsIdentifier() throws Exception {
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myObj");
            FXMLObject obj = new FXMLObject(id, new FXMLClassType(Object.class), Optional.empty(), List.of());

            Optional<Object> result = invokeFindIdentifierForValue(obj);

            assertThat(result).contains(id);
        }

        /// Verifies that an [FXMLCopy] returns its identifier.
        @Test
        void fxmlCopyReturnsItsIdentifier() throws Exception {
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myCopy");
            FXMLCopy copy = new FXMLCopy(id, new FXMLExposedIdentifier("source"));

            Optional<Object> result = invokeFindIdentifierForValue(copy);

            assertThat(result).contains(id);
        }

        /// Verifies that an [FXMLInclude] returns its identifier.
        @Test
        void fxmlIncludeReturnsItsIdentifier() throws Exception {
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myInclude");
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            FXMLInclude include = new FXMLInclude(
                    id, "inc.fxml", java.nio.charset.StandardCharsets.UTF_8, Optional.empty(), lazyDoc
            );

            Optional<Object> result = invokeFindIdentifierForValue(include);

            assertThat(result).contains(id);
        }

        /// Verifies that an [FXMLReference] returns an [FXMLExposedIdentifier] with the reference name.
        @Test
        void fxmlReferenceReturnsExposedIdentifierWithName() throws Exception {
            FXMLReference reference = new FXMLReference("myRef");

            Optional<Object> result = invokeFindIdentifierForValue(reference);

            assertThat(result).contains(new FXMLExposedIdentifier("myRef"));
        }

        /// Verifies that an [FXMLValue] with an identifier returns that identifier.
        @Test
        void fxmlValueWithIdentifierReturnsIdentifier() throws Exception {
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myVal");
            FXMLValue value = new FXMLValue(Optional.of(id), new FXMLClassType(String.class), "hello");

            Optional<Object> result = invokeFindIdentifierForValue(value);

            assertThat(result).contains(id);
        }

        /// Verifies that an [FXMLValue] without an identifier returns empty.
        @Test
        void fxmlValueWithoutIdentifierReturnsEmpty() throws Exception {
            FXMLValue value = new FXMLValue(Optional.empty(), new FXMLClassType(String.class), "hello");

            Optional<Object> result = invokeFindIdentifierForValue(value);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLConstant] returns empty.
        @Test
        void fxmlConstantReturnsEmpty() throws Exception {
            FXMLConstant constant = new FXMLConstant(new FXMLClassType(String.class), "MY_CONST", new FXMLClassType(String.class));

            Optional<Object> result = invokeFindIdentifierForValue(constant);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLExpression] returns empty.
        @Test
        void fxmlExpressionReturnsEmpty() throws Exception {
            FXMLExpression expression = new FXMLExpression("myExpr");

            Optional<Object> result = invokeFindIdentifierForValue(expression);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLInlineScript] returns empty.
        @Test
        void fxmlInlineScriptReturnsEmpty() throws Exception {
            FXMLInlineScript script = new FXMLInlineScript("var x = 1;");

            Optional<Object> result = invokeFindIdentifierForValue(script);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLLiteral] returns empty.
        @Test
        void fxmlLiteralReturnsEmpty() throws Exception {
            FXMLLiteral literal = new FXMLLiteral("hello");

            Optional<Object> result = invokeFindIdentifierForValue(literal);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLMethod] returns empty.
        @Test
        void fxmlMethodReturnsEmpty() throws Exception {
            FXMLMethod fxmlMethod = new FXMLMethod("onClick", List.of(), new FXMLClassType(void.class));

            Optional<Object> result = invokeFindIdentifierForValue(fxmlMethod);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLResource] returns empty.
        @Test
        void fxmlResourceReturnsEmpty() throws Exception {
            FXMLResource resource = new FXMLResource("style.css");

            Optional<Object> result = invokeFindIdentifierForValue(resource);

            assertThat(result).isEmpty();
        }

        /// Verifies that [FXMLTranslation] returns empty.
        @Test
        void fxmlTranslationReturnsEmpty() throws Exception {
            FXMLTranslation translation = new FXMLTranslation("key.label");

            Optional<Object> result = invokeFindIdentifierForValue(translation);

            assertThat(result).isEmpty();
        }
    }

    /// Tests for the private `prepareArgumentsLists(StringBuilder, Constructions, SourceCodeGeneratorContext)` method.
    @Nested
    class PrepareArgumentsListsPrivateTest {

        /// Invokes the private `prepareArgumentsLists` method via reflection.
        ///
        /// @param builder       The [StringBuilder] to append to.
        /// @param constructions The [Constructions] instance to process.
        /// @param context       The [SourceCodeGeneratorContext] to use.
        /// @return The resulting [StringBuilder].
        /// @throws Exception if reflection fails.
        private StringBuilder invokePrepareArgumentsLists(
                StringBuilder builder,
                Constructions constructions,
                SourceCodeGeneratorContext context
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "prepareArgumentsLists",
                    StringBuilder.class,
                    Constructions.class,
                    SourceCodeGeneratorContext.class
            );
            method.setAccessible(true);
            return (StringBuilder) method.invoke(classUnderTest, builder, constructions, context);
        }

        /// Verifies that an [AbstractFXMLObjectAndDependencies] with no constructor properties
        /// produces an empty argument list (no commas, no values appended).
        @Test
        void objectWithNoConstructorPropertiesProducesEmptyArgs() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLObject obj = new FXMLObject(
                    new FXMLInternalIdentifier(0), new FXMLClassType(Object.class),
                    Optional.empty(), List.of()
            );
            AbstractFXMLObjectAndDependencies construction = new AbstractFXMLObjectAndDependencies(
                    obj, List.of(), List.of()
            );
            StringBuilder builder = new StringBuilder();

            invokePrepareArgumentsLists(builder, construction, context);

            assertThat(builder.toString()).isEmpty();
        }

        /// Verifies that an [AbstractFXMLObjectAndDependencies] with constructor properties matching
        /// `javafx.geometry.Point2D`'s `x` and `y` `@NamedArg` parameters produces a comma-separated
        /// argument list with the encoded values.
        @Test
        void objectWithMatchingConstructorPropertiesProducesCommaSeparatedArgs() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLConstructorProperty propX = new FXMLConstructorProperty(
                    "x", new FXMLClassType(double.class), new FXMLLiteral("1.0")
            );
            FXMLConstructorProperty propY = new FXMLConstructorProperty(
                    "y", new FXMLClassType(double.class), new FXMLLiteral("2.0")
            );
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("myPoint"),
                    new FXMLClassType(javafx.geometry.Point2D.class),
                    Optional.empty(), List.of(propX, propY)
            );
            AbstractFXMLObjectAndDependencies construction = new AbstractFXMLObjectAndDependencies(
                    obj, List.of(propX, propY), List.of()
            );
            StringBuilder builder = new StringBuilder();

            invokePrepareArgumentsLists(builder, construction, context);

            assertThat(builder.toString()).contains("1.0").contains("2.0").contains(", ");
        }

        /// Verifies that when a constructor property is absent from the FXML properties map,
        /// the default value from the `@NamedArg` annotation is used (or the type default).
        @Test
        void objectWithMissingConstructorPropertyUsesDefaultValue() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            // Point2D has x and y; provide only x — y should use default (0.0)
            FXMLConstructorProperty propX = new FXMLConstructorProperty(
                    "x", new FXMLClassType(double.class), new FXMLLiteral("3.0")
            );
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("myPoint"),
                    new FXMLClassType(javafx.geometry.Point2D.class),
                    Optional.empty(), List.of(propX)
            );
            AbstractFXMLObjectAndDependencies construction = new AbstractFXMLObjectAndDependencies(
                    obj, List.of(propX), List.of()
            );
            StringBuilder builder = new StringBuilder();

            invokePrepareArgumentsLists(builder, construction, context);

            // x is provided, y falls back to default (0.0 from @NamedArg or type default)
            assertThat(builder.toString()).contains("3.0");
        }

        /// Verifies that an [FXMLCopyAndDependencies] appends the `$$`-prefixed source name to the builder.
        @Test
        void fxmlCopyAndDependenciesAppendsPrefixedSourceName() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLExposedIdentifier sourceId = new FXMLExposedIdentifier("original");
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), sourceId);
            FXMLCopyAndDependencies construction = new FXMLCopyAndDependencies(copy, List.of());
            StringBuilder builder = new StringBuilder();

            invokePrepareArgumentsLists(builder, construction, context);

            assertThat(builder.toString()).isEqualTo("$$original");
        }

        /// Verifies that an [FXMLIncludeAndDependencies] leaves the builder unchanged.
        @Test
        void fxmlIncludeAndDependenciesLeavesBuilderUnchanged() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("myInclude"),
                    "view.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazyDoc
            );
            FXMLIncludeAndDependencies construction = new FXMLIncludeAndDependencies(include, List.of());
            StringBuilder builder = new StringBuilder("prefix");

            invokePrepareArgumentsLists(builder, construction, context);

            assertThat(builder.toString()).isEqualTo("prefix");
        }

        /// Verifies that an [FXMLValueConstruction] leaves the builder unchanged.
        @Test
        void fxmlValueConstructionLeavesBuilderUnchanged() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLValue value = new FXMLValue(Optional.empty(), new FXMLClassType(String.class), "hello");
            FXMLValueConstruction construction = new FXMLValueConstruction(value, List.of());
            StringBuilder builder = new StringBuilder("prefix");

            invokePrepareArgumentsLists(builder, construction, context);

            assertThat(builder.toString()).isEqualTo("prefix");
        }
    }

    /// Tests for the private `addPrologue` method of [FXMLSourceCodeBuilder].
    @Nested
    class AddProloguePrivateTest {

        /// Invokes the private `addPrologue` method via reflection.
        ///
        /// @param context                  The source code generator context.
        /// @param constructorDependencies  The set of identifiers that are constructor dependencies.
        /// @param typeString               The type string for the identifier.
        /// @param identifier               The FXML identifier to initialize.
        /// @param sourceCodeConsumer       A consumer that appends additional source code logic.
        /// @throws Exception If reflection fails or the method throws.
        private void invokeAddPrologue(
                SourceCodeGeneratorContext context,
                Set<FXMLIdentifier> constructorDependencies,
                String typeString,
                FXMLIdentifier identifier,
                Consumer<StringBuilder> sourceCodeConsumer
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addPrologue",
                    SourceCodeGeneratorContext.class,
                    Set.class,
                    String.class,
                    FXMLIdentifier.class,
                    Consumer.class
            );
            method.setAccessible(true);
            method.invoke(classUnderTest, context, constructorDependencies, typeString, identifier, sourceCodeConsumer);
        }

        /// Verifies that an [FXMLInternalIdentifier] produces a local variable declaration
        /// with the type prefix and the `$internalVariable$`-prefixed id, and no bind line.
        @Test
        void internalIdentifierProducesLocalVarWithTypePrefix() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInternalIdentifier id = new FXMLInternalIdentifier(1);

            invokeAddPrologue(context, Set.of(), "javafx.scene.Node", id, sb -> sb.append("new javafx.scene.Node()"));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).isEqualTo("javafx.scene.Node $internalVariable$001 = new javafx.scene.Node();\n");
        }

        /// Verifies that an [FXMLExposedIdentifier] that is not a dependency produces a plain assignment
        /// without a type prefix and without a bind line.
        @Test
        void exposedIdentifierNotDependencyProducesPlainAssignment() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myLabel");

            invokeAddPrologue(context, Set.of(), "javafx.scene.control.Label", id, sb -> sb.append("new javafx.scene.control.Label()"));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).isEqualTo("myLabel = new javafx.scene.control.Label();\n");
        }

        /// Verifies that an [FXMLExposedIdentifier] that is a constructor dependency produces a
        /// `$$`-prefixed local variable declaration followed by a bind line assigning it to the field.
        @Test
        void exposedIdentifierAsDependencyProducesPrefixedVarAndBindLine() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myButton");

            invokeAddPrologue(context, Set.of(id), "javafx.scene.control.Button", id, sb -> sb.append("new javafx.scene.control.Button()"));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).contains("javafx.scene.control.Button $$myButton = new javafx.scene.control.Button();\n");
            assertThat(result).contains("myButton = $$myButton;\n");
        }

        /// Verifies that the `sourceCodeConsumer` is called and its output is appended between
        /// the identifier and the semicolon.
        @Test
        void sourceCodeConsumerOutputIsAppendedCorrectly() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInternalIdentifier id = new FXMLInternalIdentifier(42);

            invokeAddPrologue(context, Set.of(), "String", id, sb -> sb.append("\"hello\""));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).isEqualTo("String $internalVariable$042 = \"hello\";\n");
        }

        /// Verifies that an [FXMLInternalIdentifier] that is present in the constructor dependencies set
        /// still produces a plain local variable declaration without a `$$` prefix and without a bind line,
        /// because `isDependency` is `false` when `isInternalIdentifier` is `true`.
        @Test
        void internalIdentifierInDependencySetProducesLocalVarWithoutBindLine() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInternalIdentifier id = new FXMLInternalIdentifier(5);

            invokeAddPrologue(context, Set.of(id), "javafx.scene.Node", id, sb -> sb.append("new javafx.scene.Node()"));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).isEqualTo("javafx.scene.Node $internalVariable$005 = new javafx.scene.Node();\n");
        }

        /// Verifies that calling `addPrologue` twice appends both lines sequentially.
        @Test
        void callingTwiceAppendsBothLines() throws Exception {
            SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
            FXMLInternalIdentifier id1 = new FXMLInternalIdentifier(10);
            FXMLInternalIdentifier id2 = new FXMLInternalIdentifier(20);

            invokeAddPrologue(context, Set.of(), "int", id1, sb -> sb.append("1"));
            invokeAddPrologue(context, Set.of(), "int", id2, sb -> sb.append("2"));

            String result = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString();
            assertThat(result).isEqualTo("int $internalVariable$010 = 1;\nint $internalVariable$020 = 2;\n");
        }
    }

    /// Tests for the private `bindPrologueVariableToField` method of [FXMLSourceCodeBuilder].
    @Nested
    class BindPrologueVariableToFieldPrivateTest {

        /// Invokes the private `bindPrologueVariableToField` method via reflection.
        ///
        /// @param sourceCode     The [StringBuilder] to append to.
        /// @param fxmlIdentifier The FXML identifier to bind.
        /// @throws Exception If reflection fails or the method throws.
        private void invokeBindPrologueVariableToField(
                StringBuilder sourceCode,
                FXMLIdentifier fxmlIdentifier
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "bindPrologueVariableToField",
                    StringBuilder.class,
                    FXMLIdentifier.class
            );
            method.setAccessible(true);
            method.invoke(classUnderTest, sourceCode, fxmlIdentifier);
        }

        /// Verifies that an [FXMLExposedIdentifier] produces `name = $$name;\n`.
        @Test
        void exposedIdentifierProducesPrefixedBindLine() throws Exception {
            StringBuilder sb = new StringBuilder();
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myField");

            invokeBindPrologueVariableToField(sb, id);

            assertThat(sb.toString()).isEqualTo("myField = $$myField;\n");
        }

        /// Verifies that an [FXMLInternalIdentifier] produces `$internalVariable$NNN = $internalVariable$NNN;\n` (no `$$` prefix).
        @Test
        void internalIdentifierProducesUnprefixedBindLine() throws Exception {
            StringBuilder sb = new StringBuilder();
            FXMLInternalIdentifier id = new FXMLInternalIdentifier(7);

            invokeBindPrologueVariableToField(sb, id);

            assertThat(sb.toString()).isEqualTo("$internalVariable$007 = $internalVariable$007;\n");
        }

        /// Verifies that calling the method appends to existing content in the builder.
        @Test
        void appendsToExistingBuilderContent() throws Exception {
            StringBuilder sb = new StringBuilder("existing;\n");
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("node");

            invokeBindPrologueVariableToField(sb, id);

            assertThat(sb.toString()).isEqualTo("existing;\nnode = $$node;\n");
        }
    }

    /// Tests for the private `addConstructorEpilogue(SourceCodeGeneratorContext, FXMLDocument)` method
    /// and all methods it calls.
    @Nested
    class AddConstructorEpiloguePrivateTest {

        private SourceCodeGeneratorContext buildContext() {
            return new SourceCodeGeneratorContext(new Imports(List.of(), Map.of()), "", Map.of(), null);
        }

        private FXMLDocument buildDocument(AbstractFXMLObject root, List<AbstractFXMLValue> definitions) {
            return new FXMLDocument(
                    "MyClass", root, List.of(), Optional.empty(), Optional.empty(),
                    definitions, List.of()
            );
        }

        private FXMLObject buildRoot(FXMLIdentifier identifier, List<FXMLProperty> properties) {
            return new FXMLObject(identifier, new FXMLClassType(Object.class), Optional.empty(), properties);
        }

        private void invokeAddConstructorEpilogue(SourceCodeGeneratorContext context, FXMLDocument document)
                throws Exception {
            Method m = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addConstructorEpilogue",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class
            );
            m.setAccessible(true);
            m.invoke(classUnderTest, context, document);
        }

        /// Verifies that a document with no properties and no definitions produces an empty epilogue.
        @Test
        void documentWithNoPropertiesProducesEmptyEpilogue() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument document = buildDocument(
                    buildRoot(FXMLRootIdentifier.INSTANCE, List.of()),
                    List.of()
            );

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString()).isEmpty();
        }

        /// Verifies that a `FXMLCollection` root produces `.add(...)` calls for each child.
        @Test
        void fxmlCollectionRootProducesAddCalls() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral lit = new FXMLLiteral("hello");
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("items"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(lit)
            );
            FXMLDocument document = buildDocument(collection, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("items.add(");
        }

        /// Verifies that a `FXMLMap` root produces `.put(...)` calls for each entry.
        @Test
        void fxmlMapRootProducesPutCalls() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral key = new FXMLLiteral("k");
            FXMLLiteral val = new FXMLLiteral("v");
            FXMLMap map = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLClassType(java.util.HashMap.class),
                    FXMLType.of(String.class),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    Map.of(key, val)
            );
            FXMLDocument document = buildDocument(map, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("myMap.put(");
        }

        /// Verifies that a `FXMLObject` root with an `FXMLObjectProperty` produces a setter call.
        @Test
        void fxmlObjectWithObjectPropertyProducesSetterCall() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral value = new FXMLLiteral("42");
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "width", "setWidth", FXMLType.of(double.class), value
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("pane"), List.of(prop));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("pane.setWidth(");
        }

        /// Verifies that a `FXMLObject` with a `FXMLCollectionProperties` produces `.add(...)` calls.
        @Test
        void fxmlObjectWithCollectionPropertyProducesAddCalls() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral item = new FXMLLiteral("item1");
            FXMLCollectionProperties collProp = new FXMLCollectionProperties(
                    "children", "getChildren",
                    FXMLType.of(javafx.scene.layout.Pane.class),
                    FXMLType.OBJECT,
                    List.of(item),
                    Optional.empty()
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("pane"), List.of(collProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("pane.getChildren().add(");
        }

        /// Verifies that a `FXMLObject` with a `FXMLMapProperty` produces `.put(...)` calls.
        @Test
        void fxmlObjectWithMapPropertyProducesPutCalls() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral key = new FXMLLiteral("k");
            FXMLLiteral val = new FXMLLiteral("v");
            FXMLMapProperty mapProp = new FXMLMapProperty(
                    "properties", "getProperties",
                    FXMLType.of(java.util.Map.class),
                    FXMLType.of(String.class),
                    FXMLType.of(String.class),
                    Map.of(key, val),
                    Optional.empty()
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("node"), List.of(mapProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("node.getProperties().put(");
        }

        /// Verifies that a `FXMLObject` with a `FXMLStaticObjectProperty` produces a static setter call.
        @Test
        void fxmlObjectWithStaticObjectPropertyProducesStaticSetterCall() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral value = new FXMLLiteral("0");
            FXMLStaticObjectProperty staticProp = new FXMLStaticObjectProperty(
                    "rowIndex",
                    new FXMLClassType(javafx.scene.layout.GridPane.class),
                    "setRowIndex",
                    FXMLType.of(int.class),
                    value
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("btn"), List.of(staticProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("GridPane.setRowIndex(btn, ");
        }

        /// Verifies that a `FXMLObject` with a `FXMLConstructorProperty` recurses into the value.
        @Test
        void fxmlObjectWithConstructorPropertyRecursesIntoValue() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral innerLit = new FXMLLiteral("5");
            FXMLConstructorProperty ctorProp = new FXMLConstructorProperty(
                    "x", FXMLType.of(double.class), innerLit
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("pt"), List.of(ctorProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            // FXMLLiteral is a no-op in the value-level switch, so epilogue stays empty
            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString()).isEmpty();
        }

        /// Verifies that a `FXMLInclude` with a controller produces the controller suffix assignment.
        @Test
        void fxmlIncludeWithControllerProducesControllerSuffixAssignment() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of()
            );
            FXMLDocument includedDoc = new FXMLDocument(
                    "SubClass",
                    buildRoot(FXMLRootIdentifier.INSTANCE, List.of()),
                    List.of(),
                    Optional.of(controller),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            lazy.set(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("sub"),
                    "sub.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = buildRoot(
                    FXMLRootIdentifier.INSTANCE,
                    List.of(new FXMLConstructorProperty("sub", FXMLType.OBJECT, include))
            );
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("subController = sub.")
                    .contains("controller");
        }

        /// Verifies that a `FXMLInclude` without a controller produces no epilogue output.
        @Test
        void fxmlIncludeWithoutControllerProducesNoOutput() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "SubClass",
                    buildRoot(FXMLRootIdentifier.INSTANCE, List.of()),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            lazy.set(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("sub"),
                    "sub.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = buildRoot(
                    FXMLRootIdentifier.INSTANCE,
                    List.of(new FXMLConstructorProperty("sub", FXMLType.OBJECT, include))
            );
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString()).isEmpty();
        }

        /// Verifies that definitions are also processed in the epilogue.
        @Test
        void definitionsAreProcessedInEpilogue() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral lit = new FXMLLiteral("x");
            FXMLCollection defCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("defItems"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(lit)
            );
            FXMLDocument document = buildDocument(
                    buildRoot(FXMLRootIdentifier.INSTANCE, List.of()),
                    List.of(defCollection)
            );

            invokeAddConstructorEpilogue(context, document);

            assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                    .contains("defItems.add(");
        }

        /// Verifies that no-op value types (`FXMLConstant`, `FXMLCopy`, `FXMLExpression`,
        /// `FXMLInlineScript`, `FXMLLiteral`, `FXMLMethod`, `FXMLReference`, `FXMLResource`,
        /// `FXMLTranslation`, `FXMLValue`) produce no epilogue output when used as constructor
        /// property values inside an `FXMLObject` root.
        @Test
        void noOpValueTypesProduceNoEpilogueOutput() throws Exception {
            List<AbstractFXMLValue> noOps = List.of(
                    new FXMLConstant(new FXMLClassType(Object.class), "CONST", new FXMLClassType(Object.class)),
                    new FXMLCopy(new FXMLExposedIdentifier("cp"), new FXMLExposedIdentifier("src")),
                    new FXMLExpression("1+1"),
                    new FXMLInlineScript("var x = 1;"),
                    new FXMLLiteral("lit"),
                    new FXMLMethod("doIt", List.of(), new FXMLClassType(void.class)),
                    new FXMLReference("someRef"),
                    new FXMLResource("img.png"),
                    new FXMLTranslation("key"),
                    new FXMLValue(Optional.empty(), new FXMLClassType(String.class), "hello")
            );
            for (AbstractFXMLValue noOp : noOps) {
                SourceCodeGeneratorContext context = buildContext();
                FXMLObject root = buildRoot(
                        FXMLRootIdentifier.INSTANCE,
                        List.of(new FXMLConstructorProperty("x", FXMLType.OBJECT, noOp))
                );
                FXMLDocument document = buildDocument(root, List.of());
                invokeAddConstructorEpilogue(context, document);
                assertThat(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString())
                        .as("Expected no epilogue for " + noOp.getClass().getSimpleName())
                        .isEmpty();
            }
        }

        /// Verifies that `FXMLCollection` children are recursed into for nested epilogue generation.
        @Test
        void fxmlCollectionChildrenAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral innerLit = new FXMLLiteral("inner");
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("inner"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(innerLit)
            );
            FXMLCollection outer = new FXMLCollection(
                    new FXMLExposedIdentifier("outer"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(innerCollection)
            );
            FXMLDocument document = buildDocument(outer, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("outer.add(");
            assertThat(epilogue).contains("inner.add(");
        }

        /// Verifies that `FXMLMap` entry values are recursed into for nested epilogue generation.
        @Test
        void fxmlMapEntryValuesAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral key = new FXMLLiteral("k");
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("nested"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(new FXMLLiteral("x"))
            );
            FXMLMap map = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLClassType(java.util.HashMap.class),
                    FXMLType.of(String.class),
                    FXMLType.OBJECT,
                    Optional.empty(),
                    Map.of(key, innerCollection)
            );
            FXMLDocument document = buildDocument(map, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("myMap.put(");
            assertThat(epilogue).contains("nested.add(");
        }

        /// Verifies that `FXMLObjectProperty` value is recursed into for nested epilogue generation.
        @Test
        void fxmlObjectPropertyValueIsRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("inner"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(new FXMLLiteral("x"))
            );
            FXMLObjectProperty prop = new FXMLObjectProperty(
                    "items", "setItems", FXMLType.OBJECT, innerCollection
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("ctrl"), List.of(prop));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("ctrl.setItems(");
            assertThat(epilogue).contains("inner.add(");
        }

        /// Verifies that `FXMLStaticObjectProperty` value is recursed into for nested epilogue generation.
        @Test
        void fxmlStaticObjectPropertyValueIsRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("inner"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(new FXMLLiteral("x"))
            );
            FXMLStaticObjectProperty staticProp = new FXMLStaticObjectProperty(
                    "constraint",
                    new FXMLClassType(javafx.scene.layout.GridPane.class),
                    "setConstraints",
                    FXMLType.OBJECT,
                    innerCollection
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("btn"), List.of(staticProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("GridPane.setConstraints(btn, ");
            assertThat(epilogue).contains("inner.add(");
        }

        /// Verifies that `FXMLCollectionProperties` values are recursed into for nested epilogue generation.
        @Test
        void fxmlCollectionPropertiesValuesAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("inner"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(new FXMLLiteral("x"))
            );
            FXMLCollectionProperties collProp = new FXMLCollectionProperties(
                    "children", "getChildren",
                    FXMLType.of(javafx.scene.layout.Pane.class),
                    FXMLType.OBJECT,
                    List.of(innerCollection),
                    Optional.empty()
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("pane"), List.of(collProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("pane.getChildren().add(");
            assertThat(epilogue).contains("inner.add(");
        }

        /// Verifies that `FXMLMapProperty` entry values are recursed into for nested epilogue generation.
        @Test
        void fxmlMapPropertyEntryValuesAreRecursed() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLLiteral key = new FXMLLiteral("k");
            FXMLCollection innerCollection = new FXMLCollection(
                    new FXMLExposedIdentifier("inner"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(new FXMLLiteral("x"))
            );
            FXMLMapProperty mapProp = new FXMLMapProperty(
                    "properties", "getProperties",
                    FXMLType.of(java.util.Map.class),
                    FXMLType.of(String.class),
                    FXMLType.OBJECT,
                    Map.of(key, innerCollection),
                    Optional.empty()
            );
            FXMLObject root = buildRoot(new FXMLExposedIdentifier("node"), List.of(mapProp));
            FXMLDocument document = buildDocument(root, List.of());

            invokeAddConstructorEpilogue(context, document);

            String epilogue = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString();
            assertThat(epilogue).contains("node.getProperties().put(");
            assertThat(epilogue).contains("inner.add(");
        }
    }

    /// Tests for the private `bindControllerFields(SourceCodeGeneratorContext, FXMLDocument)` method
    /// of [FXMLSourceCodeBuilder] and all methods it calls.
    @Nested
    class BindControllerFieldsPrivateTest {

        /// Invokes the private `bindControllerFields(SourceCodeGeneratorContext, FXMLDocument)` method via reflection.
        ///
        /// @param context  The source code generator context.
        /// @param document The FXML document to process.
        /// @throws Exception If reflection fails or the method throws.
        private void invokeBindControllerFields(
                SourceCodeGeneratorContext context,
                FXMLDocument document
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "bindControllerFields",
                    SourceCodeGeneratorContext.class,
                    FXMLDocument.class
            );
            method.setAccessible(true);
            method.invoke(classUnderTest, context, document);
        }

        /// Builds a minimal [FXMLDocument] with the given root and definitions.
        ///
        /// @param root        The root FXML object.
        /// @param definitions The list of definition values.
        /// @return A new [FXMLDocument].
        private FXMLDocument buildDocument(AbstractFXMLObject root, List<AbstractFXMLValue> definitions) {
            return new FXMLDocument(
                    "TestClass", root, List.of(), Optional.empty(), Optional.empty(), definitions, List.of()
            );
        }

        /// Builds a minimal [FXMLDocument] with the given root, definitions, and controller.
        ///
        /// @param root        The root FXML object.
        /// @param definitions The list of definition values.
        /// @param controller  The FXML controller.
        /// @return A new [FXMLDocument].
        private FXMLDocument buildDocument(
                AbstractFXMLObject root,
                List<AbstractFXMLValue> definitions,
                FXMLController controller
        ) {
            return new FXMLDocument(
                    "TestClass", root, List.of(), Optional.of(controller), Optional.empty(), definitions, List.of()
            );
        }

        /// Wraps a non-[AbstractFXMLObject] value inside an [FXMLObject] via a [FXMLConstructorProperty],
        /// so it can be used as the root of an [FXMLDocument].
        ///
        /// @param value The value to wrap.
        /// @return An [FXMLObject] containing the value as a constructor property.
        private FXMLObject wrapValue(AbstractFXMLValue value) {
            return new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of(new FXMLConstructorProperty("arg", new FXMLClassType(Object.class), value))
            );
        }

        /// Builds a minimal root [FXMLObject] with the given identifier and no properties.
        ///
        /// @param identifier The identifier for the root object.
        /// @return A new [FXMLObject].
        private FXMLObject buildRoot(FXMLIdentifier identifier) {
            return new FXMLObject(identifier, new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of());
        }

        /// Builds a [SourceCodeGeneratorContext] with the given package name.
        ///
        /// @param packageName The package name for the context.
        /// @return A new [SourceCodeGeneratorContext].
        private SourceCodeGeneratorContext buildContext(String packageName) {
            return new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), packageName, Map.of(), null
            );
        }

        /// Verifies that when no controller is present, no `BIND_CONTROLLER` feature is added
        /// and the `CONTROLLER_FIELDS` source part remains empty.
        @Test
        void noControllerProducesNoBindings() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLDocument document = buildDocument(buildRoot(new FXMLExposedIdentifier("root")), List.of());

            invokeBindControllerFields(context, document);

            assertThat(context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString()).isEmpty();
            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that when a controller has no fields, no `BIND_CONTROLLER` feature is added
        /// and the `CONTROLLER_FIELDS` source part remains empty.
        @Test
        void controllerWithNoFieldsProducesNoBindings() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(), List.of()
            );
            FXMLDocument document = buildDocument(buildRoot(new FXMLExposedIdentifier("root")), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString()).isEmpty();
            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that when a controller has a field matching an [FXMLExposedIdentifier] on the root,
        /// a direct assignment is generated and `BIND_CONTROLLER` feature is added.
        @Test
        void exposedIdentifierOnRootProducesDirectAssignmentAndBindControllerFeature() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "myNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLExposedIdentifier("myNode")), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myNode");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that when no identifier in the document matches any controller field,
        /// no `BIND_CONTROLLER` feature is added.
        @Test
        void noMatchingFieldProducesNoBindControllerFeature() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "otherField", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLExposedIdentifier("myNode")), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that definitions are also processed: an [FXMLExposedIdentifier] in the definitions list
        /// matching a controller field produces a binding.
        @Test
        void definitionWithMatchingFieldProducesBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "defNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLObject defObj = new FXMLObject(
                    new FXMLExposedIdentifier("defNode"),
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(FXMLRootIdentifier.INSTANCE), List.of(defObj), controller
            );

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("defNode");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLNamedRootIdentifier] on the root produces a binding using `this`
        /// as the assign identifier.
        @Test
        void namedRootIdentifierProducesThisBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "rootNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLNamedRootIdentifier("rootNode")), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("this");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLRootIdentifier] on the root produces no binding.
        @Test
        void rootIdentifierProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(FXMLRootIdentifier.INSTANCE), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLInternalIdentifier] on the root produces no binding.
        @Test
        void internalIdentifierProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLInternalIdentifier(1)), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that a PRIVATE controller field produces a reflective field mapping.
        @Test
        void privateControllerFieldProducesReflectiveMapping() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PRIVATE, "myNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLDocument document = buildDocument(
                    buildRoot(new FXMLExposedIdentifier("myNode")), List.of(), controller
            );

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("setAccessible");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLCollection] root binds its own identifier and recurses into children.
        @Test
        void fxmlCollectionBindsIdentifierAndRecursesChildren() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField fieldParent = new FXMLControllerField(
                    Visibility.PUBLIC, "myList", new FXMLClassType(java.util.ArrayList.class)
            );
            FXMLControllerField fieldChild = new FXMLControllerField(
                    Visibility.PUBLIC, "childNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(fieldParent, fieldChild), List.of()
            );
            FXMLObject child = new FXMLObject(
                    new FXMLExposedIdentifier("childNode"),
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of()
            );
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(child)
            );
            FXMLDocument document = buildDocument(collection, List.of(), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myList");
            assertThat(result).contains("childNode");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLMap] root binds its own identifier and recurses into entry values.
        @Test
        void fxmlMapBindsIdentifierAndRecursesEntries() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField fieldMap = new FXMLControllerField(
                    Visibility.PUBLIC, "myMap", new FXMLClassType(java.util.HashMap.class)
            );
            FXMLControllerField fieldEntry = new FXMLControllerField(
                    Visibility.PUBLIC, "entryNode", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(fieldMap, fieldEntry), List.of()
            );
            FXMLObject entryValue = new FXMLObject(
                    new FXMLExposedIdentifier("entryNode"),
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of()
            );
            FXMLMap map = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLClassType(java.util.HashMap.class),
                    FXMLType.OBJECT,
                    FXMLType.OBJECT,
                    Optional.empty(),
                    Map.of(new FXMLLiteral("k"), entryValue)
            );
            FXMLDocument document = buildDocument(map, List.of(), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myMap");
            assertThat(result).contains("entryNode");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLObject] root binds its own identifier and recurses into properties.
        @Test
        void fxmlObjectBindsIdentifierAndRecursesProperties() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField fieldRoot = new FXMLControllerField(
                    Visibility.PUBLIC, "myPane", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLControllerField fieldChild = new FXMLControllerField(
                    Visibility.PUBLIC, "childBtn", new FXMLClassType(javafx.scene.control.Button.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(fieldRoot, fieldChild), List.of()
            );
            FXMLObject child = new FXMLObject(
                    new FXMLExposedIdentifier("childBtn"),
                    new FXMLClassType(javafx.scene.control.Button.class),
                    Optional.empty(), List.of()
            );
            FXMLCollectionProperties prop = new FXMLCollectionProperties(
                    "children", "getChildren",
                    FXMLType.of(javafx.scene.layout.Pane.class),
                    FXMLType.OBJECT,
                    List.of(child),
                    Optional.empty()
            );
            FXMLObject root = new FXMLObject(
                    new FXMLExposedIdentifier("myPane"),
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of(prop)
            );
            FXMLDocument document = buildDocument(root, List.of(), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myPane");
            assertThat(result).contains("childBtn");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLCopy] root binds its identifier when it matches a controller field.
        @Test
        void fxmlCopyBindsIdentifier() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "myCopy", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLObject source = new FXMLObject(
                    new FXMLExposedIdentifier("src"),
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of()
            );
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), new FXMLExposedIdentifier("src"));
            FXMLDocument document = buildDocument(wrapValue(copy), List.of(source), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myCopy");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLInclude] root binds its identifier when it matches a controller field.
        @Test
        void fxmlIncludeBindsIdentifier() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "myInclude", new FXMLClassType(javafx.scene.layout.Pane.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("myInclude"),
                    "view.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    new FXMLLazyLoadedDocument()
            );
            FXMLDocument document = buildDocument(wrapValue(include), List.of(), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myInclude");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLValue] with a matching identifier binds the field.
        @Test
        void fxmlValueWithMatchingIdentifierBindsField() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "myVal", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLValue value = new FXMLValue(
                    Optional.of(new FXMLExposedIdentifier("myVal")),
                    new FXMLClassType(String.class),
                    "hello"
            );
            FXMLDocument document = buildDocument(wrapValue(value), List.of(), controller);

            invokeBindControllerFields(context, document);

            String result = context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString();
            assertThat(result).contains("myVal");
            assertThat(context.features()).contains(Feature.BIND_CONTROLLER);
        }

        /// Verifies that an [FXMLValue] without an identifier produces no binding.
        @Test
        void fxmlValueWithoutIdentifierProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "myVal", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLValue value = new FXMLValue(
                    Optional.empty(),
                    new FXMLClassType(String.class),
                    "hello"
            );
            FXMLDocument document = buildDocument(wrapValue(value), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLMethod] produces no binding (no-op branch).
        @Test
        void fxmlMethodProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLMethod method = new FXMLMethod("onClick", List.of(), new FXMLClassType(void.class));
            FXMLDocument document = buildDocument(wrapValue(method), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLInlineScript] produces no binding (no-op branch).
        @Test
        void fxmlInlineScriptProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLInlineScript script = new FXMLInlineScript("var x = 1;");
            FXMLDocument document = buildDocument(wrapValue(script), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLLiteral] produces no binding (no-op branch).
        @Test
        void fxmlLiteralProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLLiteral literal = new FXMLLiteral("hello");
            FXMLDocument document = buildDocument(wrapValue(literal), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLConstant] produces no binding (no-op branch).
        @Test
        void fxmlConstantProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLConstant constant = new FXMLConstant(new FXMLClassType(String.class), "EMPTY", new FXMLClassType(String.class));
            FXMLDocument document = buildDocument(wrapValue(constant), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLExpression] produces no binding (no-op branch).
        @Test
        void fxmlExpressionProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLExpression expression = new FXMLExpression("someExpr");
            FXMLDocument document = buildDocument(wrapValue(expression), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLReference] produces no binding (no-op branch).
        @Test
        void fxmlReferenceProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLReference reference = new FXMLReference("src");
            FXMLDocument document = buildDocument(wrapValue(reference), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLResource] produces no binding (no-op branch).
        @Test
        void fxmlResourceProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLResource resource = new FXMLResource("image.png");
            FXMLDocument document = buildDocument(wrapValue(resource), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }

        /// Verifies that [FXMLTranslation] produces no binding (no-op branch).
        @Test
        void fxmlTranslationProducesNoBinding() throws Exception {
            SourceCodeGeneratorContext context = buildContext("");
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC, "anyField", new FXMLClassType(String.class)
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(Object.class), List.of(field), List.of()
            );
            FXMLTranslation translation = new FXMLTranslation("key");
            FXMLDocument document = buildDocument(wrapValue(translation), List.of(), controller);

            invokeBindControllerFields(context, document);

            assertThat(context.features()).doesNotContain(Feature.BIND_CONTROLLER);
        }
    }

    /// Tests for the private `addInnerClass(FXMLDocument, SourceCodeGeneratorContext)` method
    /// of [FXMLSourceCodeBuilder].
    @Nested
    class AddInnerClassPrivateTest {

        /// Invokes the private `addInnerClass(FXMLDocument, SourceCodeGeneratorContext)` method via reflection.
        ///
        /// @param document The [FXMLDocument] to process.
        /// @param context  The [SourceCodeGeneratorContext] to use.
        /// @throws Exception if reflection fails or the method throws.
        private void invokeAddInnerClass(
                FXMLDocument document,
                SourceCodeGeneratorContext context
        ) throws Exception {
            Method method = FXMLSourceCodeBuilder.class.getDeclaredMethod(
                    "addInnerClass",
                    FXMLDocument.class,
                    SourceCodeGeneratorContext.class
            );
            method.setAccessible(true);
            method.invoke(classUnderTest, document, context);
        }

        /// Builds a minimal [FXMLDocument] with the given root and no definitions.
        ///
        /// @param root The root FXML object.
        /// @return A new [FXMLDocument].
        private FXMLDocument buildDocument(AbstractFXMLObject root) {
            return new FXMLDocument(
                    "TestClass", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
        }

        /// Builds a minimal [FXMLDocument] with the given root and definitions.
        ///
        /// @param root        The root FXML object.
        /// @param definitions The list of definition values.
        /// @return A new [FXMLDocument].
        private FXMLDocument buildDocument(AbstractFXMLObject root, List<AbstractFXMLValue> definitions) {
            return new FXMLDocument(
                    "TestClass", root, List.of(), Optional.empty(), Optional.empty(), definitions, List.of()
            );
        }

        /// Builds a minimal root [FXMLObject] with no identifier and no properties.
        ///
        /// @return A new [FXMLObject].
        private FXMLObject buildRoot() {
            return new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(), List.of()
            );
        }

        /// Builds a [SourceCodeGeneratorContext] with an empty package name.
        ///
        /// @return A new [SourceCodeGeneratorContext].
        private SourceCodeGeneratorContext buildContext() {
            return new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of()), "", Map.of(), null
            );
        }

        /// Builds a loaded [FXMLLazyLoadedDocument] wrapping the given [FXMLDocument].
        ///
        /// @param document The document to wrap.
        /// @return A loaded [FXMLLazyLoadedDocument].
        private FXMLLazyLoadedDocument buildLazy(FXMLDocument document) {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            lazy.set(document);
            return lazy;
        }

        /// Verifies that a document with no includes produces only a trailing newline in `NESTED_TYPES`.
        @Test
        void documentWithNoIncludesProducesOnlyTrailingNewline() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument document = buildDocument(buildRoot());

            invokeAddInnerClass(document, context);

            assertThat(context.sourceCode(SourcePart.NESTED_TYPES).toString()).isEqualTo("\n");
        }

        /// Verifies that an [FXMLInclude] in the root produces a generated inner class in `NESTED_TYPES`.
        @Test
        void fxmlIncludeInRootProducesInnerClass() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "SubView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = buildLazy(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("sub"),
                    "sub.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of(new FXMLObjectProperty("content", "getContent",
                            FXMLType.of(javafx.scene.layout.Pane.class), include))
            );
            FXMLDocument document = buildDocument(root);

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("SubView");
        }

        /// Verifies that two different [FXMLInclude] source files produce two separate inner classes.
        @Test
        void twoDifferentFxmlIncludesProduceTwoInnerClasses() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc1 = new FXMLDocument(
                    "FirstView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLDocument includedDoc2 = new FXMLDocument(
                    "SecondView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLInclude include1 = new FXMLInclude(
                    new FXMLExposedIdentifier("first"),
                    "first.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    buildLazy(includedDoc1)
            );
            FXMLInclude include2 = new FXMLInclude(
                    new FXMLExposedIdentifier("second"),
                    "second.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    buildLazy(includedDoc2)
            );
            FXMLDocument document = buildDocument(buildRoot(), List.of(include1, include2));

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("FirstView");
            assertThat(result).contains("SecondView");
        }

        /// Verifies that an [FXMLInclude] in the definitions list produces a generated inner class.
        @Test
        void fxmlIncludeInDefinitionsProducesInnerClass() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "DefView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = buildLazy(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("defInclude"),
                    "def.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLDocument document = buildDocument(buildRoot(), List.of(include));

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("DefView");
        }

        /// Verifies that an [FXMLCollection] root with an [FXMLInclude] child recurses and generates an inner class.
        @Test
        void fxmlCollectionWithIncludeChildProducesInnerClass() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "CollectionChild",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = buildLazy(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("child"),
                    "child.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLCollection collection = new FXMLCollection(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(java.util.ArrayList.class),
                    Optional.empty(),
                    List.of(include)
            );
            FXMLDocument document = buildDocument(collection);

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("CollectionChild");
        }

        /// Verifies that an [FXMLMap] root with an [FXMLInclude] entry value recurses and generates an inner class.
        @Test
        void fxmlMapWithIncludeEntryProducesInnerClass() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "MapEntry",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = buildLazy(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("entry"),
                    "entry.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLMap map = new FXMLMap(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(java.util.HashMap.class),
                    FXMLType.OBJECT,
                    FXMLType.OBJECT,
                    Optional.empty(),
                    Map.of(new FXMLLiteral("k"), include)
            );
            FXMLDocument document = buildDocument(map);

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("MapEntry");
        }

        /// Verifies that an [FXMLInclude] with a resource bundle uses the encoded resource bundle expression
        /// in the generated inner class context.
        @Test
        void fxmlIncludeWithResourceBundlePassesResourceBundleToInnerClass() throws Exception {
            SourceCodeGeneratorContext context = buildContext();
            FXMLDocument includedDoc = new FXMLDocument(
                    "ResourceView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = buildLazy(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("res"),
                    "res.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.of("com.example.Messages"),
                    lazy
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of(new FXMLObjectProperty("content", "getContent",
                            FXMLType.of(javafx.scene.layout.Pane.class), include))
            );
            FXMLDocument document = buildDocument(root);

            invokeAddInnerClass(document, context);

            String result = context.sourceCode(SourcePart.NESTED_TYPES).toString();
            assertThat(result).contains("ResourceView");
        }

        /// Verifies that all no-op value types (`FXMLConstant`, `FXMLCopy`, `FXMLExpression`,
        /// `FXMLInlineScript`, `FXMLLiteral`, `FXMLMethod`, `FXMLReference`, `FXMLResource`,
        /// `FXMLTranslation`, `FXMLValue`) produce only a trailing newline in `NESTED_TYPES`.
        @Test
        void noOpValueTypesProduceOnlyTrailingNewline() throws Exception {
            List<AbstractFXMLValue> noOpValues = List.of(
                    new FXMLConstant(new FXMLClassType(String.class), "EMPTY", new FXMLClassType(String.class)),
                    new FXMLCopy(new FXMLExposedIdentifier("copy"), new FXMLExposedIdentifier("src")),
                    new FXMLExpression("x + 1"),
                    new FXMLInlineScript("var x = 1;"),
                    new FXMLLiteral("hello"),
                    new FXMLMethod("handler", List.of(), FXMLType.OBJECT),
                    new FXMLReference("src"),
                    new FXMLResource("img.png"),
                    new FXMLTranslation("key"),
                    new FXMLValue(Optional.empty(), new FXMLClassType(String.class), "val")
            );
            for (AbstractFXMLValue noOpValue : noOpValues) {
                SourceCodeGeneratorContext context = buildContext();
                FXMLDocument document = buildDocument(buildRoot(), List.of(noOpValue));

                invokeAddInnerClass(document, context);

                assertThat(context.sourceCode(SourcePart.NESTED_TYPES).toString())
                        .as("Expected only trailing newline for %s", noOpValue.getClass().getSimpleName())
                        .isEqualTo("\n");
            }
        }
    }

    /// Tests for the public `generateSourceCode(FXMLDocument, String)` method of [FXMLSourceCodeBuilder].
    @Nested
    class GenerateSourceCodePublicTest {

        /// Builds a minimal [FXMLDocument] with the given class name and root object.
        private FXMLDocument buildDocument(String className, AbstractFXMLObject root) {
            return new FXMLDocument(
                    className, root,
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
        }

        /// Builds a simple [FXMLObject] root with no properties.
        private FXMLObject buildRoot() {
            return new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of()
            );
        }

        /// Verifies that passing a null document throws [NullPointerException].
        @Test
        void nullDocumentThrowsNullPointerException() {
            assertThatThrownBy(() -> classUnderTest.generateSourceCode(null, "com.example"))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a minimal document with a package name produces a `package` declaration.
        @Test
        void documentWithPackageNameProducesPackageDeclaration() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).startsWith("package com.example;\n");
        }

        /// Verifies that a null package name produces no `package` declaration.
        @Test
        void nullPackageNameProducesNoPackageDeclaration() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, null);

            assertThat(result).doesNotContain("package ");
        }

        /// Verifies that the generated source contains the class name in the class declaration.
        @Test
        void generatedSourceContainsClassName() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).contains("class MyView");
        }

        /// Verifies that the `@Generated` annotation is included when `addGeneratedAnnotation` is true.
        @Test
        void generatedAnnotationIncludedWhenEnabled() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).contains("@Generated").contains(FXMLSourceCodeBuilder.class.getName());
        }

        /// Verifies that the `@Generated` annotation is omitted when `addGeneratedAnnotation` is false.
        @Test
        void generatedAnnotationOmittedWhenDisabled() {
            DefaultLog log = new DefaultLog(new ConsoleLogger());
            FXMLSourceCodeBuilder builder = new FXMLSourceCodeBuilder(log, "org.example.Bundle", false);
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = builder.generateSourceCode(document, "com.example");

            assertThat(result).doesNotContain("@Generated");
        }

        /// Verifies that the generated source contains a constructor matching the class name.
        @Test
        void generatedSourceContainsConstructor() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).contains("MyView()");
        }

        /// Verifies that the generated source ends with a closing brace.
        @Test
        void generatedSourceEndsWithClosingBrace() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).endsWith("}\n");
        }

        /// Verifies that a document with an exposed identifier produces a `protected final` field.
        @Test
        void documentWithExposedIdentifierProducesProtectedField() {
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of(new FXMLObjectProperty(
                            "children", "getChildren",
                            FXMLType.of(javafx.scene.layout.Pane.class),
                            new FXMLObject(
                                    new FXMLExposedIdentifier("myButton"),
                                    new FXMLClassType(javafx.scene.control.Button.class),
                                    Optional.empty(),
                                    List.of()
                            )
                    ))
            );
            FXMLDocument document = buildDocument("MyView", root);

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).contains("protected final").contains("myButton");
        }

        /// Verifies that a document with an [FXMLInclude] produces a nested inner class.
        @Test
        void documentWithIncludeProducesInnerClass() {
            FXMLDocument includedDoc = new FXMLDocument(
                    "SubView",
                    new FXMLObject(FXMLRootIdentifier.INSTANCE, new FXMLClassType(javafx.scene.layout.Pane.class),
                            Optional.empty(), List.of()),
                    List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            lazy.set(includedDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("sub"),
                    "sub.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE,
                    new FXMLClassType(javafx.scene.layout.Pane.class),
                    Optional.empty(),
                    List.of(new FXMLObjectProperty("content", "getContent",
                            FXMLType.of(javafx.scene.layout.Pane.class), include))
            );
            FXMLDocument document = buildDocument("MainView", root);

            String result = classUnderTest.generateSourceCode(document, "com.example");

            assertThat(result).contains("SubView");
        }

        /// Verifies that the `import` statements appear before the class declaration.
        @Test
        void importsAppearBeforeClassDeclaration() {
            FXMLDocument document = buildDocument("MyView", buildRoot());

            String result = classUnderTest.generateSourceCode(document, "com.example");

            int importIndex = result.indexOf("import ");
            int classIndex = result.indexOf("class MyView");
            assertThat(importIndex).isLessThan(classIndex);
        }
    }
}
