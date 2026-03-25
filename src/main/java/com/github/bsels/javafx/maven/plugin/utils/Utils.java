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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
            (_, typeName, downstream) -> findTypeForName(typeName)
                    .map(downstream::push)
                    .orElseGet(isDownstreamAccepting(downstream))
    );
    /// A constant list containing sets of primitive types and their corresponding wrapper classes.
    /// Each set within the list groups a primitive type with its wrapper equivalent, facilitating lookups
    /// or type comparisons where both primitive and wrapper representations are relevant.
    private static final List<Set<Class<?>>> PRIMITIVE_TYPE_AND_WRAPPER = List.of(
            Set.of(boolean.class, Boolean.class),
            Set.of(byte.class, Byte.class),
            Set.of(short.class, Short.class),
            Set.of(int.class, Integer.class),
            Set.of(long.class, Long.class),
            Set.of(float.class, Float.class),
            Set.of(double.class, Double.class),
            Set.of(char.class, Character.class)
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

    /// Generates the getter method name for a given field name.
    ///
    /// @param name the name of the field for which to generate the getter method name
    /// @return the getter method name corresponding to the specified field name
    public static String getSetterName(String name) {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /// Generates the getter method name for a given field name.
    ///
    /// @param name the name of the field for which to generate the getter method name
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
    public static <T> Gatherer<Optional<T>, Void, T> optional() {
        return Gatherer.of(
                (_, optional, downstream) -> optional.map(downstream::push)
                        .orElseGet(() -> !downstream.isRejecting())
        );
    }

    /// Creates a sequential [Gatherer] that collects elements into a list and then processes them in reverse order.
    ///
    /// @param <T> the type of elements to be processed
    /// @return a [Gatherer] that accumulates elements into a list and outputs them in reverse order during downstream processing
    public static <T> Gatherer<T, List<T>, T> reverse() {
        return Gatherer.ofSequential(
                ArrayList::new,
                (state, line, _) -> state.add(line),
                (state, downstream) -> {
                    int i = state.size() - 1;
                    while (i >= 0 && downstream.push(state.get(i))) {
                        i--;
                    }
                }
        );
    }

    /// Removes leading whitespace common to all non-blank lines in the given text.
    /// The method ensures that the indentation is uniformly stripped while maintaining the relative structure
    /// of the text.
    ///
    /// @param text the input string containing multiple lines of text, which may include leading or trailing whitespace.
    /// @return a new string with the common leading whitespace removed from all non-blank lines. Blank lines will remain unaltered.
    public static String stripIndentNonBlankLines(String text) {
        int firstNonWhitespace = text.lines()
                .filter(Predicate.not(String::isBlank))
                .mapToInt(line -> {
                    int length = line.length();
                    int i = 0;
                    while (i < length && Character.isWhitespace(line.charAt(i))) {
                        i++;
                    }
                    return i;
                })
                .min()
                .orElse(Integer.MAX_VALUE);

        return text.lines()
                .dropWhile(String::isBlank)
                .map(line -> {
                    if (line.isBlank()) {
                        return "";
                    } else {
                        int lastNonWhitespace = line.length() - 1;
                        while (lastNonWhitespace >= 0 && Character.isWhitespace(line.charAt(lastNonWhitespace))) {
                            lastNonWhitespace--;
                        }
                        return line.substring(firstNonWhitespace, lastNonWhitespace + 1);
                    }
                })
                .gather(Utils.reverse())
                .dropWhile(String::isBlank)
                .gather(Utils.reverse())
                .collect(Collectors.joining("\n", "", "\n"));
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
            throw new IllegalStateException("Expected exactly one functional interface method, found %d".formatted(
                    methods.size()));
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
    /// in the default Java package java.lang as a last resort.
    ///
    /// @param imports  a list of strings representing imported packages or classes
    /// @param typeName the name of the type to be resolved; may be a simple name or fully qualified name
    /// @return the resolved [Class<?>] object corresponding to the typeName
    /// @throws InternalClassNotFoundException if the type cannot be resolved or if multiple types are found for a given name in wildcard imports
    public static Class<?> findType(List<String> imports, String typeName) {
        return findTypeOptional(imports, typeName)
                .orElseThrow(() -> new InternalClassNotFoundException("Unable to find type for name: %s".formatted(
                        typeName)));
    }

    /// Attempts to find a class type corresponding to the provided type name and imports list.
    /// This method resolves the type name to its fully qualified class name based on the given imports.
    /// It prefers explicitly imported classes, followed by wildcard imports. Additionally, it checks
    /// in the default Java package java.lang as a last resort.
    ///
    /// @param imports  a list of strings representing imported packages or classes
    /// @param typeName the name of the type to be resolved; may be a simple name or fully qualified name
    /// @return an [Optional] containing the resolved [Class<?>] object corresponding to the typeName, or empty if not found
    public static Optional<Class<?>> findTypeOptional(List<String> imports, String typeName) {
        if (typeName.contains(".")) {
            return findTypeForName(typeName);
        }

        String suffixTypeName = "." + typeName;
        Optional<? extends Class<?>> type = imports.stream()
                .filter(fullImport -> fullImport.endsWith(suffixTypeName))
                .gather(Utils.CLASS_FINDER)
                .findFirst();

        if (type.isPresent()) {
            return Optional.of(type.get());
        }

        List<? extends Class<?>> types = imports.stream()
                .filter(fullImport -> fullImport.endsWith("*"))
                .map(fullImport -> fullImport.substring(0, fullImport.length() - 1) + typeName)
                .gather(Utils.CLASS_FINDER)
                .toList();
        if (!types.isEmpty()) {
            if (types.size() == 1) {
                return Optional.of(types.getFirst());
            } else {
                throw new InternalClassNotFoundException("Found multiple types for name: %s".formatted(typeName));
            }
        }

        return findTypeForName("java.lang.%s".formatted(typeName));
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

    /// Finds a getter method in the specified class that returns a map and determines the value type of the map.
    ///
    /// @param clazz         the class to inspect for the specified getter method
    /// @param mapGetterName the name of the getter method that returns a map
    /// @return the value type of the map returned by the getter method
    /// @throws NoSuchMethodException if the specified method does not exist, or if the method does not return a map
    public static Type findGetterMapAndReturnValueType(Class<?> clazz, String mapGetterName) throws NoSuchMethodException {
        Method method = clazz.getMethod(mapGetterName);
        if (!Map.class.isAssignableFrom(method.getReturnType())) {
            throw new NoSuchMethodException("Not a map getter: %s".formatted(mapGetterName));
        }
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length == 2) {
                Type valueType = typeArgs[1];
                if (valueType instanceof WildcardType wildcardType) {
                    return Stream.of(wildcardType.getLowerBounds())
                            .findFirst()
                            .or(() -> Stream.of(wildcardType.getUpperBounds()).findFirst())
                            .orElse(valueType);
                }
                return valueType;
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

    /// Returns a [BinaryOperator] that throws an [IllegalStateException] in case of duplicates.
    /// This method can be used as a merge function in collectors
    /// or other operations where duplicates are not permitted.
    ///
    /// @param <T> the type of the input arguments to the operator
    /// @return a [BinaryOperator] that throws an [IllegalStateException] when a duplicate is detected
    public static <T> BinaryOperator<T> duplicateThrowException() {
        return (_, _) -> {
            throw new IllegalStateException("Duplicate key not allowed in set or map");
        };
    }

    /// Returns a binary operator that applies the given bi-function to its inputs and always
    /// returns the first input.
    ///
    /// @param <T>      the type of the input arguments and the result
    /// @param function a bi-function that is applied to the two input arguments
    /// @return a binary operator that applies the given bi-function and returns the first input
    public static <T> BinaryOperator<T> merge(BiFunction<T, T, ?> function) {
        return (a, b) -> {
            function.apply(a, b);
            return a;
        };
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
                .map(
                        parameters -> parameters.stream()
                                .filter(parameter -> parameter.isAnnotationPresent(NamedArg.class))
                                .filter(parameter -> propertyName.equals(parameter.getAnnotation(NamedArg.class).value()))
                                .findFirst()
                )
                .gather(optional)
                .map(Parameter::getParameterizedType)
                .distinct()
                .toList();
    }

    /// Retrieves a list of static getter methods from the specified class that match the given method name.
    /// The method must be static, have the specified name, and take exactly two parameters,
    /// where the first parameter is assignable from the Node class.
    ///
    /// @param staticClass      the class to search for static getter methods
    /// @param staticSetterName the name of the static getter methods to look for
    /// @return a list of methods that match the criteria, or an empty list if no matching methods are found
    public static List<Method> findStaticSettersForNode(Class<?> staticClass, String staticSetterName) {
        return Stream.of(staticClass.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> staticSetterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0].isAssignableFrom(Node.class))
                .toList();
    }

    /// Finds all non-static getter methods in the given class with the specified name.
    ///
    /// @param clazz      the class to search for the methods
    /// @param setterName the name of the getter methods to find
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
        return predicateClass -> Stream.of(classes).anyMatch(clazz -> isAssignableFrom(clazz, predicateClass));
    }

    /// Checks if a class or interface represented by the `variable` parameter is either the same as or is a superclass
    /// or superinterface of the class or interface represented by the `expression` parameter.
    /// Additionally, handles primitive and wrapper class compatibility.
    ///
    /// @param variable   the class or interface to be checked as the potential assignable target
    /// @param expression the class or interface to be checked for assignability to `variable`
    /// @return `true` if `expression` is assignable to `variable`, or if both belong to compatible primitive-wrapper pairs; otherwise `false`
    public static boolean isAssignableFrom(Class<?> variable, Class<?> expression) {
        return PRIMITIVE_TYPE_AND_WRAPPER.stream()
                .anyMatch(classes -> classes.contains(variable) && classes.contains(expression))
                || variable.isAssignableFrom(expression);
    }

    /// Checks if a given class is a primitive type or its wrapper equivalent.
    ///
    /// @param clazz the class to be checked
    /// @return `true` if the class is a primitive type or a wrapper; otherwise `false`
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return PRIMITIVE_TYPE_AND_WRAPPER.stream()
                .anyMatch(classes -> classes.contains(clazz));
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
    /// @throws IllegalStateException if the method's return type is not a collection, or its generic type is incompatible with the given parameter type
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
    /// @return a simplified class name if the parameter can be reduced using the import set, otherwise the original fully qualified class name of the parameter
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
    /// @param log                     a logging instance used for debugging and tracing type resolution
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

    /// Converts the path of a given [URL] into an OS-specific file path string.
    ///
    /// @param url the [URL] whose path is to be converted to an OS-specific file path string
    /// @return the OS-specific file path string corresponding to the [URL] path
    /// @throws RuntimeException if the [URL] cannot be converted to a [URI] or the file path cannot be resolved
    public static String urlPathToOsPathString(URL url) throws RuntimeException {
        try {
            return Path.of(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /// Creates a [Collector] that collects strings matching the specified pattern into an immutable list.
    ///
    /// @param pattern the regular expression pattern used to match strings; must not be null
    /// @return a [Collector] that accumulates strings matching the pattern into an immutable list
    public static Collector<String, ?, List<String>> collectPattern(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        return Collectors.filtering(
                pattern.asPredicate(),
                Collector.of(
                        ArrayList::new,
                        collectPatternConsumer(pattern),
                        merge(List::addAll),
                        List::copyOf
                )
        );
    }

    /// Returns a function that processes a collection and retrieves its single element wrapped in an [Optional],
    /// or an empty [Optional] if the collection is empty.
    /// If the collection contains more than one element, an [IllegalStateException] is thrown.
    ///
    /// @param <E> the type of elements in the collection
    /// @param <C> the type of the collection
    /// @return a function that converts a collection to an [Optional] containing its single element, or empty if the collection is empty
    public static <E, C extends Collection<E>> Function<C, Optional<E>> singletonOrEmpty() {
        return collection -> {
            if (collection.size() > 1) {
                throw new IllegalStateException(
                        "Expected an empty or singleton collection, found %d elements".formatted(collection.size())
                );
            }
            return collection.stream()
                    .findFirst();
        };
    }

    /// Creates a [BiConsumer] that collects matches of a given [Pattern] from strings into a list.
    ///
    /// @param pattern the regular expression pattern to match against input strings; must not be null
    /// @return a [BiConsumer] that takes a [List] of strings and an input string and adds the first capture group of all matches in the input string to the list
    private static BiConsumer<List<String>, String> collectPatternConsumer(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        return (list, line) -> {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                list.add(matcher.group(1));
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
            if (!isAssignableFrom(parameterType, parameterTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /// Checks if the downstream is accepting items by evaluating whether it is not rejecting.
    ///
    /// @param downstream the downstream consumer whose acceptance status is to be checked
    /// @return a supplier that returns true if the downstream is accepting items, false otherwise
    private static Supplier<Boolean> isDownstreamAccepting(Gatherer.Downstream<? super Class<?>> downstream) {
        return () -> !downstream.isRejecting();
    }
}
