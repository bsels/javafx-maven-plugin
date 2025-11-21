package com.github.bsels.javafx.maven.plugin.utils;

import javafx.beans.NamedArg;
import javafx.scene.Node;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

/// Utility class providing helper methods for generating method names based on field names.
/// This class is not meant to be instantiated.
public final class Utils {
    /// A static final instance of [Gatherer] used to identify and collect class types based on their type names.
    /// It uses the `findTypeForName` method to attempt resolving a class by its fully qualified name and pushes it to
    /// the gatherer's downstream consumer if successfully located.
    ///
    /// This gatherer operates by processing type name strings and mapping them into [Class] instances.
    /// If the type name cannot be resolved, the operation is skipped without halting further processing.
    public static final Gatherer<String, Void, ? extends Class<?>> CLASS_FINDER = Gatherer.of(
            (_, typeName, downstream) -> findTypeForName(typeName).map(downstream::push).orElse(true)
    );
    /// Represents a reusable, thread-safe instance of a [Gatherer] that works with [Optional].
    /// It processes an [Optional] value by applying a downstream operation when the optional contains a value,
    /// or returning `true` if the optional is empty.
    ///
    /// This is particularly useful for collecting and managing optional values during processing
    /// pipelines within the [FXMLProcessor].
    private static final Gatherer<? super Optional<?>, Void, ?> OPTIONAL = Gatherer.of(
            (_, optional, downstream) -> optional.map(downstream::push).orElse(true)
    );

    /// Utility class providing helper methods for generating method names based on field names.
    /// This class is not meant to be instantiated.
    private Utils() {
    }

