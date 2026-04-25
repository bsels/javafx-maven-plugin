package io.github.bsels.javafx.maven.plugin.utils;

import javafx.beans.NamedArg;
import javafx.scene.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
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

/// Utility class providing helper methods for generating method names and processing types.
public final class Utils {
    /// Gatherer used to identify and collect class types based on their type names.
    /// It uses [findTypeForName] to resolve each type name to an [Class] object.
    /// If the type is found, it is pushed downstream; otherwise, the downstream's
    /// acceptance status is checked to decide if the stream should continue.
    public static final Gatherer<String, Void, ? extends Class<?>> CLASS_FINDER = Gatherer.of(
            (_, typeName, downstream) -> findTypeForName(typeName)
                    .map(downstream::push)
                    .orElseGet(isDownstreamAccepting(downstream))
    );

    /// Private constructor to prevent instantiation.
    private Utils() {
    }

    /// Converts a [URI] to a [URL].
    /// It attempts to call `uri.toURL()` and wraps any exception in a [RuntimeException].
    ///
    /// @param uri The [URI] to convert
    /// @return The corresponding [URL]
    /// @throws RuntimeException If the [URI] cannot be converted to a [URL]
    public static URL uriToUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /// Generates the setter method name for a field name.
    /// It prepends "set" to the field name and capitalizes the first character.
    ///
    /// @param name The name of the field
    /// @return The setter method name
    public static String getSetterName(String name) {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /// Generates the getter method name for a field name.
    /// It prepends "get" to the field name and capitalizes the first character.
    ///
    /// @param name The name of the field
    /// @return The getter method name
    public static String getGetterName(String name) {
        return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /// Resolves the raw class type associated with a [Type].
    /// If the type is a [ParameterizedType], it retrieves its raw type.
    /// If the type is a [GenericArrayType], it retrieves the raw array class.
    /// If the final type is not a [Class], it throws an [IllegalStateException].
    ///
    /// @param type The type to evaluate
    /// @return The class object associated with the provided type
    /// @throws IllegalStateException If the type cannot be resolved to a class
    public static Class<?> getClassType(Type type) {
        Type finalType = switch (type) {
            case ParameterizedType parameterizedType -> parameterizedType.getRawType();
            case GenericArrayType genericArrayType -> {
                Class<?> componentClass = getClassType(genericArrayType.getGenericComponentType());
                yield componentClass.arrayType();
            }
            default -> type;
        };
        if (!(finalType instanceof Class<?> clazz)) {
            throw new IllegalStateException("Unable to find class type for %s".formatted(type));
        }
        return clazz;
    }

    /// Returns a [Gatherer] instance that works with [Optional] values.
    /// It pushes the element contained within the [Optional] to the downstream if present.
    /// If the [Optional] is empty, it returns the downstream's acceptance status.
    ///
    /// @param <T> The type of the element contained within the [Optional]
    /// @return A [Gatherer] instance that processes Optional values
    public static <T> Gatherer<Optional<T>, Void, T> optional() {
        return Gatherer.of(
                (_, optional, downstream) -> optional.map(downstream::push)
                        .orElseGet(isDownstreamAccepting(downstream))
        );
    }

    /// Creates a [Gatherer] that removes blank lines at the end of a stream.
    /// Blank lines in the middle of the stream are preserved.
    /// It uses a [LinkedList] to buffer consecutive blank lines until a non-blank line
    /// or the end of the stream is encountered.
    ///
    /// @return A [Gatherer] that removes trailing blank lines
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

    /// Creates a [Gatherer] that collects unique items from a stream.
    /// It maintains a [HashSet] of encountered items and only pushes an item downstream
    /// if it is not already present in the set.
    ///
    /// @param <T> The type of items to be processed
    /// @return A [Gatherer] instance that pushes unique items to the downstream
    public static <T> Gatherer<T, Set<T>, T> unique() {
        return Gatherer.ofSequential(
                HashSet::new,
                (state, line, downstream) -> Optional.ofNullable(line)
                        .filter(state::add)
                        .map(downstream::push)
                        .orElseGet(isDownstreamAccepting(downstream))
        );
    }

    /// Removes leading whitespace common to all non-blank lines in the text.
    /// It first calculates the minimum number of leading whitespace characters across
    /// all non-blank lines. Then, it strips that amount of leading whitespace from each line,
    /// trims trailing whitespace from each line, and removes blank lines from the end of the stream.
    ///
    /// @param text The input string
    /// @return A new string with common leading whitespace removed from non-blank lines
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

    /// Identifies the single functional method of a class.
    /// It filters all public methods of the class using [getFunctionalInterfaceFilter].
    /// If exactly one method matches, it is returned; otherwise, an [IllegalStateException] is thrown.
    ///
    /// @param clazz The class to analyze
    /// @return The single functional method of the class
    /// @throws IllegalStateException If the class is not a functional interface
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
    /// It uses the current thread's context class loader to load the specified class.
    /// If the class is not found, an empty [Optional] is returned.
    ///
    /// @param typeName The fully qualified name of the class
    /// @return An [Optional] containing the [Class] if found
    public static Optional<Class<?>> findTypeForName(String typeName) {
        try {
            return Optional.of(Thread.currentThread().getContextClassLoader().loadClass(typeName));
        } catch (ClassNotFoundException _) {
            return Optional.empty();
        }
    }

    /// Attempts to find a class type for the provided type name and imports.
    /// It delegates the resolution logic to [findTypeOptional] and throws an
    /// [InternalClassNotFoundException] if the type cannot be found.
    ///
    /// @param imports  The list of imported packages or classes
    /// @param typeName The name of the type to resolve
    /// @return The resolved [Class]
    /// @throws InternalClassNotFoundException If the type cannot be resolved
    public static Class<?> findType(List<String> imports, String typeName) throws InternalClassNotFoundException {
        return findTypeOptional(imports, typeName)
                .orElseThrow(() -> new InternalClassNotFoundException("Unable to find type for name: %s".formatted(
                        typeName)));
    }

    /// Attempts to find a class type for the provided type name and imports.
    /// It first checks if the type name is fully qualified. If not, it searches through
    /// explicit imports for a matching suffix. If still not found, it checks wildcard
    /// imports. As a last resort, it attempts to load the class from the `java.lang` package.
    ///
    /// @param imports  The list of imported packages or classes
    /// @param typeName The name of the type to resolve
    /// @return An [Optional] containing the resolved [Class]
    /// @throws InternalClassNotFoundException If multiple matching types are found in wildcard imports
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

    /// Returns a [BinaryOperator] that throws an [IllegalStateException] on duplicates.
    /// This is typically used as a merge function for collectors that do not expect
    /// duplicate keys.
    ///
    /// @param <T> The type of the input arguments
    /// @return A [BinaryOperator] that throws an [IllegalStateException]
    public static <T> BinaryOperator<T> duplicateThrowException() {
        return (_, _) -> {
            throw new IllegalStateException("Duplicate key not allowed in set or map");
        };
    }

    /// Returns a binary operator that applies the bi-function and returns the first input.
    /// It applies the provided [BiFunction] to the two inputs and returns the first one.
    /// This is useful for merge operations that perform side effects during the merge.
    ///
    /// @param <T>      The type of the input arguments and result
    /// @param function The bi-function to apply
    /// @return A binary operator that returns the first input
    public static <T> BinaryOperator<T> merge(BiFunction<T, T, ?> function) {
        return (a, b) -> {
            function.apply(a, b);
            return a;
        };
    }

    /// Returns a new [BiPredicate] that swaps the order of the arguments when testing the given predicate.
    ///
    /// @param <A>       the type of the first argument to the resulting [BiPredicate]
    /// @param <B>       the type of the second argument to the resulting [BiPredicate]
    /// @param predicate the [BiPredicate] whose arguments should be swapped
    /// @return a [BiPredicate] that tests with swapped argument order
    public static <A, B> BiPredicate<A, B> swap(BiPredicate<B, A> predicate) {
        return (a, b) -> predicate.test(b, a);
    }

    /// Finds parameterized types for a property name in public constructors.
    /// It iterates through all public constructors, identifies parameters annotated with
    /// `@NamedArg` matching the given property name, and collects their parameterized types.
    ///
    /// @param clazz        The class to inspect
    /// @param propertyName The property name to match
    /// @return A list of distinct parameterized types
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

    /// Retrieves static setter methods that take exactly two parameters and are assignable from [Node].
    /// It searches for methods that are static, match the specified name, have two parameters,
    /// and whose first parameter is assignable from the `forClass` parameter.
    ///
    /// @param forClass         The class to search
    /// @param staticClass      The class to search
    /// @param staticSetterName The name of the static setter
    /// @return A list of matching methods
    public static List<Method> findStaticSettersForNode(
            Class<?> forClass, Class<?> staticClass, String staticSetterName
    ) {
        return Stream.of(staticClass.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> staticSetterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0].isAssignableFrom(forClass))
                .toList();
    }

    /// Finds non-static setter methods with the specified name.
    /// It identifies methods that are not static, match the specified name, and have exactly
    /// one parameter.
    ///
    /// @param clazz      The class to search
    /// @param setterName The name of the setter
    /// @return A list of non-static setter methods with one parameter
    public static List<Method> findObjectSetters(Class<?> clazz, String setterName) {
        return Stream.of(clazz.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> setterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 1)
                .toList();
    }

    /// Searches for a non-static, parameterless method with the specified name.
    /// It returns an [Optional] containing the first matching method that is not static
    /// and takes no parameters.
    ///
    /// @param clazz      The class to search
    /// @param getterName The name of the method
    /// @return An [Optional] containing the found [Method]
    public static Optional<Method> findObjectGetter(Class<?> clazz, String getterName) {
        return Stream.of(clazz.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> getterName.equals(method.getName()))
                .filter(method -> method.getParameterCount() == 0)
                .findFirst();
    }

    /// Converts the path of a [URL] into an OS-specific file path string.
    /// It first converts the [URL] to a [URI] and then uses [Path#of] to get an OS-specific path.
    /// Any [URISyntaxException] encountered is wrapped in a [RuntimeException].
    ///
    /// @param url The [URL] to convert
    /// @return The OS-specific file path string
    /// @throws RuntimeException If the [URL] cannot be converted to a [URI]
    public static String urlPathToOsPathString(URL url) throws RuntimeException {
        try {
            return Path.of(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /// Creates a [Collector] that collects strings matching the pattern into an immutable list.
    /// It filters the input stream using the pattern as a predicate and accumulates
    /// matching groups (specifically the first captured group) using [collectPatternConsumer].
    ///
    /// @param pattern The regex pattern to match
    /// @return A [Collector] that accumulates matching strings
    /// @throws NullPointerException If the pattern is null
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

    /// Returns a function that retrieves the single element of a collection as an [Optional].
    /// Throws [IllegalStateException] if the collection contains more than one element.
    ///
    /// @param <E> The type of elements
    /// @param <C> The type of the collection
    /// @return A function that converts a collection to an [Optional]
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

    /// Splits the string into a stream of substrings based on the delimiter.
    /// Consecutive delimiters are treated as a single separator; empty substrings are ignored.
    /// It iterates through the string character by character, identifying the delimiter,
    /// and builds a stream of non-empty substrings between delimiters.
    ///
    /// @param string    The input string
    /// @param character The delimiter character
    /// @return A stream of substrings
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

    /// Splits a string by commas, excluding commas inside angle brackets.
    /// It iterates through the string while tracking the depth of angle brackets to ensure only commas at the top level
    /// are used as delimiters.
    /// Non-blank, stripped substrings are added to the result stream.
    ///
    /// @param string The string to split
    /// @return A stream of substrings split by commas outside brackets
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

    /// Creates a [BiConsumer] that collects pattern matches into a list.
    /// It uses a [Matcher] to find all occurrences of the pattern and adds the first captured
    /// group of each match to the provided list.
    ///
    /// @param pattern The regex pattern to match
    /// @return A [BiConsumer] that adds matches to a list
    /// @throws NullPointerException If the pattern is null
    private static BiConsumer<List<String>, String> collectPatternConsumer(Pattern pattern) {
        Objects.requireNonNull(pattern, "pattern must not be null");
        return (list, line) -> {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                list.add(matcher.group(1));
            }
        };
    }

    /// Returns a filter for methods that are public, abstract, non-default, and non-static.
    /// These are the criteria typically used to identify functional methods in an interface.
    ///
    /// @return A predicate for functional interface methods
    private static Predicate<Method> getFunctionalInterfaceFilter() {
        return Predicate.not((Method method) -> Modifier.isStatic(method.getModifiers()))
                .and(Predicate.not(Method::isDefault))
                .and(method -> Modifier.isPublic(method.getModifiers()))
                .and(method -> Modifier.isAbstract(method.getModifiers()));
    }

    /// Evaluates whether the downstream is not rejecting.
    /// It returns a [Supplier] that checks the downstream's current rejection status.
    ///
    /// @param downstream The downstream gatherer
    /// @return A supplier returning the acceptance status
    private static Supplier<Boolean> isDownstreamAccepting(Gatherer.Downstream<?> downstream) {
        return () -> !downstream.isRejecting();
    }
}
