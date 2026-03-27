package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unit tests for [FXMLUtils].
class FXMLUtilsTest {

    // -------------------------------------------------------------------------
    // Helper fixtures
    // -------------------------------------------------------------------------

    /** A class that directly implements Collection<String>. */
    static class DirectStringCollection extends ArrayList<String> implements Collection<String> {}

    /** A raw (non-generic) ArrayList subclass (does not directly implement Collection<E>). */
    static class RawStringList extends ArrayList<String> {}

    /** A generic subclass that passes its own type parameter to ArrayList. */
    static class GenericList<T> extends ArrayList<T> {}

    /** A class that directly implements Map<String, Integer>. */
    static class DirectStringIntMap extends HashMap<String, Integer> implements Map<String, Integer> {}

    /** A raw HashMap subclass with concrete key/value types (does not directly implement Map<K,V>). */
    static class StringIntMap extends HashMap<String, Integer> {}

    /** A generic subclass that passes its own type parameters to HashMap. */
    static class GenericMap<K, V> extends HashMap<K, V> {}

    /** A class that implements Collection indirectly via a custom interface. */
    interface MyCollection<E> extends Collection<E> {}
    static class MyCollectionImpl<E> extends ArrayList<E> implements MyCollection<E> {}

    /** A custom Type implementation for testing non-standard Type handling. */
    static class NonStandardType implements Type {
        @Override
        public String getTypeName() {
            return "NonStandardType";
        }
    }

    /** A custom ParameterizedType implementation for testing null actual type arguments. */
    static class CustomParameterizedType implements ParameterizedType {
        private final Class<?> rawType;
        private final Type[] actualTypeArguments;

        CustomParameterizedType(Class<?> rawType, Type[] actualTypeArguments) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    /** Fixtures for testing FXMLUtils methods. */
    static class FXMLUtilsFixtures {
        public static final String CONSTANT = "value";
        public String nonStaticField = "oops";
        @javafx.beans.DefaultProperty("myDefaultProperty")
        public static class WithDefaultProperty {}
        public static class WithoutDefaultProperty {}
        public static class SubWithDefaultProperty extends WithDefaultProperty {}

        public static String factory() { return "factory"; }
        public String nonStaticFactory() { return "nonStatic"; }
        public static String factoryWithParam(String p) { return p; }
    }

    /** Fixtures for functional interface testing. */
    interface EmptyInterface {}
    @FunctionalInterface
    interface MyFunctionalInterface { void run(); }
    interface ImplicitFunctionalInterface { void run(); }
    interface NotAFunctionalInterface { void run(); void walk(); }

    // -------------------------------------------------------------------------
    // findRawType
    // -------------------------------------------------------------------------

    @Nested
    class FindRawTypeTest {

        @Test
        void shouldReturnClassForFXMLClassType() {
            // Given
            FXMLType type = FXMLType.of(String.class);

            // When
            Class<?> result = FXMLUtils.findRawType(type);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldReturnClassForFXMLGenericType() {
            // Given
            FXMLType type = FXMLType.of(List.class, List.of(FXMLType.of(String.class)));

            // When
            Class<?> result = FXMLUtils.findRawType(type);

            // Then
            assertThat(result).isEqualTo(List.class);
        }

        @Test
        void shouldReturnObjectClassForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            Class<?> result = FXMLUtils.findRawType(type);

            // Then
            assertThat(result).isEqualTo(Object.class);
        }

        @Test
        void shouldReturnObjectClassForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            Class<?> result = FXMLUtils.findRawType(type);

            // Then
            assertThat(result).isEqualTo(Object.class);
        }

        @Test
        void shouldReturnObjectClassForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            Class<?> result = FXMLUtils.findRawType(type);

