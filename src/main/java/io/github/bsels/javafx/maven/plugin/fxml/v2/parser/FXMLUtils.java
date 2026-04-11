package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import javafx.beans.DefaultProperty;
import javafx.event.EventHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Utility class for common FXML-related validation tasks.
public final class FXMLUtils {
    /// Pattern to validate standard Java identifier names.
    private static final Predicate<String> VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z$][a-zA-Z0-9_$]*$")
            .asPredicate();

    /// A cache for storing resolved default property names for classes.
    private static final Map<Class<?>, Optional<String>> CLASS_TO_DEFAULT_PROPERTY_NAME_CACHE = new HashMap<>();
    /// A cache for storing resolved return types for static factory methods.
    private static final Map<ClassAndString, Type> CLASS_FACTORY_METHOD_TYPE_CACHE = new HashMap<>();
    /// A cache for storing resolved types for constant fields.
    private static final Map<ClassAndString, Type> CLASS_CONSTANT_TYPE_CACHE = new HashMap<>();

    /// Private constructor to prevent instantiation.
    private FXMLUtils() {
        // No instances needed
    }

    /// Determines the raw type of an [FXMLType] instance.
    ///
    /// @param type The [FXMLType] instance
    /// @return The raw [Class] type; returns `Object.class` for unsupported or wildcard types
    /// @throws NullPointerException If `type` is null
    public static Class<?> findRawType(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findTypeInformation(
                type,
                Object.class,
                Function.identity(),
                (clazz, _) -> clazz
        );
    }

    /// Determines the element type of an [FXMLType] representing a [Collection].
    ///
    /// Logic:
    /// 1. For [FXMLWildcardType], [FXMLUncompiledClassType], and [FXMLUncompiledGenericType], returns `Object`.
    /// 2. For [FXMLClassType], traverses the hierarchy to find the element type.
    /// 3. For [FXMLGenericType], builds a type mapping and traverses the hierarchy to resolve the element type.
    ///
    /// @param type The [FXMLType] representing the collection
    /// @return The [FXMLType] of the collection's elements, or `Object` if undetermined
    /// @throws NullPointerException If `type` is null
    public static FXMLType findCollectionValueType(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findTypeInformation(
                type,
                FXMLType.of(Object.class),
                clazz -> FXMLType.of(Utils.findCollectionValueTypeFromHierarchy(clazz)),
                (clazz, generics) -> {
                    Map<String, FXMLType> mapping = buildInitialTypeMapping(clazz, generics);
                    resolveTypeMapping(clazz, mapping, new HashSet<>());
                    // Collection's type parameter is E (index 0)
                    String elementTypeParamName = Collection.class.getTypeParameters()[0].getName();
                    return mapping.getOrDefault(elementTypeParamName, FXMLType.of(Object.class));
                }
        );
    }

    /// Determines the key and value types of an [FXMLType] representing a [Map].
    ///
    /// Logic:
    /// 1. For [FXMLWildcardType], [FXMLUncompiledClassType], and [FXMLUncompiledGenericType], returns `Object` for both.
    /// 2. For [FXMLClassType], traverses the hierarchy to find concrete key and value types.
    /// 3. For [FXMLGenericType], builds a type mapping and traverses the hierarchy to resolve key and value types.
    ///
    /// @param type The [FXMLType] representing the map
    /// @return A [Map.Entry] containing the key and value types, both defaulting to `Object` if undetermined
    /// @throws NullPointerException If `type` is null
    public static Map.Entry<FXMLType, FXMLType> findMapKeyAndValueTypes(FXMLType type)
            throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findTypeInformation(
                type,
                Map.entry(FXMLType.of(Object.class), FXMLType.of(Object.class)),
                clazz -> Map.entry(
                        FXMLType.of(Utils.findMapKeyTypeFromHierarchy(clazz)),
                        FXMLType.of(Utils.findMapValueTypeFromHierarchy(clazz))
                ),
                (clazz, generics) -> {
                    Map<String, FXMLType> mapping = buildInitialTypeMapping(clazz, generics);
                    resolveTypeMapping(clazz, mapping, new HashSet<>());
                    // Map's type parameters are K (index 0) and V (index 1)
                    String keyTypeParamName = Map.class.getTypeParameters()[0].getName();
                    String valueTypeParamName = Map.class.getTypeParameters()[1].getName();
                    return Map.entry(
                            mapping.getOrDefault(keyTypeParamName, FXMLType.of(Object.class)),
                            mapping.getOrDefault(valueTypeParamName, FXMLType.of(Object.class))
                    );
                }
        );
    }

    /// Recursively resolves the type mapping for a type and updates the mapping.
    /// Traverses the class hierarchy to propagate type variable bindings.
    ///
    /// @param type    The type to resolve
    /// @param mapping The mapping to update
    /// @param visited The set of visited types to avoid infinite recursion
    public static void resolveTypeMapping(Type type, Map<String, FXMLType> mapping, Set<Type> visited) {
        if (type == null || type == Object.class || !visited.add(type)) {
            return;
        }
        if (type instanceof Class<?> clazz) {
            Type superclass = clazz.getGenericSuperclass();
            if (superclass != null) {
                resolveTypeMapping(superclass, mapping, visited);
            }
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                resolveTypeMapping(genericInterface, mapping, visited);
            }
        } else if (type instanceof ParameterizedType pt) {
            Class<?> rawClass = (Class<?>) pt.getRawType();
            TypeVariable<?>[] rawTypeParameters = rawClass.getTypeParameters();
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            int size = Math.min(rawTypeParameters.length, actualTypeArguments.length);
            for (int i = 0; i < size; i++) {
                Type arg = actualTypeArguments[i];
                switch (arg) {
                    case TypeVariable<?> tv -> {
                        FXMLType resolved = mapping.get(tv.getName());
                        if (resolved != null) {
                            mapping.put(rawTypeParameters[i].getName(), resolved);
                        }
                    }
                    case Class<?> argClass -> mapping.put(rawTypeParameters[i].getName(), FXMLType.of(argClass));
                    case ParameterizedType argPt -> {
                        Class<?> argRawClass = (Class<?>) argPt.getRawType();
                        List<FXMLType> argTypeArgs = Stream.of(argPt.getActualTypeArguments())
                                .map(innerArg -> {
                                    if (innerArg instanceof Class<?> innerClass) {
                                        return FXMLType.of(innerClass);
                                    }
                                    return FXMLType.wildcard();
                                })
                                .toList();
                        mapping.put(rawTypeParameters[i].getName(), FXMLType.of(argRawClass, argTypeArgs));
                    }
                    case null, default -> mapping.put(rawTypeParameters[i].getName(), FXMLType.wildcard());
                }
            }
            resolveTypeMapping(rawClass, mapping, visited);
        }
    }

    /// Checks if the specified name is an invalid Java identifier.
    ///
    /// @param name The name to check
    /// @return `true` if the name is invalid; `false` otherwise
    /// @throws NullPointerException If `name` is null
    public static boolean isInvalidIdentifierName(String name) {
        Objects.requireNonNull(name, "`name` must not be null");
        return !VALID_NAME_PATTERN.test(name);
    }

    /// Checks whether the specified class is or extends `javafx.event.EventHandler`.
    ///
    /// @param clazz The class to check
    /// @return `true` if assignable to `javafx.event.EventHandler`; `false` otherwise
    /// @throws NullPointerException If `clazz` is null
    public static boolean isEventHandlerType(Class<?> clazz) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return EventHandler.class.isAssignableFrom(clazz);
    }

    /// Checks whether the specified class is a functional interface.
    ///
    /// @param clazz The class to check
    /// @return `true` if a functional interface; `false` otherwise
    /// @throws NullPointerException If `clazz` is null
    public static boolean isFunctionalInterface(Class<?> clazz) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        if (!clazz.isInterface()) {
            return false;
        }
        if (clazz.isAnnotationPresent(FunctionalInterface.class)) {
            return true;
        }
        long abstractMethodCount = Stream.of(clazz.getMethods())
                .map(Method::getModifiers)
                .filter(Modifier::isAbstract)
                .count();
        return abstractMethodCount == 1;
    }

    /// Resolves the [Type] of a constant field on the specified class.
    ///
    /// @param clazz        The class defining the constant
    /// @param constantName The name of the constant field
    /// @return The [Type] of the constant field
    /// @throws IllegalArgumentException If the field does not exist or is not static
    /// @throws NullPointerException     If `clazz` or `constantName` is null
    public static Type resolveConstantType(Class<?> clazz, String constantName)
            throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(constantName, "`constantName` must not be null");
        return CLASS_CONSTANT_TYPE_CACHE.computeIfAbsent(
                new ClassAndString(clazz, constantName),
                FXMLUtils::internalResolveConstantType
        );
    }

    /// Finds the return type of a static factory method.
    ///
    /// @param clazz             The class declaring the factory method
    /// @param factoryMethodName The name of the factory method
    /// @return The return [Type] of the factory method
    /// @throws IllegalArgumentException If the factory method cannot be found
    /// @throws NullPointerException     If `clazz` or `factoryMethodName` is null
    public static Type findFactoryMethodReturnType(Class<?> clazz, String factoryMethodName)
            throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(factoryMethodName, "`factoryMethodName` must not be null");
        return CLASS_FACTORY_METHOD_TYPE_CACHE.computeIfAbsent(
                new ClassAndString(clazz, factoryMethodName),
                FXMLUtils::internalFindFactoryMethodReturnType
        );
    }

    /// Resolves the default property name for a class.
    ///
    /// @param clazz The class to inspect
    /// @return An [Optional] containing the default property name
    /// @throws NullPointerException If `clazz` is null
    public static Optional<String> resolveDefaultPropertyName(Class<?> clazz) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return CLASS_TO_DEFAULT_PROPERTY_NAME_CACHE.computeIfAbsent(
                clazz,
                FXMLUtils::internalResolveDefaultPropertyName
        );
    }

    /// Checks if the specified string has a non-skippable prefix.
    ///
    /// @param key The string to check
    /// @return `true` if the string has a non-skippable prefix; `false` otherwise
    /// @throws NullPointerException If `key` is null
    public static boolean hasNonSkippablePrefix(String key) throws NullPointerException {
        Objects.requireNonNull(key, "`key` must not be null");
        return !key.startsWith(FXMLConstants.FX_PREFIX) && !key.startsWith(FXMLConstants.XML_NAMESPACE_PREFIX);
    }

    /// Builds an initial type variable mapping from a class's own type parameters to the provided generic arguments.
    ///
    /// @param clazz    The class whose type parameters are to be mapped.
    /// @param generics The list of [FXMLType] arguments corresponding to the class's type parameters.
    /// @return A [Map] from a type parameter name to its resolved [FXMLType].
    private static Map<String, FXMLType> buildInitialTypeMapping(Class<?> clazz, List<FXMLType> generics) {
        Map<String, FXMLType> mapping = new LinkedHashMap<>();
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        int size = Math.min(typeParameters.length, generics.size());
        for (int i = 0; i < size; i++) {
            mapping.put(typeParameters[i].getName(), generics.get(i));
        }
        return mapping;
    }

    /// Resolves the default property name for a class using the [DefaultProperty] annotation.
    ///
    /// The logic traverses the class hierarchy (including superclasses) to find the first
    /// occurrence of the [DefaultProperty] annotation and returns its value.
    ///
    /// @param clazz The class to inspect for a [DefaultProperty] annotation.
    /// @return The default property name if found, or an empty [Optional] if not found.
    private static Optional<String> internalResolveDefaultPropertyName(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null) {
            DefaultProperty annotation = current.getDeclaredAnnotation(DefaultProperty.class);
            if (annotation != null) {
                return Optional.of(annotation.value());
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /// Finds the return type of the static factory method.
    ///
    /// The logic searches for a static method on the given class that:
    /// - Matches the provided `factoryMethodName`.
    /// - Has no parameters.
    /// - Is public and accessible.
    ///
    /// @param classAndString The class declaring the factory method and the name of the factory method.
    /// @return The return [Type] of the factory method.
    /// @throws IllegalArgumentException if the factory method cannot be found.
    private static Type internalFindFactoryMethodReturnType(ClassAndString classAndString)
            throws IllegalArgumentException {
        Class<?> clazz = classAndString.clazz();
        String factoryMethodName = classAndString.string();
        return Stream.of(clazz.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getName().equals(factoryMethodName))
                .filter(method -> method.getParameterCount() == 0)
                .map(Method::getGenericReturnType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No static factory method '%s' found on '%s'".formatted(
                        factoryMethodName,
                        clazz.getName()
                )));
    }

    /// Resolves the [Type] of a constant field on the given class.
    ///
    /// The logic:
    /// 1. Attempts to find a public field with the given name.
    /// 2. Verifies that the field is static.
    /// 3. Returns the field's generic type.
    ///
    /// @param classAndString The class defining the constant and the name of the constant field.
    /// @return The [Type] of the constant field.
    /// @throws IllegalArgumentException if the field does not exist or is not static.
    private static Type internalResolveConstantType(ClassAndString classAndString)
            throws IllegalArgumentException {
        Class<?> clazz = classAndString.clazz();
        String constantName = classAndString.string();
        try {
            Field field = clazz.getField(constantName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Field `%s` on `%s` is not static".formatted(
                        constantName,
                        clazz.getName()
                ));
            }
            return field.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(
                    "No such constant field `%s` on `%s`".formatted(
                            constantName,
                            clazz.getName()
                    ), e
            );
        }
    }

    /// Resolves type information based on the provided [FXMLType].
    /// The method determines the resolved value by applying the appropriate resolver
    /// or returns a default value for specific type cases.
    ///
    /// @param <T>                 The type of the resolved value.
    /// @param type                The [FXMLType] to analyze.
    /// @param defaultValue        The default value to return for certain type cases.
    /// @param simpleTypeResolver  A function to handle resolution for simple class types.
    /// @param genericTypeResolver A function to handle resolution for generic types, provided with the raw class and its list of type arguments.
    /// @return Resolved type information of type `T`, determined by the `type` and the corresponding resolver.
    private static <T> T findTypeInformation(
            FXMLType type,
            T defaultValue,
            Function<Class<?>, T> simpleTypeResolver,
            BiFunction<Class<?>, List<FXMLType>, T> genericTypeResolver
    ) {
        return switch (type) {
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ -> defaultValue;
            case FXMLClassType(Class<?> clazz) -> simpleTypeResolver.apply(clazz);
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) -> genericTypeResolver.apply(clazz, generics);
        };
    }

    /// Internal record used as a cache key, combining a class and a related string (e.g., method or field name).
    ///
    /// @param clazz  The class component of the key.
    /// @param string The string component of the key.
    private record ClassAndString(Class<?> clazz, String string) {
        /// Constructs a `ClassAndString` record.
        ///
        /// @param clazz  The class component.
        /// @param string The string component.
        /// @throws NullPointerException if `clazz` or `string` is `null`.
        private ClassAndString {
            Objects.requireNonNull(clazz, "`clazz` must not be null");
            Objects.requireNonNull(string, "`string` must not be null");
        }
    }
}
