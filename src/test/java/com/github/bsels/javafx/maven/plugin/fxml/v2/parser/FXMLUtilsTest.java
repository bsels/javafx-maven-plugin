package com.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
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
}