            // Then
            assertThat(result).isEqualTo(Object.class);
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findRawType(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findCollectionValueType
    // -------------------------------------------------------------------------

    @Nested
    class FindCollectionValueTypeTest {

        @Test
        void shouldReturnObjectTypeForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnConcreteElementTypeForFXMLClassTypeWithDirectCollectionInterface() {
            // Given – DirectStringCollection directly implements Collection<String>
            FXMLType type = FXMLType.of(DirectStringCollection.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnObjectTypeForFXMLClassTypeWithNoConcreteElementType() {
            // Given – raw ArrayList has no concrete element type
            FXMLType type = FXMLType.of(ArrayList.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnElementTypeForFXMLGenericTypeWithConcreteArgument() {
            // Given – List<String>
            FXMLType type = FXMLType.of(ArrayList.class, List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnWildcardTypeForFXMLGenericTypeWhenMappingCannotBeResolved() {
            // Given – GenericList<T> with wildcard argument (no concrete binding)
            FXMLType type = FXMLType.of(GenericList.class, List.of(FXMLType.wildcard()));

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then – wildcard arg propagates as wildcard FXMLType
            assertThat(result).isEqualTo(FXMLType.wildcard());
        }

        @Test
        void shouldReturnElementTypeForIndirectCollectionImplementation() {
            // Given – MyCollectionImpl<Integer>
            FXMLType type = FXMLType.of(MyCollectionImpl.class, List.of(FXMLType.of(Integer.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueType(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findCollectionValueType(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findMapKeyAndValueTypes
    // -------------------------------------------------------------------------

    @Nested
    class FindMapKeyAndValueTypesTest {

        @Test
        void shouldReturnObjectTypesForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(Object.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnObjectTypesForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(Object.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnObjectTypesForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(Object.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnConcreteKeyAndValueTypesForFXMLClassTypeWithDirectMapInterface() {
            // Given – DirectStringIntMap directly implements Map<String, Integer>
            FXMLType type = FXMLType.of(DirectStringIntMap.class);

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(String.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldReturnObjectTypesForFXMLClassTypeWithNoConcreteKeyValueTypes() {
            // Given – raw HashMap has no concrete key/value types
            FXMLType type = FXMLType.of(HashMap.class);

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(Object.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Object.class));
        }

        @Test
        void shouldReturnKeyAndValueTypesForFXMLGenericTypeWithConcreteArguments() {
            // Given – HashMap<String, Integer>
            FXMLType type = FXMLType.of(HashMap.class, List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(String.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldReturnWildcardTypesForFXMLGenericTypeWhenMappingCannotBeResolved() {
            // Given – GenericMap<K,V> with wildcard arguments
            FXMLType type = FXMLType.of(GenericMap.class, List.of(FXMLType.wildcard(), FXMLType.wildcard()));

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then – wildcard args propagate as wildcard FXMLTypes
            assertThat(result.getKey()).isEqualTo(FXMLType.wildcard());
            assertThat(result.getValue()).isEqualTo(FXMLType.wildcard());
        }

        @Test
        void shouldReturnKeyAndValueTypesForIndirectMapImplementation() {
            // Given – TreeMap<String, Integer> (implements Map via SortedMap)
            FXMLType type = FXMLType.of(TreeMap.class, List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));

            // When
            Map.Entry<FXMLType, FXMLType> result = FXMLUtils.findMapKeyAndValueTypes(type);

            // Then
            assertThat(result.getKey()).isEqualTo(FXMLType.of(String.class));
            assertThat(result.getValue()).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findMapKeyAndValueTypes(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // resolveTypeMapping
    // -------------------------------------------------------------------------

    @Nested
    class ResolveTypeMappingTest {

        @Test
        void shouldDoNothingForNullType() {
            // Given
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(null, mapping, new HashSet<>());

            // Then
            assertThat(mapping).isEmpty();
        }

        @Test
        void shouldDoNothingForObjectClass() {
            // Given
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(Object.class, mapping, new HashSet<>());

            // Then
            assertThat(mapping).isEmpty();
        }

        @Test
        void shouldDoNothingForAlreadyVisitedType() {
            // Given
            Map<String, FXMLType> mapping = new LinkedHashMap<>();
            Set<Type> visited = new HashSet<>();
            visited.add(ArrayList.class);

            // When
            FXMLUtils.resolveTypeMapping(ArrayList.class, mapping, visited);

            // Then
            assertThat(mapping).isEmpty();
        }

        @Test
        void shouldResolveTypeMappingForParameterizedTypeWithConcreteArgument() throws Exception {
            // Given – ParameterizedType for ArrayList<String>
            ParameterizedType pt = (ParameterizedType) RawStringList.class.getGenericSuperclass();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – ArrayList's type parameter "E" should be mapped to String
            assertThat(mapping).containsEntry("E", FXMLType.of(String.class));
        }

        @Test
        void shouldResolveTypeMappingForParameterizedTypeWithTypeVariableArgument() throws Exception {
            // Given – GenericList<T> extends ArrayList<T>; seed mapping with T -> Integer
            ParameterizedType pt = (ParameterizedType) GenericList.class.getGenericSuperclass();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();
            mapping.put("T", FXMLType.of(Integer.class));

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – ArrayList's "E" should be resolved to Integer via T
            assertThat(mapping).containsEntry("E", FXMLType.of(Integer.class));
        }

        @Test
        void shouldResolveTypeMappingForClassByTraversingHierarchy() {
            // Given – RawStringList extends ArrayList<String>; traversal should propagate E -> String
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(RawStringList.class, mapping, new HashSet<>());

            // Then
            assertThat(mapping).containsEntry("E", FXMLType.of(String.class));
        }

        @Test
        void shouldMapWildcardForUnresolvableTypeArgument() throws Exception {
            // Given – a ParameterizedType whose actual type argument is a wildcard (?)
            // We use a field type: List<?> via an anonymous holder
            class Holder {
                List<?> field;
            }
            ParameterizedType pt = (ParameterizedType) Holder.class.getDeclaredField("field").getGenericType();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – wildcard arg should produce a wildcard FXMLType for "E"
            assertThat(mapping).containsEntry("E", FXMLType.wildcard());
        }

        @Test
        void shouldMapNestedParameterizedTypeArgument() throws Exception {
            // Given – a ParameterizedType whose actual type argument is itself parameterized: List<List<String>>
            class Holder {
                List<List<String>> field;
            }
            ParameterizedType pt = (ParameterizedType) Holder.class.getDeclaredField("field").getGenericType();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – "E" should be mapped to FXMLGenericType(List, [String])
            assertThat(mapping).containsKey("E");
            FXMLType elementType = mapping.get("E");
            assertThat(elementType).isInstanceOf(FXMLGenericType.class);
            FXMLGenericType genericType = (FXMLGenericType) elementType;
            assertThat(genericType.type()).isEqualTo(List.class);
            assertThat(genericType.typeArguments()).containsExactly(FXMLType.of(String.class));
        }

        @Test
        void shouldMapNestedParameterizedTypeArgumentWithWildcardInnerArg() throws Exception {
            // Given – a ParameterizedType whose actual type argument is itself parameterized with a wildcard: List<List<?>>
            class Holder {
                List<List<?>> field;
            }
            ParameterizedType pt = (ParameterizedType) Holder.class.getDeclaredField("field").getGenericType();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – "E" should be mapped to FXMLGenericType(List, [wildcard])
            assertThat(mapping).containsKey("E");
            FXMLType elementType = mapping.get("E");
            assertThat(elementType).isInstanceOf(FXMLGenericType.class);
            FXMLGenericType genericType = (FXMLGenericType) elementType;
            assertThat(genericType.type()).isEqualTo(List.class);
            assertThat(genericType.typeArguments()).containsExactly(FXMLType.wildcard());
        }

        @Test
        void shouldDoNothingForTypeVariableNotPresentInMapping() throws Exception {
            // Given – GenericList<T> extends ArrayList<T>; T is NOT in mapping
            ParameterizedType pt = (ParameterizedType) GenericList.class.getGenericSuperclass();
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – ArrayList's "E" should NOT be resolved, mapping stays empty (for E)
            assertThat(mapping).doesNotContainKey("E");
        }

        @Test
        void shouldDoNothingForNonStandardType() {
            // Given
            Map<String, FXMLType> mapping = new LinkedHashMap<>();
            NonStandardType nonStandardType = new NonStandardType();

            // When
            FXMLUtils.resolveTypeMapping(nonStandardType, mapping, new HashSet<>());

            // Then – it's not a Class, and it's not a ParameterizedType, so nothing should happen
            assertThat(mapping).isEmpty();
        }

        @Test
        void shouldMapWildcardForNullActualTypeArgument() {
            // Given – a ParameterizedType for List whose actual type argument is null
            ParameterizedType pt = new CustomParameterizedType(List.class, new Type[] {null});
            Map<String, FXMLType> mapping = new LinkedHashMap<>();

            // When
            FXMLUtils.resolveTypeMapping(pt, mapping, new HashSet<>());

            // Then – null arg should produce a wildcard FXMLType for "E"
            assertThat(mapping).containsEntry("E", FXMLType.wildcard());
        }
    }

    // -------------------------------------------------------------------------
    // isInvalidIdentifierName
    // -------------------------------------------------------------------------

    @Nested
    class IsInvalidIdentifierNameTest {

        @Test
        void shouldThrowNullPointerExceptionForNullName() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.isInvalidIdentifierName(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"validName", "ValidName", "$valid", "valid123", "valid_name", "$", "a"})
        void shouldReturnFalseForValidIdentifiers(String name) {
            // When
            boolean result = FXMLUtils.isInvalidIdentifierName(name);

            // Then
            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1invalid", "-invalid", "invalid name", "invalid-name", "invalid.name", "123"})
        void shouldReturnTrueForInvalidIdentifiers(String name) {
            // When
            boolean result = FXMLUtils.isInvalidIdentifierName(name);

            // Then
            assertThat(result).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // isEventHandlerType
    // -------------------------------------------------------------------------

    @Nested
    class IsEventHandlerTypeTest {

        @Test
        void shouldReturnTrueForEventHandler() {
            assertThat(FXMLUtils.isEventHandlerType(javafx.event.EventHandler.class)).isTrue();
        }

        @Test
        void shouldReturnFalseForNonEventHandler() {
            assertThat(FXMLUtils.isEventHandlerType(String.class)).isFalse();
        }

        @Test
        void shouldThrowNullPointerExceptionForNullClass() {
            assertThatThrownBy(() -> FXMLUtils.isEventHandlerType(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // isFunctionalInterface
    // -------------------------------------------------------------------------

    @Nested
    class IsFunctionalInterfaceTest {

        @Test
        void shouldReturnTrueForAnnotatedInterface() {
            assertThat(FXMLUtils.isFunctionalInterface(MyFunctionalInterface.class)).isTrue();
        }

        @Test
        void shouldReturnTrueForImplicitFunctionalInterface() {
            assertThat(FXMLUtils.isFunctionalInterface(ImplicitFunctionalInterface.class)).isTrue();
        }

        @Test
        void shouldReturnFalseForNonInterface() {
            assertThat(FXMLUtils.isFunctionalInterface(String.class)).isFalse();
        }

        @Test
        void shouldReturnFalseForEmptyInterface() {
            assertThat(FXMLUtils.isFunctionalInterface(EmptyInterface.class)).isFalse();
        }

        @Test
        void shouldReturnFalseForInterfaceWithMultipleMethods() {
            assertThat(FXMLUtils.isFunctionalInterface(NotAFunctionalInterface.class)).isFalse();
        }

        @Test
        void shouldThrowNullPointerExceptionForNullClass() {
            assertThatThrownBy(() -> FXMLUtils.isFunctionalInterface(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // resolveConstantType
    // -------------------------------------------------------------------------

    @Nested
    class ResolveConstantTypeTest {

        @Test
        void shouldResolveConstantType() {
            Type result = FXMLUtils.resolveConstantType(FXMLUtilsFixtures.class, "CONSTANT");
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldThrowExceptionForNonStaticField() {
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(FXMLUtilsFixtures.class, "nonStaticField"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("is not static");
        }

        @Test
        void shouldThrowExceptionForMissingField() {
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(FXMLUtilsFixtures.class, "MISSING"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No such constant field");
        }

        @Test
        void shouldThrowNullPointerExceptionForNullArgs() {
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(null, "CONSTANT"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(FXMLUtilsFixtures.class, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findFactoryMethodReturnType
    // -------------------------------------------------------------------------

    @Nested
    class FindFactoryMethodReturnTypeTest {

        @Test
        void shouldResolveFactoryMethodReturnType() {
            Type result = FXMLUtils.findFactoryMethodReturnType(FXMLUtilsFixtures.class, "factory");
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldThrowExceptionForNonStaticMethod() {
            assertThatThrownBy(() -> FXMLUtils.findFactoryMethodReturnType(FXMLUtilsFixtures.class, "nonStaticFactory"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No static factory method");
        }

        @Test
        void shouldThrowExceptionForMethodWithParam() {
            assertThatThrownBy(() -> FXMLUtils.findFactoryMethodReturnType(FXMLUtilsFixtures.class, "factoryWithParam"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No static factory method");
        }

        @Test
        void shouldThrowExceptionForMissingMethod() {
            assertThatThrownBy(() -> FXMLUtils.findFactoryMethodReturnType(FXMLUtilsFixtures.class, "missing"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No static factory method");
        }

        @Test
        void shouldThrowNullPointerExceptionForNullArgs() {
            assertThatThrownBy(() -> FXMLUtils.findFactoryMethodReturnType(null, "factory"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FXMLUtils.findFactoryMethodReturnType(FXMLUtilsFixtures.class, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // resolveDefaultPropertyName
    // -------------------------------------------------------------------------

    @Nested
    class ResolveDefaultPropertyNameTest {

        @Test
        void shouldResolveDefaultProperty() {
            assertThat(FXMLUtils.resolveDefaultPropertyName(FXMLUtilsFixtures.WithDefaultProperty.class))
                    .contains("myDefaultProperty");
        }

        @Test
        void shouldResolveDefaultPropertyFromSuperclass() {
            assertThat(FXMLUtils.resolveDefaultPropertyName(FXMLUtilsFixtures.SubWithDefaultProperty.class))
                    .contains("myDefaultProperty");
        }

        @Test
        void shouldReturnEmptyForNoDefaultProperty() {
            assertThat(FXMLUtils.resolveDefaultPropertyName(FXMLUtilsFixtures.WithoutDefaultProperty.class))
                    .isEmpty();
        }

        @Test
        void shouldThrowNullPointerExceptionForNullClass() {
            assertThatThrownBy(() -> FXMLUtils.resolveDefaultPropertyName(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // hasNonSkippablePrefix
    // -------------------------------------------------------------------------

    @Nested
    class HasNonSkippablePrefixTest {

        @Test
        void shouldReturnTrueForNonSkippable() {
            assertThat(FXMLUtils.hasNonSkippablePrefix("myId")).isTrue();
        }

        @Test
        void shouldReturnFalseForFxPrefix() {
            assertThat(FXMLUtils.hasNonSkippablePrefix("fx:id")).isFalse();
        }

        @Test
        void shouldReturnFalseForXmlns() {
            assertThat(FXMLUtils.hasNonSkippablePrefix("xmlns:fx")).isFalse();
        }

        @Test
        void shouldThrowNullPointerExceptionForNullKey() {
            assertThatThrownBy(() -> FXMLUtils.hasNonSkippablePrefix(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`key` must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // constructGenericType
    // -------------------------------------------------------------------------

    @Nested
    class ConstructGenericTypeTest {

        @Test
        void shouldConstructGenericType() {
            FXMLType result = FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: java.lang.String"),
                    List.of()
            );
            assertThat(result).isEqualTo(FXMLType.of(List.class, List.of(FXMLType.of(String.class))));
        }

        @Test
        void shouldConstructNestedGenericType() {
            FXMLType result = FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: java.util.List<java.lang.String>"),
                    List.of()
            );
            assertThat(result).isInstanceOf(FXMLGenericType.class);
            FXMLGenericType gt = (FXMLGenericType) result;
            assertThat(gt.type()).isEqualTo(List.class);
            assertThat(gt.typeArguments().getFirst()).isInstanceOf(FXMLGenericType.class);
            FXMLGenericType inner = (FXMLGenericType) gt.typeArguments().getFirst();
            assertThat(inner.type()).isEqualTo(List.class);
            assertThat(inner.typeArguments().getFirst()).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldHandleDeeplyNestedGenerics() {
            FXMLType result = FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: java.util.List<java.util.List<java.lang.String>>"),
                    List.of()
            );
            assertThat(result.toString()).contains("List").contains("String");
        }

        @Test
        void shouldHandleUncompiledTypesInGenerics() {
            FXMLType result = FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: com.example.Uncompiled<java.lang.String>"),
                    List.of()
            );
            assertThat(result.toString()).contains("Uncompiled").contains("String");
        }

        @Test
        void shouldReturnUncompiledTypeWhenGenericTypeNotFound() {
            // Given
            Class<?> rawClass = List.class;
            List<String> comments = List.of("generic 0: com.example.DoesNotExist");
            List<String> imports = List.of();

            // When
            FXMLType result = FXMLUtils.constructGenericType(rawClass, comments, imports);

            // Then
            assertThat(result).isInstanceOf(FXMLGenericType.class);
            FXMLGenericType gt = (FXMLGenericType) result;
            assertThat(gt.typeArguments().getFirst())
                    .isInstanceOf(FXMLUncompiledClassType.class)
                    .hasFieldOrPropertyWithValue("name", "com.example.DoesNotExist");
        }

        @Test
        void shouldReturnUncompiledTypeWhenNestedGenericTypeNotFound() {
            // Given
            Class<?> rawClass = List.class;
            List<String> comments = List.of("generic 0: java.util.List<com.example.DoesNotExist>");
            List<String> imports = List.of();

            // When
            FXMLType result = FXMLUtils.constructGenericType(rawClass, comments, imports);

            // Then
            assertThat(result)
                    .isInstanceOf(FXMLGenericType.class)
                    .extracting(FXMLGenericType.class::cast)
                    .hasFieldOrPropertyWithValue("type", List.class)
                    .extracting(FXMLGenericType::typeArguments, InstanceOfAssertFactories.list(FXMLType.class))
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLGenericType.class)
                    .extracting(FXMLGenericType.class::cast)
                    .hasFieldOrPropertyWithValue("type", List.class)
                    .extracting(FXMLGenericType::typeArguments, InstanceOfAssertFactories.list(FXMLType.class))
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLUncompiledClassType.class)
                    .hasFieldOrPropertyWithValue("name", "com.example.DoesNotExist");
        }

        @Test
        void shouldThrowExceptionForInvalidGenericString() {
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: invalid[String]"),
                    List.of()
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldThrowExceptionForMismatchingGenericCount() {
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 0: String", "generic 1: Integer"),
                    List.of()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Generic types count (2) does not match the number of type parameters (1)");
        }

        @Test
        void shouldThrowExceptionForNonSequentialGenericIndices() {
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(
                    List.class,
                    List.of("generic 1: String"),
                    List.of()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Generic types are having non-sequential indices");
        }

        @Test
        void shouldThrowNullPointerExceptionForNullArgs() {
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(null, List.of(), List.of()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(List.class, null, List.of()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FXMLUtils.constructGenericType(List.class, List.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
