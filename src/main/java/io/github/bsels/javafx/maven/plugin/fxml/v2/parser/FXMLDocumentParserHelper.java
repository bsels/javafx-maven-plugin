package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
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
import java.util.HashMap;
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

/// Helper providing utility methods for parsing FXML documents.
/// Handles type resolution, property identification, method reference resolution,
/// resource path handling, and identifier extraction.
final class FXMLDocumentParserHelper {
    /// Pattern to match generic type definitions in comments.
    private static final Pattern GENERICS = Pattern.compile("""
            ^\\s*generic\\s+(?<index>\\d+)\\s*:\\s*(?<type>\\S(.*\\S)?)\\s*$\
            """);
    /// Pattern for matching and parsing nested generic type definitions.
    private static final Pattern NESTED_GENERICS = Pattern.compile("""
            ^\\s*((?<first>((((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*,\\s*)*(((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*),\
            \\s*)?((?<rawType>(\\w+\\.)*\\w+)(<(?<generics>[\\s\\w<,>.]*)>)?)$\
            """);
    /// Pattern for identifying FXML interfaces in a string.
    private static final Pattern INTERFACES = Pattern.compile("""
            ^\\s*interface\\s*:\\s*(?<interface>\\S([^;]*\\S)?)\\s*((;?)|\
            (;\\s*methods\\s*:\\s*(?<methods>(((\\w+\\.)*\\w+)(<([\\s\\w<,>.]*)>)?)\\s*(\\w+)\\s*\\(
            (([\\s\\w<>,.]+)(\\s*,\\s*[\\s\\w<>,.]+)*)?\\)(\\s*;\\s*(((\\w+\\.)*\\w+)(<([\\s\\w<,>.]*)>)?)\\s*(\\w+)
            \\s*\\((([\\s\\w<>,.]+)(\\s*,\\s*[\\s\\w<>,.]+)*)?\\))*\\s*;?)))\\s*$\
            """);
    /// Pattern for parsing individual method signatures defined within an interface block.
    private static final Pattern INTERFACE_METHOD = Pattern.compile("""
            \\s*(?<returnType>((\\w+\\.)*\\w+)(<([\\s\\w<,>.]*)>)?)\\s*(?<methodName>\\w+)\
            \\s*\\((?<parameters>([\\s\\w<>,.]+)(\\s*,\\s*[\\s\\w<>,.]+)*)?\\)\\s*\
            """);

    /// Logger instance for recording runtime information, debugging, and error messages.
    private final Log log;
    /// Default character set used for encoding and decoding operations.
    private final Charset defaultCharset;

    /// Initializes a new [FXMLDocumentParserHelper] instance.
    ///
    /// @param log            The logger instance
    /// @param defaultCharset The default character set
    /// @throws NullPointerException If `log` or `defaultCharset` is null
    FXMLDocumentParserHelper(Log log, Charset defaultCharset) throws NullPointerException {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.defaultCharset = Objects.requireNonNull(defaultCharset, "`defaultCharset` must not be null");
    }

