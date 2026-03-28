package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import io.github.bsels.javafx.maven.plugin.utils.InternalClassNotFoundException;
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// A helper class providing utility methods for parsing FXML documents.
///
/// This class handles type resolution, property identification (static and instance), method reference resolution,
/// resource path handling, and identifier extraction. It is designed to work in conjunction with
/// [FXMLDocumentParser] to transform parsed XML structures into JavaFX-compatible FXML model representations.
final class FXMLDocumentParserHelper {
    /// Pattern to match generic type definitions in comments (e.g., `generic 0: java.lang.String`).
    private static final Pattern GENERICS = Pattern.compile(
            "^\\s*generic\\s+(?<index>\\d+)\\s*:\\s*(?<type>\\S(.*\\S)?)\\s*$");
    /// A regular expression pattern used to match and parse nested generic type definitions.
    ///
    /// This pattern identifies sequences of generic type declarations that may be nested
    /// and separated by commas. It considers fully qualified names, optional generic type
    /// parameters enclosed in angle brackets, and allows for whitespace around elements.
    ///
    /// The pattern handles:
    /// - Nested generic definitions, e.g., `List<Map<String, Integer>>`.
    /// - Fully qualified class names with optional generics, e.g., `java.util.Map<String, Integer>`.
    /// - Multiple generic type declarations separated by commas, e.g., `Map<String, Integer>, List<String>`.
    /// - Arbitrary whitespace around the types and delimiters.
    ///
    /// It uses named groups `first` for the preceding types, `rawType` for the current class name,
    /// and `generics` for its type arguments.
    private static final Pattern NESTED_GENERICS = Pattern.compile(
            "^\\s*((?<first>((((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*,\\s*)*(((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*),\\s*)?((?<rawType>(\\w+\\.)*\\w+)(<(?<generics>[\\s\\w<,>.]*)>)?)$"
    );
    /// Provides a logger instance for recording runtime information, debugging, and error messages.
    ///
    /// This logger is immutable and initialized once via the constructor, ensuring consistent logging
    /// throughout the application. It is used to report warnings for unresolvable types or properties
    /// and to provide debug information during the parsing process.
    private final Log log;
    /// Represents the default character set used for encoding and decoding operations.
    /// This character set acts as the fallback mechanism when no specific character set is provided in the context of
    /// text handling processes.
    /// It is immutable and cannot be changed after initialization.
    private final Charset defaultCharset;

    /// Constructs a new [FXMLDocumentParserHelper] with the specified logger and default charset.
    ///
    /// @param log            The logger instance for reporting information and warnings.
    /// @param defaultCharset The default character set for text encoding operations.
    /// @throws NullPointerException if `log` or `defaultCharset` is null.
    FXMLDocumentParserHelper(Log log, Charset defaultCharset) throws NullPointerException {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.defaultCharset = Objects.requireNonNull(defaultCharset, "`defaultCharset` must not be null");
    }

    /// Builds an [FXMLType] from a [Type] and a list of generic type name strings.
    ///
    /// The logic:
    /// - For [Class]: Resolves type mapping and returns [FXMLClassType] or [FXMLGenericType].
    /// - For [ParameterizedType]: Recursively resolves type arguments.
    /// - For [TypeVariable]: Looks up the name in `typeMapping`.
    /// - For [WildcardType]: Resolves upper or lower bounds.
    ///
    /// @param type         The base type.
    /// @param buildContext The build context used for class resolution.
    /// @return The corresponding [FXMLType].
    /// @throws NullPointerException if `type` or `buildContext` is null.
    public FXMLType buildFXMLType(Type type, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Map<String, FXMLType> typeMapping = buildContext.typeMapping();
        return switch (type) {
            case Class<?> clazz -> {
                Map<String, FXMLType> resolvedMapping = resolveTypeMapping(clazz, buildContext);
                if (clazz.getTypeParameters().length > 0) {
                    List<FXMLType> typeArgs = Stream.of(clazz.getTypeParameters())
                            .map(tp -> resolvedMapping.getOrDefault(tp.getName(), FXMLType.of(Object.class)))
                            .toList();
                    yield FXMLType.of(clazz, typeArgs);
                }
                // Check if any superclass/interface resolved type parameters for this class
                // But wait, if this class has no type parameters itself, it should be FXMLClassType
                // UNLESS we want to represent it as a generic type of its superclass?
                // No, the FXMLType should represent the class itself.
                yield FXMLType.of(clazz);
            }
            case ParameterizedType pt -> {
                Class<?> rawClass = (Class<?>) pt.getRawType();
                List<FXMLType> typeArgs = Stream.of(pt.getActualTypeArguments())
                        .map(arg -> buildFXMLType(arg, buildContext))
                        .toList();
                yield FXMLType.of(rawClass, typeArgs);
            }
            case TypeVariable<?> tv -> typeMapping.getOrDefault(tv.getName(), FXMLType.wildcard());
            case WildcardType wt -> Arrays.stream(wt.getUpperBounds())
                    .findFirst()
                    .filter(Predicate.not(Predicate.isEqual(Object.class)))
                    .or(() -> Arrays.stream(wt.getLowerBounds()).findFirst())
                    .map(bound -> buildFXMLType(bound, buildContext))
                    .orElseGet(FXMLType::wildcard);
            default -> FXMLType.of(Utils.getClassType(type));
        };
    }

