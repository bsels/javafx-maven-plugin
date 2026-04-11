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
///
/// This class handles the resolution of Java types from FXML strings,
/// identification of properties (setters, getters, and constructor parameters), resolution of method references,
/// resource path handling, and extraction of FXML identifiers.
/// It also provides introspection for controller classes and parsing of generic type information from FXML comments.
final class FXMLDocumentParserHelper {
    /// Regular expression pattern used to match generic type definitions within FXML comments.
    /// It expects a format like `generic <index> : <type>`.
    private static final Pattern GENERICS = Pattern.compile("""
            ^\\s*generic\\s+(?<index>\\d+)\\s*:\\s*(?<type>\\S(.*?\\S)?)\\s*$\
            """);
    /// Regular expression pattern for matching and parsing nested generic type definitions.
    /// This pattern is used recursively to decompose strings like `Map<String, List<Integer>>`.
    private static final Pattern NESTED_GENERICS = Pattern.compile("""
            ^\\s*((?<first>((((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*,\\s*)*(((\\w+\\.)*\\w+)(<[\\s\\w<>,.]*>)?)\\s*),\
            \\s*)?((?<rawType>(\\w+\\.)*\\w+)(<(?<generics>[\\s\\w<,>.]*)>)?)$\
            """);
    /// Regular expression pattern for identifying FXML interfaces and their methods in a string.
    /// It matches the format `interface : <type> [; methods : <method_signatures>]`.
    private static final Pattern INTERFACES = Pattern.compile("""
            ^\\s*interface\\s*:\\s*(?<interface>\\S([^;]*\\S)?)\\s*((;?)|\
            (;\\s*methods\\s*:\\s*(?<methods>(((\\w+\\.)*\\w+)(<([\\s\\w<,>.]*)>)?)\\s*(\\w+)\\s*\\(
            (([\\s\\w<>,.]+)(\\s*,\\s*[\\s\\w<>,.]+)*)?\\)(\\s*;\\s*(((\\w+\\.)*\\w+)(<([\\s\\w<,>.]*)>)?)\\s*(\\w+)
            \\s*\\((([\\s\\w<>,.]+)(\\s*,\\s*[\\s\\w<>,.]+)*)?\\))*\\s*;?)))\\s*$\
            """);
    /// Pattern for parsing individual method signatures defined within an interface block.
    /// It extracts the return type, method name, and comma-separated parameter types.
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