    /// Converts a given [URI] to a [URL].
    ///
    /// @param uri the [URI] to be converted
    /// @return the corresponding [URL] for the given [URI]
    /// @throws RuntimeException if the [URI] cannot be converted to a [URL]
    public static URL uriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /// Generates the setter method name for a given field name.
    ///
    /// @param name the name of the field for which to generate the setter method name
    /// @return the setter method name corresponding to the specified field name
    public static String getSetterName(String name) {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /// Generates the getter method name for a given field name.
    ///
    /// @param name the name of the field for which to generate the setter method name
    /// @return the getter method name corresponding to the specified field name
    public static String getGetterName(String name) {
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /// Resolves and returns the raw class type associated with a given [Type].
    /// If the provided type is of a [ParameterizedType], it retrieves the raw type.
    /// Otherwise, it directly evaluates the given type.
    ///
    /// @param type the type for which to determine the corresponding class
    /// @return the class object associated with the provided type
    /// @throws IllegalStateException if the type cannot be resolved to a class
    public static Class<?> getClassType(Type type) {
        Type finalType = type instanceof ParameterizedType parameterizedType ? parameterizedType.getRawType() : type;
        if (!(finalType instanceof Class<?> clazz)) {
            throw new IllegalStateException("Unable to find class type for %s".formatted(type));
        }
        return clazz;
    }

    /// Searches for a method in the specified class by name and parameter type.
    /// If the method is not found, the search continues recursively in the superclass of the parameter type.
    ///
    /// @param clazz         the class in which to search for the method
    /// @param name          the name of the method to search for
    /// @param parameterType the parameter type of the method to search for
    /// @return an [Optional] containing the found method if present, or an empty [Optional] if no method is found
    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?> parameterType) {
        if (parameterType == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(clazz.getMethod(name, parameterType));
        } catch (NoSuchMethodException _) {
            return findMethod(clazz, name, parameterType.getSuperclass())
                    .or(
                            () -> Stream.of(clazz.getInterfaces())
                                    .map(interfaceClass -> findMethod(clazz, name, interfaceClass))
                                    .filter(Optional::isPresent)
                                    .findFirst()
                                    .flatMap(Function.identity())
                    );
        }
    }

    /// Checks if a static method with the specified name and parameter types exists in the given class.
    ///
    /// This method searches through all the public methods in the given class and verifies if a static
    /// method matches the provided name, has the specified number and types of parameters.
    ///
    /// @param clazz          the class in which to search for the static method
    /// @param methodName     the name of the method to search for
    /// @param parameterTypes a list of parameter types expected for the method
    /// @return true if a matching static method exists in the class, false otherwise
    public static boolean checkIfStaticMethodExists(Class<?> clazz, String methodName, List<Class<?>> parameterTypes) {
        return Stream.of(clazz.getMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getParameterCount() == parameterTypes.size())
                .anyMatch(method -> validateParameters(parameterTypes, method));
    }

    /// Returns a [Gatherer] instance that works with [Optional] values.
    /// This method is designed to handle Optional instances by facilitating operations on their contained
    /// values in a generic and type-safe manner.
    ///
    /// @param <T> the type of the element contained within the [Optional]
    /// @return a [Gatherer] instance that processes Optional values
    @SuppressWarnings("unchecked")
    public static <T> Gatherer<? super Optional<T>, Void, T> optional() {
        return (Gatherer<? super Optional<T>, Void, T>) OPTIONAL;
    }

    /// Identifies and retrieves the single functional method of a given class if the class represents a functional interface.
    /// A functional interface is expected to have exactly one abstract method.
    ///
    /// @param clazz the class to analyze for its functional interface method
    /// @return the single functional method of the provided class
    /// @throws IllegalStateException if the class does not represent a functional interface or has more than one abstract method
    public static Method getFunctionalMethod(Class<?> clazz) {
        List<Method> methods = Stream.of(clazz.getMethods())
                .filter(getFunctionalInterfaceFilter())
                .toList();
        if (methods.size() != 1) {
            throw new IllegalStateException("Expected exactly one functional interface method, found %d".formatted(methods.size()));
        }
        return methods.getFirst();
    }

    /// Attempts to find and load a class by its fully qualified name.
    ///
    /// @param typeName the fully qualified name of the class to be loaded
    /// @return an [Optional] containing the [Class] object if the class is found, or an empty [Optional] if the class cannot be found
    public static Optional<Class<?>> findTypeForName(String typeName) {
        try {
            return Optional.of(Thread.currentThread().getContextClassLoader().loadClass(typeName));
        } catch (ClassNotFoundException _) {
            return Optional.empty();
        }
    }

    /// Attempts to find a class type corresponding to the provided type name and imports list.
    /// This method resolves the type name to its fully qualified class name based on the given imports.
    /// It prefers explicitly imported classes, followed by wildcard imports. Additionally, it checks
    /// in default Java package java.lang as a last resort.
    ///
    /// @param imports  a list of strings representing imported packages or classes
    /// @param typeName the name of the type to be resolved; may be a simple name or fully qualified name
    /// @return the resolved [Class<?>] object corresponding to the typeName
    /// @throws InternalClassNotFoundException if the type cannot be resolved or if multiple types are found
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 for a given name in wildcard imports
    public static Class<?> findType(List<String> imports, String typeName) {
        if (typeName.contains(".")) {
            return findTypeForName(typeName)
                    .orElseThrow(() -> new InternalClassNotFoundException("Unable to find type for name: %s".formatted(typeName)));
        }

        String suffixTypeName = "." + typeName;
        Optional<? extends Class<?>> type = imports.stream()
                .filter(fullImport -> fullImport.endsWith(suffixTypeName))
                .gather(Utils.CLASS_FINDER)
                .findFirst();

        if (type.isPresent()) {
            return type.get();
        }

        List<? extends Class<?>> types = imports.stream()
                .filter(fullImport -> fullImport.endsWith("*"))
                .map(fullImport -> fullImport.substring(0, fullImport.length() - 1) + typeName)
                .gather(Utils.CLASS_FINDER)
                .toList();
        if (!types.isEmpty()) {
            if (types.size() == 1) {
                return types.getFirst();
            } else {
                throw new InternalClassNotFoundException("Found multiple types for name: %s".formatted(typeName));
            }
        }

        return findTypeForName("java.lang.%s".formatted(typeName))
                .orElseThrow(() -> new InternalClassNotFoundException("Unable to find type for name: %s".formatted(typeName)));
    }

    /// Finds a getter method in the specified class that returns a list and determines the element type of the list.
    ///
    /// @param clazz          the class to inspect for the specified getter method
    /// @param listGetterName the name of the getter method that returns a collection
    /// @return the element type of the collection returned by the getter method
    /// @throws NoSuchMethodException if the specified method does not exist, or if the method does not return a collection
    public static Type findGetterListAndReturnElementType(Class<?> clazz, String listGetterName) throws NoSuchMethodException {
        Method method = clazz.getMethod(listGetterName);
        if (!Collection.class.isAssignableFrom(method.getReturnType())) {
            throw new NoSuchMethodException("Not a collection getter: %s".formatted(listGetterName));
        }
        Type parameterType = method.getGenericReturnType();
        if (parameterType instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getActualTypeArguments().length == 1) {
                Type type = parameterizedType.getActualTypeArguments()[0];
                if (type instanceof WildcardType wildcardType) {
                    return Stream.of(wildcardType.getLowerBounds())
                            .findFirst()
                            .or(() -> Stream.of(wildcardType.getUpperBounds()).findFirst())
                            .orElse(type);
                }
                return type;
            }
        }
        return Object.class;
    }

