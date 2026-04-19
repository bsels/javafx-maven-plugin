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
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
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
}
