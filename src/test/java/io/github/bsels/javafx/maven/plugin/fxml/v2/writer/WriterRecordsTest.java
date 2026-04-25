package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Unit tests for all record and enum types in the [io.github.bsels.javafx.maven.plugin.fxml.v2.writer] package:
/// [ConstructorProperty], [FXMLTypeWrapper], [GroupedClassCount], [ReferenceWrapper], [ClassCount],
/// [AbstractFXMLObjectAndDependencies], [FXMLConstructor], [FXMLCopyAndDependencies],
/// [FXMLIncludeAndDependencies], [FXMLValueConstruction], [Imports], [SourceCodeGeneratorContext],
/// [Feature], and [SourcePart].
class WriterRecordsTest {

    /// A reusable [FXMLType] for use in tests.
    private static final FXMLType STRING_TYPE = FXMLType.of(String.class);

    /// A reusable [FXMLLiteral] for use in tests.
    private static final FXMLLiteral LITERAL = new FXMLLiteral("hello");

    /// A reusable [FXMLIdentifier] for use in tests.
    private static final FXMLIdentifier ROOT_IDENTIFIER = FXMLRootIdentifier.INSTANCE;

    /// A reusable [FXMLObject] for use in tests.
    private static final FXMLObject FXML_OBJECT = new FXMLObject(
            ROOT_IDENTIFIER, STRING_TYPE, Optional.empty(), List.of());

    /// A reusable internal id counter for [FXMLInternalIdentifier].
    private static final int INTERNAL_ID = 1;

    /// A reusable [FXMLExposedIdentifier] for use in tests.
    private static final FXMLExposedIdentifier EXPOSED_IDENTIFIER = new FXMLExposedIdentifier("myId");

    /// A reusable [FXMLCopy] for use in tests.
    private static final FXMLCopy FXML_COPY = new FXMLCopy(ROOT_IDENTIFIER, EXPOSED_IDENTIFIER);

    /// A reusable [FXMLInclude] for use in tests.
    private static final FXMLInclude FXML_INCLUDE = new FXMLInclude(
            new FXMLInternalIdentifier(INTERNAL_ID),
            "/examples/SubDocument.fxml",
            StandardCharsets.UTF_8,
            Optional.empty(),
            new FXMLLazyLoadedDocument());

    /// A reusable [FXMLValue] for use in tests.
    private static final FXMLValue FXML_VALUE = new FXMLValue(Optional.empty(), STRING_TYPE, "hello");

    /// A reusable [Imports] for use in tests.
    private static final Imports EMPTY_IMPORTS = new Imports(List.of(), Map.of());

    /// Tests for the [Feature] enum.
    @Nested
    class FeatureTest {

        /// Verifies that all expected constants are present in the enum.
        @Test
        void allConstantsArePresent() {
            assertThat(EnumSet.allOf(Feature.class))
                    .containsExactlyInAnyOrder(
                            Feature.ABSTRACT_CLASS,
                            Feature.RESOURCE_BUNDLE,
                            Feature.STRING_TO_URL_METHOD,
                            Feature.STRING_TO_URI_METHOD,
                            Feature.STRING_TO_PATH_METHOD,
                            Feature.STRING_TO_FILE_METHOD,
                            Feature.BIND_CONTROLLER);
        }

        /// Verifies that [Feature#valueOf] returns the correct constant.
        @Test
        void valueOfReturnsCorrectConstant() {
            assertThat(Feature.valueOf("RESOURCE_BUNDLE"))
                    .isEqualTo(Feature.RESOURCE_BUNDLE);
        }
    }

    /// Tests for the [SourcePart] enum.
    @Nested
    class SourcePartTest {

        /// Verifies that all expected constants are present in the enum.
        @Test
        void allConstantsArePresent() {
            assertThat(EnumSet.allOf(SourcePart.class))
                    .containsExactlyInAnyOrder(
                            SourcePart.PACKAGE,
                            SourcePart.IMPORTS,
                            SourcePart.CLASS_DECLARATION,
                            SourcePart.FIELDS,
                            SourcePart.CONSTRUCTOR_PROLOGUE,
                            SourcePart.CONSTRUCTOR_SUPER_CALL,
                            SourcePart.CONSTRUCTOR_EPILOGUE,
                            SourcePart.CONTROLLER_FIELDS,
                            SourcePart.CONTROLLER_INITIALIZATION,
                            SourcePart.METHODS,
                            SourcePart.NESTED_TYPES);
        }