    /// Returns a binary operator that always selects the first argument, ignoring the second argument.
    ///
    /// @param <T> the type of the operands and result of the operator
    /// @return a binary operator that returns the first of its input arguments
    public static <T> BinaryOperator<T> getFirstLambda() {
        return (a, _) -> a;
    }

    /// Finds and returns a list of parameterized types for the specified property name in the public constructors of
    /// a given class.
    /// The method inspects constructor parameters annotated with [@NamedArg] and matches their values against
    /// the specified property name.
    ///
    /// @param clazz        the class to inspect for public constructors.
    /// @param propertyName the name of the property to match in [@NamedArg] annotations.
    /// @return a list of distinct parameterized types for the matched property name.
    public static List<Type> findParameterTypeForConstructors(Class<?> clazz, String propertyName) {
        Gatherer<? super Optional<Parameter>, Void, Parameter> optional = optional();
        return Stream.of(clazz.getConstructors())
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .map(Constructor::getParameters)
                .map(List::of)
                .filter(Predicate.not(List::isEmpty))
                .filter(parameters -> parameters.stream().allMatch(parameter -> parameter.isAnnotationPresent(NamedArg.class)))
                .map(
                        parameters -> parameters.stream()
                                .filter(parameter -> propertyName.equals(parameter.getAnnotation(NamedArg.class).value()))
                                .findFirst()
                )
                .gather(optional)
                .map(Parameter::getParameterizedType)
                .distinct()
                .toList();
    }

