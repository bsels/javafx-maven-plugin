package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.TestHelpers;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import static org.assertj.core.api.Assertions.assertThat;

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

        @ParameterizedTest
        @ValueSource(strings = {
                "/examples/ColorDefinitions.fxml",
                "/examples/SetList.fxml",
                "/examples/MapWithReferences.fxml",
                "/examples/GridPaneStaticPropertyElement.fxml",
                "/examples/GridPaneStaticPropertyAttribute.fxml",
                "/examples/GridPaneStaticPropertyElementExplicitValue.fxml",
                "/examples/MyButtonWithConstants.fxml",
                "/examples/MyHashMap.fxml",
                "/examples/NodeProperties.fxml",
                "/examples/FXInclude.fxml",
                "/examples/ButtonWithNoControllerAction.fxml",
                "/examples/ButtonWithControllerAction.fxml",
                "/examples/FXIncludeNestedController.fxml",
        })
        @Disabled("working on it")
        void dummy(String file) throws MojoExecutionException {
            FXMLDocument document = parse(file);
            String sourceCode = classUnderTest.generateSourceCode(document, "com.example");
            assertThat(sourceCode)
                    .isNotNull()
                    .isEqualTo("");
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
}
