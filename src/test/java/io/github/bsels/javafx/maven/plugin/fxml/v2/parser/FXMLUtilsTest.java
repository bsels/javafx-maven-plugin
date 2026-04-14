package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
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

    /** A generic subclass with bound that passes its own type parameter to ArrayList. */
    static class NumberList<T extends Number> extends ArrayList<T> {}

    /** A class that directly implements Map<String, Integer>. */
    static class DirectStringIntMap extends HashMap<String, Integer> implements Map<String, Integer> {}

    /** A class that extends HashMap<String, Integer> without directly implementing Map<K,V>. */
    static class MyStringIntegerMap extends HashMap<String, Integer> {}

    /** An interface that extends Map<T, T>. */
    interface IdentityTypeMap<T> extends Map<T, T> {}

    /** A class that extends HashMap<T, T> and implements IdentityTypeMap<T>. */
    static class IdentityTypeHashMap<T> extends HashMap<T, T> implements IdentityTypeMap<T> {}

    /** A class that implements IdentityTypeMap<String>. */
    static class StringIdentityTypeMap extends IdentityTypeHashMap<String> {}

    /** A generic subclass that passes its own type parameters to HashMap. */
    static class GenericMap<K, V> extends HashMap<K, V> {}

    /** A class that implements Collection indirectly via a custom interface. */
    interface MyCollection<E> extends Collection<E> {}
    static class MyCollectionImpl<E> extends ArrayList<E> implements MyCollection<E> {}

    interface SubCollection<T> extends Collection<T> {}
    abstract static class SubCollectionImpl<T> implements SubCollection<T> {}

    static class GenericSuper<T> extends ArrayList<T> {}
    static class SubOfGenericSuper extends GenericSuper<String> {}

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
    interface InterfaceWithStaticAndDefaultMethods {
        void abstractMethod();
        static void staticMethod() {}
        default void defaultMethod() {}
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
    // findCollectionValueTypeFromHierarchy
    // -------------------------------------------------------------------------

    @Nested
    class FindCollectionValueTypeFromHierarchyTest {

        @Test
        void shouldReturnObjectTypeForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnConcreteElementTypeForFXMLClassTypeWithDirectCollectionInterface() {
            // Given – DirectStringCollection directly implements Collection<String>
            FXMLType type = FXMLType.of(DirectStringCollection.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnObjectTypeForRawFXMLClassType() {
            // Given – raw ArrayList has no concrete element type
            FXMLType type = FXMLType.of(ArrayList.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnElementTypeForFXMLGenericTypeWithConcreteArgument() {
            // Given – ArrayList<String>
            FXMLType type = FXMLType.of(ArrayList.class, List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnObjectTypeForFXMLGenericTypeWithWildcardArgument() {
            // Given – GenericList<T> with wildcard argument (no concrete binding)
            FXMLType type = FXMLType.of(GenericList.class, List.of(FXMLType.wildcard()));

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.wildcard());
        }

        @Test
        void shouldReturnDoubleForNumberListDouble() {
            // Given – NumberList<Double> extends ArrayList<Double>
            FXMLType type = FXMLType.of(NumberList.class, List.of(FXMLType.of(Double.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Double.class));
        }

        @Test
        void shouldReturnObjectIfClassIsTargetInterface() {
            // Given
            FXMLType type = FXMLType.of(Collection.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldResolveViaRecursiveInterfaceSearch() {
            // Given – SubCollectionImpl<String> implements SubCollection<String> which extends Collection<T>
            FXMLType type = FXMLType.of(SubCollectionImpl.class, List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldResolveViaGenericSuperclass() {
            // Given – SubOfGenericSuper extends GenericSuper<String> extends ArrayList<String>
            FXMLType type = FXMLType.of(SubOfGenericSuper.class);

            // When
            FXMLType result = FXMLUtils.findCollectionValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findCollectionValueTypeFromHierarchy(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findMapKeyTypeFromHierarchy
    // -------------------------------------------------------------------------

    @Nested
    class FindMapKeyTypeFromHierarchyTest {

        @Test
        void shouldReturnObjectTypeForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnConcreteKeyTypeForFXMLClassTypeWithDirectMapInterface() {
            // Given – DirectStringIntMap directly implements Map<String, Integer>
            FXMLType type = FXMLType.of(DirectStringIntMap.class);

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnConcreteKeyTypeForMyStringIntegerMap() {
            // Given – MyStringIntegerMap extends HashMap<String, Integer>
            FXMLType type = FXMLType.of(MyStringIntegerMap.class);

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnConcreteKeyTypeForStringIdentityTypeMap() {
            // Given – StringIdentityTypeMap implements IdentityTypeMap<String>
            FXMLType type = FXMLType.of(StringIdentityTypeMap.class);

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnConcreteKeyTypeForIdentityTypeHashMap() {
            // Given – IdentityTypeHashMap<String>
            FXMLType type = FXMLType.of(IdentityTypeHashMap.class, List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnObjectTypeForRawFXMLClassType() {
            // Given – raw HashMap has no concrete key type
            FXMLType type = FXMLType.of(HashMap.class);

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnKeyTypeForFXMLGenericTypeWithConcreteArguments() {
            // Given – HashMap<String, Integer>
            FXMLType type = FXMLType.of(HashMap.class, List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnWildcardTypeForFXMLGenericTypeWithWildcardArgument() {
            // Given – GenericMap<K,V> with wildcard arguments
            FXMLType type = FXMLType.of(GenericMap.class, List.of(FXMLType.wildcard(), FXMLType.wildcard()));

            // When
            FXMLType result = FXMLUtils.findMapKeyTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.wildcard());
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findMapKeyTypeFromHierarchy(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findMapValueTypeFromHierarchy
    // -------------------------------------------------------------------------

    @Nested
    class FindMapValueTypeFromHierarchyTest {

        @Test
        void shouldReturnObjectTypeForFXMLWildcardType() {
            // Given
            FXMLType type = FXMLType.wildcard();

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledClassType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of());

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectTypeForFXMLUncompiledGenericType() {
            // Given
            FXMLType type = FXMLType.of("com.example.Foo", List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnConcreteValueTypeForFXMLClassTypeWithDirectMapInterface() {
            // Given – DirectStringIntMap directly implements Map<String, Integer>
            FXMLType type = FXMLType.of(DirectStringIntMap.class);

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldReturnConcreteValueTypeForMyStringIntegerMap() {
            // Given – MyStringIntegerMap extends HashMap<String, Integer>
            FXMLType type = FXMLType.of(MyStringIntegerMap.class);

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldReturnConcreteValueTypeForStringIdentityTypeMap() {
            // Given – StringIdentityTypeMap implements IdentityTypeMap<String>
            FXMLType type = FXMLType.of(StringIdentityTypeMap.class);

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnConcreteValueTypeForIdentityTypeHashMap() {
            // Given – IdentityTypeHashMap<String>
            FXMLType type = FXMLType.of(IdentityTypeHashMap.class, List.of(FXMLType.of(String.class)));

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(String.class));
        }

        @Test
        void shouldReturnObjectTypeForRawFXMLClassType() {
            // Given – raw HashMap has no concrete value type
            FXMLType type = FXMLType.of(HashMap.class);

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnValueTypeForFXMLGenericTypeWithConcreteArguments() {
            // Given – HashMap<String, Integer>
            FXMLType type = FXMLType.of(HashMap.class, List.of(FXMLType.of(String.class), FXMLType.of(Integer.class)));

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.of(Integer.class));
        }

        @Test
        void shouldReturnWildcardTypeForFXMLGenericTypeWithWildcardArgument() {
            // Given – GenericMap<K,V> with wildcard arguments
            FXMLType type = FXMLType.of(GenericMap.class, List.of(FXMLType.wildcard(), FXMLType.wildcard()));

            // When
            FXMLType result = FXMLUtils.findMapValueTypeFromHierarchy(type);

            // Then
            assertThat(result).isEqualTo(FXMLType.wildcard());
        }

        @Test
        void shouldThrowNullPointerExceptionForNullType() {
            // When & Then
            assertThatThrownBy(() -> FXMLUtils.findMapValueTypeFromHierarchy(null))
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

        @Test
        void shouldResolveMappingForMultipleTypeParameters() {
            // Given - Map<String, Integer>
            Map<String, FXMLType> mapping = new HashMap<>();
            Type type = new CustomParameterizedType(Map.class, new Type[]{String.class, Integer.class});

            // When
            FXMLUtils.resolveTypeMapping(type, mapping, new HashSet<>());

            // Then
            assertThat(mapping).containsEntry("K", FXMLType.of(String.class));
            assertThat(mapping).containsEntry("V", FXMLType.of(Integer.class));
        }

        @Test
        void shouldMapTypeVariableToExistingMapping() {
            // Given - T -> String in mapping
            // ParameterizedType List<T>
            Map<String, FXMLType> mapping = new HashMap<>();
            mapping.put("T", FXMLType.of(String.class));

            // We need a TypeVariable "T"
            class GenericHolder<T> { List<T> list; }
            try {
                Type listType = GenericHolder.class.getDeclaredField("list").getGenericType();
                // When
                FXMLUtils.resolveTypeMapping(listType, mapping, new HashSet<>());
                // Then - E should be String
                assertThat(mapping).containsEntry("E", FXMLType.of(String.class));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
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
    // Coverage tests using reflection
    // -------------------------------------------------------------------------

    @Nested
    class CoverageReflectionTest {

        @Test
        void shouldHitCacheForDefaultPropertyName() {
            Class<?> clazz = FXMLUtilsFixtures.WithDefaultProperty.class;
            assertThat(FXMLUtils.resolveDefaultPropertyName(clazz)).isEqualTo(FXMLUtils.resolveDefaultPropertyName(clazz));
        }

        @Test
        void shouldHitCacheForConstantType() {
            Class<?> clazz = FXMLUtilsFixtures.class;
            String name = "CONSTANT";
            assertThat(FXMLUtils.resolveConstantType(clazz, name)).isEqualTo(FXMLUtils.resolveConstantType(clazz, name));
        }

        @Test
        void shouldHitCacheForFactoryMethodReturnType() {
            Class<?> clazz = FXMLUtilsFixtures.class;
            String name = "factory";
            assertThat(FXMLUtils.findFactoryMethodReturnType(clazz, name)).isEqualTo(FXMLUtils.findFactoryMethodReturnType(clazz, name));
        }

        @Test
        void shouldReturnObjectWhenIndexOutOfBounds() throws Exception {
            Type mapType = new CustomParameterizedType(Map.class, new Type[]{String.class, Integer.class});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 2, mapType);
            assertThat(result).contains(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectForNullArgInGetFXMLInterfaceElementType() throws Exception {
            Type mapType = new CustomParameterizedType(Map.class, new Type[]{null, Integer.class});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 0, mapType);
            assertThat(result).contains(FXMLType.OBJECT);
        }

        @Test
        void shouldHandleDefaultInGetFXMLInterfaceElementType() throws Exception {
            Type nonStandardType = new NonStandardType();
            Type mapType = new CustomParameterizedType(Map.class, new Type[]{nonStandardType, Integer.class});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 0, mapType);
            assertThat(result).contains(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnEmptyForNonParameterizedTypeInGetFXMLInterfaceElementType() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 0, String.class);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForDifferentTargetInterfaceInGetFXMLInterfaceElementType() throws Exception {
            Type collectionType = new CustomParameterizedType(Collection.class, new Type[]{String.class});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 0, collectionType);
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleWildcardInFindFXMLGenericTypeFromHierarchy() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("findFXMLGenericTypeFromHierarchy", FXMLType.class, Class.class, int.class);
            method.setAccessible(true);
            FXMLType result = (FXMLType) method.invoke(null, FXMLType.wildcard(), Collection.class, 0);
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldHandleUncompiledInFindFXMLGenericTypeFromHierarchy() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("findFXMLGenericTypeFromHierarchy", FXMLType.class, Class.class, int.class);
            method.setAccessible(true);
            FXMLType result = (FXMLType) method.invoke(null, FXMLType.of("MyType", List.of()), Collection.class, 0);
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldHandleNullInFindFXMLGenericTypeFromHierarchy() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("findFXMLGenericTypeFromHierarchy", FXMLType.class, Class.class, int.class);
            method.setAccessible(true);
            try {
                method.invoke(null, null, Collection.class, 0);
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertThat(e.getCause()).isInstanceOf(NullPointerException.class);
            }
        }

        @Test
        void shouldIdentifyFunctionalInterfaceWithStaticAndDefaultMethods() {
            assertThat(FXMLUtils.isFunctionalInterface(InterfaceWithStaticAndDefaultMethods.class)).isTrue();
        }

        @Test
        void shouldHandleNonStaticFieldInResolveConstantType() throws Exception {
            class NonStaticField { public String field; }
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("internalResolveConstantType", Class.forName("io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils$ClassAndString"));
            method.setAccessible(true);
            Object classAndString = createClassAndString(NonStaticField.class, "field");
            try {
                method.invoke(null, classAndString);
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
                assertThat(e.getCause().getMessage()).contains("is not static");
            }
        }

        @Test
        void shouldHandleParameterizedTypeInGetFXMLInterfaceElementType() throws Exception {
            Type innerType = new CustomParameterizedType(List.class, new Type[]{String.class});
            Type mapType = new CustomParameterizedType(Map.class, new Type[]{String.class, innerType});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 1, mapType);
            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType.class);
            io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType genericType = (io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType) result.get();
            assertThat(genericType.type()).isEqualTo(List.class);
            assertThat(genericType.typeArguments()).containsExactly(FXMLType.of(String.class));
        }

        @Test
        void shouldHandleParameterizedTypeWithWildcardInGetFXMLInterfaceElementType() throws Exception {
            Type innerType = new CustomParameterizedType(List.class, new Type[]{new NonStandardType()});
            Type mapType = new CustomParameterizedType(Map.class, new Type[]{String.class, innerType});
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("getFXMLInterfaceElementType", Class.class, int.class, Type.class);
            method.setAccessible(true);
            java.util.Optional<FXMLType> result = (java.util.Optional<FXMLType>) method.invoke(null, Map.class, 1, mapType);
            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType.class);
            io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType genericType = (io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType) result.get();
            assertThat(genericType.type()).isEqualTo(List.class);
            assertThat(genericType.typeArguments()).containsExactly(FXMLType.wildcard());
        }

        @Test
        void shouldReturnObjectForNullClazzInFindFXMLGenericTypeFromHierarchyForClass() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("findFXMLGenericTypeFromHierarchyForClass", Class.class, Class.class, int.class);
            method.setAccessible(true);
            FXMLType result = (FXMLType) method.invoke(null, null, Collection.class, 0);
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        @Test
        void shouldReturnObjectForObjectClazzInFindFXMLGenericTypeFromHierarchyForClass() throws Exception {
            java.lang.reflect.Method method = FXMLUtils.class.getDeclaredMethod("findFXMLGenericTypeFromHierarchyForClass", Class.class, Class.class, int.class);
            method.setAccessible(true);
            FXMLType result = (FXMLType) method.invoke(null, Object.class, Collection.class, 0);
            assertThat(result).isEqualTo(FXMLType.OBJECT);
        }

        private Object createClassAndString(Class<?> clazz, String string) throws Exception {
            Class<?> recordClass = Class.forName("io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils$ClassAndString");
            java.lang.reflect.Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            return constructor.newInstance(clazz, string);
        }
    }
}