    /// Builds an [FXMLType] from a [Type] and a list of generic type name strings.
    ///
    /// @param type         The base type
    /// @param buildContext The build context used for class resolution
    /// @return The corresponding [FXMLType]
    /// @throws NullPointerException If `type` or `buildContext` is null
    public FXMLType buildFXMLType(Type type, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Map<String, FXMLType> typeMapping = buildContext.typeMapping();
        return switch (type) {
            case Class<?> clazz -> {
                Map<String, FXMLType> resolvedMapping = resolveTypeMapping(clazz, buildContext);
                if (clazz.getTypeParameters().length > 0) {
                    List<FXMLType> typeArgs = Stream.of(clazz.getTypeParameters()).map(tp -> resolvedMapping.getOrDefault(
                            tp.getName(),
                            FXMLType.of(Object.class)
                    )).toList();
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
                List<FXMLType> typeArgs = Stream.of(pt.getActualTypeArguments()).map(arg -> buildFXMLType(
                        arg,
                        buildContext
                )).toList();
                yield FXMLType.of(rawClass, typeArgs);
            }
            case TypeVariable<?> tv -> typeMapping.getOrDefault(tv.getName(), FXMLType.wildcard());
            case WildcardType wt ->
                    Arrays.stream(wt.getUpperBounds()).findFirst().filter(Predicate.not(Predicate.isEqual(Object.class))).or(
                            () -> Arrays.stream(wt.getLowerBounds()).findFirst()).map(bound -> buildFXMLType(
                            bound,
                            buildContext
                    )).orElseGet(FXMLType::wildcard);
            default -> FXMLType.of(Utils.getClassType(type));
        };
    }

    /// Resolves the type mapping for a class by inspecting its hierarchy.
    ///
    /// @param clazz        The class to resolve the mapping for
    /// @param buildContext The build context used for resolving type variables
    /// @return A map of type variable names to their resolved [FXMLType]s
    /// @throws NullPointerException If `clazz` or `buildContext` is null
    public Map<String, FXMLType> resolveTypeMapping(Class<?> clazz, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Map<String, FXMLType> mapping = new LinkedHashMap<>(buildContext.typeMapping());
        Set<Type> visited = new HashSet<>();
        FXMLUtils.resolveTypeMapping(clazz, mapping, visited);
        return mapping;
    }

    /// Attempts to find a static setter method for a property name.
    ///
    /// @param buildContext The [BuildContext] used to resolve the class name
    /// @param name         The qualified name of the static property
    /// @return An [Optional] containing an [InternalStaticSetterProperty] if found
    /// @throws NullPointerException If `buildContext` or `name` is null
    public Optional<InternalStaticSetterProperty> findStaticSetter(BuildContext buildContext, String name) throws NullPointerException {
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
        return Optional.of(new InternalStaticSetterProperty(propName, staticClass, setterName, fxmlType));

    }

    /// Finds an object property for the specified class and property name.
    ///
    /// @param buildContext The build context used for type resolution
    /// @param clazz        The class to search for the property
    /// @param name         The name of the property
    /// @return An [Optional] containing the [ObjectProperty] if found
    public Optional<ObjectProperty> findObjectProperty(BuildContext buildContext, Class<?> clazz, String name) throws NullPointerException {
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
    /// @param methodName   The method name
    /// @param paramType    The expected getter parameter type
    /// @param buildContext The current build context
    /// @return The corresponding [FXMLMethod]
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If `paramType` is not a functional interface
    public FXMLMethod findMethodReferenceType(String methodName, FXMLType paramType, BuildContext buildContext) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(methodName, "`methodName` must not be null");
        Objects.requireNonNull(paramType, "`paramType` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Class<?> clazz = FXMLUtils.findRawType(paramType);
        if (FXMLUtils.isFunctionalInterface(clazz)) {
            Method functionalMethod = Utils.getFunctionalMethod(clazz);
            Map<String, FXMLType> typeMapping = buildContext.typeMapping();
            if (paramType instanceof FXMLGenericType(_, List<FXMLType> typeArgs)) {
                TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
                for (int i = 0; i < typeParameters.length; i++) {
                    String typeParamName = typeParameters[i].getName();
                    typeMapping.put(typeParamName, typeArgs.get(i));
                }
            }
            BuildContext localBuildContext = new BuildContext(buildContext, typeMapping);
            FXMLType returnType = buildFXMLType(functionalMethod.getGenericReturnType(), localBuildContext);
            List<FXMLType> parameterTypes = Arrays.stream(functionalMethod.getGenericParameterTypes()).map(parameterType -> buildFXMLType(
                    parameterType,
                    localBuildContext
            )).toList();
            return new FXMLMethod(methodName, parameterTypes, returnType);
        }
        throw new IllegalArgumentException("The parameter type '%s' must be a functional interface".formatted(paramType));
    }

    /// Resolves a resource-related path value.
    ///
    /// @param value        The raw path value to resolve
    /// @param buildContext The build context
    /// @return The resolved path string
    /// @throws NullPointerException If `value` or `buildContext` is null
    public String resolveResourcePath(String value, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(value, "`value` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (value.startsWith("/")) {
            return value;
        }
        return buildContext.resourcePath() + value;
    }

    /// Resolves an optional [FXMLIdentifier] from the specified attributes map.
    ///
    /// @param attributes The XML attributes map
    /// @return An [Optional] containing the identifier
    /// @throws NullPointerException If `attributes` is null
    public Optional<FXMLIdentifier> resolveOptionalIdentifier(Map<String, String> attributes) throws NullPointerException {
        Objects.requireNonNull(attributes, "`attributes` must not be null");
        if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            return Optional.of(new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)));
        }
        return Optional.empty();
    }

    /// Resolves and returns a [ClassAndIdentifier] based on node name, attributes, and context.
    ///
    /// @param structure    The parsed XML structure
    /// @param buildContext The current build context
    /// @param isRoot       Whether the node is the root
    /// @return A [ClassAndIdentifier] object
    /// @throws IllegalStateException    If the node is labeled as `fx:root` but is not the root
    /// @throws IllegalArgumentException If the class type cannot be resolved
    /// @throws NullPointerException     If `structure` or `buildContext` is null
    public ClassAndIdentifier resolveClassAndIdentifier(ParsedXMLStructure structure, BuildContext buildContext, boolean isRoot) throws IllegalStateException, IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        FXMLIdentifier identifier;
        Class<?> clazz;
        clazz = Utils.findType(buildContext.imports(), structure.name());
        Map<String, String> attributes = structure.properties();
        if (isRoot) {
            identifier = Optional.ofNullable(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)).map(FXMLNamedRootIdentifier::new).map(
                    Function.<FXMLIdentifier>identity()).orElse(FXMLRootIdentifier.INSTANCE);
        } else if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            identifier = new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE));
        } else {
            identifier = new FXMLInternalIdentifier(buildContext.nextInternalId());
        }
        log.debug("Parsing object with type: %s".formatted(clazz.getName()));
        return new ClassAndIdentifier(clazz, identifier);
    }

    /// Retrieves the charset of the specified XML element.
    ///
    /// @param structure The parsed XML structure
    /// @return The specified charset, or the default if none
    /// @throws NullPointerException If `structure` is null
    public Charset getCharsetOfElement(ParsedXMLStructure structure) throws NullPointerException {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Map<String, String> properties = structure.properties();
        return Optional.ofNullable(properties.get(FXMLConstants.CHARSET_ATTRIBUTE)).map(Charset::forName).orElse(
                defaultCharset);
    }

    /// Introspects the specified controller class.
    ///
    /// @param clazz        The controller class
    /// @param buildContext The build context
    /// @return An [FXMLController] instance
    /// @throws NullPointerException     If `clazz` or `buildContext` is null
    /// @throws IllegalArgumentException If the class is not public, is abstract, or lacks a public no-arg constructor
    public FXMLController introspectControllerClass(Class<?> clazz, BuildContext buildContext) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (!Modifier.isPublic(clazz.getModifiers()) || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Class '%s' must be public and not abstract".formatted(clazz.getName()));
        }
        Optional<Constructor<?>> defaultConstructor = Arrays.stream(clazz.getDeclaredConstructors()).filter(constructor -> constructor.getParameterCount() == 0).filter(
                constructor -> Modifier.isPublic(constructor.getModifiers())).findFirst();
        if (defaultConstructor.isEmpty()) {
            throw new IllegalArgumentException("Controller class must have a public no-arg constructor");
        }
        List<FXMLControllerField> fields = Stream.of(
                clazz.getDeclaredFields(),
                clazz.getFields()
        ).flatMap(Arrays::stream).gather(Utils.unique()).map(field -> createFXMLControllerField(
                buildContext,
                field
        )).toList();
        List<FXMLControllerMethod> methods = Stream.of(
                clazz.getDeclaredMethods(),
                clazz.getMethods()
        ).flatMap(Arrays::stream).gather(Utils.unique()).map(method -> createFXMLControllerMethod(
                buildContext,
                method
        )).toList();
        return new FXMLController(new FXMLClassType(clazz), fields, methods);
    }

    /// Constructs an [FXMLType] representing a generic type based on a raw class and FXML comments.
    ///
    /// @param rawClass     The raw class of the generic type
    /// @param comments     The list of comments to parse
    /// @param buildContext The build context
    /// @return The constructed [FXMLType]
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If the generic types count does not match the type parameters count
    public FXMLType constructGenericType(Class<?> rawClass, List<String> comments, BuildContext buildContext) throws NullPointerException, IllegalArgumentException {
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
                    ));
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

    /// Parses a list of comments to extract FXML interface definitions.
    ///
    /// @param comments     The list of comment strings
    /// @param buildContext The build context
    /// @return A list of [FXMLInterface] objects
    public List<FXMLInterface> parseInterfaces(List<String> comments, BuildContext buildContext) {
        Objects.requireNonNull(comments, "`comments` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        return comments.stream()
                .map(comment -> parseFXMLInterface(comment, buildContext))
                .gather(Utils.optional())
                .toList();
    }

    /// Parses a given FXML interface string and converts it into an `Optional<FXMLInterface>` by interpreting
    /// the structure and extracting relevant details such as the interface type and methods.
    ///
    /// @param interfaceString the string representation of the FXML interface to be parsed
    /// @param buildContext    the context containing import references and other metadata required for parsing
    /// @return an `Optional<FXMLInterface>` containing the constructed interface if parsing is successful, or an empty `Optional` if the string does not match the expected format
    private Optional<FXMLInterface> parseFXMLInterface(String interfaceString, BuildContext buildContext) {
        return INTERFACES.matcher(interfaceString)
                .results()
                .findFirst()
                .map(matchResult -> {
                    String interfaceTypeString = matchResult.group("interface").strip();
                    FXMLType interfaceType = parseGenericString(interfaceTypeString, buildContext.imports());
                    List<FXMLControllerMethod> interfaceMethods = switch (interfaceType) {
                        case FXMLClassType(Class<?> clazz) -> findMethodsOfInterface(clazz, List.of(), buildContext);
                        case FXMLGenericType(Class<?> clazz, List<FXMLType> genericTypes) ->
                                findMethodsOfInterface(clazz, genericTypes, buildContext);
                        case FXMLUncompiledClassType _, FXMLUncompiledGenericType _, FXMLWildcardType _ ->
                                parseInterfaceMethodsFromComment(matchResult.group("methods"), buildContext);
                    };
                    return new FXMLInterface(interfaceType, interfaceMethods);
                });
    }

    /// Parses interface methods from a given comment string and builds a list of [FXMLControllerMethod] objects based
    /// on the extracted method definitions.
    ///
    /// @param methodsString the string containing method definitions, separated by semicolons
    /// @param buildContext  the context used to construct interface methods
    /// @return a list of [FXMLControllerMethod] objects parsed from the provided string
    private List<FXMLControllerMethod> parseInterfaceMethodsFromComment(
            String methodsString,
            BuildContext buildContext
    ) {
        return Utils.splitString(methodsString, ';')
                .map(String::strip)
                .map(methodString -> parseInterfaceMethod(methodString, buildContext))
                .gather(Utils.optional())
                .toList();
    }

    /// Parses a method from a string and converts it into an [Optional] of [FXMLControllerMethod].
    ///
    /// This method extracts the return type, method name, and parameters from a string
    /// representing an interface method. It handles generic type arguments in parameters,
    /// ensuring they are correctly split even if they contain commas.
    ///
    /// @param methodString the string containing the method signature to be parsed
    /// @param buildContext the context used to resolve types and imports
    /// @return an [Optional] containing the [FXMLControllerMethod] if parsing is successful, or an empty [Optional] otherwise
    private Optional<FXMLControllerMethod> parseInterfaceMethod(String methodString, BuildContext buildContext) {
        return INTERFACE_METHOD.matcher(methodString)
                .results()
                .findFirst()
                .map(matchResult -> {
                    String returnType = matchResult.group("returnType");
                    String methodName = matchResult.group("methodName");
                    String parameters = matchResult.group("parameters");
                    List<FXMLType> parameterTypes = Stream.ofNullable(parameters)
                            .filter(Predicate.not(String::isBlank))
                            .flatMap(Utils::splitByCommaOutsideBrackets)
                            .map(parameter -> parseGenericString(parameter, buildContext.imports()))
                            .toList();
                    return new FXMLControllerMethod(
                            Visibility.PUBLIC,
                            methodName,
                            true, // Unknown
                            parseGenericString(returnType, buildContext.imports()),
                            parameterTypes
                    );
                });
    }

    /// Finds and retrieves a list of methods defined in the specified interface class while resolving generic types
    /// based on the provided type mappings.
    ///
    /// @param clazz        the interface class from which methods are to be retrieved
    /// @param genericTypes a list of generic type mappings corresponding to the type parameters of the class
    /// @param buildContext the context containing type mapping and other information needed for method building
    /// @return a list of `FXMLControllerMethod` objects representing non-static methods of the interface
    /// @throws IllegalArgumentException if the number of generic types provided does not match the number of type parameters in the specified class
    private List<FXMLControllerMethod> findMethodsOfInterface(
            Class<?> clazz,
            List<FXMLType> genericTypes,
            BuildContext buildContext
    ) throws IllegalArgumentException {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        Map<String, FXMLType> typeMapping = new HashMap<>(buildContext.typeMapping());
        if (typeParameters.length != genericTypes.size()) {
            throw new IllegalArgumentException(
                    "The number of generic types does not match the number of type parameters");
        }
        for (int i = 0; i < typeParameters.length; i++) {
            typeMapping.put(typeParameters[i].getName(), genericTypes.get(i));
        }
        BuildContext localBuildContext = new BuildContext(buildContext, typeMapping);
        return Arrays.stream(clazz.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .map(method -> createFXMLControllerMethod(localBuildContext, method))
                .toList();
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
                Modifier.isAbstract(method.getModifiers()),
                buildFXMLType(method.getGenericReturnType(), buildContext),
                Arrays.stream(method.getGenericParameterTypes()).map(parameterType -> buildFXMLType(
                        parameterType,
                        buildContext
                )).toList()
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
        if (Modifier.isPublic(modifier)) {
            return Visibility.PUBLIC;
        }
        if (Modifier.isPrivate(modifier)) {
            return Visibility.PRIVATE;
        }
        if (Modifier.isProtected(modifier)) {
            return Visibility.PROTECTED;
        }
        return Visibility.PACKAGE_PRIVATE;
    }

    /// Parses generic type definitions from comments and resolves them using the provided imports.
    ///
    /// @param comments The list of comments to parse.
    /// @param imports  The list of imports to resolve type names.
    /// @return A list of [FXMLType]s parsed from the comments.
    /// @throws IllegalArgumentException if generic indices are not sequential.
    private List<FXMLType> parseGenericsFromComments(List<String> comments, List<String> imports) throws IllegalArgumentException {
        return extractGenericsFromComments(comments).stream().map(generic -> parseGenericString(
                generic,
                imports
        )).toList();
    }

    /// Extracts generic type strings from comments and sorts them by their index.
    ///
    /// @param comments The list of comments to extract from.
    /// @return A list of generic type strings in sequential order.
    /// @throws IllegalArgumentException if generic indices are non-sequential or missing.
    private List<String> extractGenericsFromComments(List<String> comments) throws IllegalArgumentException {
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
            MatchResult matcher = NESTED_GENERICS.matcher(remaining).results().findFirst().orElseThrow();
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
