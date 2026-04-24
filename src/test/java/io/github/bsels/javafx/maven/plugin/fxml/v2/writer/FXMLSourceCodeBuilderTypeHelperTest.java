package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
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
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import javafx.beans.NamedArg;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for [FXMLSourceCodeBuilderTypeHelper].
class FXMLSourceCodeBuilderTypeHelperTest {

    private FXMLSourceCodeBuilderTypeHelper classUnderTest;
    private SourceCodeGeneratorContext context;

    /// Creates a minimal [SourceCodeGeneratorContext] with the given inline class names.
    ///
    /// @param inlineClassNames The inline class name mappings.
    /// @return A new [SourceCodeGeneratorContext].
    private SourceCodeGeneratorContext makeContext(Map<String, String> inlineClassNames) {
        Imports imports = new Imports(List.of(), inlineClassNames);
        return new SourceCodeGeneratorContext(imports, "rb", Map.of(), "com.example");
    }

    @BeforeEach
    void setUp() {
        classUnderTest = new FXMLSourceCodeBuilderTypeHelper();
        context = makeContext(Map.of(
                "java.lang.String", "String",
                "java.lang.Integer", "Integer",
                "java.util.List", "List",
                "java.net.URI", "URI",
                "java.net.URL", "URL",
                "java.nio.file.Path", "Path",
                "java.io.File", "File",
                "io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilderTypeHelperTest.NamedArgBean",
                "NamedArgBean",
                "io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilderTypeHelperTest.FactoryBean",
                "FactoryBean"
        ));
    }

    /// A simple bean with a [@NamedArg] constructor for testing.
    public static class NamedArgBean {
        /// Creates a new [NamedArgBean] with the given text.
        ///
        /// @param text The text value.
        public NamedArgBean(@NamedArg("text") String text) {
        }
    }

    /// A bean with a factory method annotated with [@NamedArg] for testing.
    public static class FactoryBean {
        private FactoryBean() {
        }

        /// Creates a new [FactoryBean] via factory method.
        ///
        /// @param value The value.
        /// @return A new [FactoryBean].
        public static FactoryBean create(@NamedArg("value") String value) {
            return new FactoryBean();
        }
    }

    // -------------------------------------------------------------------------
    // isPrimitive
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#isPrimitive].
    @Nested
    class IsPrimitiveTest {

        /// Verifies that all Java primitive type names return `true`.
        @Test
        void primitiveTypesReturnTrue() {
            assertThat(classUnderTest.isPrimitive("boolean")).isTrue();
            assertThat(classUnderTest.isPrimitive("byte")).isTrue();
            assertThat(classUnderTest.isPrimitive("char")).isTrue();
            assertThat(classUnderTest.isPrimitive("short")).isTrue();
            assertThat(classUnderTest.isPrimitive("int")).isTrue();
            assertThat(classUnderTest.isPrimitive("long")).isTrue();
            assertThat(classUnderTest.isPrimitive("float")).isTrue();
            assertThat(classUnderTest.isPrimitive("double")).isTrue();
            assertThat(classUnderTest.isPrimitive("void")).isTrue();
        }