    /// Resolves the type mapping for a given class by inspecting its hierarchy.
    ///
    /// The logic creates a copy of the current type mapping and recursively populates it
    /// by visiting the class hierarchy (superclasses and interfaces).
    ///
    /// @param clazz        The class to resolve the mapping for.
    /// @param buildContext The build context used for resolving type variables.
    /// @return A map of type variable names to their resolved [FXMLType]s.
    /// @throws NullPointerException if `clazz` or `buildContext` is null.
    public Map<String, FXMLType> resolveTypeMapping(Class<?> clazz, BuildContext buildContext)
            throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Map<String, FXMLType> mapping = new LinkedHashMap<>(buildContext.typeMapping());
        Set<Type> visited = new HashSet<>();
        FXMLUtils.resolveTypeMapping(clazz, mapping, visited);
        return mapping;
    }

    /// Attempts to find a static setter method for a given property name.
    ///
    /// The property name is expected to be in the format `ClassName.propertyName`.
    /// The method resolves the class and looks for a static setter following JavaFX conventions.
    ///
    /// @param buildContext The [BuildContext] used to resolve the class name.
    /// @param name         The qualified name of the static property.
    /// @return An [Optional] containing an [InternalStaticSetterProperty] if found, or empty otherwise.
    /// @throws NullPointerException if `buildContext` or `name` is null
    public Optional<InternalStaticSetterProperty> findStaticSetter(BuildContext buildContext, String name)
            throws NullPointerException {
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        int dotIndex = name.lastIndexOf('.');
        String className = name.substring(0, dotIndex);
        String propName = name.substring(dotIndex + 1);
        String setterName = Utils.getSetterName(propName);
        Class<?> staticClass;
        List<Method> setters;
        try {
            staticClass = Utils.findType(buildContext.imports(), className);
            setters = Utils.findStaticSettersForNode(staticClass, setterName);
        } catch (InternalClassNotFoundException e) {
            log.warn("Could not resolve static property class '%s', skipping attribute '%s'".formatted(
                    className,
                    name
            ));
            return Optional.empty();
        }
        if (setters.isEmpty()) {
            log.warn("No static setter '%s' found on '%s', skipping".formatted(setterName, staticClass.getName()));
            return Optional.empty();
        }
        if (setters.size() > 1) {
            log.warn("Multiple static setters '%s' found on '%s', skipping".formatted(
                    setterName,
                    staticClass.getName()
            ));
            return Optional.empty();
        }
        Method setter = setters.getFirst();
        Type paramType = setter.getGenericParameterTypes()[1];
        FXMLType fxmlType = buildFXMLType(paramType, buildContext);
        return Optional.of(new InternalStaticSetterProperty(
                propName,
                staticClass,
                setterName,
                fxmlType
        ));

    }

    /// Finds an object property for the given class and property name.
    ///
    /// The logic searches for:
    /// 1. A setter method matching the property name.
    /// 2. A constructor parameter matching the property name.
    /// 3. A getter method matching the property name.
    ///
    /// @param buildContext The build context used for type resolution.
    /// @param clazz        The class to search for the property.
    /// @param name         The name of the property.
    /// @return An [Optional] containing the [ObjectProperty] if found, or empty otherwise.
    public Optional<ObjectProperty> findObjectProperty(BuildContext buildContext, Class<?> clazz, String name)
            throws NullPointerException {
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        // region: setter
        String setterName = Utils.getSetterName(name);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        if (!setters.isEmpty()) {
            if (setters.size() > 1) {
                log.warn("Multiple setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
                return Optional.empty();
            }
            Method setter = setters.getFirst();
            Type paramType = setter.getGenericParameterTypes()[0];
            FXMLType fxmlType = buildFXMLType(paramType, buildContext);
            return Optional.of(new ObjectProperty(
                    fxmlType,
                    name,
                    Optional.of(setterName),
                    ObjectProperty.MethodType.SETTER
            ));
        }
        // endregion
        // region: constructor parameter
        List<Type> constructorParamTypes = Utils.findParameterTypeForConstructors(clazz, name);
        if (!constructorParamTypes.isEmpty()) {
            if (constructorParamTypes.size() > 1) {
                log.warn("Multiple constructor parameters found for '%s' on '%s', skipping".formatted(
                        name,
                        clazz.getName()
                ));
                return Optional.empty();
            }
            Type paramType = constructorParamTypes.getFirst();
            FXMLType fxmlType = buildFXMLType(paramType, buildContext);
            return Optional.of(new ObjectProperty(
                    fxmlType,
                    name,
                    Optional.empty(),
                    ObjectProperty.MethodType.CONSTRUCTOR
            ));
        }
        // endregion
        // region: getter
        String getterName = Utils.getGetterName(name);
        Optional<Method> getterOptional = Utils.findObjectGetter(clazz, getterName);
        if (getterOptional.isPresent()) {
            Method getter = getterOptional.get();
            FXMLType fxmlType = buildFXMLType(getter.getGenericReturnType(), buildContext);
            return Optional.of(new ObjectProperty(
                    fxmlType,
                    name,
                    Optional.of(getterName),
                    ObjectProperty.MethodType.GETTER
            ));
        }
        // endregion
        return Optional.empty();
    }

    /// Resolves a method reference value string into an [FXMLMethod].
    ///
    /// The logic:
    /// 1. If `paramType` is a functional interface, it attempts to resolve the return type from its single abstract method.
    /// 2. If it's not a functional interface, it defaults to `void`.
    /// 3. Returns a new [FXMLMethod] instance.
    ///
    /// @param methodName   The method name (without the `#` prefix).
    /// @param paramType    The expected getter parameter type.
    /// @param buildContext The current build context.
    /// @return The corresponding [FXMLMethod].
    /// @throws NullPointerException     if any of the parameters are null
    /// @throws IllegalArgumentException if the paramType is not a functional interface
    public FXMLMethod findMethodReferenceType(String methodName, Class<?> paramType, BuildContext buildContext)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(methodName, "`methodName` must not be null");
        Objects.requireNonNull(paramType, "`paramType` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (FXMLUtils.isFunctionalInterface(paramType)) {
            Method functionalMethod = Utils.getFunctionalMethod(paramType);
            FXMLType returnType = buildFXMLType(functionalMethod.getGenericReturnType(), buildContext);
            List<FXMLType> parameterTypes = Arrays.stream(functionalMethod.getGenericParameterTypes())
                    .map(parameterType -> buildFXMLType(parameterType, buildContext))
                    .toList();
            return new FXMLMethod(methodName, parameterTypes, returnType);
        }
        throw new IllegalArgumentException("The parameter type '%s' must be a functional interface".formatted(paramType));
    }

    /// Resolves a resource-related path value against the given resource root path.
    ///
    /// If `value` starts with `/`, it is returned unchanged (absolute resource path).
    /// Otherwise, the relative directory derived from `resourcePath` is prepended to `value`.
    /// The relative directory is computed by stripping the leading `/` from `resourcePath`
    /// and appending a `/` separator if the result is non-empty.
    ///
    /// Examples:
    /// - `resourcePath = "/"`, `value = "foo.fxml"` → `"/foo.fxml"`
    /// - `resourcePath = "/examples"`, `value = "foo.fxml"` → `"/examples/foo.fxml"`
    /// - `resourcePath = "/examples"`, `value = "/foo.fxml"` → `"/foo.fxml"`
    ///
    /// @param value        The raw path value to resolve.
    /// @param buildContext The build context used for resolving resource paths.
    /// @return The resolved path string.
    /// @throws NullPointerException if `value` or `buildContext` is null.
    public String resolveResourcePath(String value, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(value, "`value` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (value.startsWith("/")) {
            return value;
        }
        return buildContext.resourcePath() + value;
    }

    /// Resolves an optional [FXMLIdentifier] from the given attributes map.
    ///
    /// The logic checks if the `fx:id` attribute is present and returns an [FXMLExposedIdentifier] if so.
    ///
    /// @param attributes The XML attributes map.
    /// @return An [Optional] containing the identifier, or empty if no `fx:id` is present.
    /// @throws NullPointerException if `attributes` is null.
    public Optional<FXMLIdentifier> resolveOptionalIdentifier(Map<String, String> attributes)
            throws NullPointerException {
        Objects.requireNonNull(attributes, "`attributes` must not be null");
        if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            return Optional.of(new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)));
        }
        return Optional.empty();
    }

    /// Resolves and returns an [ClassAndIdentifier] object based on the given node name, attributes, and context.
    ///
    /// The logic:
    /// 1. Handles `fx:root`: ensures it's at the document root and extracts its `type`.
    /// 2. Handles regular nodes: resolves the class via imports.
    /// 3. Resolves the identifier:
    ///    - Root element gets [FXMLRootIdentifier].
    ///    - Nodes with `fx:id` get [FXMLExposedIdentifier].
    ///    - Others get an [FXMLInternalIdentifier].
    ///
    /// @param structure    The parsed XML structure representing the node.
    /// @param buildContext The context in which the FXML document is being built.
    /// @param isRoot       Whether the node is the root of the FXML document.
    /// @return A [ClassAndIdentifier] containing the resolved class type and identifier for the node.
    /// @throws IllegalStateException    if the node is labeled as `fx:root` but is not the document root, or if `fx:root` is missing the required `type` attribute.
    /// @throws IllegalArgumentException if the class type cannot be resolved.
    /// @throws NullPointerException     if `structure` or `buildContext` is null.
    public ClassAndIdentifier resolveClassAndIdentifier(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            boolean isRoot
    ) throws IllegalStateException, IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        FXMLIdentifier identifier;
        Class<?> clazz;
        clazz = Utils.findType(buildContext.imports(), structure.name());
        Map<String, String> attributes = structure.properties();
        if (isRoot) {
            identifier = Optional.ofNullable(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE))
                    .map(FXMLNamedRootIdentifier::new)
                    .map(Function.<FXMLIdentifier>identity())
                    .orElse(FXMLRootIdentifier.INSTANCE);
        } else if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            identifier = new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE));
        } else {
            identifier = new FXMLInternalIdentifier(buildContext.nextInternalId());
        }
        log.debug("Parsing object with type: %s".formatted(clazz.getName()));
        return new ClassAndIdentifier(clazz, identifier);
    }

    /// Retrieves the charset of the specified XML element from the provided parsed structure.
    ///
    /// @param structure the parsed XML structure containing the element's properties
    /// @return the charset specified for the XML element, or the default charset if none is specified
    /// @throws NullPointerException if the provided structure is null
    public Charset getCharsetOfElement(ParsedXMLStructure structure) throws NullPointerException {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Map<String, String> properties = structure.properties();
        return Optional.ofNullable(properties.get(FXMLConstants.CHARSET_ATTRIBUTE))
                .map(Charset::forName)
                .orElse(defaultCharset);
    }

    /// Analyzes the provided controller class to inspect its fields, methods, and other relevant details,
    /// and returns a corresponding [FXMLController] object.
    ///
    /// @param clazz        the class object representing the controller to be introspected; must not be null, must be public, and must not be abstract.
    /// @param buildContext the context that provides necessary build-time information for processing the class; must not be null.
    /// @return an `FXMLController` instance that encapsulates the introspected fields and methods of the class.
    /// @throws NullPointerException     if either `clazz` or `buildContext` is null.
    /// @throws IllegalArgumentException if the provided class is not public, is abstract, or does not have a public no-arg constructor.
    public FXMLController introspectControllerClass(Class<?> clazz, BuildContext buildContext)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (!Modifier.isPublic(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Class '%s' must be public and not abstract".formatted(clazz.getName()));
        }
        Optional<Constructor<?>> defaultConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.getParameterCount() == 0)
                .filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
                .findFirst();
        if (defaultConstructor.isEmpty()) {
            throw new IllegalArgumentException("Controller class must have a public no-arg constructor");
        }
        List<FXMLControllerField> fields = Stream.of(clazz.getDeclaredFields(), clazz.getFields())
                .flatMap(Arrays::stream)
                .gather(Utils.unique())
                .map(field -> createFXMLControllerField(buildContext, field))
                .toList();
        List<FXMLControllerMethod> methods = Stream.of(clazz.getDeclaredMethods(), clazz.getMethods())
                .flatMap(Arrays::stream)
                .gather(Utils.unique())
                .map(method -> createFXMLControllerMethod(buildContext, method))
                .toList();
        return new FXMLController(clazz, fields, methods);
    }

    /// Creates an instance of [FXMLControllerMethod] based on the provided build context and method.
    ///
    /// @param buildContext the context used to build FXML-related types and configurations
    /// @param method       the method being analyzed and converted into an [FXMLControllerMethod]
    /// @return a newly constructed [FXMLControllerMethod] instance representing the given method
    private FXMLControllerMethod createFXMLControllerMethod(BuildContext buildContext, Method method) {
        return new FXMLControllerMethod(
                visibilityOfModifier(method.getModifiers()),
                method.getName(),
                buildFXMLType(method.getGenericReturnType(), buildContext),
                Arrays.stream(method.getGenericParameterTypes())
                        .map(parameterType -> buildFXMLType(parameterType, buildContext))
                        .toList()
        );
    }

    /// Creates an instance of [FXMLControllerField] using the provided field metadata and build context.
    ///
    /// @param buildContext the context used to build the FXML type and other field properties
    /// @param field        the field from which the [FXMLControllerField] is constructed
    /// @return a new [FXMLControllerField] instance representing the specified field with appropriate properties
    private FXMLControllerField createFXMLControllerField(BuildContext buildContext, Field field) {
        return new FXMLControllerField(
                visibilityOfModifier(field.getModifiers()),
                field.getName(),
                buildFXMLType(field.getGenericType(), buildContext)
        );
    }

    /// Determines the visibility of a given modifier.
    ///
    /// @param modifier the integer value representing a modifier, typically from the [Modifier] class
    /// @return the corresponding [Visibility] enum value, such as `PUBLIC`, `PROTECTED`, `PRIVATE`, or `PACKAGE_PRIVATE`
    private Visibility visibilityOfModifier(int modifier) {
        return switch (modifier) {
            case Modifier.PUBLIC -> Visibility.PUBLIC;
            case Modifier.PROTECTED -> Visibility.PROTECTED;
            case Modifier.PRIVATE -> Visibility.PRIVATE;
            default -> Visibility.PACKAGE_PRIVATE;
        };
    }

    /// Constructs an [FXMLType] representing a generic type based on a raw class and FXML comments.
    ///
    /// The logic parses generic type definitions from comments (e.g., `generic 0: T`)
    /// and ensures the number of provided generics matches the class's type parameters.
    ///
    /// @param rawClass     The raw class of the generic type.
    /// @param comments     The list of comments to parse for generic definitions.
    /// @param buildContext The build context containing imports to resolve type names in the generic definitions and to store the type mapping.
    /// @return The constructed [FXMLType] representing the generic type.
    /// @throws NullPointerException     if `rawClass`, `comments`, or `imports` is `null`.
    /// @throws IllegalArgumentException if the number of generic types does not match the class's type parameters.
    public FXMLType constructGenericType(Class<?> rawClass, List<String> comments, BuildContext buildContext)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(rawClass, "`rawClass` must not be null");
        Objects.requireNonNull(comments, "`comments` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        List<FXMLType> genericTypes = parseGenericsFromComments(comments, buildContext.imports());
        TypeVariable<? extends Class<?>>[] typeVariables = rawClass.getTypeParameters();
        int typeParameters = typeVariables.length;
        int numberOfGenerics = genericTypes.size();
        if (!genericTypes.isEmpty() && numberOfGenerics != typeParameters) {
            throw new IllegalArgumentException(
                    "Generic types count (%d) does not match the number of type parameters (%d).".formatted(
                            numberOfGenerics,
                            typeParameters
                    )
            );
        }
        if (typeParameters > 0) {
            Map<String, FXMLType> typeMapping = buildContext.typeMapping();
            if (genericTypes.isEmpty()) {
                for (TypeVariable<?> typeVariable : typeVariables) {
                    typeMapping.put(typeVariable.getName(), FXMLType.wildcard());
                }
            } else {
                for (int i = 0; i < numberOfGenerics; i++) {
                    FXMLType genericType = genericTypes.get(i);
                    TypeVariable<?> typeVariable = typeVariables[i];
                    typeMapping.put(typeVariable.getName(), genericType);
                }
            }
        }
        return FXMLType.of(rawClass, genericTypes);
    }

    /// Parses generic type definitions from comments and resolves them using the provided imports.
    ///
    /// @param comments The list of comments to parse.
    /// @param imports  The list of imports to resolve type names.
    /// @return A list of [FXMLType]s parsed from the comments.
    /// @throws IllegalArgumentException if generic indices are not sequential.
    private List<FXMLType> parseGenericsFromComments(List<String> comments, List<String> imports)
            throws IllegalArgumentException {
        return extractGenericsFromComments(comments)
                .stream()
                .map(generic -> parseGenericString(generic, imports))
                .toList();
    }

    /// Extracts generic type strings from comments and sorts them by their index.
    ///
    /// @param comments The list of comments to extract from.
    /// @return A list of generic type strings in sequential order.
    /// @throws IllegalArgumentException if generic indices are non-sequential or missing.
    private List<String> extractGenericsFromComments(List<String> comments)
            throws IllegalArgumentException {
        Map<Integer, String> genericTypes = new TreeMap<>(Comparator.naturalOrder());
        for (String comment : comments) {
            Matcher matcher = GENERICS.matcher(comment);
            if (matcher.find()) {
                genericTypes.put(Integer.parseInt(matcher.group("index")), matcher.group("type"));
            }
        }
        List<Integer> sortedKeys = genericTypes.keySet().stream().sorted().toList();
        List<String> generics = new ArrayList<>();
        for (int i = 0; i < sortedKeys.size(); i++) {
            if (i != sortedKeys.get(i)) {
                throw new IllegalArgumentException("Generic types are having non-sequential indices: %s".formatted(
                        sortedKeys));
            }
            generics.add(genericTypes.get(i));
        }
        return generics;
    }

    /// Parses a generic type string into an [FXMLType].
    ///
    /// The logic handles nested generics and type resolution using the provided imports.
    ///
    /// @param genericString The string representing the generic type.
    /// @param imports       The list of imports for type resolution.
    /// @return The parsed [FXMLType].
    /// @throws IllegalArgumentException if the generic string is invalid.
    private FXMLType parseGenericString(String genericString, List<String> imports) {
        Matcher matcher = NESTED_GENERICS.matcher(genericString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid generic type string: %s".formatted(genericString));
        }
        String rawType = matcher.group("rawType").strip();
        String nestedGenerics = matcher.group("generics");
        List<FXMLType> nestedTypeArgs = parseNestedGenerics(nestedGenerics, imports);
        try {
            Class<?> resolvedClass = Utils.findType(imports, rawType);
            return FXMLType.of(resolvedClass, nestedTypeArgs);
        } catch (InternalClassNotFoundException _) {
            return FXMLType.of(rawType, nestedTypeArgs);
        }
    }

    /// Recursively parses nested generic types from a string.
    ///
    /// @param nestedGenerics The string containing nested generic type definitions.
    /// @param imports        The list of imports for type resolution.
    /// @return A list of parsed [FXMLType]s.
    private List<FXMLType> parseNestedGenerics(String nestedGenerics, List<String> imports) {
        if (nestedGenerics == null) {
            return List.of();
        }
        List<FXMLType> result = new LinkedList<>();
        String remaining = nestedGenerics;
        while (remaining != null) {
            MatchResult matcher = NESTED_GENERICS.matcher(remaining)
                    .results()
                    .findFirst()
                    .orElseThrow();
            String rawType = matcher.group("rawType").strip();
            String deeper = matcher.group("generics");
            List<FXMLType> deeperArgs = parseNestedGenerics(deeper, imports);
            FXMLType type;
            try {
                Class<?> resolvedClass = Utils.findType(imports, rawType);
                type = FXMLType.of(resolvedClass, deeperArgs);
            } catch (InternalClassNotFoundException _) {
                type = FXMLType.of(rawType, deeperArgs);
            }
            result.addFirst(type);
            remaining = matcher.group("first");
        }
        return List.copyOf(result);
    }

}
