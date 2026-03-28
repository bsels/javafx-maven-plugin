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

    /// Determines the raw type of the given [FXMLType] instance.
    ///
    /// @param type the [FXMLType] instance whose raw type is to be identified
    /// @return the raw [Class] type corresponding to the FXMLType instance; returns `Object.class` for unsupported or wildcard types
    public static Class<?> findRawType(FXMLType type) {
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> clazz;
            case FXMLGenericType(Class<?> clazz, List<FXMLType> _) -> clazz;
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ -> Object.class;
        };
    }

    /// Determines the element type of the given [FXMLType] when it represents a [Collection].
    ///
    /// The logic:
    /// 1. For [FXMLWildcardType], [FXMLUncompiledClassType], and [FXMLUncompiledGenericType],
    ///    returns `Object` as the element type.
    /// 2. For [FXMLClassType], traverses the class hierarchy via [Utils#findCollectionValueTypeFromHierarchy(Class)] to find
    ///    the [Collection] interface and extract the concrete element type from its type parameter,
    ///    defaulting to `Object` if not found.
    /// 3. For [FXMLGenericType], builds an initial type mapping from the class's own type parameters to the provided
    ///    generic arguments, then traverses the full class hierarchy via [#resolveTypeMapping(Type, Map, Set)]
    ///    to propagate type variable bindings down to the [Collection] interface's type parameter.
    ///    The element type (`E`, at index 0 of [Collection]'s type parameters) is then looked up in the resolved mapping.
    ///
    /// @param type The [FXMLType] representing the collection type.
    /// @return The [FXMLType] of the collection's element type, or `FXMLType.of(Object.class)` if it cannot be determined.
    public static FXMLType findCollectionValueType(FXMLType type) {
        return switch (type) {
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ ->
                    FXMLType.of(Object.class);
            case FXMLClassType(Class<?> clazz) -> FXMLType.of(Utils.findCollectionValueTypeFromHierarchy(clazz));
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) -> {
                Map<String, FXMLType> mapping = buildInitialTypeMapping(clazz, generics);
                resolveTypeMapping(clazz, mapping, new HashSet<>());
                // Collection's type parameter is E (index 0)
                String elementTypeParamName = Collection.class.getTypeParameters()[0].getName();
                yield mapping.getOrDefault(elementTypeParamName, FXMLType.of(Object.class));
            }
        };
    }

    /// Determines the key and value types of the given [FXMLType] when it represents a [Map].
    ///
    /// The logic:
    /// 1. For [FXMLWildcardType], [FXMLUncompiledClassType], and [FXMLUncompiledGenericType],
    ///    returns `Object` as both the key and value types.
    /// 2. For [FXMLClassType], traverses the class hierarchy via [Utils#findMapKeyTypeFromHierarchy(Class)] and
    ///    [Utils#findMapValueTypeFromHierarchy(Class)] to find the [Map] interface and extract the concrete key and
    ///    value types from its type parameters, defaulting to `Object` if not found.
    /// 3. For [FXMLGenericType], builds an initial type mapping from the class's own type parameters to the provided
    ///    generic arguments, then traverses the full class hierarchy via [#resolveTypeMapping(Type, Map, Set)]
    ///    to propagate type variable bindings down to the [Map] interface's type parameters.
    ///    The key type (`K`, at index 0) and value type (`V`, at index 1) of [Map]'s type parameters are then
    ///    looked up in the resolved mapping.
    ///
    /// @param type The [FXMLType] representing the map type.
    /// @return A [Map.Entry] where the key is the [FXMLType] of the map's key type and the value is the [FXMLType]
    ///                                                                                                                                                                                                         of the map's value type, both defaulting to `FXMLType.of(Object.class)` if they cannot be determined.
    public static Map.Entry<FXMLType, FXMLType> findMapKeyAndValueTypes(FXMLType type) {
        return switch (type) {
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ ->
                    Map.entry(FXMLType.of(Object.class), FXMLType.of(Object.class));
            case FXMLClassType(Class<?> clazz) -> Map.entry(
                    FXMLType.of(Utils.findMapKeyTypeFromHierarchy(clazz)),
                    FXMLType.of(Utils.findMapValueTypeFromHierarchy(clazz))
            );
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) -> {
                Map<String, FXMLType> mapping = buildInitialTypeMapping(clazz, generics);
                resolveTypeMapping(clazz, mapping, new HashSet<>());
                // Map's type parameters are K (index 0) and V (index 1)
                String keyTypeParamName = Map.class.getTypeParameters()[0].getName();
                String valueTypeParamName = Map.class.getTypeParameters()[1].getName();
                yield Map.entry(
                        mapping.getOrDefault(keyTypeParamName, FXMLType.of(Object.class)),
                        mapping.getOrDefault(valueTypeParamName, FXMLType.of(Object.class))
                );
            }
        };
    }

    /// Recursively resolves the type mapping for a given type and updates the mapping,
    /// traversing the class hierarchy to propagate type variable bindings.
    ///
    /// The logic handles:
    /// - [Class]: Recursively calls for superclass and interfaces.
    /// - [ParameterizedType]: Maps actual type arguments to the raw class's type parameters.
    /// It avoids infinite recursion by tracking visited types.
    ///
    /// @param type    The type to resolve.
    /// @param mapping The mapping to update.
    /// @param visited The set of visited types to avoid infinite recursion.
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

    /// Checks if the provided name is an invalid Java identifier.
    ///
    /// @param name The name to check.
    /// @return `true` if the name is invalid, `false` otherwise.
    /// @throws NullPointerException if `name` is `null`.
    public static boolean isInvalidIdentifierName(String name) {
        Objects.requireNonNull(name, "`name` must not be null");
        return !VALID_NAME_PATTERN.test(name);
    }

    /// Checks whether the given class is or extends `javafx.event.EventHandler`.
    ///
    /// @param clazz The class to check.
    /// @return `true` if the class is assignable to `javafx.event.EventHandler`; `false` otherwise.
    /// @throws NullPointerException if `clazz` is `null`.
    public static boolean isEventHandlerType(Class<?> clazz) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return EventHandler.class.isAssignableFrom(clazz);
    }

    /// Checks whether the given class is a functional interface.
    ///
    /// The logic:
    /// 1. Verifies the class is an interface.
    /// 2. Checks for the [@FunctionalInterface] annotation.
    /// 3. Alternatively, counts the number of abstract methods; if exactly one, it's considered a functional interface.
    ///
    /// @param clazz The class to check.
    /// @return `true` if the class is a functional interface; `false` otherwise.
    /// @throws NullPointerException if `clazz` is `null`.
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

    /// Resolves the [Type] of a constant field on the given class.
    ///
    /// The logic:
    /// 1. Attempts to find a public field with the given name.
    /// 2. Verifies that the field is static.
    /// 3. Returns the field's generic type.
    ///
    /// @param clazz        The class defining the constant.
    /// @param constantName The name of the constant field.
    /// @return The [Type] of the constant field.
    /// @throws IllegalArgumentException if the field does not exist or is not static.
    /// @throws NullPointerException     if `clazz` or `constantName` is `null`.
    public static Type resolveConstantType(Class<?> clazz, String constantName)
            throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(constantName, "`constantName` must not be null");
        return CLASS_CONSTANT_TYPE_CACHE.computeIfAbsent(
                new ClassAndString(clazz, constantName),
                FXMLUtils::internalResolveConstantType
        );
    }

    /// Finds the return type of the static factory method.
    ///
    /// The logic searches for a static method on the given class that:
    /// - Matches the provided `factoryMethodName`.
    /// - Has no parameters.
    /// - Is public and accessible.
    ///
    /// @param clazz             The class declaring the factory method.
    /// @param factoryMethodName The name of the factory method.
    /// @return The return [Type] of the factory method.
    /// @throws IllegalArgumentException if the factory method cannot be found.
    /// @throws NullPointerException     if either `clazz` or `factoryMethodName` is null.
    public static Type findFactoryMethodReturnType(Class<?> clazz, String factoryMethodName)
            throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(factoryMethodName, "`factoryMethodName` must not be null");
        return CLASS_FACTORY_METHOD_TYPE_CACHE.computeIfAbsent(
                new ClassAndString(clazz, factoryMethodName),
                FXMLUtils::internalFindFactoryMethodReturnType
        );
    }

    /// Resolves the default property name for a class using the [DefaultProperty] annotation.
    ///
    /// The logic traverses the class hierarchy (including superclasses) to find the first
    /// occurrence of the [DefaultProperty] annotation and returns its value.
    ///
    /// @param clazz The class to inspect for a [DefaultProperty] annotation.
    /// @return The default property name if found, or an empty [Optional] if not found.
    /// @throws NullPointerException if `clazz` is null.
    public static Optional<String> resolveDefaultPropertyName(Class<?> clazz) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        return CLASS_TO_DEFAULT_PROPERTY_NAME_CACHE.computeIfAbsent(
                clazz,
                FXMLUtils::internalResolveDefaultPropertyName
        );
    }

    /// Determines if the given string has a non-skippable prefix.
    ///
    /// The logic checks if the string starts with `fx:` or `xmlns`.
    ///
    /// @param key The string to check for skippable prefixes.
    /// @return `true` if the string starts with a non skippable prefix; `false` otherwise.
    /// @throws NullPointerException if `key` is null.
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