        /// Verifies that non-primitive type names return `false`.
        @Test
        void nonPrimitiveTypeReturnsFalse() {
            assertThat(classUnderTest.isPrimitive("String")).isFalse();
            assertThat(classUnderTest.isPrimitive("java.lang.Integer")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // defaultTypeValue
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#defaultTypeValue].
    @Nested
    class DefaultTypeValueTest {

        /// Verifies that null throws [NullPointerException].
        @Test
        void nullThrows() {
            assertThatThrownBy(() -> classUnderTest.defaultTypeValue(null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies default values for all primitive types.
        @Test
        void primitiveDefaultValues() {
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(boolean.class))).isEqualTo("false");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(char.class))).isEqualTo("'\0'");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(byte.class))).isEqualTo("(byte) 0");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(short.class))).isEqualTo("(short) 0");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(int.class))).isEqualTo("0");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(long.class))).isEqualTo("0L");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(float.class))).isEqualTo("0f");
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(double.class))).isEqualTo("0.0");
        }

        /// Verifies that reference types default to `null`.
        @Test
        void referenceTypeDefaultsToNull() {
            assertThat(classUnderTest.defaultTypeValue(FXMLType.of(String.class))).isEqualTo("null");
        }
    }

    // -------------------------------------------------------------------------
    // getTypeForMapEntry
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#getTypeForMapEntry].
    @Nested
    class GetTypeForMapEntryTest {

        /// Verifies that [FXMLType#OBJECT] is replaced with [String] type.
        @Test
        void objectTypeReplacedWithString() {
            assertThat(classUnderTest.getTypeForMapEntry(FXMLType.OBJECT))
                    .isEqualTo(FXMLType.of(String.class));
        }

        /// Verifies that non-Object types are returned unchanged.
        @Test
        void nonObjectTypeReturnedUnchanged() {
            FXMLType intType = FXMLType.of(int.class);
            assertThat(classUnderTest.getTypeForMapEntry(intType)).isEqualTo(intType);
        }
    }

    // -------------------------------------------------------------------------
    // encodeString
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#encodeString].
    @Nested
    class EncodeStringTest {

        /// Verifies that a plain string is encoded with surrounding quotes.
        @Test
        void plainStringIsQuoted() {
            assertThat(classUnderTest.encodeString("hello")).isEqualTo("\"hello\"");
        }

        /// Verifies that special characters are escaped.
        @Test
        void specialCharactersAreEscaped() {
            assertThat(classUnderTest.encodeString("a\"b")).isEqualTo("\"a\\\"b\"");
        }
    }

    // -------------------------------------------------------------------------
    // encodeLiteral
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#encodeLiteral].
    @Nested
    class EncodeLiteralTest {

        private static Stream<Arguments> provideLiteralEncodingCases() {
            return Stream.of(
                    // byte
                    Arguments.of("5", FXMLType.of(byte.class), "(byte) 5"),
                    Arguments.of("5", FXMLType.of(Byte.class), "(byte) 5"),
                    // short
                    Arguments.of("5", FXMLType.of(short.class), "(short) 5"),
                    Arguments.of("5", FXMLType.of(Short.class), "(short) 5"),
                    // int
                    Arguments.of("5", FXMLType.of(int.class), "5"),
                    Arguments.of("5", FXMLType.of(Integer.class), "5"),
                    // long
                    Arguments.of("5", FXMLType.of(long.class), "5L"),
                    Arguments.of("5", FXMLType.of(Long.class), "5L"),
                    // float
                    Arguments.of("1.5", FXMLType.of(float.class), "1.5f"),
                    Arguments.of("1.5", FXMLType.of(Float.class), "1.5f"),
                    // double
                    Arguments.of("3.14", FXMLType.of(double.class), "3.14"),
                    Arguments.of("3.14", FXMLType.of(Double.class), "3.14"),
                    // boolean
                    Arguments.of("true", FXMLType.of(boolean.class), "true"),
                    Arguments.of("true", FXMLType.of(Boolean.class), "true"),
                    Arguments.of("false", FXMLType.of(boolean.class), "false"),
                    Arguments.of("false", FXMLType.of(Boolean.class), "false"),
                    // char
                    Arguments.of("'A'", FXMLType.of(char.class), "'A'"),
                    Arguments.of("'A'", FXMLType.of(Character.class), "'A'"),
                    // float special values
                    Arguments.of("-Infinity", FXMLType.of(Float.class), "Float.NEGATIVE_INFINITY"),
                    Arguments.of("Infinity", FXMLType.of(Float.class), "Float.POSITIVE_INFINITY"),
                    Arguments.of("NaN", FXMLType.of(Float.class), "Float.NaN"),
                    // double special values
                    Arguments.of("-Infinity", FXMLType.of(Double.class), "Double.NEGATIVE_INFINITY"),
                    Arguments.of("Infinity", FXMLType.of(Double.class), "Double.POSITIVE_INFINITY"),
                    Arguments.of("NaN", FXMLType.of(Double.class), "Double.NaN"),
                    // default
                    Arguments.of("#ffffff", FXMLType.of(Color.class), "javafx.scene.paint.Color.valueOf(\"#ffffff\")")
            );
        }

        /// Verifies that null context throws [NullPointerException].
        @Test
        void nullContextThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeLiteral(null, "x", FXMLType.of(String.class)))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null value throws [NullPointerException].
        @Test
        void nullValueThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeLiteral(context, null, FXMLType.of(String.class)))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null type throws [NullPointerException].
        @Test
        void nullTypeThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeLiteral(context, "x", null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a String literal is encoded with quotes.
        @Test
        void stringLiteralIsQuoted() {
            assertThat(classUnderTest.encodeLiteral(context, "hello", FXMLType.of(String.class)))
                    .isEqualTo("\"hello\"");
        }

        /// Verifies that an enum literal is encoded as `EnumType.VALUE`.
        @Test
        void enumLiteralIsEncoded() {
            assertThat(classUnderTest.encodeLiteral(context, "DAYS", FXMLType.of(java.util.concurrent.TimeUnit.class)))
                    .contains("DAYS");
        }

        /// Verifies that a valid char literal is encoded.
        @Test
        void charLiteralIsEncoded() {
            assertThat(classUnderTest.encodeLiteral(context, "'A'", FXMLType.of(char.class)))
                    .isEqualTo("'A'");
            assertThat(classUnderTest.encodeLiteral(context, "'A'", FXMLType.of(Character.class)))
                    .isEqualTo("'A'");
        }

        /// Verifies that an invalid char literal throws [IllegalArgumentException].
        @Test
        void invalidCharLiteralThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeLiteral(context, "AB", FXMLType.of(char.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies double special values.
        @Test
        void doubleSpecialValues() {
            assertThat(classUnderTest.encodeLiteral(context, "-Infinity", FXMLType.of(double.class)))
                    .isEqualTo("Double.NEGATIVE_INFINITY");
            assertThat(classUnderTest.encodeLiteral(context, "Infinity", FXMLType.of(double.class)))
                    .isEqualTo("Double.POSITIVE_INFINITY");
            assertThat(classUnderTest.encodeLiteral(context, "NaN", FXMLType.of(double.class)))
                    .isEqualTo("Double.NaN");
            assertThat(classUnderTest.encodeLiteral(context, "3.14", FXMLType.of(double.class)))
                    .isEqualTo("3.14");
        }

        /// Verifies float special values and suffix.
        @Test
        void floatSpecialValues() {
            assertThat(classUnderTest.encodeLiteral(context, "-Infinity", FXMLType.of(float.class)))
                    .isEqualTo("Float.NEGATIVE_INFINITY");
            assertThat(classUnderTest.encodeLiteral(context, "Infinity", FXMLType.of(float.class)))
                    .isEqualTo("Float.POSITIVE_INFINITY");
            assertThat(classUnderTest.encodeLiteral(context, "NaN", FXMLType.of(float.class)))
                    .isEqualTo("Float.NaN");
            assertThat(classUnderTest.encodeLiteral(context, "1.5", FXMLType.of(float.class)))
                    .isEqualTo("1.5f");
        }

        /// Verifies boolean literal encoding.
        @Test
        void booleanLiteralEncoding() {
            assertThat(classUnderTest.encodeLiteral(context, "true", FXMLType.of(boolean.class))).isEqualTo("true");
            assertThat(classUnderTest.encodeLiteral(context, "false", FXMLType.of(boolean.class))).isEqualTo("false");
            assertThat(classUnderTest.encodeLiteral(context, "TRUE", FXMLType.of(Boolean.class))).isEqualTo("true");
        }

        /// Verifies literal encoding for various types.
        ///
        /// @param value    The value to encode.
        /// @param type     The type to encode for.
        /// @param expected The expected encoding.
        @ParameterizedTest
        @MethodSource("provideLiteralEncodingCases")
        void shouldEncodeLiteralsCorrectly(String value, FXMLType type, String expected) {
            assertThat(classUnderTest.encodeLiteral(context, value, type))
                    .isEqualTo(expected);
        }

        /// Verifies that an uncompiled type falls back to `valueOf`.
        @Test
        void uncompiledTypeFallsBackToValueOf() {
            assertThat(classUnderTest.encodeLiteral(context, "foo", new FXMLUncompiledClassType("com.example.MyType")))
                    .isEqualTo("com.example.MyType.valueOf(\"foo\")");
        }
    }

    // -------------------------------------------------------------------------
    // typeToSourceCode
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#typeToSourceCode].
    @Nested
    class TypeToSourceCodeTest {

        /// Verifies that null context throws [NullPointerException].
        @Test
        void nullContextThrows() {
            assertThatThrownBy(() -> classUnderTest.typeToSourceCode(null, FXMLType.of(String.class)))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null type throws [NullPointerException].
        @Test
        void nullTypeThrows() {
            assertThatThrownBy(() -> classUnderTest.typeToSourceCode(context, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies [FXMLClassType] renders the inline class name.
        @Test
        void classTypeRendersInlineName() {
            assertThat(classUnderTest.typeToSourceCode(context, FXMLType.of(String.class)))
                    .isEqualTo("String");
        }

        /// Verifies [FXMLGenericType] renders with generics.
        @Test
        void genericTypeRendersWithGenerics() {
            FXMLGenericType listOfString = new FXMLGenericType(List.class, List.of(FXMLType.of(String.class)));
            assertThat(classUnderTest.typeToSourceCode(context, listOfString))
                    .isEqualTo("List<String>");
        }

        /// Verifies [FXMLUncompiledClassType] renders the class name.
        @Test
        void uncompiledClassTypeRendersName() {
            assertThat(classUnderTest.typeToSourceCode(context, new FXMLUncompiledClassType("com.example.Foo")))
                    .isEqualTo("com.example.Foo");
        }

        /// Verifies [FXMLUncompiledGenericType] renders with generics.
        @Test
        void uncompiledGenericTypeRendersWithGenerics() {
            FXMLUncompiledGenericType type = new FXMLUncompiledGenericType(
                    "java.util.List", List.of(FXMLType.of(String.class)));
            assertThat(classUnderTest.typeToSourceCode(context, type))
                    .contains("String");
        }

        /// Verifies [FXMLWildcardType] renders as `?`.
        @Test
        void wildcardTypeRendersAsQuestionMark() {
            assertThat(classUnderTest.typeToSourceCode(context, FXMLWildcardType.INSTANCE))
                    .isEqualTo("?");
        }
    }

    // -------------------------------------------------------------------------
    // encodeFXMLValue
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#encodeFXMLValue].
    @Nested
    class EncodeFXMLValueTest {

        /// Verifies that an [FXMLLiteral] is encoded as a literal.
        @Test
        void literalIsEncoded() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLLiteral("42"), FXMLType.of(int.class)))
                    .isEqualTo("42");
        }

        /// Verifies that an [FXMLReference] returns the reference name.
        @Test
        void referenceReturnsName() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLReference("myRef"), FXMLType.of(String.class)))
                    .isEqualTo("myRef");
        }

        /// Verifies that an [FXMLMethod] returns a method reference.
        @Test
        void methodReturnsMethodReference() {
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            assertThat(classUnderTest.encodeFXMLValue(context, method, FXMLType.of(void.class)))
                    .isEqualTo("this::onClick");
        }

        /// Verifies that an [FXMLTranslation] returns a resource bundle lookup.
        @Test
        void translationReturnsResourceBundleLookup() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLTranslation("key"), FXMLType.of(String.class)))
                    .contains("getString")
                    .contains("\"key\"");
        }

        /// Verifies that an [FXMLResource] for String returns the encoded resource.
        @Test
        void resourceForStringReturnsEncodedResource() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(String.class)))
                    .isEqualTo("\"/img.png\"");
        }

        /// Verifies that an [FXMLResource] for URI returns the URI conversion call.
        @Test
        void resourceForUriReturnsUriConversion() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(URI.class)))
                    .contains("URI");
        }

        /// Verifies that an [FXMLResource] for URL returns the URL conversion call.
        @Test
        void resourceForUrlReturnsUrlConversion() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(URL.class)))
                    .contains("URL");
        }

        /// Verifies that an [FXMLResource] for Path returns the Path conversion call.
        @Test
        void resourceForPathReturnsPathConversion() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(Path.class)))
                    .contains("Path");
        }

        /// Verifies that an [FXMLResource] for File returns the File conversion call.
        @Test
        void resourceForFileReturnsFileConversion() {
            assertThat(classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(File.class)))
                    .contains("File");
        }

        /// Verifies that an [FXMLResource] for an unsupported type throws [IllegalArgumentException].
        @Test
        void resourceForUnsupportedTypeThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(
                    context,
                    new FXMLResource("/img.png"),
                    FXMLType.of(int.class)
            )).isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that an [FXMLResource] for an unsupported type throws [IllegalArgumentException].
        @Test
        void resourceForUnsupportedGenericTypeThrows() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(
                    context,
                    new FXMLResource("/img.png"),
                    FXMLType.of(List.class, List.of(FXMLType.of(Integer.class)))
            )).isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that an [FXMLExpression] throws [UnsupportedOperationException].
        @Test
        void expressionThrowsUnsupported() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(
                    context,
                    new FXMLExpression("x+1"),
                    FXMLType.of(int.class)
            ))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        /// Verifies that an [FXMLInlineScript] throws [UnsupportedOperationException].
        @Test
        void inlineScriptThrowsUnsupported() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(
                    context,
                    new FXMLInlineScript("x=1"),
                    FXMLType.of(void.class)
            ))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        /// Verifies that an [FXMLConstant] returns `ClassName.CONSTANT`.
        @Test
        void constantReturnsClassDotConstant() {
            FXMLConstant constant = new FXMLConstant(
                    new FXMLClassType(String.class),
                    "CASE_INSENSITIVE_ORDER",
                    FXMLType.of(java.util.Comparator.class)
            );
            assertThat(classUnderTest.encodeFXMLValue(context, constant, FXMLType.of(Object.class)))
                    .isEqualTo("String.CASE_INSENSITIVE_ORDER");
        }

        /// Verifies that an [FXMLObject] returns the identifier string.
        @Test
        void objectReturnsIdentifier() {
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("myObj"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            assertThat(classUnderTest.encodeFXMLValue(context, obj, FXMLType.of(String.class)))
                    .isEqualTo("myObj");
        }

        /// Verifies that an [FXMLObject] with internal identifier returns the internal identifier string.
        @Test
        void objectWithInternalIdentifierReturnsInternalId() {
            FXMLObject obj = new FXMLObject(
                    new FXMLInternalIdentifier(1),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            assertThat(classUnderTest.encodeFXMLValue(context, obj, FXMLType.of(String.class)))
                    .isEqualTo("$internalVariable$001");
        }

        /// Verifies that an [FXMLCopy] returns the identifier string.
        @Test
        void copyReturnsIdentifier() {
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), new FXMLExposedIdentifier("src"));
            assertThat(classUnderTest.encodeFXMLValue(context, copy, FXMLType.of(String.class)))
                    .isEqualTo("myCopy");
        }

        /// Verifies that an [FXMLInclude] returns the identifier string.
        @Test
        void includeReturnsIdentifier() {
            FXMLLazyLoadedDocument doc = new FXMLLazyLoadedDocument();
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("myInclude"),
                    "/sub.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    doc
            );
            assertThat(classUnderTest.encodeFXMLValue(context, include, FXMLType.of(Object.class)))
                    .isEqualTo("myInclude");
        }

        /// Verifies that an [FXMLValue] with identifier returns the identifier.
        @Test
        void fxmlValueWithIdentifierReturnsIdentifier() {
            FXMLValue value = new FXMLValue(
                    Optional.of(new FXMLExposedIdentifier("myVal")),
                    FXMLType.of(String.class),
                    "hello"
            );
            assertThat(classUnderTest.encodeFXMLValue(context, value, FXMLType.of(String.class)))
                    .isEqualTo("myVal");
        }

        /// Verifies that an [FXMLValue] without identifier encodes the literal.
        @Test
        void fxmlValueWithoutIdentifierEncodesLiteral() {
            FXMLValue value = new FXMLValue(Optional.empty(), FXMLType.of(String.class), "hello");
            assertThat(classUnderTest.encodeFXMLValue(context, value, FXMLType.of(String.class)))
                    .isEqualTo("\"hello\"");
        }

        /// Verifies that the overload with prefix prepends the prefix to exposed identifiers.
        @Test
        void overloadWithPrefixPrependsPrefix() {
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("myObj"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            assertThat(classUnderTest.encodeFXMLValue(context, "prefix_", obj, FXMLType.of(String.class)))
                    .isEqualTo("prefix_myObj");
        }
    }

    // -------------------------------------------------------------------------
    // createIdentifierToTypeMap
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#createIdentifierToTypeMap].
    @Nested
    class CreateIdentifierToTypeMapTest {

        /// Verifies that null document throws [NullPointerException].
        @Test
        void nullDocumentThrows() {
            assertThatThrownBy(() -> classUnderTest.createIdentifierToTypeMap(null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a document with a root object produces a type map entry.
        @Test
        void documentWithRootProducesEntry() {
            FXMLObject root = new FXMLObject(
                    new FXMLExposedIdentifier("root"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("root")
                    .containsValue(FXMLType.of(String.class));
        }

        /// Verifies that a copy in definitions resolves to the referenced type.
        @Test
        void copyInDefinitionsResolvesToReferencedType() {
            FXMLObject original = new FXMLObject(
                    new FXMLExposedIdentifier("orig"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), new FXMLExposedIdentifier("orig"));
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    original,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(copy),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myCopy")
                    .containsEntry("myCopy", FXMLType.of(String.class));
        }

        /// Verifies that an [FXMLCollection] in definitions produces a type map entry.
        @Test
        void collectionInDefinitionsProducesEntry() {
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLGenericType(java.util.ArrayList.class, List.of(FXMLType.of(String.class))),
                    Optional.empty(),
                    List.of()
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(collection),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myList");
        }

        /// Verifies that an [FXMLCollection] with a nested child also maps the child's identifier.
        @Test
        void collectionWithNestedChildMapsChildIdentifier() {
            FXMLObject child = new FXMLObject(
                    new FXMLExposedIdentifier("childObj"),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLGenericType(java.util.ArrayList.class, List.of(FXMLType.of(Integer.class))),
                    Optional.empty(),
                    List.of(child)
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(collection),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myList").containsKey("childObj");
        }

        /// Verifies that an [FXMLInclude] in definitions produces a type map entry using the document class name.
        @Test
        void includeInDefinitionsProducesEntry() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            FXMLObject subRoot = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument subDoc = new FXMLDocument(
                    "SubDoc",
                    subRoot,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            lazy.set(subDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("included"),
                    "/sub.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(include),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("included")
                    .hasEntrySatisfying(
                            "included", t -> assertThat(t)
                                    .isInstanceOf(FXMLUncompiledClassType.class)
                                    .hasFieldOrPropertyWithValue("name", "SubDoc")
                    );
        }

        /// Verifies that an [FXMLMap] in definitions produces a type map entry.
        @Test
        void mapInDefinitionsProducesEntry() {
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLGenericType(
                            java.util.HashMap.class,
                            List.of(FXMLType.of(String.class), FXMLType.of(String.class))
                    ),
                    FXMLType.of(String.class),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    Map.of()
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(fxmlMap),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myMap");
        }

        /// Verifies that an [FXMLMap] with a nested value entry also maps the nested identifier.
        @Test
        void mapWithNestedValueMapsNestedIdentifier() {
            FXMLObject nestedVal = new FXMLObject(
                    new FXMLExposedIdentifier("nestedVal"),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLGenericType(
                            java.util.HashMap.class,
                            List.of(FXMLType.of(String.class), FXMLType.of(Integer.class))
                    ),
                    FXMLType.of(String.class),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    Map.of(new FXMLLiteral("key"), nestedVal)
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(fxmlMap),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myMap").containsKey("nestedVal");
        }

        /// Verifies that an [FXMLValue] with an identifier in definitions produces a type map entry.
        @Test
        void fxmlValueWithIdentifierInDefinitionsProducesEntry() {
            FXMLValue value = new FXMLValue(
                    Optional.of(new FXMLExposedIdentifier("myVal")),
                    FXMLType.of(String.class),
                    "hello"
            );
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(value),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myVal")
                    .containsEntry("myVal", FXMLType.of(String.class));
        }

        /// Verifies that an [FXMLObject] with a nested object property also maps the nested identifier.
        @Test
        void objectWithNestedObjectPropertyMapsNestedIdentifier() {
            FXMLObject nested = new FXMLObject(
                    new FXMLExposedIdentifier("nestedObj"),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    List.of()
            );
            io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty prop =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty(
                            "value",
                            "setValue",
                            FXMLType.of(Integer.class),
                            nested
                    );
            FXMLObject root = new FXMLObject(
                    new FXMLExposedIdentifier("root"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of(prop)
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("root").containsKey("nestedObj");
        }

        /// Verifies that an [FXMLValue] without an identifier in definitions produces no entry.
        @Test
        void fxmlValueWithoutIdentifierInDefinitionsProducesNoEntry() {
            FXMLValue value = new FXMLValue(Optional.empty(), FXMLType.of(String.class), "hello");
            FXMLObject root = new FXMLObject(
                    new FXMLInternalIdentifier(0),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of()
            );
            FXMLDocument document = new FXMLDocument(
                    "MyDoc",
                    root,
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(value),
                    List.of()
            );
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).doesNotContainKey("myVal");
        }
    }

    // -------------------------------------------------------------------------
    // createIdentifierToTypeMapEntry (private reflection test)
    // -------------------------------------------------------------------------

    @Nested
    class CreateIdentifierToTypeMapEntryTest {

        /// Invokes the private `createIdentifierToTypeMapEntry` method.
        ///
        /// @param value The value.
        /// @return The result as a map.
        @SuppressWarnings("unchecked")
        private Map<String, TypeWrapper> invokeCreateIdentifierToTypeMapEntry(AbstractFXMLValue value) throws Exception {
            Method method = FXMLSourceCodeBuilderTypeHelper.class.getDeclaredMethod("createIdentifierToTypeMapEntry", AbstractFXMLValue.class);
            method.setAccessible(true);
            Stream<Map.Entry<String, TypeWrapper>> stream = (Stream<Map.Entry<String, TypeWrapper>>) method.invoke(classUnderTest, value);
            return stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        /// Verifies that [FXMLCollection] maps its identifier and children.
        @Test
        void fxmlCollectionMapsIdentifierAndChildren() throws Exception {
            FXMLObject child = new FXMLObject(new FXMLExposedIdentifier("child"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("list"),
                    new FXMLGenericType(List.class, List.of(FXMLType.of(Integer.class))),
                    Optional.empty(),
                    List.of(child)
            );
            Map<String, TypeWrapper> result = invokeCreateIdentifierToTypeMapEntry(collection);
            assertThat(result).hasSize(2)
                    .containsKeys("list", "child");
        }

        /// Verifies that [FXMLCopy] maps to [ReferenceWrapper].
        @Test
        void fxmlCopyMapsToReferenceWrapper() throws Exception {
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("copy"), new FXMLExposedIdentifier("orig"));
            Map<String, TypeWrapper> result = invokeCreateIdentifierToTypeMapEntry(copy);
            assertThat(result).hasSize(1)
                    .containsKey("copy")
                    .extractingByKey("copy")
                    .isInstanceOf(ReferenceWrapper.class);
        }

        /// Verifies that [FXMLInclude] maps to [FXMLUncompiledClassType] of its document.
        @Test
        void fxmlIncludeMapsToUncompiledClassType() throws Exception {
            FXMLLazyLoadedDocument lazyDoc = new FXMLLazyLoadedDocument();
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            lazyDoc.set(new FXMLDocument("Sub", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of()));
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("inc"),
                    "/inc.fxml",
                    StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazyDoc
            );
            Map<String, TypeWrapper> result = invokeCreateIdentifierToTypeMapEntry(include);
            assertThat(result).hasSize(1)
                    .containsKey("inc")
                    .extractingByKey("inc")
                    .isInstanceOf(FXMLTypeWrapper.class)
                    .extracting("type")
                    .isInstanceOf(FXMLUncompiledClassType.class)
                    .hasFieldOrPropertyWithValue("name", "Sub");
        }

        /// Verifies that [FXMLMap] maps its identifier and values.
        @Test
        void fxmlMapMapsIdentifierAndValues() throws Exception {
            FXMLObject val = new FXMLObject(new FXMLExposedIdentifier("val"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLExposedIdentifier("map"),
                    FXMLType.of(Map.class),
                    FXMLType.of(String.class),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    Map.of(new FXMLLiteral("k"), val)
            );
            Map<String, TypeWrapper> result = invokeCreateIdentifierToTypeMapEntry(fxmlMap);
            assertThat(result).hasSize(2)
                    .containsKeys("map", "val");
        }

        /// Verifies that [FXMLObject] maps its identifier and nested property objects.
        @Test
        void fxmlObjectMapsIdentifierAndNestedPropertyObjects() throws Exception {
            FXMLObject nested = new FXMLObject(new FXMLExposedIdentifier("nested"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty prop =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty("p", "s", FXMLType.of(Integer.class), nested);
            FXMLObject obj = new FXMLObject(
                    new FXMLExposedIdentifier("obj"),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    List.of(prop)
            );
            Map<String, TypeWrapper> result = invokeCreateIdentifierToTypeMapEntry(obj);
            assertThat(result).hasSize(2)
                    .containsKeys("obj", "nested");
        }

        /// Verifies that leaf types produce no entries.
        @Test
        void leafTypesProduceNoEntries() throws Exception {
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLConstant(new FXMLClassType(Color.class), "BLACK", FXMLType.of(Color.class)))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLExpression("expr"))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLInlineScript("code"))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLLiteral("lit"))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLMethod("name", List.of(), FXMLType.of(void.class)))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLReference("other"))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLResource("key"))).isEmpty();
            assertThat(invokeCreateIdentifierToTypeMapEntry(new FXMLTranslation("key"))).isEmpty();
        }

        /// Verifies that [FXMLValue] maps its identifier if present.
        @Test
        void fxmlValueMapsIdentifierIfPresent() throws Exception {
            FXMLValue withId = new FXMLValue(Optional.of(new FXMLExposedIdentifier("v")), FXMLType.of(String.class), "val");
            assertThat(invokeCreateIdentifierToTypeMapEntry(withId)).hasSize(1).containsKey("v");

            FXMLValue noId = new FXMLValue(Optional.empty(), FXMLType.of(String.class), "val");
            assertThat(invokeCreateIdentifierToTypeMapEntry(noId)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findMinimalConstructor
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#findMinimalConstructor].
    @Nested
    class FindMinimalConstructorTest {

        /// Verifies that null class throws [NullPointerException].
        @Test
        void nullClassThrows() {
            assertThatThrownBy(() -> classUnderTest.findMinimalConstructor(null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null properties throws [NullPointerException].
        @Test
        void nullPropertiesThrows() {
            assertThatThrownBy(() -> classUnderTest.findMinimalConstructor(NamedArgBean.class, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a matching constructor is found.
        @Test
        void matchingConstructorIsFound() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "text",
                    FXMLType.of(String.class),
                    new FXMLLiteral("hi")
            );
            FXMLConstructor result = classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop));
            assertThat(result).isNotNull()
                    .satisfies(c -> assertThat(c.properties()).hasSize(1));
        }

        /// Verifies that no matching constructor throws [IllegalArgumentException].
        @Test
        void noMatchingConstructorThrows() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "nonexistent",
                    FXMLType.of(String.class),
                    new FXMLLiteral("x")
            );
            assertThatThrownBy(() -> classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that the constructor cache is used on repeated calls.
        @Test
        void constructorCacheIsUsedOnRepeatedCalls() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "text",
                    FXMLType.of(String.class),
                    new FXMLLiteral("hi")
            );
            FXMLConstructor first = classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop));
            FXMLConstructor second = classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop));
            assertThat(first).isEqualTo(second);
        }
    }

    // -------------------------------------------------------------------------
    // findFactoryMethodConstructor
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#findFactoryMethodConstructor].
    @Nested
    class FindFactoryMethodConstructorTest {

        /// Verifies that null factory method throws [NullPointerException].
        @Test
        void nullFactoryMethodThrows() {
            assertThatThrownBy(() -> classUnderTest.findFactoryMethodConstructor(null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null properties throws [NullPointerException].
        @Test
        void nullPropertiesThrows() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            assertThatThrownBy(() -> classUnderTest.findFactoryMethodConstructor(fm, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a matching factory method is found.
        @Test
        void matchingFactoryMethodIsFound() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "value",
                    FXMLType.of(String.class),
                    new FXMLLiteral("x")
            );
            FXMLConstructor result = classUnderTest.findFactoryMethodConstructor(fm, List.of(prop));
            assertThat(result).isNotNull()
                    .satisfies(c -> assertThat(c.properties()).hasSize(1));
        }

        /// Verifies that no matching factory method throws [IllegalArgumentException].
        @Test
        void noMatchingFactoryMethodThrows() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "nonexistent",
                    FXMLType.of(String.class),
                    new FXMLLiteral("x")
            );
            assertThatThrownBy(() -> classUnderTest.findFactoryMethodConstructor(fm, List.of(prop)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that the factory method cache is used on repeated calls.
        @Test
        void factoryMethodCacheIsUsedOnRepeatedCalls() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            FXMLConstructorProperty prop = new FXMLConstructorProperty(
                    "value",
                    FXMLType.of(String.class),
                    new FXMLLiteral("x")
            );
            FXMLConstructor first = classUnderTest.findFactoryMethodConstructor(fm, List.of(prop));
            FXMLConstructor second = classUnderTest.findFactoryMethodConstructor(fm, List.of(prop));
            assertThat(first).isEqualTo(second);
        }
    }

    // -------------------------------------------------------------------------
    // renderMethod
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#renderMethod].
    @Nested
    class RenderMethodTest {

        /// Verifies that null context throws [NullPointerException].
        @Test
        void nullContextThrows() {
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            assertThatThrownBy(() -> classUnderTest.renderMethod(null, null, List.of(), method).toList())
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null method throws [NullPointerException].
        @Test
        void nullMethodThrows() {
            assertThatThrownBy(() -> classUnderTest.renderMethod(context, null, List.of(), null).toList())
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null interfaces throws [NullPointerException].
        @Test
        void nullInterfacesThrows() {
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            assertThatThrownBy(() -> classUnderTest.renderMethod(context, null, null, method).toList())
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a method with no controller generates an abstract method.
        @Test
        void noControllerGeneratesAbstractMethod() {
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, null, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("abstract").contains("onClick");
        }

        /// Verifies that a public controller method generates a direct call.
        @Test
        void publicControllerMethodGeneratesDirectCall() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PUBLIC,
                    "onClick",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("onClick").doesNotContain("abstract");
        }

        /// Verifies that a private controller method generates a reflection call.
        @Test
        void privateControllerMethodGeneratesReflectionCall() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PRIVATE,
                    "onClick",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("getDeclaredMethod").contains("onClick");
        }

        /// Verifies that a protected controller method in same package generates a direct call.
        @ParameterizedTest
        @EnumSource(value = Visibility.class, names = {"PROTECTED", "PACKAGE_PRIVATE"})
        void protectedControllerMethodInSamePackageGeneratesDirectCall(Visibility visibility) {
            SourceCodeGeneratorContext samePackageContext = new SourceCodeGeneratorContext(
                    new Imports(
                            List.of(), Map.of(
                            "io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilderTypeHelperTest.NamedArgBean",
                            "NamedArgBean"
                    )
                    ), "rb", Map.of(),
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer"
            );
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    visibility,
                    "onClick",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(samePackageContext, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("onClick").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a public controller method with two parameters generates comma-separated params in direct call.
        @Test
        void publicMethodWithTwoParamsGeneratesCommaSeparatedDirectCall() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PUBLIC, "onEvent", false,
                    FXMLType.of(void.class), List.of(FXMLType.of(String.class), FXMLType.of(Integer.class))
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod(
                    "onEvent",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(void.class)
            );
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("param0").contains(", param1").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a method with parameters generates correct parameter list.
        @Test
        void methodWithParametersGeneratesParameterList() {
            FXMLMethod method = new FXMLMethod("onEvent", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, null, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("param0");
        }

        /// Verifies that a method with two parameters generates a comma separator between them.
        @Test
        void methodWithTwoParametersGeneratesCommaSeparator() {
            FXMLMethod method = new FXMLMethod(
                    "onEvent",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(void.class)
            );
            String result = classUnderTest.renderMethod(context, null, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("param0").contains("param1").contains(", ");
        }

        /// Verifies that a method with non-void return type generates return statement.
        @Test
        void methodWithReturnTypeGeneratesReturnStatement() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PUBLIC,
                    "getValue",
                    false,
                    FXMLType.of(String.class),
                    List.of()
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod("getValue", List.of(), FXMLType.of(String.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("return");
        }

        /// Verifies that a private controller method with parameters and non-void return generates full reflection body.
        @Test
        void privateMethodWithParamsAndNonVoidReturnGeneratesFullReflectionBody() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PRIVATE, "compute", false,
                    FXMLType.of(String.class), List.of(FXMLType.of(String.class), FXMLType.of(Integer.class))
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            FXMLMethod method = new FXMLMethod(
                    "compute",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(String.class)
            );
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result)
                    .contains("getDeclaredMethod")
                    .contains("String.class")
                    .contains("Integer.class")
                    .contains("return (String)")
                    .contains(", param0")
                    .contains(", param1");
        }

        /// Verifies that a controller method with mismatched parameter types falls back to abstract.
        @Test
        void mismatchedParameterTypesFallsBackToAbstract() {
            FXMLControllerMethod cm = new FXMLControllerMethod(
                    Visibility.PUBLIC, "onEvent", false,
                    FXMLType.of(void.class), List.of(FXMLType.of(Integer.class))
            );
            FXMLController controller = new FXMLController(
                    new FXMLClassType(NamedArgBean.class),
                    List.of(),
                    List.of(cm)
            );
            // method expects String param but controller has Integer param — no match → abstract
            FXMLMethod method = new FXMLMethod("onEvent", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("abstract");
        }
    }

    // -------------------------------------------------------------------------
    // findMethodInController (private reflection test)
    // -------------------------------------------------------------------------

    @Nested
    class FindMethodInControllerTest {

        /// Invokes the private `findMethodInController` method.
        ///
        /// @param controller The controller.
        /// @param method     The method.
        /// @return The result.
        @SuppressWarnings("unchecked")
        private Optional<FXMLControllerMethod> invokeFindMethodInController(FXMLController controller, FXMLMethod method) throws Exception {
            Method m = FXMLSourceCodeBuilderTypeHelper.class.getDeclaredMethod("findMethodInController", FXMLController.class, FXMLMethod.class);
            m.setAccessible(true);
            return (Optional<FXMLControllerMethod>) m.invoke(classUnderTest, controller, method);
        }

        /// Verifies that null controller returns empty [Optional].
        @Test
        void nullControllerReturnsEmpty() throws Exception {
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            assertThat(invokeFindMethodInController(null, method)).isEmpty();
        }

        /// Verifies that a match is found by name, params, and types.
        @Test
        void matchFoundByNameParamsAndTypes() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(void.class), List.of(FXMLType.of(String.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));

            assertThat(invokeFindMethodInController(controller, method))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "onClick");
        }

        /// Verifies that no match is found for wrong name.
        @Test
        void noMatchForWrongName() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(void.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onPress", List.of(), FXMLType.of(void.class));

            assertThat(invokeFindMethodInController(controller, method)).isEmpty();
        }

        /// Verifies that no match is found for wrong parameter count.
        @Test
        void noMatchForWrongParameterCount() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(void.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));

            assertThat(invokeFindMethodInController(controller, method)).isEmpty();
        }

        /// Verifies that no match is found for incompatible return type.
        @Test
        void noMatchForIncompatibleReturnType() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(Integer.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(String.class));

            assertThat(invokeFindMethodInController(controller, method)).isEmpty();
        }

        /// Verifies that no match is found for incompatible parameter type.
        @Test
        void noMatchForIncompatibleParameterType() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(void.class), List.of(FXMLType.of(Integer.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));

            assertThat(invokeFindMethodInController(controller, method)).isEmpty();
        }

        /// Verifies that covariant return type matches.
        @Test
        void covariantReturnTypeMatches() throws Exception {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "getData", false, FXMLType.of(Object.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("getData", List.of(), FXMLType.of(String.class));

            assertThat(invokeFindMethodInController(controller, method)).isPresent();
        }

        /// Verifies that contravariant parameter type matches.
        @Test
        void contravariantParameterTypeMatches() throws Exception {
            // Controller method expects String
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "consume", false, FXMLType.of(void.class), List.of(FXMLType.of(String.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(Object.class), List.of(), List.of(cm));
            // FXML call provides Object - this should NOT match because String is NOT assignable from Object
            // Wait, check the logic: parameterTypes.get(i).isAssignableFrom(FXMLUtils.findRawType(fxmlTypes.get(i)))
            // where parameterTypes.get(i) is from FXMLMethod (Object) and fxmlTypes.get(i) is from FXMLControllerMethod (String).
            // Object.isAssignableFrom(String) is TRUE.
            // So contravariant (providing more specific to broader) is TRUE.
            FXMLMethod method = new FXMLMethod("consume", List.of(FXMLType.of(Object.class)), FXMLType.of(void.class));

            assertThat(invokeFindMethodInController(controller, method)).isPresent();
        }
    }

    // -------------------------------------------------------------------------
    // renderControllerFieldMapping
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#renderControllerFieldMapping].
    @Nested
    class RenderControllerFieldMappingTest {

        /// Verifies that null context throws [NullPointerException].
        @Test
        void nullContextThrows() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC,
                    "myField",
                    FXMLType.of(String.class)
            );
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(null, controller, field, "id"))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null controller throws [NullPointerException].
        @Test
        void nullControllerThrows() {
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC,
                    "myField",
                    FXMLType.of(String.class)
            );
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(context, null, field, "id"))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null field throws [NullPointerException].
        @Test
        void nullFieldThrows() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(context, controller, null, "id"))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null identifier throws [NullPointerException].
        @Test
        void nullIdentifierThrows() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC,
                    "myField",
                    FXMLType.of(String.class)
            );
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(context, controller, field, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a public field generates a direct assignment.
        @Test
        void publicFieldGeneratesDirectAssignment() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PUBLIC,
                    "myField",
                    FXMLType.of(String.class)
            );
            String result = classUnderTest.renderControllerFieldMapping(context, controller, field, "myId");
            assertThat(result).contains("myField").contains("myId").doesNotContain("getDeclaredField");
        }

        /// Verifies that a private field generates a reflection assignment.
        @Test
        void privateFieldGeneratesReflectionAssignment() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PRIVATE,
                    "myField",
                    FXMLType.of(String.class)
            );
            String result = classUnderTest.renderControllerFieldMapping(context, controller, field, "myId");
            assertThat(result).contains("getDeclaredField").contains("myField");
        }

        /// Verifies that a protected field in a different package generates a reflection assignment.
        @Test
        void protectedFieldInDifferentPackageGeneratesReflection() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PROTECTED,
                    "myField",
                    FXMLType.of(String.class)
            );
            String result = classUnderTest.renderControllerFieldMapping(context, controller, field, "myId");
            assertThat(result).contains("getDeclaredField");
        }

        /// Verifies that a package-private field in same package generates a direct assignment.
        @Test
        void packagePrivateFieldInSamePackageGeneratesDirectAssignment() {
            // NamedArgBean is in "io.github.bsels.javafx.maven.plugin.fxml.v2.writer" package
            SourceCodeGeneratorContext samePackageContext = makeContext(Map.of(
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilderTypeHelperTest.NamedArgBean",
                    "NamedArgBean"
            ));
            // Override package to match NamedArgBean's package
            Imports imports = new Imports(List.of(), Map.of());
            SourceCodeGeneratorContext pkgContext = new SourceCodeGeneratorContext(
                    imports, "rb", Map.of(),
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer"
            );
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(
                    Visibility.PACKAGE_PRIVATE,
                    "myField",
                    FXMLType.of(String.class)
            );
            String result = classUnderTest.renderControllerFieldMapping(pkgContext, controller, field, "myId");
            assertThat(result).contains("myField").doesNotContain("getDeclaredField");
        }
    }

    // -------------------------------------------------------------------------
    // renderControllerInitialization
    // -------------------------------------------------------------------------

    /// Tests for [FXMLSourceCodeBuilderTypeHelper#renderControllerInitialization].
    @Nested
    class RenderControllerInitializationTest {

        /// Verifies that null context throws [NullPointerException].
        @Test
        void nullContextThrows() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PUBLIC,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(null, new FXMLController(ct, List.of(), List.of()), m))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null controller class throws [NullPointerException].
        @Test
        void nullControllerClassThrows() {
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PUBLIC,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(context, null, m))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null initialize method throws [NullPointerException].
        @Test
        void nullInitializeMethodThrows() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a public initialize method generates a direct call.
        @Test
        void publicInitializeMethodGeneratesDirectCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PUBLIC,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            String result = classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a public initialize method with 2 parameters adds resource bundle.
        @Test
        void publicInitializeMethodWithTwoParamsAddsResourceBundle() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PUBLIC, "initialize", false, FXMLType.of(void.class),
                    List.of(FXMLType.of(URL.class), FXMLType.of(java.util.ResourceBundle.class))
            );
            String result = classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("initialize").contains("null");
        }

        /// Verifies that a private initialize method generates a reflection call.
        @Test
        void privateInitializeMethodGeneratesReflectionCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PRIVATE,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            String result = classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("getDeclaredMethod").contains("initialize");
        }

        /// Verifies that a private initialize method with 2 parameters adds resource bundle in reflection call.
        @Test
        void privateInitializeMethodWithTwoParamsAddsResourceBundleInReflection() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PRIVATE, "initialize", false, FXMLType.of(void.class),
                    List.of(FXMLType.of(URL.class), FXMLType.of(java.util.ResourceBundle.class))
            );
            String result = classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("getDeclaredMethod").contains("ResourceBundle");
        }

        /// Verifies that a protected initialize method in same package generates a direct call.
        @Test
        void protectedInitializeMethodInSamePackageGeneratesDirectCall() {
            Imports imports = new Imports(List.of(), Map.of());
            SourceCodeGeneratorContext pkgContext = new SourceCodeGeneratorContext(
                    imports, "rb", Map.of(),
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer"
            );
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PROTECTED,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            String result = classUnderTest.renderControllerInitialization(pkgContext, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a protected initialize method in different package generates a reflection call.
        @Test
        void protectedInitializeMethodInDifferentPackageGeneratesReflectionCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PROTECTED,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            String result = classUnderTest.renderControllerInitialization(context, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("getDeclaredMethod");
        }

        /// Verifies that a package-private initialize method in same package generates a direct call.
        @Test
        void packagePrivateInitializeMethodInSamePackageGeneratesDirectCall() {
            Imports imports = new Imports(List.of(), Map.of());
            SourceCodeGeneratorContext pkgContext = new SourceCodeGeneratorContext(
                    imports, "rb", Map.of(),
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer"
            );
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(
                    Visibility.PACKAGE_PRIVATE,
                    "initialize",
                    false,
                    FXMLType.of(void.class),
                    List.of()
            );
            String result = classUnderTest.renderControllerInitialization(pkgContext, new FXMLController(ct, List.of(), List.of()), m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }
    }
}
