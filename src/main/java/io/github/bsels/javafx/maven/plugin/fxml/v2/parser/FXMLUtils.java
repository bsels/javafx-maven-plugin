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
import java.util.Arrays;
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

    /// Determines the raw type of [FXMLType] instance.
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

    /// Traverses the [FXMLType] hierarchy to find the [Collection] interface and extract its element type.
    /// For uncompiled types or raw types, returns an [FXMLClassType] of [Object].
    ///
    /// @param type The [FXMLType] to search
    /// @return The [FXMLType] of the collection's element type, or [FXMLClassType] of [Object] if undetermined
    /// @throws NullPointerException If `type` is null
    public static FXMLType findCollectionValueTypeFromHierarchy(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findFXMLGenericTypeFromHierarchy(type, Collection.class, 0);
    }

    /// Traverses the [FXMLType] hierarchy to find the [Map] interface and extract its key type.
    /// For uncompiled types or raw types, returns an [FXMLClassType] of [Object].
    ///
    /// @param type The [FXMLType] to search
    /// @return The [FXMLType] of the map's key type, or [FXMLClassType] of [Object] if undetermined
    /// @throws NullPointerException If `type` is null
    public static FXMLType findMapKeyTypeFromHierarchy(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findFXMLGenericTypeFromHierarchy(type, Map.class, 0);
    }

    /// Traverses the [FXMLType] hierarchy to find the [Map] interface and extract its value type.
    /// For uncompiled types or raw types, returns an [FXMLClassType] of [Object].
    ///
    /// @param type The [FXMLType] to search
    /// @return The [FXMLType] of the map's value type, or [FXMLClassType] of [Object] if undetermined
    /// @throws NullPointerException If `type` is null
    public static FXMLType findMapValueTypeFromHierarchy(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        return findFXMLGenericTypeFromHierarchy(type, Map.class, 1);
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
            resolveParameterizedTypeMapping(mapping, visited, pt);
        }
    }

    /// Resolves and populates a mapping of parameterized types into a provided `Map`.
    /// This method aims to map type variables of a parameterized type to their corresponding actual type arguments or
    /// to a wildcard type if resolution is not possible and updates the provided mapping with the resolved types.
    ///
    /// @param mapping           A `Map<String, FXMLType>` representing the mapping from type variable names to their resolved `FXMLType` instances. This map will be updated with the resolved mappings for the parameterized type.
    /// @param visited           A `Set<Type>` used to avoid circular type resolution by tracking the types that have already been visited during the resolution process.
    /// @param parameterizedType The `ParameterizedType` whose type variables need to be resolved and mapped to their corresponding `FXMLType`.
    private static void resolveParameterizedTypeMapping(
            Map<String, FXMLType> mapping,
            Set<Type> visited,
            ParameterizedType parameterizedType
    ) {
        Class<?> rawClass = (Class<?>) parameterizedType.getRawType();
        TypeVariable<?>[] rawTypeParameters = rawClass.getTypeParameters();
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
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

    /// Finds the return type of the static factory method.
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

    /// Traverses the class hierarchy to find the specified target interface and extract its type argument as an [FXMLType].
    /// For uncompiled types or raw types, returns an [FXMLClassType] of [Object].
    /// It recursively checks the class's generic interfaces and its superclass.
    ///
    /// @param type              The [FXMLType] to search
    /// @param targetInterface   The target interface to find
    /// @param typeArgumentIndex The index of the type argument to extract
    /// @return The [FXMLType] of the extracted type argument, or [FXMLClassType] of [Object] if undetermined
    private static FXMLType findFXMLGenericTypeFromHierarchy(FXMLType type, Class<?> targetInterface, int typeArgumentIndex) {
        return switch (type) {
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ -> FXMLType.OBJECT;
            case FXMLClassType(Class<?> clazz) ->
                    findFXMLGenericTypeFromHierarchyForClass(clazz, targetInterface, typeArgumentIndex);
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) -> {
                Map<String, FXMLType> mapping = buildInitialTypeMapping(clazz, generics);
                resolveTypeMapping(clazz, mapping, new HashSet<>());
                TypeVariable<?>[] typeParameters = targetInterface.getTypeParameters();
                if (typeArgumentIndex >= typeParameters.length) {
                    yield FXMLType.OBJECT;
                }
                yield mapping.getOrDefault(typeParameters[typeArgumentIndex].getName(), FXMLType.OBJECT);
            }
        };
    }

    /// Traverses the class hierarchy to find the specified target interface and extract its type argument as an [FXMLType].
    /// It recursively checks the class's generic interfaces, its generic superclass, and its superclass.
    /// If the target interface is found as a [ParameterizedType], the type argument at the specified index is extracted.
    ///
    /// @param clazz             The class to search
    /// @param targetInterface   The target interface to find
    /// @param typeArgumentIndex The index of the type argument to extract
    /// @return The [FXMLType] of the extracted type argument, or [FXMLClassType] of [Object] if undetermined
    private static FXMLType findFXMLGenericTypeFromHierarchyForClass(Class<?> clazz, Class<?> targetInterface, int typeArgumentIndex) {
        if (clazz == null || clazz == Object.class || clazz == targetInterface) {
            return FXMLType.OBJECT;
        }
        return Arrays.stream(clazz.getGenericInterfaces())
                .map(genericInterface -> getFXMLInterfaceElementType(
                        targetInterface,
                        typeArgumentIndex,
                        genericInterface
                ))
                .gather(Utils.optional())
                .findFirst()
                .or(() -> getFXMLGenericSuperclassElementType(clazz, targetInterface, typeArgumentIndex))
                .or(
                        () -> Optional.ofNullable(clazz.getSuperclass())
                                .map(superclass -> findFXMLGenericTypeFromHierarchyForClass(
                                        superclass,
                                        targetInterface,
                                        typeArgumentIndex
                                ))
                                .filter(Predicate.not(FXMLType.OBJECT::equals))
                )
                .or(
                        () -> Arrays.stream(clazz.getGenericInterfaces())
                                .map(Utils::getClassType)
                                .map(genericInterface -> findFXMLGenericTypeFromHierarchyForClass(
                                        genericInterface,
                                        targetInterface,
                                        typeArgumentIndex
                                ))
                                .findFirst()
                )
                .orElse(FXMLType.OBJECT);
    }

    /// Checks the generic superclass of a class for a concrete type argument of the target interface.
    /// If the generic superclass is a [ParameterizedType], it first tries to match it directly against
    /// the target interface, then resolves the superclass with its concrete type arguments and recurses.
    ///
    /// @param clazz             The class whose generic superclass is to be inspected
    /// @param targetInterface   The target interface to find
    /// @param typeArgumentIndex The index of the type argument to extract
    /// @return An [Optional] containing the extracted [FXMLType], or empty if not applicable
    private static Optional<FXMLType> getFXMLGenericSuperclassElementType(Class<?> clazz, Class<?> targetInterface, int typeArgumentIndex) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType parameterizedSuperType)) {
            return Optional.empty();
        }
        Optional<FXMLType> direct = getFXMLInterfaceElementType(targetInterface, typeArgumentIndex, genericSuperclass);
        if (direct.isPresent()) {
            return direct;
        }
        Type[] superActualArgs = parameterizedSuperType.getActualTypeArguments();
        if (Arrays.stream(superActualArgs).anyMatch(arg -> !(arg instanceof Class<?>))) {
            return Optional.empty();
        }
        Class<?> superRawClass = (Class<?>) parameterizedSuperType.getRawType();
        List<FXMLType> superTypeArgs = Arrays.stream(superActualArgs)
                .map(arg -> FXMLType.of((Class<?>) arg))
                .toList();
        FXMLType superFXMLType = FXMLType.of(superRawClass, superTypeArgs);
        FXMLType result = findFXMLGenericTypeFromHierarchy(superFXMLType, targetInterface, typeArgumentIndex);
        if (result.equals(FXMLType.OBJECT)) {
            return Optional.empty();
        }
        return Optional.of(result);
    }

    /// Extracts the element type of the target interface from a generic interface definition as an [FXMLType].
    /// It checks if the provided [Type] is a [ParameterizedType] that matches the target interface.
    /// If so, it returns the type argument at the specified index as an [FXMLType].
    ///
    /// @param targetInterface   The interface to search for
    /// @param typeArgumentIndex The index of the type argument
    /// @param genericInterface  The generic interface type to analyze
    /// @return An [Optional] containing the extracted [FXMLType], or empty if not applicable
    private static Optional<FXMLType> getFXMLInterfaceElementType(
            Class<?> targetInterface, int typeArgumentIndex, Type genericInterface
    ) {
        if (!(genericInterface instanceof ParameterizedType parameterizedType)) {
            return Optional.empty();
        }
        if (parameterizedType.getRawType() != targetInterface) {
            return Optional.empty();
        }
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (typeArgumentIndex >= actualTypeArguments.length) {
            return Optional.of(FXMLType.OBJECT);
        }
        Type arg = actualTypeArguments[typeArgumentIndex];
        FXMLType result = switch (arg) {
            case Class<?> argClass -> FXMLType.of(argClass);
            case ParameterizedType argPt -> {
                Class<?> argRawClass = (Class<?>) argPt.getRawType();
                List<FXMLType> argTypeArgs = Stream.of(argPt.getActualTypeArguments())
                        .map(innerArg -> innerArg instanceof Class<?> innerClass ? FXMLType.of(innerClass) : FXMLType.wildcard())
                        .toList();
                yield FXMLType.of(argRawClass, argTypeArgs);
            }
            case null, default -> FXMLType.OBJECT;
        };
        return Optional.of(result);
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
