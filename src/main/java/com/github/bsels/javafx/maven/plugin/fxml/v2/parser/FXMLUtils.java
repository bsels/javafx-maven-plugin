package com.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
import com.github.bsels.javafx.maven.plugin.utils.Utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Utility class for common FXML-related validation tasks.
public final class FXMLUtils {
    /// Pattern to validate standard Java identifier names.
    private static final Predicate<String> VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z$][a-zA-Z0-9_$]*$")
            .asPredicate();

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
    ///                         of the map's value type, both defaulting to `FXMLType.of(Object.class)` if they cannot be determined.
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

}
