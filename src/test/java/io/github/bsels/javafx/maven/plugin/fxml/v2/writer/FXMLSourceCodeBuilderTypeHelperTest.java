package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
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
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import java.util.ArrayList;
import javafx.beans.NamedArg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for [FXMLSourceCodeBuilderTypeHelper].
class FXMLSourceCodeBuilderTypeHelperTest {

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
        /// Creates a new [FactoryBean] via factory method.
        ///
        /// @param value The value.
        /// @return A new [FactoryBean].
        public static FactoryBean create(@NamedArg("value") String value) {
            return new FactoryBean();
        }

        private FactoryBean() {
        }
    }

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

        /// Verifies byte, short, int, long literal encoding.
        @Test
        void integerTypeLiterals() {
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(byte.class))).isEqualTo("(byte) 5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(Byte.class))).isEqualTo("(byte) 5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(short.class))).isEqualTo("(short) 5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(Short.class))).isEqualTo("(short) 5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(int.class))).isEqualTo("5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(Integer.class))).isEqualTo("5");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(long.class))).isEqualTo("5L");
            assertThat(classUnderTest.encodeLiteral(context, "5", FXMLType.of(Long.class))).isEqualTo("5L");
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
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(context, new FXMLResource("/img.png"), FXMLType.of(int.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that an [FXMLExpression] throws [UnsupportedOperationException].
        @Test
        void expressionThrowsUnsupported() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(context, new FXMLExpression("x+1"), FXMLType.of(int.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        /// Verifies that an [FXMLInlineScript] throws [UnsupportedOperationException].
        @Test
        void inlineScriptThrowsUnsupported() {
            assertThatThrownBy(() -> classUnderTest.encodeFXMLValue(context, new FXMLInlineScript("x=1"), FXMLType.of(void.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        /// Verifies that an [FXMLConstant] returns `ClassName.CONSTANT`.
        @Test
        void constantReturnsClassDotConstant() {
            FXMLConstant constant = new FXMLConstant(new FXMLClassType(String.class), "CASE_INSENSITIVE_ORDER", FXMLType.of(java.util.Comparator.class));
            assertThat(classUnderTest.encodeFXMLValue(context, constant, FXMLType.of(Object.class)))
                    .isEqualTo("String.CASE_INSENSITIVE_ORDER");
        }

        /// Verifies that an [FXMLObject] returns the identifier string.
        @Test
        void objectReturnsIdentifier() {
            FXMLObject obj = new FXMLObject(new FXMLExposedIdentifier("myObj"), FXMLType.of(String.class), Optional.empty(), List.of());
            assertThat(classUnderTest.encodeFXMLValue(context, obj, FXMLType.of(String.class)))
                    .isEqualTo("myObj");
        }

        /// Verifies that an [FXMLObject] with internal identifier returns the internal identifier string.
        @Test
        void objectWithInternalIdentifierReturnsInternalId() {
            FXMLObject obj = new FXMLObject(new FXMLInternalIdentifier(1), FXMLType.of(String.class), Optional.empty(), List.of());
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
            FXMLInclude include = new FXMLInclude(new FXMLExposedIdentifier("myInclude"), "/sub.fxml", StandardCharsets.UTF_8, Optional.empty(), doc);
            assertThat(classUnderTest.encodeFXMLValue(context, include, FXMLType.of(Object.class)))
                    .isEqualTo("myInclude");
        }

        /// Verifies that an [FXMLValue] with identifier returns the identifier.
        @Test
        void fxmlValueWithIdentifierReturnsIdentifier() {
            FXMLValue value = new FXMLValue(Optional.of(new FXMLExposedIdentifier("myVal")), FXMLType.of(String.class), "hello");
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
            FXMLObject obj = new FXMLObject(new FXMLExposedIdentifier("myObj"), FXMLType.of(String.class), Optional.empty(), List.of());
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
            FXMLObject root = new FXMLObject(new FXMLExposedIdentifier("root"), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("root")
                    .containsValue(FXMLType.of(String.class));
        }

        /// Verifies that a copy in definitions resolves to the referenced type.
        @Test
        void copyInDefinitionsResolvesToReferencedType() {
            FXMLObject original = new FXMLObject(new FXMLExposedIdentifier("orig"), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLCopy copy = new FXMLCopy(new FXMLExposedIdentifier("myCopy"), new FXMLExposedIdentifier("orig"));
            FXMLDocument document = new FXMLDocument("MyDoc", original, List.of(), Optional.empty(), Optional.empty(), List.of(copy), List.of());
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
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(collection), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myList");
        }

        /// Verifies that an [FXMLCollection] with a nested child also maps the child's identifier.
        @Test
        void collectionWithNestedChildMapsChildIdentifier() {
            FXMLObject child = new FXMLObject(new FXMLExposedIdentifier("childObj"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            FXMLCollection collection = new FXMLCollection(
                    new FXMLExposedIdentifier("myList"),
                    new FXMLGenericType(java.util.ArrayList.class, List.of(FXMLType.of(Integer.class))),
                    Optional.empty(),
                    List.of(child)
            );
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(collection), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myList").containsKey("childObj");
        }

        /// Verifies that an [FXMLInclude] in definitions produces a type map entry using the document class name.
        @Test
        void includeInDefinitionsProducesEntry() {
            FXMLLazyLoadedDocument lazy = new FXMLLazyLoadedDocument();
            FXMLObject subRoot = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument subDoc = new FXMLDocument("SubDoc", subRoot, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());
            lazy.set(subDoc);
            FXMLInclude include = new FXMLInclude(
                    new FXMLExposedIdentifier("included"),
                    "/sub.fxml",
                    java.nio.charset.StandardCharsets.UTF_8,
                    Optional.empty(),
                    lazy
            );
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(include), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("included")
                    .hasEntrySatisfying("included", t -> assertThat(t)
                            .isInstanceOf(FXMLUncompiledClassType.class)
                            .hasFieldOrPropertyWithValue("name", "SubDoc"));
        }

        /// Verifies that an [FXMLMap] in definitions produces a type map entry.
        @Test
        void mapInDefinitionsProducesEntry() {
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLGenericType(java.util.HashMap.class, List.of(FXMLType.of(String.class), FXMLType.of(String.class))),
                    FXMLType.of(String.class),
                    FXMLType.of(String.class),
                    Optional.empty(),
                    Map.of()
            );
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(fxmlMap), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myMap");
        }

        /// Verifies that an [FXMLMap] with a nested value entry also maps the nested identifier.
        @Test
        void mapWithNestedValueMapsNestedIdentifier() {
            FXMLObject nestedVal = new FXMLObject(new FXMLExposedIdentifier("nestedVal"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            FXMLMap fxmlMap = new FXMLMap(
                    new FXMLExposedIdentifier("myMap"),
                    new FXMLGenericType(java.util.HashMap.class, List.of(FXMLType.of(String.class), FXMLType.of(Integer.class))),
                    FXMLType.of(String.class),
                    FXMLType.of(Integer.class),
                    Optional.empty(),
                    Map.of(new FXMLLiteral("key"), nestedVal)
            );
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(fxmlMap), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myMap").containsKey("nestedVal");
        }

        /// Verifies that an [FXMLValue] with an identifier in definitions produces a type map entry.
        @Test
        void fxmlValueWithIdentifierInDefinitionsProducesEntry() {
            FXMLValue value = new FXMLValue(Optional.of(new FXMLExposedIdentifier("myVal")), FXMLType.of(String.class), "hello");
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(value), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("myVal")
                    .containsEntry("myVal", FXMLType.of(String.class));
        }

        /// Verifies that an [FXMLObject] with a nested object property also maps the nested identifier.
        @Test
        void objectWithNestedObjectPropertyMapsNestedIdentifier() {
            FXMLObject nested = new FXMLObject(new FXMLExposedIdentifier("nestedObj"), FXMLType.of(Integer.class), Optional.empty(), List.of());
            io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty prop =
                    new io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty("value", "setValue", FXMLType.of(Integer.class), nested);
            FXMLObject root = new FXMLObject(new FXMLExposedIdentifier("root"), FXMLType.of(String.class), Optional.empty(), List.of(prop));
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).containsKey("root").containsKey("nestedObj");
        }

        /// Verifies that an [FXMLValue] without an identifier in definitions produces no entry.
        @Test
        void fxmlValueWithoutIdentifierInDefinitionsProducesNoEntry() {
            FXMLValue value = new FXMLValue(Optional.empty(), FXMLType.of(String.class), "hello");
            FXMLObject root = new FXMLObject(new FXMLInternalIdentifier(0), FXMLType.of(String.class), Optional.empty(), List.of());
            FXMLDocument document = new FXMLDocument("MyDoc", root, List.of(), Optional.empty(), Optional.empty(), List.of(value), List.of());
            Map<String, FXMLType> map = classUnderTest.createIdentifierToTypeMap(document);
            assertThat(map).doesNotContainKey("myVal");
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
            FXMLConstructorProperty prop = new FXMLConstructorProperty("text", FXMLType.of(String.class), new FXMLLiteral("hi"));
            FXMLConstructor result = classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop));
            assertThat(result).isNotNull()
                    .satisfies(c -> assertThat(c.properties()).hasSize(1));
        }

        /// Verifies that no matching constructor throws [IllegalArgumentException].
        @Test
        void noMatchingConstructorThrows() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty("nonexistent", FXMLType.of(String.class), new FXMLLiteral("x"));
            assertThatThrownBy(() -> classUnderTest.findMinimalConstructor(NamedArgBean.class, List.of(prop)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that the constructor cache is used on repeated calls.
        @Test
        void constructorCacheIsUsedOnRepeatedCalls() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty("text", FXMLType.of(String.class), new FXMLLiteral("hi"));
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
            FXMLConstructorProperty prop = new FXMLConstructorProperty("value", FXMLType.of(String.class), new FXMLLiteral("x"));
            FXMLConstructor result = classUnderTest.findFactoryMethodConstructor(fm, List.of(prop));
            assertThat(result).isNotNull()
                    .satisfies(c -> assertThat(c.properties()).hasSize(1));
        }

        /// Verifies that no matching factory method throws [IllegalArgumentException].
        @Test
        void noMatchingFactoryMethodThrows() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            FXMLConstructorProperty prop = new FXMLConstructorProperty("nonexistent", FXMLType.of(String.class), new FXMLLiteral("x"));
            assertThatThrownBy(() -> classUnderTest.findFactoryMethodConstructor(fm, List.of(prop)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that the factory method cache is used on repeated calls.
        @Test
        void factoryMethodCacheIsUsedOnRepeatedCalls() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(FactoryBean.class), "create");
            FXMLConstructorProperty prop = new FXMLConstructorProperty("value", FXMLType.of(String.class), new FXMLLiteral("x"));
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
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onClick", false, FXMLType.of(void.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("onClick").doesNotContain("abstract");
        }

        /// Verifies that a private controller method generates a reflection call.
        @Test
        void privateControllerMethodGeneratesReflectionCall() {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PRIVATE, "onClick", false, FXMLType.of(void.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("getDeclaredMethod").contains("onClick");
        }

        /// Verifies that a protected controller method in same package generates a direct call.
        @Test
        void protectedControllerMethodInSamePackageGeneratesDirectCall() {
            SourceCodeGeneratorContext samePackageContext = new SourceCodeGeneratorContext(
                    new Imports(List.of(), Map.of(
                            "io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilderTypeHelperTest.NamedArgBean",
                            "NamedArgBean"
                    )), "rb", Map.of(),
                    "io.github.bsels.javafx.maven.plugin.fxml.v2.writer"
            );
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PROTECTED, "onClick", false, FXMLType.of(void.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onClick", List.of(), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(samePackageContext, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("onClick").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a public controller method with two parameters generates comma-separated params in direct call.
        @Test
        void publicMethodWithTwoParamsGeneratesCommaSeparatedDirectCall() {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onEvent", false,
                    FXMLType.of(void.class), List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("onEvent",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(void.class));
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
            FXMLMethod method = new FXMLMethod("onEvent",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, null, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("param0").contains("param1").contains(", ");
        }

        /// Verifies that a method with non-void return type generates return statement.
        @Test
        void methodWithReturnTypeGeneratesReturnStatement() {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "getValue", false, FXMLType.of(String.class), List.of());
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("getValue", List.of(), FXMLType.of(String.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("return");
        }

        /// Verifies that a private controller method with parameters and non-void return generates full reflection body.
        @Test
        void privateMethodWithParamsAndNonVoidReturnGeneratesFullReflectionBody() {
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PRIVATE, "compute", false,
                    FXMLType.of(String.class), List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            FXMLMethod method = new FXMLMethod("compute",
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)),
                    FXMLType.of(String.class));
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
            FXMLControllerMethod cm = new FXMLControllerMethod(Visibility.PUBLIC, "onEvent", false,
                    FXMLType.of(void.class), List.of(FXMLType.of(Integer.class)));
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of(cm));
            // method expects String param but controller has Integer param — no match → abstract
            FXMLMethod method = new FXMLMethod("onEvent", List.of(FXMLType.of(String.class)), FXMLType.of(void.class));
            String result = classUnderTest.renderMethod(context, controller, List.of(), method)
                    .reduce("", String::concat);
            assertThat(result).contains("abstract");
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
            FXMLControllerField field = new FXMLControllerField(Visibility.PUBLIC, "myField", FXMLType.of(String.class));
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(null, controller, field, "id"))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null controller throws [NullPointerException].
        @Test
        void nullControllerThrows() {
            FXMLControllerField field = new FXMLControllerField(Visibility.PUBLIC, "myField", FXMLType.of(String.class));
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
            FXMLControllerField field = new FXMLControllerField(Visibility.PUBLIC, "myField", FXMLType.of(String.class));
            assertThatThrownBy(() -> classUnderTest.renderControllerFieldMapping(context, controller, field, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a public field generates a direct assignment.
        @Test
        void publicFieldGeneratesDirectAssignment() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(Visibility.PUBLIC, "myField", FXMLType.of(String.class));
            String result = classUnderTest.renderControllerFieldMapping(context, controller, field, "myId");
            assertThat(result).contains("myField").contains("myId").doesNotContain("getDeclaredField");
        }

        /// Verifies that a private field generates a reflection assignment.
        @Test
        void privateFieldGeneratesReflectionAssignment() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(Visibility.PRIVATE, "myField", FXMLType.of(String.class));
            String result = classUnderTest.renderControllerFieldMapping(context, controller, field, "myId");
            assertThat(result).contains("getDeclaredField").contains("myField");
        }

        /// Verifies that a protected field in a different package generates a reflection assignment.
        @Test
        void protectedFieldInDifferentPackageGeneratesReflection() {
            FXMLController controller = new FXMLController(new FXMLClassType(NamedArgBean.class), List.of(), List.of());
            FXMLControllerField field = new FXMLControllerField(Visibility.PROTECTED, "myField", FXMLType.of(String.class));
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
            FXMLControllerField field = new FXMLControllerField(Visibility.PACKAGE_PRIVATE, "myField", FXMLType.of(String.class));
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
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PUBLIC, "initialize", false, FXMLType.of(void.class), List.of());
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(null, ct, m))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null controller class throws [NullPointerException].
        @Test
        void nullControllerClassThrows() {
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PUBLIC, "initialize", false, FXMLType.of(void.class), List.of());
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(context, null, m))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that null initialize method throws [NullPointerException].
        @Test
        void nullInitializeMethodThrows() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            assertThatThrownBy(() -> classUnderTest.renderControllerInitialization(context, ct, null))
                    .isInstanceOf(NullPointerException.class);
        }

        /// Verifies that a public initialize method generates a direct call.
        @Test
        void publicInitializeMethodGeneratesDirectCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PUBLIC, "initialize", false, FXMLType.of(void.class), List.of());
            String result = classUnderTest.renderControllerInitialization(context, ct, m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a public initialize method with 2 parameters adds resource bundle.
        @Test
        void publicInitializeMethodWithTwoParamsAddsResourceBundle() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PUBLIC, "initialize", false, FXMLType.of(void.class),
                    List.of(FXMLType.of(URL.class), FXMLType.of(java.util.ResourceBundle.class)));
            String result = classUnderTest.renderControllerInitialization(context, ct, m);
            assertThat(result).contains("initialize").contains("null");
        }

        /// Verifies that a private initialize method generates a reflection call.
        @Test
        void privateInitializeMethodGeneratesReflectionCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PRIVATE, "initialize", false, FXMLType.of(void.class), List.of());
            String result = classUnderTest.renderControllerInitialization(context, ct, m);
            assertThat(result).contains("getDeclaredMethod").contains("initialize");
        }

        /// Verifies that a private initialize method with 2 parameters adds resource bundle in reflection call.
        @Test
        void privateInitializeMethodWithTwoParamsAddsResourceBundleInReflection() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PRIVATE, "initialize", false, FXMLType.of(void.class),
                    List.of(FXMLType.of(URL.class), FXMLType.of(java.util.ResourceBundle.class)));
            String result = classUnderTest.renderControllerInitialization(context, ct, m);
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
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PROTECTED, "initialize", false, FXMLType.of(void.class), List.of());
            String result = classUnderTest.renderControllerInitialization(pkgContext, ct, m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }

        /// Verifies that a protected initialize method in different package generates a reflection call.
        @Test
        void protectedInitializeMethodInDifferentPackageGeneratesReflectionCall() {
            FXMLClassType ct = new FXMLClassType(NamedArgBean.class);
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PROTECTED, "initialize", false, FXMLType.of(void.class), List.of());
            String result = classUnderTest.renderControllerInitialization(context, ct, m);
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
            FXMLControllerMethod m = new FXMLControllerMethod(Visibility.PACKAGE_PRIVATE, "initialize", false, FXMLType.of(void.class), List.of());
            String result = classUnderTest.renderControllerInitialization(pkgContext, ct, m);
            assertThat(result).contains("initialize").doesNotContain("getDeclaredMethod");
        }
    }
}
