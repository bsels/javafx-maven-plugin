package io.github.bsels.javafx.maven.plugin.utils;

import javafx.beans.NamedArg;
import javafx.scene.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
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

    /// Creates a [Gatherer] that processes lines sequentially and removes blank lines at the end of a stream.
    /// Blank lines in the middle of the stream are preserved.
    ///
    /// @return a [Gatherer] that collects input strings into a queue, removing trailing blank lines at the end.
    public static Gatherer<String, Queue<String>, String> dropBlankLinesAtEnd() {
        return Gatherer.ofSequential(
                LinkedList::new,
                (state, line, downstream) -> {
                    if (line == null || line.isBlank()) {
                        return state.add(line);
                    } else {
                        while (!state.isEmpty()) {
                            if (!downstream.push(state.remove())) {
                                return false;
                            }
                        }
                        return downstream.push(line);
                    }
                }
        );
    }

    /// Creates a Gatherer that collects unique items from a stream of input.
    /// `<T>` represents the type of items being processed.
    /// The method uses a [HashSet] to ensure that each item is included only once pushed to the downstream.
    /// Duplicate items are discarded and downstream processing continues.
    ///
    /// @param <T> the type of items to be processed
    /// @return a [Gatherer] instance that pushes unique items from the input stream to the downstream.
    public static <T> Gatherer<T, Set<T>, T> unique() {
        return Gatherer.ofSequential(
                HashSet::new,
                (state, line, downstream) -> Optional.ofNullable(line)
                        .filter(state::add)
                        .map(downstream::push)
                        .orElseGet(isDownstreamAccepting(downstream))
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
                .mapToInt(line -> {
                    int length = line.length();
                    int i = 0;
                    while (i < length && Character.isWhitespace(line.charAt(i))) {
                        i++;
                    }
                    return i == length ? Integer.MAX_VALUE : i;
                })
                .min()
                .orElse(Integer.MAX_VALUE);

        return text.lines()
                .dropWhile(String::isBlank)
                .map(line -> {
                    int lastNonWhitespace = line.length() - 1;
                    while (lastNonWhitespace >= 0 && Character.isWhitespace(line.charAt(lastNonWhitespace))) {
                        lastNonWhitespace--;
                    }
                    lastNonWhitespace++;
                    return line.substring(Math.min(firstNonWhitespace, lastNonWhitespace), lastNonWhitespace);
                })
                .gather(Utils.dropBlankLinesAtEnd())
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
    /// @throws InternalClassNotFoundException if the type cannot be resolved, or if multiple types are found for a given name in wildcard imports
    public static Class<?> findType(List<String> imports, String typeName) throws InternalClassNotFoundException {
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

    /// Searches for a non-static, parameterless method with the specified name in the given class.
    ///
    /// @param clazz      the class in which to search for the method
    /// @param getterName the name of the method to be searched
    /// @return an `Optional` containing the found `Method` if one exists, or an empty `Optional` if no matching method is found
    public static Optional<Method> findObjectGetter(Class<?> clazz, String getterName) {
        return Stream.of(clazz.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> getterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 0)
                .findFirst();
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

    /// Traverses the class hierarchy of the given class to find the [Collection] interface and extract its element type.
    ///
    /// The logic:
    /// 1. If the class itself is [Collection], returns [Object] since no type arguments are available for a raw type.
    /// 2. Searches all generic interfaces of the class; if one is a [ParameterizedType] with raw type [Collection],
    ///    extracts the first type argument (`E`) and returns its raw class.
    /// 3. Recursively searches superclass and non-parameterized interfaces.
    /// 4. Returns [Object] if [Collection] is not found in the hierarchy.
    ///
    /// @param clazz The class to search.
    /// @return The raw [Class] of the collection's element type, or [Object] if it cannot be determined.
    public static Class<?> findCollectionValueTypeFromHierarchy(Class<?> clazz) {
        return findGenericTypeFromHierarchy(clazz, Collection.class, 0);
    }

    /// Traverses the class hierarchy of the given class to find the [Map] interface and extract its key type.
    ///
    /// The logic:
    /// 1. If the class itself is [Map], returns [Object] since no type arguments are available for a raw type.
    /// 2. Searches all generic interfaces of the class; if one is a [ParameterizedType] with raw type [Map],
    ///    extracts the first type argument (`K`) and returns its raw class.
    /// 3. Recursively searches superclass and non-parameterized interfaces.
    /// 4. Returns [Object] if [Map] is not found in the hierarchy.
    ///
    /// @param clazz The class to search.
    /// @return The raw [Class] of the map's key type, or [Object] if it cannot be determined.
    public static Class<?> findMapKeyTypeFromHierarchy(Class<?> clazz) {
        return findGenericTypeFromHierarchy(clazz, Map.class, 0);
    }

    /// Traverses the class hierarchy of the given class to find the [Map] interface and extract its value type.
    ///
    /// The logic:
    /// 1. If the class itself is [Map], returns [Object] since no type arguments are available for a raw type.
    /// 2. Searches all generic interfaces of the class; if one is a [ParameterizedType] with raw type [Map],
    ///    extracts the second type argument (`V`) and returns its raw class.
    /// 3. Recursively searches superclass and non-parameterized interfaces.
    /// 4. Returns [Object] if [Map] is not found in the hierarchy.
    ///
    /// @param clazz The class to search.
    /// @return The raw [Class] of the map's value type, or [Object] if it cannot be determined.
    public static Class<?> findMapValueTypeFromHierarchy(Class<?> clazz) {
        return findGenericTypeFromHierarchy(clazz, Map.class, 1);
    }

    /// Splits the given string into a stream of substrings based on the specified character delimiter.
    /// Consecutive delimiters are treated as a single separator, and empty substrings are ignored.
    ///
    /// @param string    the input string to be split
    /// @param character the character used as the delimiter for splitting the string
    /// @return a stream of substrings obtained by splitting the input string
    public static Stream<String> splitString(String string, char character) {
        Stream.Builder<String> builder = Stream.builder();
        int size = string.length();
        int lastIndex = -1;
        for (int i = 0; i < size; i++) {
            if (string.charAt(i) == character) {
                String substring = string.substring(lastIndex + 1, i);
                if (!substring.isEmpty()) {
                    builder.accept(substring);
                }
                lastIndex = i;
            }
        }
        String substring = string.substring(lastIndex + 1);
        if (!substring.isEmpty()) {
            builder.accept(substring);
        }
        return builder.build();
    }

    /// Splits a string by commas, but only when the comma is not inside angle brackets (`< >`).
    /// This is used for splitting generic type parameters where a type like `Map<String, Integer>`
    /// should not be split at the comma inside the brackets.
    ///
    /// @param string the string to split
    /// @return a stream of substrings split by commas outside of angle brackets
    public static Stream<String> splitByCommaOutsideBrackets(String string) {
        Stream.Builder<String> builder = Stream.builder();
        int size = string.length();
        int lastIndex = -1;
        int depth = 0;
        for (int i = 0; i < size; i++) {
            char c = string.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                String substring = string.substring(lastIndex + 1, i);
                if (!substring.isBlank()) {
                    builder.accept(substring.strip());
                }
                lastIndex = i;
            }
        }
        String substring = string.substring(lastIndex + 1);
        if (!substring.isBlank()) {
            builder.accept(substring.strip());
        }
        return builder.build();
    }

    /// Traverses the class hierarchy of the given class to find the specified target interface and extract its
    /// type argument at the given index.
    ///
    /// The logic:
    /// 1. If the class itself is the target,
    ///    the interface returns [Object] since no type arguments are available for a raw type.
    /// 2. Searches all generic interfaces of the class;
    ///    if one is a [ParameterizedType] with a raw type of the target interface,
    ///    extracts the type argument at the specified index and returns its raw class.
    /// 3. Recursively searches superclass and non-parameterized interfaces.
    /// 4. Returns [Object] if the target interface is not found in the hierarchy.
    ///
    /// @param clazz             the class to search
    /// @param targetInterface   the interface to look for in the hierarchy
    /// @param typeArgumentIndex the index of the type argument to extract
    /// @return the raw [Class] of the extracted type argument, or [Object] if it cannot be determined
    private static Class<?> findGenericTypeFromHierarchy(Class<?> clazz, Class<?> targetInterface, int typeArgumentIndex) {
        if (clazz == null || clazz == Object.class || clazz == targetInterface) {
            return Object.class;
        }
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt && pt.getRawType() == targetInterface) {
                Type typeArg = pt.getActualTypeArguments()[typeArgumentIndex];
                if (typeArg instanceof Class<?> typeClass) {
                    return typeClass;
                }
                return Object.class;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            Class<?> result = findGenericTypeFromHierarchy(superclass, targetInterface, typeArgumentIndex);
            if (result != Object.class) {
                return result;
            }
        }
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            Class<?> ifaceClass = genericInterface instanceof ParameterizedType pt
                    ? (Class<?>) pt.getRawType()
                    : (Class<?>) genericInterface;
            Class<?> result = findGenericTypeFromHierarchy(ifaceClass, targetInterface, typeArgumentIndex);
            if (result != Object.class) {
                return result;
            }
        }
        return Object.class;
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

    /// Checks if the downstream is accepting items by evaluating whether it is not rejecting.
    ///
    /// @param downstream the downstream consumer whose acceptance status is to be checked
    /// @return a supplier that returns true if the downstream is accepting items, false otherwise
    private static Supplier<Boolean> isDownstreamAccepting(Gatherer.Downstream<?> downstream) {
        return () -> !downstream.isRejecting();
    }
}