    /// Builds an [FXMLType] from a Java [Type] and the current [BuildContext].
    ///
    /// The logic performs the following steps based on the type of the input:
    /// - **Class**: Resolves the full type hierarchy mapping.
    ///   If the class has type parameters, they are resolved to use the mapping, defaulting to [Object] if not found.
    /// - **ParameterizedType**: Recursively builds [FXMLType]s for the raw class and all actual type arguments.
    /// - **TypeVariable**: Retrieves the resolved type from the build context's type mapping,
    ///   falling back to a wildcard if unresolved.
    /// - **WildcardType**: Extracts the first upper bound (if not [Object])
    ///   or the first lower bound to represent the type; otherwise returns a wildcard.
    /// - **Default**: Fallback to standard class resolution using [Utils.getClassType].
    ///
    /// @param type         The base Java [Type] to convert
    /// @param buildContext The build context used for class resolution and type mapping
    /// @return The corresponding [FXMLType] representing the resolved type structure
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
                    Arrays.stream(wt.getUpperBounds()).findFirst().filter(Predicate.not(Predicate.isEqual(Object.class)))
                            .or(() -> Arrays.stream(wt.getLowerBounds()).findFirst())
                            .map(bound -> buildFXMLType(
                                    bound,
                                    buildContext
                            )).orElseGet(FXMLType::wildcard);
            default -> FXMLType.of(Utils.getClassType(type));
        };
    }

    /// Resolves the type mapping for a class by inspecting its entire hierarchy.
    ///
    /// The method initializes the mapping from the build context and recursively traverses the class's superclasses
    /// and implemented interfaces.
    /// It identifies type variables and their resolved values throughout the hierarchy to ensure correct generic type
    /// resolution within the FXML structure.
    ///
    /// @param clazz        The class to resolve the mapping for
    /// @param buildContext The build context used for initial type variable resolution
    /// @return A map where keys are type variable names and values are their resolved [FXMLType]s
    /// @throws NullPointerException If `clazz` or `buildContext` is null
    public Map<String, FXMLType> resolveTypeMapping(Class<?> clazz, BuildContext buildContext)
            throws NullPointerException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        Map<String, FXMLType> mapping = new LinkedHashMap<>(buildContext.typeMapping());
        Set<Type> visited = new HashSet<>();
        FXMLUtils.resolveTypeMapping(clazz, mapping, visited);
        return mapping;
    }

    /// Attempts to find a static setter method for a property name in the form `Class.property`.
    ///
    /// The search process involves:
    /// 1. Splitting the qualified name into the class name and property name.
    /// 2. Resolving the class using the imports available in the [BuildContext].
    /// 3. Searching for a static method on that class that follows the JavaFX static setter pattern
    ///    (e.g., `set<PropertyName>(Node, Value)`).
    /// 4. Validating that exactly one such setter exists.
    /// 5. Building the [FXMLType] for the setter's value parameter.
    ///
    /// @param buildContext The [BuildContext] used to resolve the class name and types
    /// @param name         The qualified name of the static property (e.g., "GridPane.columnIndex")
    /// @return An [Optional] containing an [InternalStaticSetterProperty] if a unique setter is found;
    ///                 empty otherwise (with a warning logged).
    /// @throws NullPointerException If `buildContext` or `name` is null
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
        return Optional.of(new InternalStaticSetterProperty(propName, staticClass, setterName, fxmlType));

    }

    /// Finds an object property (setter, constructor parameter, or getter) for the specified class.
    ///
    /// The method checks for potential properties in the following priority order:
    /// 1. **Setter**: Looks for a public `set<Name>(Type)` method. If a single setter is found,
    ///    it is returned as a [ObjectProperty.MethodType.SETTER].
    /// 2. **Constructor Parameter**: Checks if the property name matches a named parameter in any of the class's
    ///    constructors (requiring `@NamedArg` or similar mechanism supported by [Utils]).
    /// 3. **Getter**: Looks for a public `get<Name>()` or `is<Name>()` method.
    ///    If found, it is returned as a [ObjectProperty.MethodType.GETTER],
    ///    often used for read-only collections that are populated rather than replaced.
    ///
    /// @param buildContext The build context used for type resolution of the property's type
    /// @param clazz        The class to search for the property
    /// @param name         The name of the property to find
    /// @return An [Optional] containing the [ObjectProperty] if a match is found; empty otherwise.
    /// @throws NullPointerException If `buildContext`, `clazz`, or `name` is null
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

    /// Resolves a method reference string from FXML into a detailed [FXMLMethod] description.
    ///
    /// The logic performs the following:
    /// 1. Verifies that the `paramType` (e.g., of an event handler property like `onAction`) is a functional interface.
    /// 2. Identifies the single abstract method (the functional method) of that interface.
    /// 3. Resolves the functional method's return type and parameter types, taking into account any generic type
    ///    arguments from `paramType`.
    /// 4. Constructs a new [FXMLMethod] that represents how the handler method in the controller should look.
    ///
    /// @param methodName   The name of the method as specified in FXML (e.g., "handleButtonAction")
    /// @param paramType    The [FXMLType] of the property where the method reference is used
    /// @param buildContext The current build context for type resolution
    /// @return An [FXMLMethod] describing the signature required for the controller method
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If `paramType` does not represent a functional interface
    public FXMLMethod findMethodReferenceType(String methodName, FXMLType paramType, BuildContext buildContext)
            throws NullPointerException, IllegalArgumentException {
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

    /// Resolves a resource-related path value relative to the current FXML location.
    ///
    /// If the value starts with `/`, it is treated as an absolute path within the classpath.
    /// Otherwise, it is prepended with the `resourcePath` stored in the [BuildContext],
    /// which represents the directory containing the FXML file currently being parsed.
    ///
    /// @param value        The raw path value from FXML (e.g., "styles.css" or "/images/logo.png")
    /// @param buildContext The build context containing the current resource path
    /// @return The resolved absolute or relative path string
    /// @throws NullPointerException If `value` or `buildContext` is null
    public String resolveResourcePath(String value, BuildContext buildContext) throws NullPointerException {
        Objects.requireNonNull(value, "`value` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        if (value.startsWith("/")) {
            return value;
        }
        return buildContext.resourcePath() + value;
    }

    /// Resolves an optional [FXMLIdentifier] from the specified attributes map of an XML element.
    ///
    /// It specifically looks for the `fx:id` attribute.
    /// If present, it creates an [FXMLExposedIdentifier] which will be mapped to a field in the controller.
    ///
    /// @param attributes The map of XML attributes for the current element
    /// @return An [Optional] containing the identifier if `fx:id` was present; empty otherwise
    /// @throws NullPointerException If `attributes` is null
    public Optional<FXMLIdentifier> resolveOptionalIdentifier(Map<String, String> attributes)
            throws NullPointerException {
        Objects.requireNonNull(attributes, "`attributes` must not be null");
        if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            return Optional.of(new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)));
        }
        return Optional.empty();
    }

    /// Resolves the Java class and FXML identifier for an XML node.
    ///
    /// This method:
    /// 1. Finds the Java [Class] corresponding to the XML node name using imports in the context.
    /// 2. Determines the appropriate [FXMLIdentifier]:
    ///    - If it's the root node: uses [FXMLNamedRootIdentifier] (if `fx:id` is present) or [FXMLRootIdentifier].
    ///    - If it has `fx:id`: uses [FXMLExposedIdentifier].
    ///    - Otherwise: generates an [FXMLInternalIdentifier] using a unique ID from the context.
    ///
    /// @param structure    The parsed XML structure of the current node
    /// @param buildContext The current build context for imports and ID generation
    /// @param isRoot       True if this node is the root of the FXML document
    /// @return A [ClassAndIdentifier] containing the resolved class and identifier
    /// @throws IllegalStateException    If validation of the root node fails
    /// @throws IllegalArgumentException If the class name cannot be resolved to a type
    /// @throws NullPointerException     If `structure` or `buildContext` is null
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

    /// Retrieves the [Charset] specified for an XML element via the `charset` attribute.
    ///
    /// It checks the element's properties for the `charset` attribute.
    /// If found, it is converted to a [Charset] instance.
    /// If not found, the default charset provided during helper initialization is returned.
    ///
    /// @param structure The parsed XML structure of the element
    /// @return The specified [Charset], or the `defaultCharset` if none is specified
    /// @throws NullPointerException If `structure` is null
    public Charset getCharsetOfElement(ParsedXMLStructure structure) throws NullPointerException {
        Objects.requireNonNull(structure, "`structure` must not be null");
        Map<String, String> properties = structure.properties();
        return Optional.ofNullable(properties.get(FXMLConstants.CHARSET_ATTRIBUTE)).map(Charset::forName).orElse(
                defaultCharset);
    }

    /// Introspects a controller class to discover its fields and methods.
    ///
    /// The introspection performs the following validations and extractions:
    /// 1. Ensures the class is public and not abstract.
    /// 2. Verifies the presence of a public no-argument constructor.
    /// 3. Extracts all fields (including private ones) and converts them to [FXMLControllerField]s.
    /// 4. Extracts all methods (including private ones) and converts them to [FXMLControllerMethod]s.
    ///
    /// Fields and methods are gathered from the class and its entire superclass hierarchy,
    /// ensuring uniqueness using [Utils.unique].
    ///
    /// @param clazz        The controller [Class] to introspect
    /// @param buildContext The build context for type resolution of members
    /// @return An [FXMLController] object containing the discovered metadata
    /// @throws NullPointerException     If `clazz` or `buildContext` is null
    /// @throws IllegalArgumentException If the class is not public, is abstract, or lacks a public no-arg constructor
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
    /// This method handles the assignment of generic type arguments to a class's type parameters.
    /// It parses the `<?generic ...?>` processing instructions (provided as `comments`) and
    /// maps them to the class's `TypeVariable`s.
    ///
    /// The process involves:
    /// 1. Parsing the list of comments to extract ordered [FXMLType] arguments.
    /// 2. Validating that the number of provided generic arguments matches the class's expected type parameters.
    /// 3. Updating the `typeMapping` in the [BuildContext] so that nested elements can resolve references to these
    ///    type variables.
    ///
    /// @param rawClass     The raw Java [Class] (e.g., `ArrayList.class`)
    /// @param comments     The list of comment/processing instruction strings from the XML
    /// @param buildContext The current build context to be updated with the new type mapping
    /// @return An [FXMLType] (typically [FXMLGenericType]) representing the specialized type
    /// @throws NullPointerException     If any parameter is null
    /// @throws IllegalArgumentException If the count of generic arguments is inconsistent with the class definition
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

    /// Parses a list of XML comments to extract `fx:interface` definitions.
    ///
    /// Each comment string is checked against the [INTERFACES] pattern.
    /// If a match is found, it is parsed into an [FXMLInterface] object,
    /// which describes an interface the controller or a specific node is expected to implement, along with its methods.
    ///
    /// @param comments     The list of comment strings from the FXML document
    /// @param buildContext The build context for type and method resolution
    /// @return A list of successfully parsed [FXMLInterface] objects
    /// @throws NullPointerException If `comments` or `buildContext` is null
    public List<FXMLInterface> parseInterfaces(List<String> comments, BuildContext buildContext) {
        Objects.requireNonNull(comments, "`comments` must not be null");
        Objects.requireNonNull(buildContext, "`buildContext` must not be null");
        return comments.stream()
                .map(comment -> parseFXMLInterface(comment, buildContext))
                .gather(Utils.optional())
                .toList();
    }

    /// Parses a given FXML interface string and converts it into an [Optional] of [FXMLInterface].
    ///
    /// The parsing logic:
    /// 1. Matches the string against the [INTERFACES] pattern.
    /// 2. Extracts the interface type name and resolves it into an [FXMLType].
    /// 3. If the type is already compiled, it reflects on the class to find its methods.
    /// 4. If the type is uncompiled (referenced by name only),
    ///    it parses method signatures provided within the comment itself.
    ///
    /// @param interfaceString The string representation of the FXML interface (from a comment)
    /// @param buildContext    The context used for resolving types and methods
    /// @return An [Optional] containing the [FXMLInterface] if parsing succeeded; empty otherwise
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

    /// Parses interface methods from a comment string and builds a list of [FXMLControllerMethod]s.
    ///
    /// The string is expected to be a semicolon-separated list of method signatures.
    /// Each signature is individually parsed by [parseInterfaceMethod].
    ///
    /// @param methodsString The string containing one or more method definitions
    /// @param buildContext  The context used to resolve types for the method signatures
    /// @return A list of [FXMLControllerMethod] objects representing the parsed methods
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

    /// Parses a single method signature from a string into an [FXMLControllerMethod].
    ///
    /// This method uses the [INTERFACE_METHOD] pattern to extract:
    /// - Return type
    /// - Method name
    /// - Parameter types (handled as a comma-separated list, respecting nested brackets for generics)
    ///
    /// All types are resolved into [FXMLType]s using the imports in the [BuildContext].
    ///
    /// @param methodString The string containing the method signature (e.g., "void handle(String)")
    /// @param buildContext The context used to resolve types and imports
    /// @return An [Optional] containing the [FXMLControllerMethod] if parsing was successful; empty otherwise
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

    /// Retrieves all non-static methods of an interface class and maps their types.
    ///
    /// This method is used when an `fx:interface` refers to an existing class.
    /// It performs the following:
    /// 1. Validates that the number of provided `genericTypes` matches the class's type parameters.
    /// 2. Creates a local type mapping for the duration of method introspection.
    /// 3. Iterates over all public methods of the class, filtering out static ones.
    /// 4. Converts each Java [Method] into an [FXMLControllerMethod] with resolved types.
    ///
    /// @param clazz        The interface [Class] to inspect
    /// @param genericTypes The list of [FXMLType] arguments for the interface's type parameters
    /// @param buildContext The parent build context
    /// @return A list of [FXMLControllerMethod] objects representing the interface's API
    /// @throws IllegalArgumentException If the count of generic types does not match the class definition
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

    /// Converts a Java [Method] into an [FXMLControllerMethod] using the provided context.
    ///
    /// It extracts visibility, name, abstract status,
    /// and resolves the return type and parameter types into [FXMLType]s using the context's type mapping.
    ///
    /// @param buildContext The context used for type resolution
    /// @param method       The Java reflection [Method] to convert
    /// @return An [FXMLControllerMethod] representing the resolved method signature
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

    /// Converts a Java [Field] into an [FXMLControllerField] using the provided context.
    ///
    /// It extracts visibility and name and resolves the field's type into an [FXMLType] based on the context's type
    /// mapping.
    ///
    /// @param buildContext The context used for type resolution
    /// @param field        The Java reflection [Field] to convert
    /// @return An [FXMLControllerField] representing the resolved field
    private FXMLControllerField createFXMLControllerField(BuildContext buildContext, Field field) {
        return new FXMLControllerField(
                visibilityOfModifier(field.getModifiers()),
                field.getName(),
                buildFXMLType(field.getGenericType(), buildContext)
        );
    }

    /// Translates Java reflection modifiers into the [Visibility] enum.
    ///
    /// It checks for `public`, `private`, and `protected` flags. If none are present, it defaults to `PACKAGE_PRIVATE`.
    ///
    /// @param modifier The integer modifier bitmask from Java reflection
    /// @return The corresponding [Visibility] level
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

    /// Parses the list of `<?generic ...?>` comments and resolves them into [FXMLType]s.
    ///
    /// The method extracts generic definitions, ensures they are indexed correctly,
    /// and resolves each type name against the provided imports.
    ///
    /// @param comments The list of comment strings from FXML
    /// @param imports  The list of active imports in the document
    /// @return A list of resolved [FXMLType]s in the order they were defined
    /// @throws IllegalArgumentException If the generic indices are not sequential starting from 0
    private List<FXMLType> parseGenericsFromComments(List<String> comments, List<String> imports)
            throws IllegalArgumentException {
        return extractGenericsFromComments(comments).stream().map(generic -> parseGenericString(
                generic,
                imports
        )).toList();
    }

    /// Extracts and sorts generic type strings from FXML comments by their defined index.
    ///
    /// It iterates over all comments, matching them against the [GENERICS] pattern.
    /// Matches are stored in a map keyed by index.
    /// Finally, it verifies that the indices form a continuous sequence starting from 0.
    ///
    /// @param comments The list of comment strings to scan
    /// @return A list of raw type strings, ordered by their index
    /// @throws IllegalArgumentException If any index is missing or out of order
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

    /// Parses a string representation of a generic type into an [FXMLType].
    ///
    /// This method handles complex types like `Map<String, List<Integer>>`.
    /// It recursively parses nested generic arguments and resolves the raw class name using the provided imports.
    /// If a class cannot be found (e.g., it's a type parameter or a class not yet compiled),
    /// it creates an uncompiled type representation.
    ///
    /// @param genericString The raw string of the type (e.g., "List<String>")
    /// @param imports       The list of imports for type resolution
    /// @return The resolved or uncompiled [FXMLType]
    /// @throws IllegalArgumentException If the string does not match the expected type format
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

    /// Recursively parses nested generic type arguments from a string.
    ///
    /// For a string like `String, Integer` (from inside `Map<...>`), it splits and parses each type.
    /// It accounts for further nesting by recursively calling itself.
    ///
    /// @param nestedGenerics The content of the angle brackets (e.g., "String, List<Integer>")
    /// @param imports        The list of imports for type resolution
    /// @return A list of [FXMLType] objects representing the arguments
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
