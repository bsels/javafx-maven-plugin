package io.github.bsels.javafx.maven.plugin.utils;

import javafx.beans.NamedArg;
import javafx.scene.Node;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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

        @Test
        void testGetClassTypeForGenericArrayType() {
            // Arrange
            Type type = new GenericArrayType() {
                @Override
                public Type getGenericComponentType() {
                    return String.class;
                }
            };

            // Act
            Class<?> classType = Utils.getClassType(type);

            // Assert
            assertThat(classType).isEqualTo(String[].class);
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

        @Test
        void testOptionalGathererStopsWhenDownstreamRejects() {
            // Arrange - limit(1) causes downstream to reject after first element
            Gatherer<? super Optional<String>, Void, String> gatherer = Utils.optional();
            List<Optional<String>> optionalList = List.of(
                    Optional.of("First"),
                    Optional.of("Second"),
                    Optional.of("Third")
            );

            // Act
            List<String> results = optionalList.stream().gather(gatherer).limit(1).toList();

            // Assert
            assertThat(results).containsExactly("First");
        }

        @Test
        void testOptionalGathererEmptyOptionalWhenDownstreamIsRejecting() {
            // Arrange - limit(1) causes downstream to reject; empty optional should return !isRejecting()
            Gatherer<? super Optional<String>, Void, String> gatherer = Utils.optional();
            List<Optional<String>> optionalList = List.of(
                    Optional.of("First"),
                    Optional.empty(),
                    Optional.of("Second")
            );

            // Act
            List<String> results = optionalList.stream().gather(gatherer).limit(1).toList();

            // Assert
            assertThat(results).containsExactly("First");
        }

        @Test
        void testOptionalGathererEmptyOptionalWithRejectingDownstreamViaIntegrator() {
            // Directly invoke the integrator with a rejecting downstream to cover the
            // orElseGet(() -> !downstream.isRejecting()) branch when isRejecting() == true
            Gatherer<Optional<String>, Void, String> gatherer = Utils.optional();
            Gatherer.Integrator<Void, Optional<String>, String> integrator = gatherer.integrator();

            Gatherer.Downstream<String> rejectingDownstream = new Gatherer.Downstream<>() {
                @Override
                public boolean push(String element) {
                    return false;
                }

                @Override
                public boolean isRejecting() {
                    return true;
                }
            };

            // Act - empty optional with rejecting downstream: orElseGet returns !true == false
            boolean result = integrator.integrate(null, Optional.empty(), rejectingDownstream);

            // Assert
            assertThat(result).isFalse();
        }

    }

    @Nested
    class UniqueGathererTest {

        @Test
        void shouldPassUniqueElements() {
            // Given
            List<String> input = List.of("a", "b", "c");

            // When
            List<String> result = input.stream().gather(Utils.unique()).toList();

            // Then
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void shouldFilterDuplicates() {
            // Given
            List<String> input = List.of("a", "b", "a", "c", "b");

            // When
            List<String> result = input.stream().gather(Utils.unique()).toList();

            // Then
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void shouldFilterNullElements() {
            // Given
            List<String> input = new ArrayList<>();
            input.add("a");
            input.add(null);
            input.add("b");

            // When
            List<String> result = input.stream().gather(Utils.unique()).toList();

            // Then
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void shouldReturnFalseWhenDownstreamRejectsOnDuplicate() {
            // Given - "a" is pushed (accepted), then "a" again is a duplicate so
            // isDownstreamAccepting is called; with limit(1) downstream is rejecting
            List<String> input = List.of("a", "a", "b");

            // When
            List<String> result = input.stream()
                    .gather(Utils.unique())
                    .limit(1)
                    .toList();

            // Then
            assertThat(result).containsExactly("a");
        }

        @Test
        void shouldReturnFalseWhenDownstreamRejectsOnNull() {
            // Given - "a" is pushed (accepted by limit(1)), then null is a duplicate-like
            // (not added to set), so isDownstreamAccepting is called with rejecting downstream
            List<String> input = new ArrayList<>();
            input.add("a");
            input.add(null);
            input.add("b");

            // When
            List<String> result = input.stream()
                    .gather(Utils.unique())
                    .limit(1)
                    .toList();

            // Then
            assertThat(result).containsExactly("a");
        }

        @Test
        void shouldReturnFalseFromIsDownstreamAcceptingWhenRejectingViaIntegrator() {
            // Directly invoke unique() integrator with a rejecting downstream and a duplicate
            // to cover the isDownstreamAccepting branch when isRejecting() == true
            Gatherer<String, Set<String>, String> gatherer = Utils.unique();
            Gatherer.Integrator<Set<String>, String, String> integrator = gatherer.integrator();

            Gatherer.Downstream<String> rejectingDownstream = new Gatherer.Downstream<>() {
                @Override
                public boolean push(String element) {
                    return false;
                }

                @Override
                public boolean isRejecting() {
                    return true;
                }
            };

            Set<String> state = new java.util.HashSet<>();
            // null element: Optional.ofNullable(null).filter(...) is empty, so orElseGet is called
            // with isRejecting() == true -> returns false
            boolean result = integrator.integrate(state, null, rejectingDownstream);

            assertThat(result).isFalse();
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
                // Static getter - should be excluded
            }

            public void setValue(String value) {
                // Overloaded getter 1
            }

            public void setValue(Integer value) {
                // Overloaded getter 2
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

            // Non-getter methods that should be ignored
            public String getName() {
                return null;
            }

            public void setName(String name) {
                // Single parameter getter
            }

            public void doSomething(String param) {
                // Not a getter
            }
        }

        static class TestParentClassWithSetters {
            public void setParentProperty(String value) {
                // Parent getter
            }

            public void setSharedProperty(String value) {
                // Will be overridden in subclass
            }
        }

        static class TestSubClassWithSetters extends TestParentClassWithSetters {
            @Override
            public void setSharedProperty(String value) {
                // Overridden getter
            }

            public void setChildProperty(String value) {
                // Child-specific getter
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
                // Public getter
            }

            protected void setProtectedProperty(String value) {
                // Protected getter
            }

            void setPackageProperty(String value) {
                // Package-private getter
            }

            @SuppressWarnings("unused")
            private void setPrivateProperty(String value) {
                // Private getter
            }
        }
    }

    @Nested
    class FindObjectGetterTest {

        @Test
        void shouldFindGetterWithNoParameters() {
            // Given
            Class<?> clazz = TestClassWithGetters.class;

            // When
            Optional<Method> result = Utils.findObjectGetter(clazz, "getName");

            // Then
            assertThat(result)
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "getName")
                    .hasFieldOrPropertyWithValue("parameterCount", 0);
        }

        @Test
        void shouldReturnEmptyWhenGetterHasParameters() {
            // Given - method named "getWithParam" but takes a parameter, so should be excluded
            Class<?> clazz = TestClassWithGetters.class;

            // When
            Optional<Method> result = Utils.findObjectGetter(clazz, "getWithParam");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyWhenGetterNotFound() {
            // Given
            Class<?> clazz = TestClassWithGetters.class;

            // When
            Optional<Method> result = Utils.findObjectGetter(clazz, "getNonExistent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeStaticGetters() {
            // Given
            Class<?> clazz = TestClassWithGetters.class;

            // When
            Optional<Method> result = Utils.findObjectGetter(clazz, "getStaticValue");

            // Then
            assertThat(result).isEmpty();
        }

        static class TestClassWithGetters {

            public static String getStaticValue() {
                return "static";
            }

            public String getName() {
                return "name";
            }

            public String getWithParam(String param) {
                return param;
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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldExcludeNonStaticMethods() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setInstanceProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

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
            List<Method> oneParamResult = Utils.findStaticSettersForNode(Node.class, clazz, oneParamSetterName);
            List<Method> threeParamResult = Utils.findStaticSettersForNode(Node.class, clazz, threeParamSetterName);

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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleGenericParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setGenericProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getParameterTypes()[1]).isEqualTo(List.class);
        }

        @Test
        void shouldHandlePrimitiveParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setPrimitiveProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getParameterTypes()[1]).isEqualTo(int.class);
        }

        @Test
        void shouldHandleArrayParameterTypes() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setArrayProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getParameterTypes()[1]).isEqualTo(String[].class);
        }

        @Test
        void shouldFindInheritedStaticMethods() {
            // Given
            Class<?> clazz = TestSubClassWithStaticSetters.class;
            String setterName = "setParentStaticProperty";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getDeclaringClass()).isEqualTo(TestParentClassWithStaticSetters.class);
        }

        @Test
        void shouldReturnEmptyListForNullSetterName() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = null;

            // When & Then
            assertThatThrownBy(() -> Utils.findStaticSettersForNode(Node.class, clazz, setterName))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldHandleEmptySetterName() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "";

            // When
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnMethodsInConsistentOrder() {
            // Given
            Class<?> clazz = TestClassWithStaticSettersForNode.class;
            String setterName = "setOverloadedProperty";

            // When - Call multiple times
            List<Method> result1 = Utils.findStaticSettersForNode(Node.class, clazz, setterName);
            List<Method> result2 = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
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
            List<Method> result = Utils.findStaticSettersForNode(Node.class, clazz, setterName);

            // Then
            assertThat(result).hasSize(1);
            Method method = result.getFirst();
            assertThat(method.getName()).isEqualTo("setColumnIndex");
            assertThat(method.getParameterTypes()[1]).isEqualTo(Integer.class);
        }

        // Helper classes for testing
        static class TestClassWithStaticSettersForNode {

            // Valid static getter for Node
            public static void setNodeProperty(Node node, String value) {
                // Valid static getter
            }

            // Overloaded static setters
            public static void setOverloadedProperty(Node node, String value) {
                // Overloaded getter 1
            }

            public static void setOverloadedProperty(Node node, Double value) {
                // Overloaded getter 2
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

            // Non-getter methods that should be ignored
            public static Node getNodeProperty() {
                return null;
            }

            public static void doSomething(Node node, String value) {
                // Not a getter name
            }

            // Non-static method - should be excluded
            public void setInstanceProperty(Node node, String value) {
                // Instance method, not static
            }
        }

        static class TestParentClassWithStaticSetters {
            public static void setParentStaticProperty(Node node, String value) {
                // Parent static getter
            }
        }

        static class TestSubClassWithStaticSetters extends TestParentClassWithStaticSetters {
            public static void setChildStaticProperty(Node node, String value) {
                // Child static getter
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
    class SplitStringTest {

        @Test
        void shouldSplitByDelimiter() {
            // Given
            String input = "a:b:c";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void shouldIgnoreEmptySegmentsBetweenConsecutiveDelimiters() {
            // Given
            String input = "a::b";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void shouldIgnoreLeadingDelimiter() {
            // Given
            String input = ":a:b";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void shouldIgnoreTrailingDelimiter() {
            // Given
            String input = "a:b:";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void shouldReturnSingleElementWhenNoDelimiter() {
            // Given
            String input = "abc";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).containsExactly("abc");
        }

        @Test
        void shouldReturnEmptyStreamForEmptyString() {
            // Given
            String input = "";

            // When
            List<String> result = Utils.splitString(input, ':').toList();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SplitByCommaOutsideBracketsTest {

        @Test
        void shouldSplitSimpleCommaSeparatedValues() {
            // Given
            String input = "a, b, c";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).containsExactly("a", "b", "c");
        }

        @Test
        void shouldNotSplitCommaInsideAngleBrackets() {
            // Given
            String input = "Map<String, Integer>, List<String>";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).containsExactly("Map<String, Integer>", "List<String>");
        }

        @Test
        void shouldHandleNestedAngleBrackets() {
            // Given
            String input = "Map<String, List<Integer>>, String";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).containsExactly("Map<String, List<Integer>>", "String");
        }

        @Test
        void shouldIgnoreBlankSegments() {
            // Given
            String input = "a,  , b";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).containsExactly("a", "b");
        }

        @Test
        void shouldReturnSingleElementWhenNoComma() {
            // Given
            String input = "String";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).containsExactly("String");
        }

        @Test
        void shouldReturnEmptyStreamForBlankString() {
            // Given
            String input = "   ";

            // When
            List<String> result = Utils.splitByCommaOutsideBrackets(input).toList();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SingletonOrEmptyTest {

        @Test
        void shouldReturnEmptyOptionalForEmptyCollection() {
            // Given
            List<String> collection = List.of();

            // When
            Optional<String> result = Utils.<String, List<String>>singletonOrEmpty().apply(collection);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnSingleElementOptional() {
            // Given
            List<String> collection = List.of("only");

            // When
            Optional<String> result = Utils.<String, List<String>>singletonOrEmpty().apply(collection);

            // Then
            assertThat(result).contains("only");
        }

        @Test
        void shouldThrowWhenCollectionHasMoreThanOneElement() {
            // Given
            List<String> collection = List.of("a", "b");

            // When & Then
            assertThatThrownBy(() -> Utils.<String, List<String>>singletonOrEmpty().apply(collection))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Expected an empty or singleton collection, found 2 elements");
        }
    }

    @Nested
    class DropBlankLinesAtEndTest {

        @Test
        void shouldDropTrailingBlankLines() {
            // Given
            List<String> input = List.of("line1", "line2", "", "");

            // When
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .toList();

            // Then
            assertThat(result).containsExactly("line1", "line2");
        }

        @Test
        void shouldPreserveBlankLinesInMiddle() {
            // Given
            List<String> input = List.of("line1", "", "line2", "");

            // When
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .toList();

            // Then
            assertThat(result).containsExactly("line1", "", "line2");
        }

        @Test
        void shouldReturnEmptyForAllBlankLines() {
            // Given
            List<String> input = List.of("", "", "");

            // When
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .toList();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleNullLines() {
            // Given
            List<String> input = new ArrayList<>();
            input.add("line1");
            input.add(null);

            // When
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .toList();

            // Then
            assertThat(result).containsExactly("line1");
        }

        @Test
        void shouldStopWhenDownstreamRejects() {
            // Given - limit(1) causes downstream to reject after first element
            List<String> input = List.of("line1", "", "line2");

            // When
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .limit(1)
                    .toList();

            // Then
            assertThat(result).containsExactly("line1");
        }

        @Test
        void shouldStopWhenDownstreamRejectsDuringQueueFlush() {
            // Given - blank lines are queued, then a non-blank triggers flush,
            // but limit(1) causes downstream to reject during the flush of queued blanks
            List<String> input = List.of("line1", "", "", "line2", "line3");

            // When - limit(2) accepts "line1" then rejects on first queued blank during flush
            List<String> result = input.stream()
                    .gather(Utils.dropBlankLinesAtEnd())
                    .limit(2)
                    .toList();

            // Then
            assertThat(result).containsExactly("line1", "");
        }
    }

    @Nested
    class MergeTest {

        @Test
        void shouldApplyFunctionAndReturnFirstArgument() {
            // Given
            List<String> first = new ArrayList<>(List.of("a"));
            List<String> second = new ArrayList<>(List.of("b"));

            // When
            List<String> result = Utils.<List<String>>merge(List::addAll).apply(first, second);

            // Then
            assertThat(result).isSameAs(first).containsExactly("a", "b");
        }
    }

    @Nested
    class DuplicateThrowExceptionTest {

        @Test
        void shouldThrowIllegalStateExceptionOnDuplicate() {
            // When & Then
            assertThatThrownBy(() -> Utils.duplicateThrowException().apply("a", "b"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Duplicate key not allowed in set or map");
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
    class StripIndentNonBlankLinesTest {

        @Test
        void shouldStripCommonLeadingWhitespace() {
            // Given
            String text = "    hello\n    world\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\nworld\n");
        }

        @Test
        void shouldPreserveRelativeIndentation() {
            // Given
            String text = "    hello\n        world\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\n    world\n");
        }

        @Test
        void shouldDropLeadingBlankLines() {
            // Given
            String text = "\n\n    hello\n    world\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\nworld\n");
        }

        @Test
        void shouldDropTrailingBlankLines() {
            // Given
            String text = "    hello\n    world\n\n\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\nworld\n");
        }

        @Test
        void shouldTreatBlankLinesAsHavingMaxIndent() {
            // Given - blank line should not affect the common indent calculation
            String text = "    hello\n\n    world\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\n\nworld\n");
        }

        @Test
        void shouldHandleEmptyString() {
            // Given
            String text = "";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("\n");
        }

        @Test
        void shouldHandleNoIndentation() {
            // Given
            String text = "hello\nworld\n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\nworld\n");
        }

        @Test
        void shouldHandleTrailingWhitespace() {
            // Given
            String text = "hello                \nworld                 \n";

            // When
            String result = Utils.stripIndentNonBlankLines(text);

            // Then
            assertThat(result).isEqualTo("hello\nworld\n");
        }
    }

    @Nested
    class CollectPatternTest {

        @Test
        void shouldCollectCapturedGroupsFromMatchingLines() {
            // Given - collectPattern uses group(1), so pattern must have a capturing group
            Pattern pattern = Pattern.compile("^import ([\\w.]+)");
            List<String> lines = List.of("import java.util.List", "class Foo {}", "import java.util.Map");

            // When
            List<String> result = lines.stream().collect(Utils.collectPattern(pattern));

            // Then - group(1) is captured
            assertThat(result).containsExactly("java.util.List", "java.util.Map");
        }

        @Test
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            Pattern pattern = Pattern.compile("^import ([\\w.]+)");
            List<String> lines = List.of("class Foo {}", "void bar() {}");

            // When
            List<String> result = lines.stream().collect(Utils.collectPattern(pattern));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnImmutableList() {
            // Given
            Pattern pattern = Pattern.compile("(\\w+)");
            List<String> lines = List.of("a", "b");

            // When
            List<String> result = lines.stream().collect(Utils.collectPattern(pattern));

            // Then
            assertThatThrownBy(() -> result.add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void shouldThrowNullPointerExceptionForNullPattern() {
            // When & Then
            assertThatThrownBy(() -> Utils.collectPattern(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("pattern must not be null");
        }

        @Test
        void shouldCollectMultipleMatchesPerLine() {
            // Given - pattern matches multiple times per line
            Pattern pattern = Pattern.compile("(\\w+)");
            List<String> lines = List.of("hello world");

            // When
            List<String> result = lines.stream().collect(Utils.collectPattern(pattern));

            // Then - each match's group(1) is collected
            assertThat(result).containsExactly("hello", "world");
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