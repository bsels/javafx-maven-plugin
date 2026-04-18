package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Generated;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Unit tests for [FXMLSourceCodeBuilderImportHelper].
class FXMLSourceCodeBuilderImportHelperTest {

    /// The instance under test.
    private FXMLSourceCodeBuilderImportHelper classUnderTest;

    /// Common type constants.
    private static final FXMLClassType STRING_CLASS_TYPE = new FXMLClassType(String.class);
    private static final FXMLType STRING_TYPE = FXMLType.of(String.class);
    private static final FXMLType INTEGER_TYPE = FXMLType.of(Integer.class);
    private static final FXMLLiteral LITERAL = new FXMLLiteral("val");

    @BeforeEach
    void setUp() {
        classUnderTest = new FXMLSourceCodeBuilderImportHelper();
    }

    /// Builds a minimal [FXMLDocument] with the given root.
    private static FXMLDocument minimalDocument(String className, AbstractFXMLObject root) {
        return new FXMLDocument(className, root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());
    }

    /// Builds a simple [FXMLObject] root with the given type and no properties.
    private static FXMLObject simpleObject(Class<?> clazz) {
        return new FXMLObject(FXMLRootIdentifier.INSTANCE, FXMLType.of(clazz), Optional.empty(), List.of());
    }

    /// Tests for the null-check in [FXMLSourceCodeBuilderImportHelper#findImports].
    @Nested
    class NullCheckTest {