    /// Retrieves a list of static setter methods from the specified class that match the given method name.
    /// The method must be static, have the specified name, and take exactly two parameters,
    /// where the first parameter is assignable from the Node class.
    ///
    /// @param staticClass      the class to search for static setter methods
    /// @param staticSetterName the name of the static setter methods to look for
    /// @return a list of methods that match the criteria, or an empty list if no matching methods are found
    public static List<Method> findStaticSettersForNode(Class<?> staticClass, String staticSetterName) {
        return Stream.of(staticClass.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> staticSetterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0].isAssignableFrom(Node.class))
                .toList();
    }

    /// Finds all non-static setter methods in the given class with the specified name.
    ///
    /// @param clazz      the class to search for the methods
    /// @param setterName the name of the setter methods to find
    /// @return a list of methods that are non-static, match the specified name, and have exactly one parameter
    public static List<Method> findObjectSetters(Class<?> clazz, String setterName) {
        return Stream.of(clazz.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> setterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 1)
                .toList();
    }

    /// Creates a predicate that checks if a given class is assignable to any of the specified target classes.
    ///
    /// @param classes the target classes to check assignability against
    /// @return a predicate that evaluates whether a class is assignable to any of the given target classes
    public static Predicate<Class<?>> isAssignableTo(Class<?>... classes) {
        return predicateClass -> Stream.of(classes).anyMatch(clazz -> clazz.isAssignableFrom(predicateClass));
    }

    /// Searches and validates a getter method in the specified class that matches the provided identifier,
    /// returns an allowed type, and is assignable from the given parameter type.
    ///
    /// @param clazz          the class to search within for the getter method
    /// @param identifier     a descriptive identifier to be included in exception messages
    /// @param listGetterName the name of the getter method to find
    /// @param paramType      the parameter type to verify against the collection's generic type
    /// @return the return type of the validated getter method
    /// @throws NoSuchMethodException if the specified getter method does not exist in the class
    /// @throws IllegalStateException if the method's return type is not a collection,
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 or its generic type is incompatible with the given parameter type
    public static Class<?> findCollectionGetterWithAllowedReturnType(
            Class<?> clazz,
            String identifier,
            String listGetterName,
            Class<?> paramType
    ) throws NoSuchMethodException {
        Method method = clazz.getMethod(listGetterName);
        Class<?> returnType = method.getReturnType();
        if (!Collection.class.isAssignableFrom(returnType)) {
            throw new IllegalStateException("Unable to find list getter for %s".formatted(identifier));
        }
        Type listType = method.getGenericReturnType();
        if (!(listType instanceof ParameterizedType parameterizedType)) {
            throw new IllegalStateException("Unable to find list getter for %s".formatted(identifier));
        }
        Type listItemType = parameterizedType.getActualTypeArguments()[0];
        if (
                !(listItemType instanceof Class<?> listItemClass && listItemClass.isAssignableFrom(paramType)) &&
                        !(listItemType instanceof ParameterizedType parameterizedType1
                                && parameterizedType1.getRawType() instanceof Class<?> listItemClass1
                                && listItemClass1.isAssignableFrom(paramType))
        ) {
            throw new IllegalStateException("Unable to find list getter for %s".formatted(identifier));
        }
        return returnType;
    }

    /// Enhances the import statements based on the provided parameter list by streamlining or
    /// modifying the imports used in the context.
    ///
    /// @param parameters a list of fully qualified class names representing parameters that influence the imports; the contents of this list will be modified
    /// @param imports    a collection of fully qualified class names representing the current imports to be updated and maintained based on the parameters
    public static void improveImportsForParameters(List<String> parameters, Collection<String> imports) {
        parameters.replaceAll(s -> improveImportForParameter(imports, s));
    }

    /// Improves the import management for a given parameter by modifying the import set and
    /// potentially simplifying the parameter's representation.
    ///
    /// @param imports   a collection of fully qualified class names representing the current imports
    /// @param parameter the fully qualified class name of the parameter to be processed
    /// @return a simplified class name if the parameter can be reduced using the import set,
    /// otherwise the original fully qualified class name of the parameter
    public static String improveImportForParameter(Collection<String> imports, String parameter) {
        String simpleName = parameter.substring(parameter.lastIndexOf('.') + 1);
        if (imports.contains(parameter)) {
            return simpleName;
        } else {
            String classSuffix = "." + simpleName;
            if (imports.stream().noneMatch(importClass -> importClass.endsWith(classSuffix))) {
                imports.add(parameter);
                return simpleName;
            }
        }
        return parameter;
    }

    /// Returns a function mapping a [Type] to another [Type] based on the provided mapping of type parameter names to
    /// types.
    /// If the provided [Type] is a [TypeVariable], the function attempts to resolve it to a corresponding [Type] using
    /// the given map.
    /// For unresolvable or non-variable types, the original [Type] or [Object] is returned as a fallback.
    ///
    /// @param log a logging instance used for debugging and tracing type resolution
    /// @param typeParameterNameToType a map containing type parameter names as keys and their corresponding [Type] objects as values
    /// @return a [Function] that maps a [Type] to another [Type] based on the provided type mappings
    public static Function<Type, Type> getTypeMapperFunction(Log log, Map<String, Type> typeParameterNameToType) {
        return parameterType -> {
            log.debug("Found parameter type %s of class %s".formatted(parameterType, parameterType.getClass()));
            if (parameterType instanceof TypeVariable<?> typeVariable) {
                log.debug("Found type variable %s".formatted(List.of(typeVariable.getAnnotatedBounds())));
                return typeParameterNameToType.getOrDefault(typeVariable.getName(), Object.class);
            } else {
                return parameterType;
            }
        };
    }

    /// Constructs and returns a filter predicate for methods, identifying those that are public, abstract, non-default,
    /// and non-static.
    /// This filter can be used to determine if a method adheres to the characteristics suitable for a functional
    /// interface.
    ///
    /// @return a predicate that identifies methods matching the criteria of a functional interface.
    private static Predicate<Method> getFunctionalInterfaceFilter() {
        return Predicate.not((Method method) -> Modifier.isStatic(method.getModifiers()))
                .and(Predicate.not(Method::isDefault))
                .and(method -> Modifier.isPublic(method.getModifiers()))
                .and(method -> Modifier.isAbstract(method.getModifiers()));
    }

    /// Validates whether the parameter types of the given method match the specified list of parameter types.
    ///
    /// @param parameterTypes the list of expected parameter types
    /// @param method         the method whose parameter types are to be validated
    /// @return true if the parameter types of the method match the specified list of parameter types, false otherwise
    private static boolean validateParameters(List<Class<?>> parameterTypes, Method method) {
        for (int i = 0; i < parameterTypes.size(); i++) {
            Class<?> parameterType = method.getParameterTypes()[i];
            if (!parameterType.isAssignableFrom(parameterTypes.get(i))) {
                return false;
            }
        }
        return true;
    }
}
