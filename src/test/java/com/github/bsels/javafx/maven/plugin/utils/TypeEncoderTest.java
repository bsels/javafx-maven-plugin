package com.github.bsels.javafx.maven.plugin.utils;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Unit tests for the TypeEncoder class.
/// These tests ensure that the encodeTypeValue method behaves as expected for various scenarios.
public class TypeEncoderTest {

    // Dummy classes/enums for testing purpose
    private enum DummyEnum {
        VALUE
    }

    @Nested
    class TypeToTypeStringTest {

        @Test
        public void testTypeToTypeStringWithClassType() {
            // Given
            Type type = String.class;

            // When
            String result = TypeEncoder.typeToTypeString(type);

            // Then
            assertThat(result).isEqualTo("String");
        }

        @Test
        public void testTypeToTypeStringWithTypeVariable() {
            // Given
            TypeVariable<?> typeVariable = new TypeVariable<>() {
                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                    return null;
                }

                @Override
                public Annotation[] getAnnotations() {
                    return new Annotation[0];
                }

                @Override
                public Annotation[] getDeclaredAnnotations() {
                    return new Annotation[0];
                }

                @Override
                public Type[] getBounds() {
                    return new Type[0];
                }

                @Override
                public GenericDeclaration getGenericDeclaration() {
                    return null;
                }

                @Override
                public String getName() {
                    return "dummyType";
                }

                @Override
                public AnnotatedType[] getAnnotatedBounds() {
                    return new AnnotatedType[0];
                }
            };

            // When
            String result = TypeEncoder.typeToTypeString(typeVariable);

            // Then
            assertThat(result).isEqualTo(typeVariable.getName());
        }