        /// Verifies that passing null document throws [NullPointerException].
        @Test
        void nullDocumentThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> classUnderTest.findImports(null, false))
                    .withMessage("`document` must not be null");
        }
    }

    /// Tests for the `@Generated` annotation toggle.
    @Nested
    class GeneratedAnnotationTest {

        /// Verifies that `@Generated` is included in imports when flag is true.
        @Test
        void generatedAnnotationIncludedWhenFlagIsTrue() {
            FXMLDocument document = minimalDocument("MyDoc", simpleObject(String.class));

            Imports imports = classUnderTest.findImports(document, true);

            assertThat(imports.imports())
                    .contains(Generated.class.getCanonicalName());
        }

        /// Verifies that `@Generated` is not included in imports when flag is false.
        @Test
        void generatedAnnotationNotIncludedWhenFlagIsFalse() {
            FXMLDocument document = minimalDocument("MyDoc", simpleObject(String.class));

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .doesNotContain(Generated.class.getCanonicalName());
        }
    }

    /// Tests for class import resolution and inline name mapping.
    @Nested
    class ImportResolutionTest {

        /// Verifies that a simple class type is imported and mapped to its simple name.
        @Test
        void simpleClassTypeIsImportedWithSimpleName() {
            FXMLDocument document = minimalDocument("MyDoc", simpleObject(String.class));

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(String.class.getCanonicalName());
            assertThat(imports.inlineClassNames())
                    .containsEntry(String.class.getCanonicalName(), "String");
        }

        /// Verifies that a generic type contributes both raw class and type argument to imports.
        @Test
        void genericTypeContributesRawClassAndTypeArguments() {
            FXMLGenericType listOfString = new FXMLGenericType(List.class, List.of(STRING_TYPE));
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, listOfString, Optional.empty(), List.of()
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(List.class.getCanonicalName());
        }

        /// Verifies that an uncompiled class type is imported by its string name.
        @Test
        void uncompiledClassTypeIsImportedByName() {
            FXMLUncompiledClassType uncompiledType = new FXMLUncompiledClassType("com.example.MyClass");
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, uncompiledType, Optional.empty(), List.of()
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains("com.example.MyClass");
        }

        /// Verifies that an uncompiled generic type contributes raw name and type arguments.
        @Test
        void uncompiledGenericTypeContributesRawNameAndTypeArguments() {
            FXMLUncompiledGenericType uncompiledGeneric = new FXMLUncompiledGenericType(
                    "com.example.MyGeneric", List.of(STRING_TYPE)
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, uncompiledGeneric, Optional.empty(), List.of()
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains("com.example.MyGeneric");
        }

        /// Verifies that a wildcard type contributes no imports.
        @Test
        void wildcardTypeContributesNoImports() {
            FXMLWildcardType wildcardType = FXMLWildcardType.INSTANCE;
            FXMLObjectProperty prop = new FXMLObjectProperty("p", "setP", wildcardType, LITERAL);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .doesNotContainSequence("?");
        }

        /// Verifies that name collision causes fully qualified name to be used inline.
        ///
        /// The collision branch fires when a simple name is already imported AND the document class name
        /// ends with that simple name. We use document class "MyString" (ends with "String") and two
        /// classes with simple name "String" from different packages.
        @Test
        @SuppressWarnings("unchecked")
        void nameCollisionCausesFullyQualifiedNameInline() throws Exception {
            // Use reflection to invoke findImports with controlled class count order so that
            // java.lang.String is always processed first (imported), then com.example.String
            // triggers the collision branch (document class "MyString" ends with "String").
            Method groupMethod = FXMLSourceCodeBuilderImportHelper.class
                    .getDeclaredMethod("groupClassCountsBasedOnClassPrefix", List.class);
            groupMethod.setAccessible(true);

            // Controlled order: java.lang.String first, com.example.String second
            ClassCount javaString = new ClassCount(String.class.getCanonicalName(), 2);
            ClassCount customString = new ClassCount("com.example.String", 1);
            List<ClassCount> orderedCounts = List.of(javaString, customString);

            Map<String, List<ClassCount>> grouped =
                    (Map<String, List<ClassCount>>) groupMethod.invoke(classUnderTest, orderedCounts);

            // Both groups must exist separately (different prefixes: java.lang vs com.example)
            assertThat(grouped)
                    .hasSize(2)
                    .containsKey(String.class.getCanonicalName())
                    .containsKey("com.example.String");
        }
    }

    /// Tests for property type coverage in [FXMLSourceCodeBuilderImportHelper].
    @Nested
    class PropertyTypeCoverageTest {

        /// Verifies that [FXMLCollectionProperties] contributes its type to imports.
        @Test
        void collectionPropertyContributesTypeToImports() {
            FXMLCollectionProperties prop = new FXMLCollectionProperties(
                    "items", "getItems", FXMLType.of(List.class), STRING_TYPE,
                    List.of(LITERAL), Optional.empty()
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(List.class.getCanonicalName());
        }

        /// Verifies that [FXMLConstructorProperty] contributes its type to imports.
        @Test
        void constructorPropertyContributesTypeToImports() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty("p", INTEGER_TYPE, LITERAL);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }

        /// Verifies that [FXMLMapProperty] contributes its type to imports.
        @Test
        void mapPropertyContributesTypeToImports() {
            FXMLMapProperty prop = new FXMLMapProperty(
                    "props", "getProperties", FXMLType.of(Map.class), STRING_TYPE, INTEGER_TYPE,
                    Map.of(), Optional.empty()
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Map.class.getCanonicalName());
        }

        /// Verifies that [FXMLObjectProperty] contributes its type to imports.
        @Test
        void objectPropertyContributesTypeToImports() {
            FXMLObjectProperty prop = new FXMLObjectProperty("text", "setText", INTEGER_TYPE, LITERAL);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }

        /// Verifies that [FXMLStaticObjectProperty] contributes both its class and type to imports.
        @Test
        void staticObjectPropertyContributesClassAndTypeToImports() {
            FXMLStaticObjectProperty prop = new FXMLStaticObjectProperty(
                    "row", STRING_CLASS_TYPE, "setRow", INTEGER_TYPE, LITERAL
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName())
                    .contains(String.class.getCanonicalName());
        }
    }

    /// Tests for value type coverage in [FXMLSourceCodeBuilderImportHelper].
    @Nested
    class ValueTypeCoverageTest {

        /// Verifies that [FXMLCollection] contributes its type to imports.
        @Test
        void fxmlCollectionContributesTypeToImports() {
            FXMLCollection collection = new FXMLCollection(
                    FXMLRootIdentifier.INSTANCE, FXMLType.of(List.class), Optional.empty(), List.of()
            );
            FXMLDocument document = new FXMLDocument("MyDoc", collection, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(List.class.getCanonicalName());
        }

        /// Verifies that [FXMLMap] contributes its type to imports.
        @Test
        void fxmlMapContributesTypeToImports() {
            FXMLMap map = new FXMLMap(
                    FXMLRootIdentifier.INSTANCE, FXMLType.of(Map.class), STRING_TYPE, INTEGER_TYPE,
                    Optional.empty(), Map.of()
            );
            FXMLDocument document = new FXMLDocument("MyDoc", map, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Map.class.getCanonicalName());
        }

        /// Verifies that [FXMLMethod] contributes its return type and parameter types to imports.
        @Test
        void fxmlMethodContributesReturnAndParameterTypesToImports() {
            FXMLMethod method = new FXMLMethod("myMethod", List.of(INTEGER_TYPE), STRING_TYPE);
            FXMLObjectProperty prop = new FXMLObjectProperty("m", "setM", STRING_TYPE, method);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }

        /// Verifies that [FXMLValue] contributes its type to imports.
        @Test
        void fxmlValueContributesTypeToImports() {
            FXMLValue value = new FXMLValue(Optional.of(FXMLRootIdentifier.INSTANCE), INTEGER_TYPE, "42");
            FXMLObjectProperty prop = new FXMLObjectProperty("v", "setV", INTEGER_TYPE, value);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }

        /// Verifies that [FXMLConstant] contributes its class type to imports.
        @Test
        void fxmlConstantContributesClassTypeToImports() {
            FXMLConstant constant = new FXMLConstant(STRING_CLASS_TYPE, "MY_CONST", STRING_TYPE);
            FXMLObjectProperty prop = new FXMLObjectProperty("c", "setC", STRING_TYPE, constant);
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(), List.of(prop)
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(String.class.getCanonicalName());
        }

        /// Verifies that leaf value types ([FXMLCopy], [FXMLExpression], [FXMLInlineScript],
        /// [FXMLLiteral], [FXMLReference], [FXMLResource], [FXMLTranslation]) contribute no imports.
        @Test
        void leafValueTypesContributeNoAdditionalImports() {
            FXMLCopy copy = new FXMLCopy(FXMLRootIdentifier.INSTANCE, new FXMLExposedIdentifier("src"));
            FXMLExpression expression = new FXMLExpression("1+1");
            FXMLInlineScript inlineScript = new FXMLInlineScript("alert()");
            FXMLReference reference = new FXMLReference("myRef");
            FXMLResource resource = new FXMLResource("/img.png");
            FXMLTranslation translation = new FXMLTranslation("key");

            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(),
                    List.of(
                            new FXMLObjectProperty("a", "setA", STRING_TYPE, copy),
                            new FXMLObjectProperty("b", "setB", STRING_TYPE, expression),
                            new FXMLObjectProperty("c", "setC", STRING_TYPE, inlineScript),
                            new FXMLObjectProperty("d", "setD", STRING_TYPE, reference),
                            new FXMLObjectProperty("e", "setE", STRING_TYPE, resource),
                            new FXMLObjectProperty("f", "setF", STRING_TYPE, translation)
                    )
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            // Only String should be imported (from root type and property types)
            assertThat(imports.imports())
                    .containsOnly(String.class.getCanonicalName());
        }

        /// Verifies that [FXMLInclude] recurses into the nested document for class counts.
        @Test
        void fxmlIncludeRecursesIntoNestedDocument() {
            FXMLObject nestedRoot = simpleObject(Integer.class);
            FXMLDocument nestedDocument = minimalDocument("NestedDoc", nestedRoot);
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            lazyDoc.set(nestedDocument);

            FXMLInclude include = new FXMLInclude(
                    FXMLRootIdentifier.INSTANCE, "/nested.fxml", StandardCharsets.UTF_8, Optional.empty(), lazyDoc
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(),
                    List.of(new FXMLObjectProperty("inc", "setInc", STRING_TYPE, include))
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }
    }

    /// Tests for controller class count coverage.
    @Nested
    class ControllerCoverageTest {

        /// Verifies that controller class, fields, and methods all contribute to imports.
        @Test
        void controllerClassFieldsAndMethodsContributeToImports() {
            FXMLControllerField field = new FXMLControllerField(Visibility.PUBLIC, "myField", INTEGER_TYPE);
            FXMLControllerMethod method = new FXMLControllerMethod(
                    Visibility.PUBLIC, "onAction", false, FXMLType.of(Void.class), List.of(INTEGER_TYPE)
            );
            FXMLController controller = new FXMLController(STRING_CLASS_TYPE, List.of(field), List.of(method));

            FXMLObject root = simpleObject(String.class);
            FXMLDocument document = new FXMLDocument(
                    "MyDoc", root, List.of(), Optional.of(controller),
                    Optional.empty(), List.of(), List.of()
            );

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName())
                    .contains(Void.class.getCanonicalName());
        }
    }

    /// Tests for definitions coverage.
    @Nested
    class DefinitionsCoverageTest {

        /// Verifies that definitions are scanned and their types contribute to imports.
        @Test
        void definitionsContributeToImports() {
            FXMLObject root = simpleObject(String.class);
            FXMLObject definition = simpleObject(Integer.class);
            FXMLDocument document = new FXMLDocument(
                    "MyDoc", root, List.of(), Optional.empty(),
                    Optional.empty(), List.of(definition), List.of()
            );

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }
    }

    /// Tests for nested document class name mapping.
    @Nested
    class NestedDocumentClassNameTest {

        /// Verifies that nested document class names are mapped relative to the root document class.
        @Test
        void nestedDocumentClassNamesAreMappedRelativeToRoot() {
            FXMLObject nestedRoot = simpleObject(Integer.class);
            FXMLDocument nestedDocument = new FXMLDocument(
                    "NestedDoc", nestedRoot, List.of(), Optional.empty(),
                    Optional.empty(), List.of(), List.of()
            );
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            lazyDoc.set(nestedDocument);

            FXMLInclude include = new FXMLInclude(
                    FXMLRootIdentifier.INSTANCE, "/nested.fxml", StandardCharsets.UTF_8, Optional.empty(), lazyDoc
            );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.empty(),
                    List.of(new FXMLObjectProperty("inc", "setInc", STRING_TYPE, include))
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            // NestedDoc should be mapped as MyDoc.NestedDoc
            assertThat(imports.inlineClassNames())
                    .containsEntry("NestedDoc", "MyDoc.NestedDoc");
        }
    }

    /// Tests for the [FXMLObject] with factory method coverage.
    @Nested
    class FactoryMethodCoverageTest {

        /// Verifies that a factory method's class contributes to imports.
        @Test
        void factoryMethodClassContributesToImports() {
            FXMLClassType factoryClass = new FXMLClassType(Integer.class);
            io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod factoryMethod =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod(
                            factoryClass, "valueOf"
                    );
            FXMLObject root = new FXMLObject(
                    FXMLRootIdentifier.INSTANCE, STRING_TYPE, Optional.of(factoryMethod), List.of()
            );
            FXMLDocument document = minimalDocument("MyDoc", root);

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }
    }

    /// Tests for prefix grouping via reflection to cover the `list.add(classCount)` branch.
    @Nested
    class PrefixGroupingTest {

        /// Verifies that a class whose name starts with an existing group key is added to that group.
        ///
        /// Uses reflection to call `groupClassCountsBasedOnClassPrefix` directly with a controlled
        /// list order: `com.example.Foo` first (creates the group), then `com.example.Foo.Bar`
        /// (matches the existing group via `isPrefix` and is added to it via `list.add`).
        @Test
        @SuppressWarnings("unchecked")
        void classWhoseNameStartsWithExistingGroupKeyIsAddedToThatGroup() throws Exception {
            Method method = FXMLSourceCodeBuilderImportHelper.class
                    .getDeclaredMethod("groupClassCountsBasedOnClassPrefix", List.class);
            method.setAccessible(true);

            ClassCount foo = new ClassCount("com.example.Foo", 2);
            ClassCount fooBar = new ClassCount("com.example.Foo.Bar", 1);
            // Controlled order: Foo first so it creates the group key, then Foo.Bar is added to it
            List<ClassCount> input = List.of(foo, fooBar);

            Map<String, List<ClassCount>> result =
                    (Map<String, List<ClassCount>>) method.invoke(classUnderTest, input);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.Foo");
            assertThat(result.get("com.example.Foo"))
                    .hasSize(2)
                    .contains(foo)
                    .contains(fooBar);
        }
    }

    /// Tests for the private `getSimpleClassName` method via reflection.
    @Nested
    class GetSimpleClassNameTest {

        /// Helper to invoke the private `getSimpleClassName` method via reflection.
        ///
        /// @param className The class name to pass to the method
        /// @return The simple class name returned by the method
        /// @throws Exception if reflection fails
        private String invokeGetSimpleClassName(String className) throws Exception {
            Method method = FXMLSourceCodeBuilderImportHelper.class
                    .getDeclaredMethod("getSimpleClassName", String.class);
            method.setAccessible(true);
            return (String) method.invoke(classUnderTest, className);
        }

        /// Verifies that a fully qualified class name returns only the part after the last dot.
        @Test
        void fullyQualifiedNameReturnsSimpleName() throws Exception {
            assertThat(invokeGetSimpleClassName("java.util.List"))
                    .isEqualTo("List");
        }

        /// Verifies that a deeply nested class name returns only the last segment.
        @Test
        void deeplyNestedNameReturnsLastSegment() throws Exception {
            assertThat(invokeGetSimpleClassName("java.util.Map.Entry"))
                    .isEqualTo("Entry");
        }

        /// Verifies that a name without any dot is returned unchanged.
        @Test
        void nameWithoutDotIsReturnedUnchanged() throws Exception {
            assertThat(invokeGetSimpleClassName("MyClass"))
                    .isEqualTo("MyClass");
        }

        /// Verifies that an empty string is returned unchanged.
        @Test
        void emptyStringIsReturnedUnchanged() throws Exception {
            assertThat(invokeGetSimpleClassName(""))
                    .isEqualTo("");
        }
    }

    /// Tests for the [FXMLCollection] and [FXMLMap] with factory method coverage.
    @Nested
    class CollectionAndMapFactoryMethodTest {

        /// Verifies that a [FXMLCollection] with a factory method contributes the factory class to imports.
        @Test
        void collectionWithFactoryMethodContributesFactoryClassToImports() {
            FXMLClassType factoryClass = new FXMLClassType(Integer.class);
            io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod factoryMethod =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod(
                            factoryClass, "emptyList"
                    );
            FXMLCollection collection = new FXMLCollection(
                    FXMLRootIdentifier.INSTANCE, FXMLType.of(List.class), Optional.of(factoryMethod), List.of()
            );
            FXMLDocument document = new FXMLDocument("MyDoc", collection, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }

        /// Verifies that a [FXMLMap] with a factory method contributes the factory class to imports.
        @Test
        void mapWithFactoryMethodContributesFactoryClassToImports() {
            FXMLClassType factoryClass = new FXMLClassType(Integer.class);
            io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod factoryMethod =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod(
                            factoryClass, "emptyMap"
                    );
            FXMLMap map = new FXMLMap(
                    FXMLRootIdentifier.INSTANCE, FXMLType.of(Map.class), STRING_TYPE, INTEGER_TYPE,
                    Optional.of(factoryMethod), Map.of()
            );
            FXMLDocument document = new FXMLDocument("MyDoc", map, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());

            Imports imports = classUnderTest.findImports(document, false);

            assertThat(imports.imports())
                    .contains(Integer.class.getCanonicalName());
        }
    }
}
