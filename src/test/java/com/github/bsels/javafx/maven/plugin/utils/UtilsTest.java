package com.github.bsels.javafx.maven.plugin.utils;

import javafx.beans.NamedArg;
import javafx.scene.Node;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Gatherer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UtilsTest {

    @Nested
    class FindTypeTest {

        @Test
        void shouldReturnClassForFullyQualifiedTypeName() {
            // Given
            List<String> imports = List.of("java.util.List", "java.lang.String");
            String typeName = "java.util.ArrayList";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(ArrayList.class);
        }

        @Test
        void shouldThrowExceptionForInvalidFullyQualifiedTypeName() {
            // Given
            List<String> imports = List.of("java.util.List");
            String typeName = "com.example.NonExistentClass";

            // When & Then
            assertThatThrownBy(() -> Utils.findType(imports, typeName))
                    .isInstanceOf(InternalClassNotFoundException.class)
                    .hasMessage("Unable to find type for name: com.example.NonExistentClass");
        }

        @Test
        void shouldReturnClassFromExplicitImports() {
            // Given
            List<String> imports = List.of("java.util.List", "java.util.ArrayList", "java.lang.String");
            String typeName = "ArrayList";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(ArrayList.class);
        }

        @Test
        void shouldReturnClassFromWildcardImports() {
            // Given
            List<String> imports = List.of("java.util.*", "java.lang.*");
            String typeName = "HashMap";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(HashMap.class);
        }

        @Test
        void shouldThrowExceptionWhenMultipleTypesFoundInWildcardImports() {
            // Given - Create a scenario where the same class name exists in multiple packages
            List<String> imports = List.of("java.util.*", "java.sql.*");
            String typeName = "Date";

            // When & Then
            assertThatThrownBy(() -> Utils.findType(imports, typeName))
                    .isInstanceOf(InternalClassNotFoundException.class)
                    .hasMessage("Found multiple types for name: Date");
        }

        @Test
        void shouldReturnClassFromJavaLangPackageByDefault() {
            // Given
            List<String> imports = List.of("java.util.List");
            String typeName = "String";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldReturnIntegerFromJavaLangPackageByDefault() {
            // Given
            List<String> imports = List.of("java.util.List");
            String typeName = "Integer";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(Integer.class);
        }

        @Test
        void shouldThrowExceptionWhenTypeNotFoundAnywhere() {
            // Given
            List<String> imports = List.of("java.util.*");
            String typeName = "NonExistentClass";

            // When & Then
            assertThatThrownBy(() -> Utils.findType(imports, typeName))
                    .isInstanceOf(InternalClassNotFoundException.class)
                    .hasMessage("Unable to find type for name: NonExistentClass");
        }

        @Test
        void shouldPreferExplicitImportOverWildcardImport() {
            // Given - explicit import should take precedence
            List<String> imports = List.of("java.util.Date", "java.sql.*");
            String typeName = "Date";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(java.util.Date.class);
        }

        @Test
        void shouldPreferExplicitImportOverJavaLangPackage() {
            // Given - explicit import should take precedence over java.lang
            List<String> imports = List.of("java.math.BigInteger");
            String typeName = "Integer"; // This won't match BigInteger but should fall back to java.lang.Integer

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(Integer.class);
        }

        @Test
        void shouldHandleEmptyImportsList() {
            // Given
            List<String> imports = List.of();
            String typeName = "String";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldHandleSingleWildcardImport() {
            // Given
            List<String> imports = List.of("java.util.*");
            String typeName = "List";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(List.class);
        }

        @Test
        void shouldHandleComplexTypeNameWithDollarsForInnerClasses() {
            // Given
            List<String> imports = List.of("java.util.*");
            String typeName = "java.util.Map$Entry";

            // When
            Class<?> result = Utils.findType(imports, typeName);

            // Then
            assertThat(result).isEqualTo(Map.Entry.class);
        }
    }

    @Nested
    class FindMethodTest {

        @Test
        void testFindMethodWithMatchingNameAndParameters() throws NoSuchMethodException {
            // Arrange
            Class<TestClass> clazz = TestClass.class;
            String methodName = "exampleMethod";
            Class<?> parameterType = String.class;

            // Act
            Optional<Method> method = Utils.findMethod(clazz, methodName, parameterType);

            // Assert
            assertThat(method).isPresent();
            assertThat(method.get())
                    .isEqualTo(TestClass.class.getMethod(methodName, parameterType));
        }

        @Test
        void testFindMethodWithNonMatchingNameOrParameters() {
            // Arrange
            Class<TestClass> clazz = TestClass.class;
            String methodName = "nonExistentMethod";
            Class<?> parameterType = Integer.class;

            // Act
            Optional<Method> method = Utils.findMethod(clazz, methodName, parameterType);

            // Assert
            assertThat(method).isEmpty();
        }

        @Test
        void testFindMethodWithNullParameterType() {
            // Arrange
            Class<TestClass> clazz = TestClass.class;
            String methodName = "exampleMethod";

            // Act
            Optional<Method> method = Utils.findMethod(clazz, methodName, null);

            // Assert
            assertThat(method).isEmpty();
        }

        @Test
        void testFindMethodInClassImplementingInterface() throws NoSuchMethodException {
            // Arrange
            Class<? extends TestInterface> clazz = TestInterfaceImpl.class;
            String methodName = "interfaceMethod";
            Class<?> parameterType = String.class;

            // Act
            Optional<Method> method = Utils.findMethod(clazz, methodName, parameterType);

            // Assert
            assertThat(method).isPresent();
            assertThat(method.get())
                    .isEqualTo(TestInterfaceImpl.class.getMethod(methodName, parameterType));
        }

        interface TestInterface {
            void interfaceMethod(String input);
        }

        // Helper classes for testing
        static class TestClass {
            public void exampleMethod(String input) {
            }

            public void anotherMethod(Integer input) {
            }
        }

        static class TestInterfaceImpl implements TestInterface {
            @Override
            public void interfaceMethod(String input) {
            }
        }
    }

    @Nested
    class GetSetterNameTest {


        @Test
        void testGetSetterNameForSimpleFieldName() {
            // Arrange
            String fieldName = "name";

            // Act
            String setterName = Utils.getSetterName(fieldName);

            // Assert
            assertThat(setterName)
                    .isEqualTo("setName");
        }

        @Test
        void testGetSetterNameForSingleCharacterFieldName() {
            // Arrange
            String fieldName = "x";

            // Act
            String setterName = Utils.getSetterName(fieldName);

            // Assert
            assertThat(setterName)
                    .isEqualTo("setX");
        }

        @Test
        void testGetSetterNameForFieldNameWithUpperCaseFirstLetter() {
            // Arrange
            String fieldName = "Name";

            // Act
            String setterName = Utils.getSetterName(fieldName);

            // Assert
            assertThat(setterName)
                    .isEqualTo("setName");
        }

        @Test
        void testGetSetterNameForEmptyFieldName() {
            // Arrange
            String fieldName = "";

            // Act and Assert
            assertThatExceptionOfType(StringIndexOutOfBoundsException.class)
                    .isThrownBy(() -> Utils.getSetterName(fieldName));
        }

        @Test
        void testGetSetterNameForNullFieldName() {
            // Act and Assert
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Utils.getSetterName(null));
        }

        @Test
        void testGetSetterNameForFieldNameWithSpecialCharacters() {
            // Arrange
            String fieldName = "user_name";

            // Act
            String setterName = Utils.getSetterName(fieldName);

            // Assert
            assertThat(setterName)
                    .isEqualTo("setUser_name");
        }

        @Test
        void testGetSetterNameForFieldNameWithNumbers() {
            // Arrange
            String fieldName = "field123";

            // Act
            String setterName = Utils.getSetterName(fieldName);

            // Assert
            assertThat(setterName)
                    .isEqualTo("setField123");
        }
    }

    @Nested
    class GetClassTypeTest {

        @Test
        void testGetClassTypeForValidClassType() {
            // Arrange
            Type type = String.class;

            // Act
            Class<?> classType = Utils.getClassType(type);

            // Assert
            assertThat(classType).isEqualTo(String.class);
        }

        @Test
        void testGetClassTypeForParameterizedType() throws NoSuchFieldException {
            // Arrange
            Type type = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public Type getRawType() {
                    return String.class;
                }

                @Override
                public Type getOwnerType() {
                    return Object.class;
                }
            };

            // Act
            Class<?> classType = Utils.getClassType(type);

            // Assert
            assertThat(classType).isEqualTo(String.class);
        }

        @Test
        void testGetClassTypeThrowsExceptionForInvalidType() {
            // Arrange
            Type type = new Type() {
            }; // Anonymous Type for simulation

            // Act & Assert
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> Utils.getClassType(type))
                    .withMessageContaining("Unable to find class type for");
        }

        @Test
        void testGetClassTypeForRawType() {
            // Arrange
            Type type = List.class;

            // Act
            Class<?> classType = Utils.getClassType(type);

            // Assert
            assertThat(classType).isEqualTo(List.class);
        }
    }

    @Nested
    class GetGetterNameTest {

        @Test
        void testGetGetterNameForSimpleFieldName() {
            // Arrange
            String fieldName = "name";

            // Act
            String getterName = Utils.getGetterName(fieldName);

            // Assert
            assertThat(getterName)
                    .isEqualTo("getName");
        }

        @Test
        void testGetGetterNameForSingleCharacterFieldName() {
            // Arrange
            String fieldName = "x";

            // Act
            String getterName = Utils.getGetterName(fieldName);

            // Assert
            assertThat(getterName)
                    .isEqualTo("getX");
        }

        @Test
        void testGetGetterNameForFieldNameWithUpperCaseFirstLetter() {
            // Arrange
            String fieldName = "Name";

            // Act
            String getterName = Utils.getGetterName(fieldName);

            // Assert
            assertThat(getterName)
                    .isEqualTo("getName");
        }

        @Test
        void testGetGetterNameForEmptyFieldName() {
            // Arrange
            String fieldName = "";

            // Act and Assert
            assertThatExceptionOfType(StringIndexOutOfBoundsException.class)
                    .isThrownBy(() -> Utils.getGetterName(fieldName));
        }

        @Test
        void testGetGetterNameForNullFieldName() {
            // Act and Assert
            assertThatExceptionOfType(NullPointerException.class)
                    .isThrownBy(() -> Utils.getGetterName(null));
        }

        @Test
        void testGetGetterNameForFieldNameWithSpecialCharacters() {
            // Arrange
            String fieldName = "user_name";

            // Act
            String getterName = Utils.getGetterName(fieldName);

            // Assert
            assertThat(getterName)
                    .isEqualTo("getUser_name");
        }

        @Test
        void testGetGetterNameForFieldNameWithNumbers() {
            // Arrange
            String fieldName = "field123";

            // Act
            String getterName = Utils.getGetterName(fieldName);

            // Assert
            assertThat(getterName)
                    .isEqualTo("getField123");
        }
    }

    @Nested
    class CheckIfStaticMethodExistsTest {

        @Test
        void testStaticMethodExistsWithMatchingNameAndParameters() {
            // Arrange
            Class<TestStaticClass> clazz = TestStaticClass.class;
            String methodName = "staticMethod";
            List<Class<?>> parameterTypes = List.of(String.class);

            // Act
            boolean result = Utils.checkIfStaticMethodExists(clazz, methodName, parameterTypes);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        void testStaticMethodDoesNotExistWithNonMatchingName() {
            // Arrange
            Class<TestStaticClass> clazz = TestStaticClass.class;
            String methodName = "nonExistentMethod";
            List<Class<?>> parameterTypes = List.of(String.class);

            // Act
            boolean result = Utils.checkIfStaticMethodExists(clazz, methodName, parameterTypes);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        void testStaticMethodExistsWithNoParameters() {
            // Arrange
            Class<TestStaticClass> clazz = TestStaticClass.class;
            String methodName = "staticMethodNoParams";
            List<Class<?>> parameterTypes = List.of();

            // Act
            boolean result = Utils.checkIfStaticMethodExists(clazz, methodName, parameterTypes);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        void testStaticMethodDoesNotExistWithNonMatchingParameterTypes() {
            // Arrange
            Class<TestStaticClass> clazz = TestStaticClass.class;
            String methodName = "staticMethod";
            List<Class<?>> parameterTypes = List.of(Integer.class);

            // Act
            boolean result = Utils.checkIfStaticMethodExists(clazz, methodName, parameterTypes);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        void testStaticMethodDoesNotExistMisMatchParameterLength() {
            // Arrange
            Class<TestStaticClass> clazz = TestStaticClass.class;
            String methodName = "staticMethod";
            List<Class<?>> parameterTypes = List.of(Integer.class, Integer.class);

            // Act
            boolean result = Utils.checkIfStaticMethodExists(clazz, methodName, parameterTypes);

            // Assert
            assertThat(result).isFalse();
        }

        // Helper class for testing static method checks
        static class TestStaticClass {
            public static void staticMethod(String input) {
            }

            public static void staticMethodNoParams() {
            }

            public void instanceMethod() {
            }
        }
    }

    @Nested
    class OptionalTest {

        @Test
        void testOptionalGathererCollectsPresentValues() {
            // Arrange
            Gatherer<? super Optional<String>, Void, String> gatherer = Utils.optional();
            List<Optional<String>> optionalList = List.of(
                    Optional.of("First"),
                    Optional.empty(),
                    Optional.of("Third")
            );

            // Act
            List<String> results = optionalList.stream().gather(gatherer).toList();

            // Assert
            assertThat(results)
                    .containsExactly("First", "Third");
        }

        @Test
        void testOptionalGathererEmptyResults() {
            // Arrange
            Gatherer<? super Optional<String>, Void, String> gatherer = Utils.optional();
            List<Optional<String>> optionalList = List.of(
                    Optional.empty(),
                    Optional.empty()
            );

            // Act
            List<String> results = optionalList.stream().gather(gatherer).toList();

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        void testOptionalGathererMixOfPresentAndEmpty() {
            // Arrange
            Gatherer<? super Optional<Integer>, Void, Integer> gatherer = Utils.optional();
            List<Optional<Integer>> optionalList = List.of(
                    Optional.of(1),
                    Optional.of(2),
                    Optional.empty(),
                    Optional.of(3)
            );

            // Act
            List<Integer> results = optionalList.stream().gather(gatherer).toList();

            // Assert
            assertThat(results)
                    .containsExactly(1, 2, 3);
        }
    }

    @Nested
    class GetFunctionalMethodTest {

        @Test
        void withFunctionalInterfaceReturnFunctionalMethod() {
            // Act
            Method method = Utils.getFunctionalMethod(TestFunctionalInterface.class);

            // Assert
            assertThat(method)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", "method");
        }

        @Test
        void withNonFunctionalInterfaceThrowIllegalStateException() {
            // Act & Assert
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> Utils.getFunctionalMethod(TestNonFunctionalInterface.class))
                    .withMessage("Expected exactly one functional interface method, found %d".formatted(2));
        }


        interface TestFunctionalInterface {
            static void staticMethod() {
            }

            void method();

            default void defaultMethod() {
                method();
            }

            private void privateMethod() {
            }
        }

        interface TestNonFunctionalInterface {
            void method();

            void anotherMethod();
        }
    }

    @Nested
    class FindTypeForNameTest {

        @Test
        void existingClassReturnClass() {
            // Act
            Optional<Class<?>> result = Utils.findTypeForName("java.lang.String");

            // Assert
            assertThat(result)
                    .isPresent()
                    .get()
                    .isEqualTo(String.class);
        }

        @Test
        void nonExistingClassReturnEmpty() {
            // Act
            Optional<Class<?>> result = Utils.findTypeForName("java.lang.NonExistingClass");

            // Assert
            assertThat(result)
                    .isEmpty();
        }
    }

    @Nested
    class GetFirstLambdaTest {

        @Test
        void checkLambda() {
            // Arrange
            String alpha = "alpha";
            String beta = "beta";

            // Act
            BinaryOperator<String> firstLambda = Utils.getFirstLambda();

            // Assert
            assertThat(firstLambda.apply(alpha, beta))
                    .isEqualTo(alpha);
        }
    }

    @Nested
    class ClassFinderTest {

        @Test
        void verifyClassFinder() {
            // Arrange
            List<String> classNames = List.of(
                    "java.lang.String",
                    "java.lang.NonExistingClass"
            );

            // Act
            List<? extends Class<?>> classes = classNames.stream()
                    .gather(Utils.CLASS_FINDER)
                    .toList();

            // Assert
            assertThat(classes)
                    .isNotEmpty()
                    .hasSize(1)
                    .satisfiesExactly(
                            clazz -> assertThat(clazz)
                                    .isEqualTo(String.class)
                    );
        }

    }

    @Nested
    class FindGetterListAndReturnElementTypeTest {

        @Test
        void shouldReturnElementTypeFromParameterizedList() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getStringList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldReturnElementTypeFromParameterizedSet() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getIntegerSet";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(Integer.class);
        }

        @Test
        void shouldReturnRawTypeForNonParameterizedCollection() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getRawList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            System.out.println(result);
            assertThat(result)
                    .isInstanceOf(Class.class);
        }

        @Test
        void shouldThrowExceptionWhenMethodDoesNotExist() {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getNonExistentMethod";

            // When & Then
            assertThatThrownBy(() -> Utils.findGetterListAndReturnElementType(testClass, methodName))
                    .isInstanceOf(NoSuchMethodException.class);
        }

        @Test
        void shouldThrowExceptionWhenMethodDoesNotReturnCollection() {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getNonCollectionMethod";

            // When & Then
            assertThatThrownBy(() -> Utils.findGetterListAndReturnElementType(testClass, methodName))
                    .isInstanceOf(NoSuchMethodException.class)
                    .hasMessage("Not a collection getter: getNonCollectionMethod");
        }

        @Test
        void shouldHandleWildcardWithUpperBound() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getWildcardUpperBoundList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(Number.class);
        }

        @Test
        void shouldHandleWildcardWithLowerBound() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getWildcardLowerBoundList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(Integer.class);
        }

        @Test
        void shouldHandleNestedGenericTypes() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getNestedGenericList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isInstanceOf(ParameterizedType.class);
            ParameterizedType paramType = (ParameterizedType) result;
            assertThat(paramType.getRawType()).isEqualTo(List.class);
        }

        @Test
        void shouldHandleArrayList() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getArrayList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(String.class);
        }

        @Test
        void shouldHandleQueue() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getQueue";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            assertThat(result).isEqualTo(Double.class);
        }

        @Test
        void shouldHandleComplexWildcardType() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getComplexWildcardList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            // For complex wildcards that don't fit the simple patterns,
            // the method should return the original wildcard type
            assertThat(result)
                    .isInstanceOf(Class.class)
                    .isEqualTo(Object.class);
        }

        @Test
        void shouldHandleSpecialListTypeWithMultipleWildcards() throws NoSuchMethodException {
            // Given
            Class<?> testClass = TestClassWithGetters.class;
            String methodName = "getSpecialList";

            // When
            Type result = Utils.findGetterListAndReturnElementType(testClass, methodName);

            // Then
            // For complex wildcards that don't fit the simple patterns,
            // the method should return the original wildcard type
            assertThat(result)
                    .isInstanceOf(Class.class)
                    .isEqualTo(Object.class);
        }


        // Helper test class with various getter methods
        static class TestClassWithGetters {

            public List<String> getStringList() {
                return new ArrayList<>();
            }

            public Set<Integer> getIntegerSet() {
                return new HashSet<>();
            }

            @SuppressWarnings("rawtypes")
            public List getRawList() {
                return new ArrayList();
            }

            public String getNonCollectionMethod() {
                return "not a collection";
            }

            public List<? extends Number> getWildcardUpperBoundList() {
                return new ArrayList<>();
            }

            public List<? super Integer> getWildcardLowerBoundList() {
                return new ArrayList<>();
            }

            public List<List<String>> getNestedGenericList() {
                return new ArrayList<>();
            }

            public ArrayList<String> getArrayList() {
                return new ArrayList<>();
            }

            public Queue<Double> getQueue() {
                return new LinkedList<>();
            }

            // Complex wildcard that has multiple bounds or doesn't fit simple patterns
            public List<?> getComplexWildcardList() {
                return new ArrayList<>();
            }

            public SpecialList<String, Integer> getSpecialList() {
                return new SpecialList<>();
            }

            public static class SpecialList<T, X> extends ArrayList<T> implements List<T>, BinaryOperator<X> {

                @Override
                public X apply(X x, X x2) {
                    return null;
                }
            }
        }
    }

    @Nested
    class IsAssignableFromTest {

        @ParameterizedTest
        @CsvSource({
                // Primitive to wrapper (happy paths)
                "int, java.lang.Integer",
                "long, java.lang.Long",
                "short, java.lang.Short",
                "byte, java.lang.Byte",
                "float, java.lang.Float",
                "double, java.lang.Double",
                "boolean, java.lang.Boolean",
                "char, java.lang.Character",
                // Wrapper to primitive (happy paths)
                "java.lang.Integer, int",
                "java.lang.Long, long",
                "java.lang.Short, short",
                "java.lang.Byte, byte",
                "java.lang.Float, float",
                "java.lang.Double, double",
                "java.lang.Boolean, boolean",
                "java.lang.Character, char",
                // Primitive to primitive (same type)
                "int, int",
                "long, long",
                "short, short",
                "byte, byte",
                "float, float",
                "double, double",
                "boolean, boolean",
                "char, char",
                // Wrapper to wrapper (same type)
                "java.lang.Integer, java.lang.Integer",
                "java.lang.Long, java.lang.Long",
                "java.lang.Short, java.lang.Short",
                "java.lang.Byte, java.lang.Byte",
                "java.lang.Float, java.lang.Float",
                "java.lang.Double, java.lang.Double",
                "java.lang.Boolean, java.lang.Boolean",
                "java.lang.Character, java.lang.Character",
                // Standard class hierarchy (happy paths)
                "java.lang.Object, java.lang.String",
                "java.lang.Number, java.lang.Integer",
                "java.lang.Number, java.lang.Double",
                "java.util.Collection, java.util.List",
                "java.util.List, java.util.ArrayList",
                "java.lang.CharSequence, java.lang.String"
        })
        void shouldReturnTrueForAssignableTypes(String variableClassName, String expressionClassName) throws ClassNotFoundException {
            // Given
            Class<?> variable = getClassForName(variableClassName);
            Class<?> expression = getClassForName(expressionClassName);

            // When
            boolean result = Utils.isAssignableFrom(variable, expression);

            // Then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                // Primitive to different primitive (unhappy paths)
                "int, long",
                "int, short",
                "int, byte",
                "long, int",
                "short, int",
                "byte, int",
                "float, double",
                "double, float",
                "boolean, int",
                "int, boolean",
                "char, int",
                "int, char",
                // Primitive to different wrapper (unhappy paths)
                "int, java.lang.Long",
                "int, java.lang.Short",
                "int, java.lang.Byte",
                "long, java.lang.Integer",
                "short, java.lang.Integer",
                "byte, java.lang.Integer",
                "float, java.lang.Double",
                "double, java.lang.Float",
                "boolean, java.lang.Integer",
                "char, java.lang.Integer",
                // Wrapper to different primitive (unhappy paths)
                "java.lang.Integer, long",
                "java.lang.Integer, short",
                "java.lang.Integer, byte",
                "java.lang.Long, int",
                "java.lang.Short, int",
                "java.lang.Byte, int",
                "java.lang.Float, double",
                "java.lang.Double, float",
                "java.lang.Boolean, int",
                "java.lang.Character, int",
                // Wrapper to different wrapper (unhappy paths)
                "java.lang.Integer, java.lang.Long",
                "java.lang.Integer, java.lang.Short",
                "java.lang.Integer, java.lang.Byte",
                "java.lang.Long, java.lang.Integer",
                "java.lang.Short, java.lang.Integer",
                "java.lang.Byte, java.lang.Integer",
                "java.lang.Float, java.lang.Double",
                "java.lang.Double, java.lang.Float",
                "java.lang.Boolean, java.lang.Integer",
                "java.lang.Character, java.lang.Integer",
                // Incompatible class hierarchies (unhappy paths)
                "java.lang.String, java.lang.Integer",
                "java.lang.Integer, java.lang.String",
                "java.util.List, java.util.Set",
                "java.util.ArrayList, java.util.LinkedList",
                "java.lang.Number, java.lang.String",
                "java.lang.String, java.lang.Number"
        })
        void shouldReturnFalseForNonAssignableTypes(String variableClassName, String expressionClassName) throws ClassNotFoundException {
            // Given
            Class<?> variable = getClassForName(variableClassName);
            Class<?> expression = getClassForName(expressionClassName);

            // When
            boolean result = Utils.isAssignableFrom(variable, expression);

            // Then
            assertThat(result).isFalse();
        }

        private Class<?> getClassForName(String className) throws ClassNotFoundException {
            return switch (className) {
                case "int" -> int.class;
                case "long" -> long.class;
                case "short" -> short.class;
                case "byte" -> byte.class;
                case "float" -> float.class;
                case "double" -> double.class;
                case "boolean" -> boolean.class;
                case "char" -> char.class;
                default -> Class.forName(className);
            };
        }
    }

    @Nested
    class IsAssignableToTest {

        @Test
        void shouldReturnTrueWhenClassIsExactMatch() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(String.class);

            // When
            boolean result = predicate.test(String.class);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrueWhenClassIsSubclass() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Number.class);

            // When
            boolean result = predicate.test(Integer.class);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrueWhenClassImplementsInterface() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(List.class);

            // When
            boolean result = predicate.test(ArrayList.class);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseWhenClassIsNotAssignable() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(String.class);

            // When
            boolean result = predicate.test(Integer.class);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnTrueWhenClassMatchesAnyOfMultipleTargets() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(String.class, Number.class, List.class);

            // When & Then
            assertThat(predicate.test(String.class)).isTrue();
            assertThat(predicate.test(Integer.class)).isTrue();  // Integer extends Number
            assertThat(predicate.test(ArrayList.class)).isTrue(); // ArrayList implements List
        }

        @Test
        void shouldReturnFalseWhenClassMatchesNoneOfMultipleTargets() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(String.class, Number.class);

            // When
            boolean result = predicate.test(Boolean.class);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void shouldHandleInterfaceHierarchy() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Collection.class);

            // When & Then
            assertThat(predicate.test(List.class)).isTrue();     // List extends Collection
            assertThat(predicate.test(Set.class)).isTrue();      // Set extends Collection
            assertThat(predicate.test(ArrayList.class)).isTrue(); // ArrayList implements List -> Collection
            assertThat(predicate.test(HashSet.class)).isTrue();   // HashSet implements Set -> Collection
        }

        @Test
        void shouldHandleClassHierarchy() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Object.class);

            // When & Then
            assertThat(predicate.test(String.class)).isTrue();
            assertThat(predicate.test(Integer.class)).isTrue();
            assertThat(predicate.test(ArrayList.class)).isTrue();
            assertThat(predicate.test(Object.class)).isTrue();
        }

        @Test
        void shouldHandlePrimitiveTypes() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(int.class);

            // When & Then
            assertThat(predicate.test(int.class)).isTrue();
            assertThat(predicate.test(Integer.class)).isTrue();
        }

        @Test
        void shouldHandleWrapperTypes() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Integer.class);

            // When & Then
            assertThat(predicate.test(Integer.class)).isTrue();
            assertThat(predicate.test(Integer.class)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNoTargetClassesProvided() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo();

            // When
            boolean result = predicate.test(String.class);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        void shouldHandleArrayTypes() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Object[].class);

            // When & Then
            assertThat(predicate.test(String[].class)).isTrue();  // String[] is assignable to Object[]
            assertThat(predicate.test(Integer[].class)).isTrue(); // Integer[] is assignable to Object[]
            assertThat(predicate.test(int[].class)).isFalse();    // int[] is not assignable to Object[]
        }

        @Test
        void shouldHandleComplexInheritanceChain() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(AbstractList.class);

            // When & Then
            assertThat(predicate.test(ArrayList.class)).isTrue();   // ArrayList extends AbstractList
            assertThat(predicate.test(LinkedList.class)).isTrue();  // LinkedList extends AbstractList
            assertThat(predicate.test(HashSet.class)).isFalse();    // HashSet does not extend AbstractList
        }

        @Test
        void shouldWorkWithCustomInterfacesAndClasses() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(TestInterface.class, TestBaseClass.class);

            // When & Then
            assertThat(predicate.test(TestImplementation.class)).isTrue();   // Implements TestInterface
            assertThat(predicate.test(TestSubClass.class)).isTrue();         // Extends TestBaseClass
            assertThat(predicate.test(TestMultipleInheritance.class)).isTrue(); // Extends TestBaseClass and implements TestInterface
            assertThat(predicate.test(String.class)).isFalse();              // Neither implements nor extends
        }

        @Test
        void shouldHandleGenericTypes() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Map.class);

            // When & Then
            assertThat(predicate.test(HashMap.class)).isTrue();
            assertThat(predicate.test(TreeMap.class)).isTrue();
            assertThat(predicate.test(ConcurrentHashMap.class)).isTrue();
            assertThat(predicate.test(ArrayList.class)).isFalse();
        }

        @Test
        void shouldBeReusableAndStateless() {
            // Given
            Predicate<Class<?>> predicate = Utils.isAssignableTo(Collection.class);

            // When - Multiple uses of the same predicate
            boolean result1 = predicate.test(ArrayList.class);
            boolean result2 = predicate.test(String.class);
            boolean result3 = predicate.test(HashSet.class);

            // Then - Results should be consistent
            assertThat(result1).isTrue();
            assertThat(result2).isFalse();
            assertThat(result3).isTrue();

            // And using it again should give same results
            assertThat(predicate.test(ArrayList.class)).isTrue();
            assertThat(predicate.test(String.class)).isFalse();
        }

        // Helper interfaces and classes for testing
        interface TestInterface {
            void testMethod();
        }

        static class TestBaseClass {
            protected String value;
        }

        static class TestImplementation implements TestInterface {
            @Override
            public void testMethod() {
                // Implementation
            }
        }

        static class TestSubClass extends TestBaseClass {
            public TestSubClass() {
                this.value = "subclass";
            }
        }

        static class TestMultipleInheritance extends TestBaseClass implements TestInterface {
            @Override
            public void testMethod() {
                // Implementation
            }
        }
    }

    @Nested
    class FindObjectSettersTest {

        @Test
        void shouldFindSingleSetterMethod() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setName";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "setName")
                    .hasFieldOrPropertyWithValue("parameterCount", 1)
                    .satisfies(method -> assertThat(method.getParameterTypes()).containsExactly(String.class));
        }

        @Test
        void shouldFindMultipleOverloadedSetters() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setValue";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(method -> method.getName().equals("setValue"));
            assertThat(result).allMatch(method -> method.getParameterCount() == 1);

            Set<Class<?>> parameterTypes = result.stream()
                    .map(method -> method.getParameterTypes()[0])
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(parameterTypes).containsExactlyInAnyOrder(String.class, Integer.class);
        }

        @Test
        void shouldReturnEmptyListWhenNoSetterFound() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setNonExistentProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeStaticMethods() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setStaticProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeMethodsWithoutParameters() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setNoParam";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeMethodsWithMultipleParameters() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setMultipleParams";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldFindInheritedSetters() {
            // Given
            Class<?> clazz = TestSubClassWithSetters.class;
            String setterName = "setParentProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "setParentProperty")
                    .hasFieldOrPropertyWithValue("declaringClass", TestParentClassWithSetters.class);
        }

        @Test
        void shouldFindBothInheritedAndOwnSetters() {
            // Given
            Class<?> clazz = TestSubClassWithSetters.class;
            String setterName = "setSharedProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            // Should find both the inherited method and the overridden method
            // but getMethods() returns only the overridden one
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "setSharedProperty")
                    .hasFieldOrPropertyWithValue("declaringClass", TestSubClassWithSetters.class);
        }

        @Test
        void shouldHandleGenericParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setGenericList";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "setGenericList")
                    .satisfies(method -> assertThat(method.getParameterTypes()).containsExactly(List.class));
            ;
        }

        @Test
        void shouldHandleInterfaceSetters() {
            // Given
            Class<?> clazz = TestInterfaceImplementation.class;
            String setterName = "setInterfaceProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("name", "setInterfaceProperty");
        }

        @Test
        void shouldFindPublicProtectedAndPackageSetters() {
            // Given
            Class<?> clazz = TestClassWithDifferentVisibilities.class;
            String publicSetterName = "setPublicProperty";
            String protectedSetterName = "setProtectedProperty";
            String packageSetterName = "setPackageProperty";

            // When
            List<Method> publicResult = Utils.findObjectSetters(clazz, publicSetterName);
            List<Method> protectedResult = Utils.findObjectSetters(clazz, protectedSetterName);
            List<Method> packageResult = Utils.findObjectSetters(clazz, packageSetterName);

            // Then
            assertThat(publicResult).hasSize(1);
            assertThat(protectedResult).isEmpty(); // getMethods() only returns public methods
            assertThat(packageResult).isEmpty(); // getMethods() only returns public methods
        }

        @Test
        void shouldExcludePrivateSetters() {
            // Given
            Class<?> clazz = TestClassWithDifferentVisibilities.class;
            String setterName = "setPrivateProperty";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty(); // getMethods() doesn't return private methods
        }

        @Test
        void shouldHandlePrimitiveParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setPrimitiveValue";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(method -> assertThat(method.getParameterTypes()).containsExactly(int.class));
        }

        @Test
        void shouldHandleArrayParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setArrayValue";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(method -> assertThat(method.getParameterTypes()).containsExactly(String[].class));
        }

        @Test
        void shouldReturnEmptyListForNullSetterName() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = null;

            // When & Then
            assertThatThrownBy(() -> Utils.findObjectSetters(clazz, setterName))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldHandleEmptySetterName() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "";

            // When
            List<Method> result = Utils.findObjectSetters(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnMethodsInConsistentOrder() {
            // Given
            Class<?> clazz = TestClassWithSetters.class;
            String setterName = "setValue";

            // When - Call multiple times
            List<Method> result1 = Utils.findObjectSetters(clazz, setterName);
            List<Method> result2 = Utils.findObjectSetters(clazz, setterName);

            // Then - Results should be consistent (same size and same methods)
            assertThat(result1).hasSize(result2.size());
            assertThat(result1).containsExactlyElementsOf(result2);
        }

        interface TestSetterInterface {
            void setInterfaceProperty(String value);
        }

        // Helper classes for testing
        static class TestClassWithSetters {
            public static void setStaticProperty(String value) {
                // Static setter - should be excluded
            }

            public void setValue(String value) {
                // Overloaded setter 1
            }

            public void setValue(Integer value) {
                // Overloaded setter 2
            }

            public void setNoParam() {
                // No parameters - should be excluded
            }

            public void setMultipleParams(String param1, String param2) {
                // Multiple parameters - should be excluded
            }

            public void setGenericList(List<String> list) {
                // Generic parameter type
            }

            public void setPrimitiveValue(int value) {
                // Primitive parameter type
            }

            public void setArrayValue(String[] array) {
                // Array parameter type
            }

            // Non-setter methods that should be ignored
            public String getName() {
                return null;
            }

            public void setName(String name) {
                // Single parameter setter
            }

            public void doSomething(String param) {
                // Not a setter
            }
        }

        static class TestParentClassWithSetters {
            public void setParentProperty(String value) {
                // Parent setter
            }

            public void setSharedProperty(String value) {
                // Will be overridden in subclass
            }
        }

        static class TestSubClassWithSetters extends TestParentClassWithSetters {
            @Override
            public void setSharedProperty(String value) {
                // Overridden setter
            }

            public void setChildProperty(String value) {
                // Child-specific setter
            }
        }

        static class TestInterfaceImplementation implements TestSetterInterface {
            @Override
            public void setInterfaceProperty(String value) {
                // Interface implementation
            }
        }

        static class TestClassWithDifferentVisibilities {
            public void setPublicProperty(String value) {
                // Public setter
            }

            protected void setProtectedProperty(String value) {
                // Protected setter
            }

            void setPackageProperty(String value) {
                // Package-private setter
            }

            @SuppressWarnings("unused")
            private void setPrivateProperty(String value) {
                // Private setter
            }
        }
    }

    @Nested
    class FindStaticSettersForNodeTest {

        @Test
        void shouldFindValidStaticSetterForNode() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setNodeProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getName()).isEqualTo("setNodeProperty");
            assertThat(method.getParameterCount()).isEqualTo(2);
            assertThat(method.getParameterTypes()[0]).isEqualTo(Node.class);
            assertThat(method.getParameterTypes()[1]).isEqualTo(String.class);
            assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
        }

        @Test
        void shouldFindMultipleOverloadedStaticSettersForNode() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setOverloadedProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(method -> method.getName().equals("setOverloadedProperty"));
            assertThat(result).allMatch(method -> method.getParameterCount() == 2);
            assertThat(result).allMatch(method -> method.getParameterTypes()[0] == Node.class);
            assertThat(result).allMatch(method -> Modifier.isStatic(method.getModifiers()));

            Set<Class<?>> secondParameterTypes = result.stream()
                    .map(method -> method.getParameterTypes()[1])
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(secondParameterTypes).containsExactlyInAnyOrder(String.class, Double.class);
        }

        @Test
        void shouldReturnEmptyListWhenNoMatchingMethodFound() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setNonExistentProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeNonStaticMethods() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setInstanceProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeMethodsWithWrongParameterCount() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String oneParamSetterName = "setOneParam";
            String threeParamSetterName = "setThreeParams";

            // When
            List<Method> oneParamResult = Utils.findStaticSettersForNode(clazz, oneParamSetterName);
            List<Method> threeParamResult = Utils.findStaticSettersForNode(clazz, threeParamSetterName);

            // Then
            assertThat(oneParamResult).isEmpty();
            assertThat(threeParamResult).isEmpty();
        }

        @Test
        void shouldExcludeMethodsWhereFirstParameterIsNotNodeAssignable() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setNonNodeProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleGenericParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setGenericProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getParameterTypes()[1]).isEqualTo(List.class);
        }

        @Test
        void shouldHandlePrimitiveParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setPrimitiveProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getParameterTypes()[1]).isEqualTo(int.class);
        }

        @Test
        void shouldHandleArrayParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setArrayProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getParameterTypes()[1]).isEqualTo(String[].class);
        }

        @Test
        void shouldFindInheritedStaticMethods() {
            // Given
            Class<?> clazz = TestSubClassWithStaticSetters.class;
            String setterName = "setParentStaticProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getDeclaringClass()).isEqualTo(TestParentClassWithStaticSetters.class);
        }

        @Test
        void shouldReturnEmptyListForNullSetterName() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = null;

            // When & Then
            assertThatThrownBy(() -> Utils.findStaticSettersForNode(clazz, setterName))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldHandleEmptySetterName() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnMethodsInConsistentOrder() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setOverloadedProperty";

            // When - Call multiple times
            List<Method> result1 = Utils.findStaticSettersForNode(clazz, setterName);
            List<Method> result2 = Utils.findStaticSettersForNode(clazz, setterName);

            // Then - Results should be consistent
            assertThat(result1).hasSize(result2.size());
            assertThat(result1).containsExactlyElementsOf(result2);
        }

        @Test
        void shouldWorkWithRealJavaFXStaticSetters() {
            // Given - Test with actual JavaFX class that has static setters
            Class<?> clazz = javafx.scene.layout.GridPane.class;
            String setterName = "setRowIndex";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getName()).isEqualTo("setRowIndex");
            assertThat(method.getParameterCount()).isEqualTo(2);
            assertThat(Node.class.isAssignableFrom(method.getParameterTypes()[0])).isTrue();
            assertThat(method.getParameterTypes()[1]).isEqualTo(Integer.class);
        }

        @Test
        void shouldWorkWithAnotherRealJavaFXStaticSetter() {
            // Given - Test with another actual JavaFX class
            Class<?> clazz = javafx.scene.layout.GridPane.class;
            String setterName = "setColumnIndex";

            // When
            List<Method> result = Utils.findStaticSettersForNode(clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.get(0);
            assertThat(method.getName()).isEqualTo("setColumnIndex");
            assertThat(method.getParameterTypes()[1]).isEqualTo(Integer.class);
        }

        // Helper classes for testing
        static class TestClassWithStaticSettersForNode {

            // Valid static setter for Node
            public static void setNodeProperty(Node node, String value) {
                // Valid static setter
            }

            // Overloaded static setters
            public static void setOverloadedProperty(Node node, String value) {
                // Overloaded setter 1
            }

            public static void setOverloadedProperty(Node node, Double value) {
                // Overloaded setter 2
            }

            // Wrong parameter count - should be excluded
            public static void setOneParam(Node node) {
                // Only one parameter
            }

            public static void setThreeParams(Node node, String value1, String value2) {
                // Three parameters
            }

            // First parameter is not Node-assignable - should be excluded
            public static void setNonNodeProperty(String notANode, String value) {
                // First parameter is not Node
            }

            // Generic parameter type
            public static void setGenericProperty(Node node, List<String> value) {
                // Generic second parameter
            }

            // Primitive parameter type
            public static void setPrimitiveProperty(Node node, int value) {
                // Primitive second parameter
            }

            // Array parameter type
            public static void setArrayProperty(Node node, String[] values) {
                // Array second parameter
            }

            // Non-setter methods that should be ignored
            public static Node getNodeProperty() {
                return null;
            }

            public static void doSomething(Node node, String value) {
                // Not a setter name
            }

            // Non-static method - should be excluded
            public void setInstanceProperty(Node node, String value) {
                // Instance method, not static
            }
        }

        static class TestParentClassWithStaticSetters {
            public static void setParentStaticProperty(Node node, String value) {
                // Parent static setter
            }
        }

        static class TestSubClassWithStaticSetters extends TestParentClassWithStaticSetters {
            public static void setChildStaticProperty(Node node, String value) {
                // Child static setter
            }
        }
    }

    @Nested
    class FindParameterTypeForConstructorsTest {

        @Test
        void shouldFindParameterTypeInInsetsConstructorWithAllNamedArgs() {
            // Given - Insets has constructors with @NamedArg annotations
            Class<?> clazz = javafx.geometry.Insets.class;
            String propertyName = "top";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .containsExactly(double.class);
        }

        @Test
        void shouldFindParameterTypeForDifferentInsetsProperties() {
            // Given
            Class<?> clazz = javafx.geometry.Insets.class;

            // When & Then - Test different properties
            assertThat(Utils.findParameterTypeForConstructors(clazz, "top"))
                    .hasSize(1)
                    .containsExactly(double.class);

            assertThat(Utils.findParameterTypeForConstructors(clazz, "right"))
                    .hasSize(1)
                    .containsExactly(double.class);

            assertThat(Utils.findParameterTypeForConstructors(clazz, "bottom"))
                    .hasSize(1)
                    .containsExactly(double.class);

            assertThat(Utils.findParameterTypeForConstructors(clazz, "left"))
                    .hasSize(1)
                    .containsExactly(double.class);
        }

        @Test
        void shouldReturnEmptyListForArrayListWithNoNamedArgs() {
            // Given - ArrayList constructors don't use @NamedArg annotations
            Class<?> clazz = java.util.ArrayList.class;
            String propertyName = "initialCapacity";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForNonExistentPropertyInInsets() {
            // Given
            Class<?> clazz = javafx.geometry.Insets.class;
            String propertyName = "nonExistentProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleMixedNamedAndNonNamedArgsInTestClass() {
            // Given
            Class<?> clazz = TestClassWithMixedConstructors.class;
            String namedProperty = "namedProperty";
            String unnamedProperty = "unnamedProperty";

            // When
            List<Type> namedResult = Utils.findParameterTypeForConstructors(clazz, namedProperty);
            List<Type> unnamedResult = Utils.findParameterTypeForConstructors(clazz, unnamedProperty);

            // Then
            // Should find the named property
            assertThat(namedResult)
                    .hasSize(1)
                    .containsExactly(String.class);

            // Should not find the unnamed property (constructor with mixed args is excluded)
            assertThat(unnamedResult).isEmpty();
        }

        @Test
        void shouldOnlyConsiderPublicConstructors() {
            // Given
            Class<?> clazz = TestClassWithPrivateConstructor.class;
            String propertyName = "privateProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result).isEmpty(); // Private constructor should be ignored
        }

        @Test
        void shouldReturnDistinctTypes() {
            // Given
            Class<?> clazz = TestClassWithDuplicateParameterTypes.class;
            String propertyName = "duplicateProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .containsExactly(String.class);
        }

        @Test
        void shouldHandleGenericParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithGenericConstructor.class;
            String propertyName = "genericProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ParameterizedType.class)
                    .extracting(ParameterizedType.class::cast)
                    .returns(List.class, ParameterizedType::getRawType);
        }

        @Test
        void shouldHandleMultipleConstructorsWithSameProperty() {
            // Given
            Class<?> clazz = TestClassWithMultipleValidConstructors.class;
            String propertyName = "commonProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(String.class, Integer.class);
        }

        @Test
        void shouldIgnoreConstructorsWithNoParameters() {
            // Given
            Class<?> clazz = TestClassWithNoArgsConstructor.class;
            String propertyName = "anyProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyListForNullPropertyName() {
            // Given
            Class<?> clazz = javafx.geometry.Insets.class;
            String propertyName = null;

            // When & Then
            assertThatThrownBy(() -> Utils.findParameterTypeForConstructors(clazz, propertyName))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldReturnEmptyListForEmptyPropertyName() {
            // Given
            Class<?> clazz = javafx.geometry.Insets.class;
            String propertyName = "";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleArrayParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithArrayParameter.class;
            String propertyName = "arrayProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .containsExactly(String[].class);
        }

        @Test
        void shouldWorkWithInheritedConstructors() {
            // Given
            Class<?> clazz = TestSubClassWithInheritedConstructors.class;
            String propertyName = "parentProperty";

            // When
            List<Type> result = Utils.findParameterTypeForConstructors(clazz, propertyName);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .containsExactly(String.class);
        }

        // Helper test classes for various scenarios
        static class TestClassWithMixedConstructors {

            // Constructor with all @NamedArg - should be considered
            public TestClassWithMixedConstructors(@NamedArg("namedProperty") String named) {
                // All parameters have @NamedArg
            }

            // Constructor with mixed named and unnamed args - should be ignored
            public TestClassWithMixedConstructors(@NamedArg("namedProperty") String named, String unnamed) {
                // Mixed parameters, should be filtered out
            }

            // Constructor with no @NamedArg - should be ignored
            public TestClassWithMixedConstructors(Double unnamed1, String unnamed2) {
                // No @NamedArg annotations
            }
        }

        static class TestClassWithPrivateConstructor {

            public TestClassWithPrivateConstructor() {
                // Public no-args constructor
            }

            // Private constructor with @NamedArg - should be ignored
            private TestClassWithPrivateConstructor(@NamedArg("privateProperty") String prop) {
                // Private constructor should be ignored
            }
        }

        static class TestClassWithDuplicateParameterTypes {

            // Two constructors with same property name and type
            public TestClassWithDuplicateParameterTypes(@NamedArg("duplicateProperty") String prop) {
                // Constructor 1
            }

            public TestClassWithDuplicateParameterTypes(@NamedArg("duplicateProperty") String prop, @NamedArg("otherProperty") String other) {
                // Constructor 2 with same property type
            }
        }

        static class TestClassWithGenericConstructor {

            public TestClassWithGenericConstructor(@NamedArg("genericProperty") List<String> genericProp) {
                // Generic parameter type
            }
        }

        static class TestClassWithMultipleValidConstructors {

            // Same property name but different types
            public TestClassWithMultipleValidConstructors(@NamedArg("commonProperty") String stringProp) {
                // String type
            }

            public TestClassWithMultipleValidConstructors(@NamedArg("commonProperty") Integer intProp, @NamedArg("otherProperty") String other) {
                // Integer type for same property name
            }
        }

        static class TestClassWithNoArgsConstructor {

            public TestClassWithNoArgsConstructor() {
                // No-args constructor should be ignored
            }
        }

        static class TestClassWithArrayParameter {

            public TestClassWithArrayParameter(@NamedArg("arrayProperty") String[] arrayProp) {
                // Array parameter type
            }
        }

        static class TestParentWithNamedConstructor {

            public TestParentWithNamedConstructor(@NamedArg("parentProperty") String parentProp) {
                // Parent constructor with @NamedArg
            }
        }

        static class TestSubClassWithInheritedConstructors extends TestParentWithNamedConstructor {

            public TestSubClassWithInheritedConstructors(@NamedArg("parentProperty") String parentProp) {
                super(parentProp);
            }

            public TestSubClassWithInheritedConstructors(@NamedArg("childProperty") String childProp, @NamedArg("parentProperty") String parentProp) {
                super(parentProp);
                // Child-specific constructor
            }
        }
    }

    @Nested
    class FindCollectionGetterWithAllowedReturnTypeTest {

        @Test
        void testFindCollectionGetterWithAllowedReturnType_ValidListGetter() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public List<String> getItems() {
                    return List.of();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "test identifier",
                    "getItems",
                    String.class
            );

            assertThat(returnType)
                    .isEqualTo(List.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_ValidSetGetter() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public Set<Number> getNumbers() {
                    return Set.of();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "number test",
                    "getNumbers",
                    Integer.class
            );

            assertThat(returnType)
                    .isEqualTo(Set.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_ValidParameterizedTypeGetter() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public Collection<List<String>> getNestedItems() {
                    return List.of();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "nested test",
                    "getNestedItems",
                    ArrayList.class  // ArrayList is assignable from List
            );

            assertThat(returnType)
                    .isEqualTo(Collection.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_MethodNotFound() {
            // Arrange
            class TestClass {
                public List<String> getItems() {
                    return List.of();
                }
            }

            // Act & Assert
            assertThatThrownBy(() -> Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "test identifier",
                    "getNonExistentMethod",
                    String.class
            )).isInstanceOf(NoSuchMethodException.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_NotCollectionReturnType() {
            // Arrange
            class TestClass {
                public String getItem() {
                    return "";
                }
            }

            // Act & Assert
            assertThatThrownBy(() -> Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "string test",
                    "getItem",
                    String.class
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to find list getter for string test");
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_RawCollectionType() {
            // Arrange
            class TestClass {
                @SuppressWarnings("rawtypes")
                public List getRawList() {
                    return List.of();
                }
            }

            // Act & Assert
            assertThatThrownBy(() -> Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "raw test",
                    "getRawList",
                    String.class
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to find list getter for raw test");
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_IncompatibleParameterType() {
            // Arrange
            class TestClass {
                public List<String> getItems() {
                    return List.of();
                }
            }

            // Act & Assert
            assertThatThrownBy(() -> Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "incompatible test",
                    "getItems",
                    Integer.class  // Integer is not assignable from String
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to find list getter for incompatible test");
        }

        @Test
        void testFindCollectionGetterWithNotAllowedReturnType_IncompatibleParameterType() {
            // Arrange
            class TestClass {
                public List<List<String>> getItems() {
                    return List.of();
                }
            }

            // Act & Assert
            assertThatThrownBy(() -> Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "incompatible test",
                    "getItems",
                    Integer.class  // Integer is not assignable from String
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to find list getter for incompatible test");
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_InheritedParameterType() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public List<Object> getObjects() {
                    return List.of();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "inheritance test",
                    "getObjects",
                    String.class  // String is assignable to Object
            );
            assertThat(returnType)
                    .isEqualTo(List.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedReturnType_ArrayListImplementation() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public ArrayList<String> getStringList() {
                    return new ArrayList<>();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "arraylist test",
                    "getStringList",
                    String.class
            );
            assertThat(returnType)
                    .isEqualTo(ArrayList.class);
        }

        @Test
        void testFindCollectionGetterWithAllowedParameterizedReturnType_ArrayListImplementation() throws NoSuchMethodException {
            // Arrange
            class TestClass {
                public ArrayList<Comparable<String>> getStringList() {
                    return new ArrayList<>();
                }
            }

            // Act & Assert
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(
                    TestClass.class,
                    "arraylist test",
                    "getStringList",
                    String.class
            );
            assertThat(returnType)
                    .isEqualTo(ArrayList.class);
        }
    }

    @Nested
    class UriToUrlTest {

        @Test
        void invalidUriThrowsRuntimeException() {
            URI uri = URI.create("invalidUri");
            assertThatThrownBy(() -> Utils.uriToUrl(uri))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasRootCauseMessage("URI is not absolute");
        }

        @Test
        void validUriConvertedToUrl() {
            URI uri = URI.create("https://www.google.com");
            assertThat(Utils.uriToUrl(uri))
                    .isNotNull()
                    .hasAuthority("www.google.com")
                    .hasNoQuery()
                    .hasNoParameters()
                    .hasNoUserInfo()
                    .hasHost("www.google.com")
                    .hasNoPort()
                    .hasProtocol("https");
        }
    }

    @Nested
    class GetTypeMapperFunctionTest {

        @Test
        void classReturnClass() {
            Function<Type, Type> function = Utils.getTypeMapperFunction(
                    new DefaultLog(new ConsoleLogger()),
                    Map.of()
            );

            assertThat(function)
                    .isNotNull();
            assertThat(function.apply(String.class))
                    .isEqualTo(String.class);
        }

        @Test
        void unknownTypeVariable() {
            Function<Type, Type> function = Utils.getTypeMapperFunction(
                    new DefaultLog(new ConsoleLogger()),
                    Map.of()
            );

            assertThat(function)
                    .isNotNull();
            assertThat(function.apply(getTypeVariable("unknownTypeVariable")))
                    .isEqualTo(Object.class);
        }

        @Test
        void namedTypeVariable() {
            String data = "data";
            Function<Type, Type> function = Utils.getTypeMapperFunction(
                    new DefaultLog(new ConsoleLogger()),
                    Map.of(data, Double.class)
            );

            assertThat(function)
                    .isNotNull();
            assertThat(function.apply(getTypeVariable(data)))
                    .isEqualTo(Double.class);
        }

        private TypeVariable<GenericDeclaration> getTypeVariable(String unknownTypeVariable) {
            return new TypeVariable<>() {
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
                    return unknownTypeVariable;
                }

                @Override
                public AnnotatedType[] getAnnotatedBounds() {
                    return new AnnotatedType[0];
                }
            };
        }
    }

    @Nested
    class UrlPathToOsPathStringTest {

        @Test
        void urlIsNotUriThrowsRuntimeException() throws URISyntaxException {
            // Given
            URISyntaxException syntaxException = new URISyntaxException("invalidUrl", "Invalid URL");
            URL url = Mockito.mock(URL.class);
            Mockito.when(url.toURI())
                    .thenThrow(syntaxException);

            // When & Then
            assertThatThrownBy(() -> Utils.urlPathToOsPathString(url))
                    .isInstanceOf(RuntimeException.class)
                    .hasRootCauseInstanceOf(URISyntaxException.class)
                    .hasRootCauseMessage("Invalid URL: invalidUrl");
        }

        @Test
        void urlIsValidPath() throws MalformedURLException {
            // Given
            Path path = Paths.get("testPath").toAbsolutePath();
            URL urlOfPath = path.toUri().toURL();

            // When & Then
            assertThat(Utils.urlPathToOsPathString(urlOfPath))
                    .isEqualTo(path.toString());
        }
    }
}