        /// Verifies that [SourcePart#valueOf] returns the correct constant.
        @Test
        void valueOfReturnsCorrectConstant() {
            assertThat(SourcePart.valueOf("IMPORTS"))
                    .isEqualTo(SourcePart.IMPORTS);
        }
    }

    /// Tests for the [AbstractFXMLObjectAndDependencies] record.
    @Nested
    class AbstractFXMLObjectAndDependenciesTest {

        /// Verifies that a null object throws [NullPointerException].
        @Test
        void nullObjectThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new AbstractFXMLObjectAndDependencies(null, List.of(), List.of()));
        }

        /// Verifies that null constructorProperties defaults to an empty list.
        @Test
        void nullConstructorPropertiesDefaultsToEmptyList() {
            AbstractFXMLObjectAndDependencies record =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, null, List.of());

            assertThat(record.constructorProperties()).isEmpty();
        }

        /// Verifies that null dependencies defaults to an empty list.
        @Test
        void nullDependenciesDefaultsToEmptyList() {
            AbstractFXMLObjectAndDependencies record =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, List.of(), null);

            assertThat(record.dependencies()).isEmpty();
        }

        /// Verifies that a valid instance stores all fields correctly and copies lists defensively.
        @Test
        void validInstanceStoresFieldsAndCopiesLists() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty("x", STRING_TYPE, LITERAL);
            List<FXMLConstructorProperty> mutableProps = new java.util.ArrayList<>(List.of(prop));
            AbstractFXMLObjectAndDependencies record =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, mutableProps, List.of(ROOT_IDENTIFIER));

            mutableProps.add(new FXMLConstructorProperty("y", STRING_TYPE, LITERAL));

            assertThat(record)
                    .hasFieldOrPropertyWithValue("object", FXML_OBJECT);
            assertThat(record.constructorProperties()).containsExactly(prop);
            assertThat(record.dependencies()).containsExactly(ROOT_IDENTIFIER);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            AbstractFXMLObjectAndDependencies a =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, List.of(), List.of());
            AbstractFXMLObjectAndDependencies b =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, List.of(), List.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [AbstractFXMLObjectAndDependencies#toString] contains the object value.
        @Test
        void toStringContainsObject() {
            AbstractFXMLObjectAndDependencies record =
                    new AbstractFXMLObjectAndDependencies(FXML_OBJECT, List.of(), List.of());

            assertThat(record.toString())
                    .contains(FXML_OBJECT.toString());
        }
    }

    /// Tests for the [FXMLConstructor] record.
    @Nested
    class FXMLConstructorTest {

        /// Verifies that a null properties list throws [NullPointerException].
        @Test
        void nullPropertiesThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLConstructor(null));
        }

        /// Verifies that a valid instance stores properties correctly and copies the list defensively.
        @Test
        void validInstanceStoresPropertiesAndCopiesList() {
            ConstructorProperty prop = new ConstructorProperty("x", STRING_TYPE, Optional.empty());
            List<ConstructorProperty> mutableList = new java.util.ArrayList<>(List.of(prop));
            FXMLConstructor constructor = new FXMLConstructor(mutableList);

            mutableList.add(new ConstructorProperty("y", STRING_TYPE, Optional.empty()));

            assertThat(constructor.properties()).containsExactly(prop);
        }

        /// Verifies that two instances with the same properties are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLConstructor a = new FXMLConstructor(List.of());
            FXMLConstructor b = new FXMLConstructor(List.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLConstructor#toString] contains the properties value.
        @Test
        void toStringContainsProperties() {
            FXMLConstructor constructor = new FXMLConstructor(List.of());

            assertThat(constructor.toString())
                    .contains("properties");
        }
    }

    /// Tests for the [FXMLCopyAndDependencies] record.
    @Nested
    class FXMLCopyAndDependenciesTest {

        /// Verifies that a null copy throws [NullPointerException].
        @Test
        void nullCopyThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLCopyAndDependencies(null, List.of()));
        }

        /// Verifies that null dependencies defaults to an empty list.
        @Test
        void nullDependenciesDefaultsToEmptyList() {
            FXMLCopyAndDependencies record = new FXMLCopyAndDependencies(FXML_COPY, null);

            assertThat(record.dependencies()).isEmpty();
        }

        /// Verifies that a valid instance stores all fields correctly.
        @Test
        void validInstanceStoresFields() {
            FXMLCopyAndDependencies record =
                    new FXMLCopyAndDependencies(FXML_COPY, List.of(ROOT_IDENTIFIER));

            assertThat(record)
                    .hasFieldOrPropertyWithValue("copy", FXML_COPY);
            assertThat(record.dependencies()).containsExactly(ROOT_IDENTIFIER);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLCopyAndDependencies a = new FXMLCopyAndDependencies(FXML_COPY, List.of());
            FXMLCopyAndDependencies b = new FXMLCopyAndDependencies(FXML_COPY, List.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLCopyAndDependencies#toString] contains the copy value.
        @Test
        void toStringContainsCopy() {
            FXMLCopyAndDependencies record = new FXMLCopyAndDependencies(FXML_COPY, List.of());

            assertThat(record.toString())
                    .contains(FXML_COPY.toString());
        }
    }

    /// Tests for the [FXMLIncludeAndDependencies] record.
    @Nested
    class FXMLIncludeAndDependenciesTest {

        /// Verifies that a null include throws [NullPointerException].
        @Test
        void nullIncludeThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLIncludeAndDependencies(null, List.of()));
        }

        /// Verifies that null dependencies defaults to an empty list.
        @Test
        void nullDependenciesDefaultsToEmptyList() {
            FXMLIncludeAndDependencies record = new FXMLIncludeAndDependencies(FXML_INCLUDE, null);

            assertThat(record.dependencies()).isEmpty();
        }

        /// Verifies that a valid instance stores all fields correctly.
        @Test
        void validInstanceStoresFields() {
            FXMLIncludeAndDependencies record =
                    new FXMLIncludeAndDependencies(FXML_INCLUDE, List.of(ROOT_IDENTIFIER));

            assertThat(record)
                    .hasFieldOrPropertyWithValue("include", FXML_INCLUDE);
            assertThat(record.dependencies()).containsExactly(ROOT_IDENTIFIER);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLIncludeAndDependencies a = new FXMLIncludeAndDependencies(FXML_INCLUDE, List.of());
            FXMLIncludeAndDependencies b = new FXMLIncludeAndDependencies(FXML_INCLUDE, List.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLIncludeAndDependencies#toString] contains the include value.
        @Test
        void toStringContainsInclude() {
            FXMLIncludeAndDependencies record = new FXMLIncludeAndDependencies(FXML_INCLUDE, List.of());

            assertThat(record.toString())
                    .contains(FXML_INCLUDE.toString());
        }
    }

    /// Tests for the [FXMLValueConstruction] record.
    @Nested
    class FXMLValueConstructionTest {

        /// Verifies that a null value throws [NullPointerException].
        @Test
        void nullValueThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLValueConstruction(null, List.of()));
        }

        /// Verifies that null dependencies defaults to an empty list.
        @Test
        void nullDependenciesDefaultsToEmptyList() {
            FXMLValueConstruction record = new FXMLValueConstruction(FXML_VALUE, null);

            assertThat(record.dependencies()).isEmpty();
        }

        /// Verifies that a valid instance stores all fields correctly.
        @Test
        void validInstanceStoresFields() {
            FXMLValueConstruction record =
                    new FXMLValueConstruction(FXML_VALUE, List.of(ROOT_IDENTIFIER));

            assertThat(record)
                    .hasFieldOrPropertyWithValue("value", FXML_VALUE);
            assertThat(record.dependencies()).containsExactly(ROOT_IDENTIFIER);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLValueConstruction a = new FXMLValueConstruction(FXML_VALUE, List.of());
            FXMLValueConstruction b = new FXMLValueConstruction(FXML_VALUE, List.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLValueConstruction#toString] contains the value.
        @Test
        void toStringContainsValue() {
            FXMLValueConstruction record = new FXMLValueConstruction(FXML_VALUE, List.of());

            assertThat(record.toString())
                    .contains(FXML_VALUE.toString());
        }
    }

    /// Tests for the [Imports] record.
    @Nested
    class ImportsTest {

        /// Verifies that a null imports list throws [NullPointerException].
        @Test
        void nullImportsThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Imports(null, Map.of()));
        }

        /// Verifies that a null inlineClassNames map throws [NullPointerException].
        @Test
        void nullInlineClassNamesThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Imports(List.of(), null));
        }

        /// Verifies that a valid instance stores all fields correctly and copies them defensively.
        @Test
        void validInstanceStoresFieldsAndCopies() {
            List<String> mutableImports = new java.util.ArrayList<>(List.of("java.util.List"));
            Map<String, String> mutableMap = new java.util.HashMap<>(Map.of("java.lang.String", "String"));
            Imports imports = new Imports(mutableImports, mutableMap);

            mutableImports.add("java.util.Map");
            mutableMap.put("java.util.List", "List");

            assertThat(imports.imports()).containsExactly("java.util.List");
            assertThat(imports.inlineClassNames()).containsOnlyKeys("java.lang.String");
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            Imports a = new Imports(List.of("java.util.List"), Map.of());
            Imports b = new Imports(List.of("java.util.List"), Map.of());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [Imports#toString] contains the imports value.
        @Test
        void toStringContainsImports() {
            Imports imports = new Imports(List.of("java.util.List"), Map.of());

            assertThat(imports.toString())
                    .contains("java.util.List");
        }
    }

    /// Tests for the [SourceCodeGeneratorContext] record.
    @Nested
    class SourceCodeGeneratorContextTest {

        /// Verifies that a null imports throws [NullPointerException] in the convenience constructor.
        @Test
        void nullImportsThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new SourceCodeGeneratorContext(null, "", Map.of(), null));
        }

        /// Verifies that a null resourceBundle throws [NullPointerException] in the convenience constructor.
        @Test
        void nullResourceBundleThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new SourceCodeGeneratorContext(EMPTY_IMPORTS, null, Map.of(), null));
        }

        /// Verifies that a null identifierToTypeMap throws [NullPointerException] in the convenience constructor.
        @Test
        void nullIdentifierToTypeMapThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new SourceCodeGeneratorContext(EMPTY_IMPORTS, "", null, null));
        }

        /// Verifies that the convenience constructor stores fields and initializes defaults.
        @Test
        void convenienceConstructorInitializesDefaults() {
            SourceCodeGeneratorContext ctx =
                    new SourceCodeGeneratorContext(EMPTY_IMPORTS, "bundle", Map.of(), "com.example");

            assertThat(ctx)
                    .hasFieldOrPropertyWithValue("imports", EMPTY_IMPORTS)
                    .hasFieldOrPropertyWithValue("resourceBundle", "bundle")
                    .hasFieldOrPropertyWithValue("packageName", Optional.of("com.example"));
            assertThat(ctx.fieldDefinitions()).isEmpty();
            assertThat(ctx.features()).isEmpty();
            assertThat(ctx.seenNestedFXMLFiles()).isEmpty();
            assertThat(ctx.seenFXMLMethods()).isEmpty();
        }

        /// Verifies that a null packageName results in [Optional#empty].
        @Test
        void nullPackageNameResultsInEmptyOptional() {
            SourceCodeGeneratorContext ctx =
                    new SourceCodeGeneratorContext(EMPTY_IMPORTS, "", Map.of(), null);

            assertThat(ctx.packageName()).isEmpty();
        }

        /// Verifies that [SourceCodeGeneratorContext#sourceCode] returns a [StringBuilder] for each [SourcePart].
        @Test
        void sourceCodeReturnsStringBuilderForEachPart() {
            SourceCodeGeneratorContext ctx =
                    new SourceCodeGeneratorContext(EMPTY_IMPORTS, "", Map.of(), null);

            for (SourcePart part : SourcePart.values()) {
                assertThat(ctx.sourceCode(part)).isNotNull();
            }
        }

        /// Verifies that [SourceCodeGeneratorContext#hasFeature] returns false initially and true after [SourceCodeGeneratorContext#addFeature].
        @Test
        void addFeatureAndHasFeature() {
            SourceCodeGeneratorContext ctx =
                    new SourceCodeGeneratorContext(EMPTY_IMPORTS, "", Map.of(), null);

            assertThat(ctx.hasFeature(Feature.RESOURCE_BUNDLE)).isFalse();
            ctx.addFeature(Feature.RESOURCE_BUNDLE);
            assertThat(ctx.hasFeature(Feature.RESOURCE_BUNDLE)).isTrue();
        }

        /// Verifies that [SourceCodeGeneratorContext#with] creates a new context with the updated resource bundle.
        @Test
        void withCreatesNewContextWithUpdatedResourceBundle() {
            SourceCodeGeneratorContext ctx =
                    new SourceCodeGeneratorContext(EMPTY_IMPORTS, "old", Map.of(), null);
            ctx.addFeature(Feature.ABSTRACT_CLASS);
            SourceCodeGeneratorContext updated = ctx.with("new");

            assertThat(updated)
                    .hasFieldOrPropertyWithValue("resourceBundle", "new")
                    .hasFieldOrPropertyWithValue("imports", EMPTY_IMPORTS);
            assertThat(updated.hasFeature(Feature.ABSTRACT_CLASS)).isFalse();
        }
    }

    /// Tests for the [ConstructorProperty] record.
    @Nested
    class ConstructorPropertyTest {

        /// Verifies that a null name throws [NullPointerException].
        @Test
        void nullNameThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty(null, STRING_TYPE, Optional.empty()));
        }

        /// Verifies that a null type throws [NullPointerException].
        @Test
        void nullTypeThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty("prop", null, Optional.empty()));
        }

        /// Verifies that a null defaultValue throws [NullPointerException].
        @Test
        void nullDefaultValueThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty("prop", STRING_TYPE, null));
        }

        /// Verifies that a valid instance without a default value stores all fields correctly.
        @Test
        void validInstanceWithoutDefaultValueStoresFields() {
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(property)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("defaultValue", Optional.empty());
        }

        /// Verifies that a valid instance with a default value stores all fields correctly.
        @Test
        void validInstanceWithDefaultValueStoresFields() {
            Optional<FXMLLiteral> defaultValue = Optional.of(LITERAL);
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, defaultValue);

            assertThat(property)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("defaultValue", defaultValue);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ConstructorProperty a = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());
            ConstructorProperty b = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ConstructorProperty#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(property.toString())
                    .contains("prop")
                    .contains(STRING_TYPE.toString());
        }
    }

    /// Tests for the [FXMLTypeWrapper] record.
    @Nested
    class FXMLTypeWrapperTest {

        /// Verifies that a null type throws [NullPointerException].
        @Test
        void nullTypeThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLTypeWrapper(null));
        }

        /// Verifies that a valid instance stores the type correctly.
        @Test
        void validInstanceStoresType() {
            FXMLTypeWrapper wrapper = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(wrapper)
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE);
        }

        /// Verifies that two instances with the same type are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLTypeWrapper a = new FXMLTypeWrapper(STRING_TYPE);
            FXMLTypeWrapper b = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLTypeWrapper#toString] contains the type value.
        @Test
        void toStringContainsType() {
            FXMLTypeWrapper wrapper = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(wrapper.toString())
                    .contains(STRING_TYPE.toString());
        }
    }

    /// Tests for the [GroupedClassCount] record.
    @Nested
    class GroupedClassCountTest {

        /// Verifies that a null group throws [NullPointerException].
        @Test
        void nullGroupThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new GroupedClassCount(null, 1, List.of()));
        }

        /// Verifies that a null classes list throws [NullPointerException].
        @Test
        void nullClassesThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new GroupedClassCount("group", 1, null));
        }

        /// Verifies that a negative count throws [IllegalArgumentException].
        @Test
        void negativeCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new GroupedClassCount("group", -1, List.of()))
                    .withMessage("`count` must be non-negative");
        }

        /// Verifies that a zero count is valid.
        @Test
        void zeroCountIsValid() {
            GroupedClassCount gcc = new GroupedClassCount("group", 0, List.of());

            assertThat(gcc)
                    .hasFieldOrPropertyWithValue("group", "group")
                    .hasFieldOrPropertyWithValue("count", 0);
        }

        /// Verifies that a valid instance stores all fields correctly and copies the list defensively.
        @Test
        void validInstanceStoresFieldsAndCopiesList() {
            ClassCount classCount = new ClassCount("com.example.Foo", 3);
            List<ClassCount> mutableList = new java.util.ArrayList<>(List.of(classCount));
            GroupedClassCount gcc = new GroupedClassCount("com.example", 3, mutableList);

            mutableList.add(new ClassCount("com.example.Bar", 1));

            assertThat(gcc)
                    .hasFieldOrPropertyWithValue("group", "com.example")
                    .hasFieldOrPropertyWithValue("count", 3);
            assertThat(gcc.classes())
                    .containsExactly(classCount);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ClassCount classCount = new ClassCount("com.example.Foo", 2);
            GroupedClassCount a = new GroupedClassCount("com.example", 2, List.of(classCount));
            GroupedClassCount b = new GroupedClassCount("com.example", 2, List.of(classCount));

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [GroupedClassCount#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            GroupedClassCount gcc = new GroupedClassCount("com.example", 5, List.of());

            assertThat(gcc.toString())
                    .contains("com.example")
                    .contains("5");
        }
    }

    /// Tests for the [ReferenceWrapper] record.
    @Nested
    class ReferenceWrapperTest {

        /// Verifies that a null reference throws [NullPointerException].
        @Test
        void nullReferenceThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ReferenceWrapper(null));
        }

        /// Verifies that a valid instance stores the reference correctly.
        @Test
        void validInstanceStoresReference() {
            ReferenceWrapper wrapper = new ReferenceWrapper("myId");

            assertThat(wrapper)
                    .hasFieldOrPropertyWithValue("reference", "myId");
        }

        /// Verifies that two instances with the same reference are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ReferenceWrapper a = new ReferenceWrapper("myId");
            ReferenceWrapper b = new ReferenceWrapper("myId");

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ReferenceWrapper#toString] contains the reference value.
        @Test
        void toStringContainsReference() {
            ReferenceWrapper wrapper = new ReferenceWrapper("myId");

            assertThat(wrapper.toString())
                    .contains("myId");
        }
    }

    /// Tests for the package-private [ClassCount] record.
    @Nested
    class ClassCountTest {

        /// Verifies that a null fullClassName throws [NullPointerException].
        @Test
        void nullFullClassNameThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ClassCount(null, 1));
        }

        /// Verifies that a zero or negative count throws [IllegalArgumentException].
        @Test
        void zeroCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ClassCount("com.example.Foo", 0));
        }

        /// Verifies that a negative count throws [IllegalArgumentException].
        @Test
        void negativeCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ClassCount("com.example.Foo", -1));
        }

        /// Verifies that a valid instance stores all fields correctly.
        @Test
        void validInstanceStoresFields() {
            ClassCount classCount = new ClassCount("com.example.Foo", 5);

            assertThat(classCount)
                    .hasFieldOrPropertyWithValue("fullClassName", "com.example.Foo")
                    .hasFieldOrPropertyWithValue("count", 5);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ClassCount a = new ClassCount("com.example.Foo", 3);
            ClassCount b = new ClassCount("com.example.Foo", 3);

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ClassCount#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            ClassCount classCount = new ClassCount("com.example.Foo", 7);

            assertThat(classCount.toString())
                    .contains("com.example.Foo")
                    .contains("7");
        }
    }
}