        @Test
        public void testTypeToTypeStringWithUnsupportedTypeThrowsException() {
            // Given
            Type unsupportedType = new Type() {
            };

            // When & Then
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> TypeEncoder.typeToTypeString(unsupportedType))
                    .withMessage("Unsupported type: %s".formatted(unsupportedType));
        }
    }

    @Nested
    class EncodingTypeTest {

        @Test
        public void testEncodeTypeValueWithPrefixDollar() {
            // Given
            String value = "$testValue";

            // When
            String result = TypeEncoder.encodeTypeValue(String.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("testValue");
        }

        @Test
        public void testEncodeTypeValueWithInterfaceAndHashPrefix() {
            // Given
            String value = "#testMethod";

            // When
            String result = TypeEncoder.encodeTypeValue(Runnable.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("this::testMethod");
        }

        @Test
        public void testEncodeTypeValueWithCharSequenceAndPercentPrefix() {
            // Given
            String value = "%key";

            // When
            String result = TypeEncoder.encodeTypeValue(String.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("RESOURCE_BUNDLE.getString(\"key\")");
        }

        @Test
        public void testEncodeTypeValueWithCharSequence() {
            // Given
            String value = "simpleText";

            // When
            String result = TypeEncoder.encodeTypeValue(String.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("\"simpleText\"");
        }

        @ParameterizedTest
        @ValueSource(classes = {char.class, Character.class})
        public void testEncodeTypeValueWithCharacterClass(Class<?> clazz) {
            // Given
            String value = "a";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("'a'");
        }

        @Test
        public void testEncodeTypeValueWithEnumType() {
            // Given
            String value = "VALUE";

            // When
            String result = TypeEncoder.encodeTypeValue(DummyEnum.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("DummyEnum.VALUE");
        }

        @ParameterizedTest
        @ValueSource(classes = {double.class, Double.class})
        public void testEncodeTypeValueWithDoubleInfinity(Class<?> clazz) {
            // Given
            String value = "Infinity";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("Double.POSITIVE_INFINITY");
        }

        @ParameterizedTest
        @ValueSource(classes = {double.class, Double.class})
        public void testEncodeTypeValueWithNegativeDoubleInfinity(Class<?> clazz) {
            // Given
            String value = "-Infinity";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("Double.NEGATIVE_INFINITY");
        }

        @ParameterizedTest
        @ValueSource(classes = {double.class, Double.class})
        public void testEncodeTypeValueWithDoubleValue(Class<?> clazz) {
            // Given
            String value = "123.456";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("123.456");
        }

        @ParameterizedTest
        @ValueSource(classes = {int.class, Integer.class})
        public void testEncodeTypeValueWithIntValue(Class<?> clazz) {
            // Given
            String value = "42";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("42");
        }

        @ParameterizedTest
        @ValueSource(classes = {boolean.class, Boolean.class})
        public void testEncodeTypeValueWithBooleanValue(Class<?> clazz) {
            // Given
            String value = "true";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("true");
        }

        @ParameterizedTest
        @ValueSource(classes = {float.class, Float.class})
        public void testEncodeTypeValueWithPositiveFloatInfinity(Class<?> clazz) {
            // Given
            String value = "Infinity";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("Float.POSITIVE_INFINITY");
        }

        @ParameterizedTest
        @ValueSource(classes = {float.class, Float.class})
        public void testEncodeTypeValueWithNegativeFloatInfinity(Class<?> clazz) {
            // Given
            String value = "-Infinity";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("Float.NEGATIVE_INFINITY");
        }

        @ParameterizedTest
        @ValueSource(classes = {float.class, Float.class})
        public void testEncodeTypeValueWithFloatValue(Class<?> clazz) {
            // Given
            String value = "3.14";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("3.14f");
        }

        @Test
        public void testEncodeTypeValueWithNewInstance() {
            // Given
            String value = "example";

            // When
            String result = TypeEncoder.encodeTypeValue(DummyClass.class, value);

            // Then
            assertThat(result)
                    .isEqualTo("new DummyClass(\"example\")");
        }

        @Test
        public void testEncodeTypeValueWithParameterizedType() {
            // Given
            Type type = new ParameterizedType() {
                public Type[] getActualTypeArguments() {
                    return new Type[0];
                }

                public Type getRawType() {
                    return DummyClass.class;
                }

                public Type getOwnerType() {
                    return null;
                }
            };
            String value = "example";

            // When
            String result = TypeEncoder.encodeTypeValue(type, value);

            // Then
            assertThat(result)
                    .isEqualTo("new DummyClass<>(example)");
        }

        @Test
        public void testEncodeTypeValueThrowsExceptionForUnsupportedType() {
            // Given
            String value = "test";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    TypeEncoder.encodeTypeValue(String[].class, value));
            assertTrue(exception.getMessage().contains("Unable to encode type value"));
        }

        @ParameterizedTest
        @ValueSource(classes = {int.class, Integer.class})
        public void testEncodeTypeValueWithIntegerValue(Class<?> clazz) {
            // Given
            String value = "123";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("123");
        }

        @ParameterizedTest
        @ValueSource(classes = {long.class, Long.class})
        public void testEncodeTypeValueWithLongValue(Class<?> clazz) {
            // Given
            String value = "123";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("123L");
        }

        @ParameterizedTest
        @ValueSource(classes = {short.class, Short.class})
        public void testEncodeTypeValueWithShortValue(Class<?> clazz) {
            // Given
            String value = "32000";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("(short) 32000");
        }

        @ParameterizedTest
        @ValueSource(classes = {byte.class, Byte.class})
        public void testEncodeTypeValueWithByteValue(Class<?> clazz) {
            // Given
            String value = "127";

            // When
            String result = TypeEncoder.encodeTypeValue(clazz, value);

            // Then
            assertThat(result)
                    .isEqualTo("(byte) 127");
        }

        @Test
        public void testEncodeTypeValueWithNullThrowsException() {
            // When & Then
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> TypeEncoder.encodeTypeValue(String.class, null));
        }

        @Test
        public void testInterfaceButValueDontStartWithHashtagThrowsException() {
            // When & Then
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> TypeEncoder.encodeTypeValue(Runnable.class, "testMethod"))
                    .withMessage("Unexpected value for interface type: %s".formatted(Runnable.class));
        }

        @Test
        public void testWildcardTypeThrowsException() {
            // Given
            Type type = new WildcardType() {
                @Override
                public Type[] getUpperBounds() {
                    return new Type[0];
                }

                @Override
                public Type[] getLowerBounds() {
                    return new Type[0];
                }
            };

            // When & Then
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> TypeEncoder.encodeTypeValue(type, "test"))
                    .withMessage("Unable to encode type value: %s".formatted(type));
        }

        @Test
        public void escapingStringFailedWithIllegalArgumentException() {
            try (MockedStatic<ObjectMapperProvider> objectMapperProviderMockedStatic = Mockito.mockStatic(ObjectMapperProvider.class)) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonSerializer<String> stringJsonSerializer = new JsonSerializer<String>() {
                    @Override
                    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                        throw new JsonGenerationException("Test exception", gen);
                    }
                };
                objectMapper.registerModule(
                        new SimpleModule()
                                .addSerializer(String.class, stringJsonSerializer)
                );
                objectMapperProviderMockedStatic.when(ObjectMapperProvider::getObjectMapper).thenReturn(objectMapper);

                assertThatThrownBy(() -> TypeEncoder.encodeTypeValue(String.class, "data"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Unable to escape the string value")
                        .cause()
                        .isInstanceOf(JsonProcessingException.class)
                        .hasMessage("Test exception");
            }
        }
    }

    @Nested
    class DefaultValueAsStringTest {

        @Test
        void forBooleanReturnFalse() {
            assertThat(TypeEncoder.defaultValueAsString(boolean.class))
                    .isEqualTo("false");
        }

        @Test
        void forCharReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(char.class))
                    .isEqualTo("'\\0'");
        }

        @Test
        void forDoubleReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(double.class))
                    .isEqualTo("0.0");
        }

        @Test
        void forFloatReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(float.class))
                    .isEqualTo("0.0f");
        }

        @Test
        void forIntReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(int.class))
                    .isEqualTo("0");
        }

        @Test
        void forLongReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(long.class))
                    .isEqualTo("0L");
        }

        @Test
        void forShortReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(short.class))
                    .isEqualTo("(short) 0");
        }

        @Test
        void forByteReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(byte.class))
                    .isEqualTo("(byte) 0");
        }

        @Test
        void forStringReturnNull() {
            assertThat(TypeEncoder.defaultValueAsString(String.class))
                    .isEqualTo("null");
        }
    }

    @Nested
    class GetIdentifierTest {

        @Test
        void shouldReturnIdentifierForFXMLObjectNode() {
            // Given
            FXMLObjectNode node = new FXMLObjectNode(
                    false, "objectIdentifier", Object.class, List.of(), List.of(), List.of()
            );

            // When
            String result = TypeEncoder.getIdentifier(node);

            // Then
            assertThat(result).isEqualTo("objectIdentifier");
        }

        @Test
        void shouldReturnIdentifierForFXMLValueNode() {
            // Given
            FXMLValueNode node = new FXMLValueNode(false, "valueIdentifier", String.class, "dummy");

            // When
            String result = TypeEncoder.getIdentifier(node);

            // Then
            assertThat(result).isEqualTo("valueIdentifier");
        }

        @Test
        void shouldReturnFormattedIdentifierForFXMLConstantNode() {
            // Given
            FXMLConstantNode node = new FXMLConstantNode(
                    String.class, "LITERAL", String.class
            );

            // When
            String result = TypeEncoder.getIdentifier(node);

            // Then
            assertThat(result).isEqualTo("String.LITERAL");
        }

        @Test
        void shouldThrowIllegalStateExceptionForWrapperNode() {
            // Given
            FXMLWrapperNode node = new FXMLWrapperNode("wrapper", List.of());

            // When & Then
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> TypeEncoder.getIdentifier(node))
                    .withMessage("Unexpected child node");
        }

        @Test
        void shouldThrowIllegalStateExceptionForStaticMethodNode() {
            // Given
            FXMLStaticMethod node = new FXMLStaticMethod(String.class, "format", List.of());

            // When & Then
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> TypeEncoder.getIdentifier(node))
                    .withMessage("Unexpected child node");
        }
    }

    @Nested
    class TypeToClassTest {
        @Test
        public void testTypeToClassWithClassType() {
            // Given
            Type type = String.class;

            // When
            Class<?> result = TypeEncoder.typeToClass(type);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        public void testTypeToClassWithParameterizedType() {
            // Given
            ParameterizedType type = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            // When
            Class<?> result = TypeEncoder.typeToClass(type);

            // Then
            assertThat(result).isEqualTo(List.class);
        }

        @Test
        public void testTypeToClassWithUnsupportedType() {
            // Given
            Type unsupportedType = new Type() {};

            // When & Then
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> TypeEncoder.typeToClass(unsupportedType))
                    .withMessage("Unsupported type: " + unsupportedType);
        }
    }

    @Nested
    class TypeToReflectionClassStringTest {
        @Test
        public void testTypeToReflectionClassStringWithClassType() {
            // Given
            Type type = String.class;

            // When
            String result = TypeEncoder.typeToReflectionClassString(type);

            // Then
            assertThat(result).isEqualTo("java.lang.String.class");
        }

        @Test
        public void testTypeToReflectionClassStringWithParameterizedType() {
            // Given
            ParameterizedType type = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            // When
            String result = TypeEncoder.typeToReflectionClassString(type);

            // Then
            assertThat(result).isEqualTo("java.util.List.class");
        }

        @Test
        public void testTypeToReflectionClassStringWithUnsupportedType() {
            // Given
            Type unsupportedType = new Type() {};

            // When & Then
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> TypeEncoder.typeToReflectionClassString(unsupportedType))
                    .withMessage("Unsupported type: " + unsupportedType);
        }
    }

    private static class DummyClass {
        public DummyClass(String s) {
        }
    }
}