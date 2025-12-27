package com.github.bsels.javafx.maven.plugin.utils;

import com.github.bsels.javafx.maven.plugin.CheckAndCast;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLController;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLField;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLParentNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.github.bsels.javafx.maven.plugin.fxml.ProcessedFXML;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerField;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.Visibility;
import com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import javafx.css.Styleable;
import javafx.scene.Scene;
import javafx.scene.layout.ConstraintsBase;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// The [FXMLProcessor] class is responsible for processing parsed FXML data
/// into a structured representation suitable for further FXML-related operations.
///
/// This class provides methods to parse, analyze, and optimize FXML structures.
/// Key responsibilities include converting parsed XML data into object representations
/// and optimizing the structure of FXML node trees by identifying and consolidating redundant nodes.
///
/// Dependencies such as the logging system are injected during instantiation to provide diagnostic
/// and process logging capabilities.
///
/// Note:
/// - This class operates on parsed FXML structures defined by models such as [ParsedFXML] and [FXMLNode].
/// - Private methods encapsulate the logic for handling node conversion, optimization, property extraction,
///   and static property resolution.
///
/// @param log the logging system used for internal diagnostic and process logging. Must not be null.
public record FXMLProcessor(Log log) {
    /// A compiled regular expression pattern used to match and capture generic type definitions.
    ///
    /// The pattern specifically focuses on strings in the format "generic {number}: {fully.qualified.TypeName}".
    /// It captures:
    /// - The number following the keyword "generic".
    /// - The fully qualified type name that follows the colon.
    ///
    /// This regular expression is useful for parsing and extracting generic type information
    /// from input strings that adhere to this predefined structure.
    private static final Pattern GENERICS_REGEX = Pattern.compile("generic\\s+(\\d+):\\s+((\\w+\\.)*\\w+)");

    /// Constructs a new instance of the FXMLProcessor.
    /// Ensures that the required log dependency is not null during instantiation.
    ///
    /// @param log the logging system used for internal diagnostic and process logging. Must not be null.
    public FXMLProcessor {
        Objects.requireNonNull(log, "`log` must not be null");
    }

    /// Processes the parsed FXML data to generate a [ProcessedFXML] structure. This includes:
    /// extracting imports, fields, and methods, converting the root structure, and preparing the processed data
    /// for further use in FXML-related operations.
    ///
    /// @param parsedFXML the parsed FXML structure to process. This includes the import statements, root XML structure, and generated class name. Must not be null.
    /// @return a [ProcessedFXML] object containing the processed import statements, fields, methods, root node representation, and class name.
    public ProcessedFXML process(ParsedFXML parsedFXML) {
        AtomicInteger internalVariableCounter = new AtomicInteger();
        List<String> imports = new ArrayList<>(parsedFXML.imports());
        ParsedXMLStructure fxmlRootStructure = parsedFXML.root();
        FXMLNode root = convert(imports, fxmlRootStructure, internalVariableCounter);
        root = deduplicateConstants(root);
        root = deduplicateValueNodes(root);
        root = deduplicateObjectNodes(root);
        List<FXMLField> fields = extractFields(root).distinct().toList();
        List<FXMLMethod> methods = extractMethods(imports, root).distinct().toList();
        Optional<FXMLController> controllerOptional = inspectControllerClass(imports, fxmlRootStructure);
        return new ProcessedFXML(
                Set.copyOf(imports),
                fields,
                methods,
                root,
                parsedFXML.className(),
                controllerOptional
        );
    }

    /// Inspects the controller class based on the provided imports and parsed XML structure
    /// and returns an [Optional] containing an [FXMLController] if the controller is valid.
    ///
    /// @param imports   a list of imported class names to resolve the controller class
    /// @param structure the parsed XML structure containing properties and other details
    /// @return an [Optional] containing the [FXMLController] if a valid controller is found, otherwise [Optional#empty()]
    private Optional<FXMLController> inspectControllerClass(List<String> imports, ParsedXMLStructure structure) {
        Map<String, String> properties = structure.properties();
        if (!properties.containsKey("fx:controller")) {
            return Optional.empty();
        }
        Class<?> controllerClass = Utils.findType(imports, structure.properties().get("fx:controller"));
        if (Modifier.isAbstract(controllerClass.getModifiers())) {
            throw new IllegalArgumentException("Controller class '%s' is abstract".formatted(controllerClass.getName()));
        }
        String className = Utils.improveImportForParameter(imports, controllerClass.getName());
        IntPredicate isFinal = Modifier::isFinal;
        List<ControllerField> fields = iterateClass(
                controllerClass,
                Class::getDeclaredFields,
                field -> new ControllerField(
                        getVisibility(field.getModifiers()),
                        field.getName(),
                        field.getGenericType()
                ),
                isFinal.negate()
        ).toList();
        List<ControllerMethod> methods = iterateClass(
                controllerClass,
                Class::getDeclaredMethods,
                method -> new ControllerMethod(
                        getVisibility(method.getModifiers()),
                        method.getName(),
                        method.getGenericReturnType(),
                        List.of(method.getGenericParameterTypes())),
                _ -> true
        ).toList();
        return Optional.of(new FXMLController(className, controllerClass, fields, methods));
    }

    /// Iterates through the given class and its superclasses (if any) to apply specific operations
    /// on class members using the provided mapper and filters.
    ///
    /// @param <I>              The type of class members, which must extend Member.
    /// @param <O>              The type of the output produced by the mapper function.
    /// @param clazz            The class to iterate and retrieve members from. If null, the operation will not proceed.
    /// @param elements         A function to extract an array of members from the given class.
    /// @param mapper           A function to map individual members to a desired output type.
    /// @param additionalFilter A predicate to filter members based on their modifiers.
    /// @return A Stream of the mapped outputs for members that satisfy the given filters.
    private <I extends Member, O> Stream<O> iterateClass(
            Class<?> clazz,
            Function<Class<?>, I[]> elements,
            Function<I, O> mapper,
            IntPredicate additionalFilter
    ) {
        return Optional.ofNullable(clazz)
                .stream()
                .flatMap(c -> Stream.concat(
                        Stream.of(elements.apply(c))
                                .filter(element -> !Modifier.isStatic(element.getModifiers())
                                        && additionalFilter.test(element.getModifiers()))
                                .map(mapper),
                        iterateClass(c.getSuperclass(), elements, mapper, additionalFilter)
                ));
    }

    /// Determines the visibility type of member based on its modifier flags.
    ///
    /// @param modifiers the integer value representing the modifier flags of a member
    /// @return the Visibility enumeration value corresponding to the member's visibility
    private Visibility getVisibility(int modifiers) {
        if (Modifier.isPrivate(modifiers)) {
            return Visibility.PRIVATE;
        } else if (Modifier.isProtected(modifiers)) {
            return Visibility.PROTECTED;
        } else if (Modifier.isPublic(modifiers)) {
            return Visibility.PUBLIC;
        } else {
            return Visibility.PACKAGE;
        }
    }

    /// Deduplicates object nodes within the given [FXMLNode] by combining identical nodes.
    /// The method repeatedly scans the node tree for internal object nodes of the same type with identical properties
    /// and children and replaces duplicate nodes with a single instance until no further deduplication is possible.
    ///
    /// @param fxmlNode the root [FXMLNode] to process and deduplicate object nodes.
    /// @return the modified [FXMLNode] with deduplicated object nodes.
    private FXMLNode deduplicateObjectNodes(FXMLNode fxmlNode) {
        FXMLNode originalRoot = null;
        while (!fxmlNode.equals(originalRoot)) {
            originalRoot = fxmlNode;
            List<FXMLObjectNode> objectNodes = findNodesOfType(fxmlNode, FXMLObjectNode.class)
                    .filter(FXMLObjectNode::internal)
                    .filter(Predicate.not(
                            objectNode -> Utils.isAssignableTo(Styleable.class, ConstraintsBase.class, Scene.class)
                                    .test(objectNode.clazz())
                    ))
                    .toList();
            Map<FXMLObjectNode, FXMLObjectNode> deduplicatedNodes = createDeduplicateMap(
                    objectNodes,
                    (a, b) -> a.clazz().equals(b.clazz()) &&
                            a.properties().equals(b.properties()) &&
                            a.children().equals(b.children())
            );
            fxmlNode = deduplicateNodes(fxmlNode, FXMLObjectNode.class, deduplicatedNodes);
        }
        return fxmlNode;
    }

    /// Deduplicates value nodes within the given FXMLNode by identifying and merging internal nodes
    /// with identical values and class types.
    ///
    /// @param fxmlNode the root [FXMLNode] from which to deduplicate internal value nodes
    /// @return the modified [FXMLNode] with deduplicated value nodes
    private FXMLNode deduplicateValueNodes(FXMLNode fxmlNode) {
        List<FXMLValueNode> internalValueNodes = findNodesOfType(fxmlNode, FXMLValueNode.class)
                .filter(FXMLValueNode::internal)
                .toList();
        Map<FXMLValueNode, FXMLValueNode> deduplicatedNodes = createDeduplicateMap(
                internalValueNodes,
                (a, b) -> a.value().equals(b.value()) && a.clazz().equals(b.clazz())
        );
        return deduplicateNodes(fxmlNode, FXMLValueNode.class, deduplicatedNodes);
    }

    /// Removes duplicate constant nodes from the given FXMLNode by ensuring that equivalent constant nodes are
    /// replaced with a single unified instance.
    /// The deduplication process uses an [IdentityHashMap] to track and replace duplicate instances.
    ///
    /// @param fxmlNode the root [FXMLNode] from which to deduplicate constant nodes
    /// @return a new [FXMLNode] with deduplicated constant nodes
    private FXMLNode deduplicateConstants(FXMLNode fxmlNode) {
        List<FXMLConstantNode> constantNodes = findNodesOfType(fxmlNode, FXMLConstantNode.class)
                .toList();
        Map<FXMLConstantNode, FXMLConstantNode> deduplicatedNodes = createDeduplicateMap(
                constantNodes,
                FXMLConstantNode::equals
        );
        return deduplicateNodes(fxmlNode, FXMLConstantNode.class, deduplicatedNodes);
    }

    /// Creates a deduplicate map from the provided list of nodes, where duplicate elements are identified
    /// based on the specified equality predicate.
    /// Each node will map to either itself or an already existing equivalent node in the map,
    /// as determined by the equality predicate.
    ///
    /// @param <T>               the type of elements in the list and the resulting map
    /// @param nodes             the list of nodes to deduplicate
    /// @param equalityPredicate a predicate used to determine equality between two nodes
    /// @return a map where each node from the input list is either mapped to itself or to an equivalent existing node
    private <T> Map<T, T> createDeduplicateMap(List<T> nodes, BiPredicate<T, T> equalityPredicate) {
        Map<T, T> deduplicatedNodes = new IdentityHashMap<>();
        for (T node : nodes) {
            deduplicatedNodes.values()
                    .stream()
                    .filter(existingNode -> equalityPredicate.test(existingNode, node))
                    .findAny()
                    .ifPresentOrElse(
                            duplicateNode -> deduplicatedNodes.put(node, duplicateNode),
                            () -> deduplicatedNodes.put(node, node)
                    );
        }
        return deduplicatedNodes;
    }

    /// Recursively deduplicates nodes in the given FXMLNode tree, replacing instances of the specified type
    /// with their corresponding deduplicated versions from a provided map.
    ///
    /// @param fxmlNode          the root node of the [FXMLNode] tree to process
    /// @param mapClass          the class type of nodes to deduplicate
    /// @param deduplicatedNodes a map containing deduplicated instances of the specified type
    /// @param <T>               the type of the [FXMLNode]
    /// @return the deduplicated version of the given [FXMLNode] tree
    private <T extends FXMLNode> FXMLNode deduplicateNodes(FXMLNode fxmlNode, Class<T> mapClass, Map<T, T> deduplicatedNodes) {
        if (mapClass.isInstance(fxmlNode)) {
            log.debug("Deduplicating node: %s".formatted(fxmlNode));
            T node = mapClass.cast(fxmlNode);
            return deduplicatedNodes.getOrDefault(node, node);
        }
        UnaryOperator<List<FXMLNode>> deduplicationFunction = children -> children.stream()
                .map(child -> deduplicateNodes(child, mapClass, deduplicatedNodes))
                .toList();
        return switch (fxmlNode) {
            case FXMLConstantNode node -> node;
            case FXMLObjectNode(
                    boolean internal,
                    String identifier,
                    Class<?> clazz,
                    List<FXMLProperty> properties,
                    List<FXMLNode> children,
                    List<String> genericTypes
            ) -> new FXMLObjectNode(
                    internal,
                    identifier,
                    clazz,
                    properties,
                    deduplicationFunction.apply(children),
                    genericTypes
            );
            case FXMLStaticMethod(Class<?> clazz, String method, List<FXMLNode> children) -> new FXMLStaticMethod(
                    clazz,
                    method,
                    deduplicationFunction.apply(children)
            );
            case FXMLValueNode node -> node;
            case FXMLWrapperNode(String name, List<FXMLNode> children) -> new FXMLWrapperNode(
                    name,
                    deduplicationFunction.apply(children)
            );
        };
    }

    /// Finds and retrieves all nodes of the specified type from the given FXML node and its descendants.
    /// This method traverses the node tree recursively and collects all nodes that match the given type.
    ///
    /// @param fxmlNode the root node from which the search begins
    /// @param type     the class type of the nodes to be found
    /// @param <T>      the type of [FXMLNode] to filter and return
    /// @return a stream of nodes that are of the specified type
    private <T extends FXMLNode> Stream<T> findNodesOfType(FXMLNode fxmlNode, Class<T> type) {
        Stream<T> element;
        if (type.isInstance(fxmlNode)) {
            element = Stream.of(type.cast(fxmlNode));
        } else {
            element = Stream.empty();
        }
        return Stream.concat(
                element,
                getChildrenStream(fxmlNode)
                        .flatMap(child -> findNodesOfType(child, type))
        );
    }

    /// Converts a [ParsedXMLStructure] into an [FXMLNode] representation, resolving properties, types, and children nodes as necessary.
    ///
    /// @param imports                 a list of strings representing the import statements used to resolve class types
    /// @param structure               the parsed XML structure to be converted into an FXML node
    /// @param internalVariableCounter an atomic counter used to generate unique internal variable names when no "fx:id" is provided
    /// @return an [FXMLNode] representation of the input [ParsedXMLStructure]
    private FXMLNode convert(
            List<String> imports,
            ParsedXMLStructure structure,
            AtomicInteger internalVariableCounter
    ) {
        Map<String, String> properties = structure.properties();
        try {
            final Class<?> classType;
            final String variableName;
            final boolean internal;
            if ("fx:root".equals(structure.name())) {
                if (properties.containsKey("type")) {
                    log.debug("fx:root and type properties are both set. fx:root will be ignored");
                    classType = Utils.findType(imports, properties.get("type"));
                } else {
                    throw new IllegalArgumentException("fx:root must have a type attribute");
                }
                properties = new HashMap<>(properties);
                properties.remove("type");
                variableName = FXMLSourceCodeBuilder.THIS;
                internal = true;
            } else if (structure.name().contains(".")) {
                int endIndex = structure.name().lastIndexOf('.');
                classType = Utils.findType(imports, structure.name().substring(0, endIndex));
                return new FXMLStaticMethod(
                        classType,
                        structure.name().substring(endIndex + 1),
                        structure.children()
                                .stream()
                                .map(subNode -> convert(imports, subNode, internalVariableCounter))
                                .toList()
                );
            } else {
                classType = Utils.findType(imports, structure.name());
                if (properties.containsKey("fx:id")) {
                    variableName = properties.get("fx:id");
                    internal = false;
                } else {
                    variableName = "$internalVariable$%03d".formatted(internalVariableCounter.getAndIncrement());
                    internal = true;
                }
            }
            if (properties.containsKey("fx:value")) {
                return new FXMLValueNode(internal, variableName, classType, properties.get("fx:value"));
            }
            if (properties.containsKey("fx:constant")) {
                String constantFieldName = properties.get("fx:constant");
                Type type;
                try {
                    Field field = classType.getField(constantFieldName);
                    if (!Modifier.isStatic(field.getModifiers())) {
                        throw new NoSuchFieldException("Not a static field: %s".formatted(constantFieldName));
                    }
                    type = field.getGenericType();
                } catch (NoSuchFieldException e) {
                    log.error("No such field '%s' found in class '%s'".formatted(constantFieldName, classType.getName()));
                    throw new RuntimeException(e);
                }
                return new FXMLConstantNode(classType, constantFieldName, type);
            }
            return new FXMLObjectNode(
                    internal,
                    variableName,
                    classType,
                    extractProperties(imports, classType, properties),
                    structure.children()
                            .stream()
                            .map(subNode -> convert(imports, subNode, internalVariableCounter))
                            .toList(),
                    extractGenerics(imports, structure.comments())
            );
        } catch (InternalClassNotFoundException _) {
            return new FXMLWrapperNode(
                    structure.name(),
                    structure.children()
                            .stream()
                            .map(subNode -> convert(imports, subNode, internalVariableCounter))
                            .toList()
            );
        }
    }

    /// Extracts and organizes generic type definitions from provided comments and resolves them against imports.
    /// This method processes a list of comments to identify generic types, validates their indices for sequential
    /// order, and resolves these types against the provided imports.
    /// The resulting list contains improved import definitions for the generic types.
    ///
    /// @param imports  a list of import statements used to resolve the generic types
    /// @param comments a list of comments potentially containing generic type definitions
    /// @return a list of resolved generic types with improved import definitions
    /// @throws IllegalStateException if the generics defined in the comments are not sequential
    private List<String> extractGenerics(List<String> imports, List<String> comments) {
        record Generic(int index, String type) {
        }
        return comments.stream()
                .map(String::strip)
                .gather(Gatherer.ofSequential(
                        (Void _, String input, Gatherer.Downstream<? super Generic> downStream) -> {
                            Matcher matcher = GENERICS_REGEX.matcher(input);
                            boolean keepGoing = true;
                            while (matcher.find()) {
                                keepGoing = downStream.push(
                                        new Generic(Integer.parseUnsignedInt(matcher.group(1)), matcher.group(2))
                                );
                            }
                            return keepGoing;
                        }
                ))
                .sorted(Comparator.comparing(Generic::index))
                .gather(Gatherer.ofSequential(
                        AtomicInteger::new,
                        (AtomicInteger state, Generic input, Gatherer.Downstream<? super Generic> downStream) -> {
                            if (input.index() != state.getAndIncrement()) {
                                throw new IllegalStateException(
                                        "Generic type indices are not sequential after sort: %d"
                                                .formatted(input.index())
                                );
                            }
                            return downStream.push(input);
                        }
                ))
                .map(generic -> Utils.improveImportForParameter(imports, generic.type()))
                .toList();
    }

    /// Extracts FXML properties from the given map of properties, filtering and processing entries
    /// that do not contain a ":" character in their keys. The method resolves these properties into
    /// a list of [FXMLProperty] objects, leveraging the helper function to process each entry.
    ///
    /// @param imports    the list of import statements used to resolve type references during property extraction.
    /// @param clazz      the class associated with the FXML properties being extracted. This is used to match methods or constructors.
    /// @param properties a map of property key-value pairs where keys are property names and values are their corresponding values.
    /// @return a list of `FXMLProperty` objects representing the extracted properties after resolution and filtering.
    private List<FXMLProperty> extractProperties(List<String> imports, Class<?> clazz, Map<String, String> properties) {
        Gatherer<? super Optional<FXMLProperty>, Void, FXMLProperty> optional = Utils.optional();
        return properties.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().contains(":"))
                .map(entry -> extractProperty(imports, clazz, entry))
                .gather(optional)
                .toList();
    }

    /// Extracts a single FXML property from the provided entry, determining whether it is a static property
    /// or an object/constructor property based on the property name format. If the property name contains a dot ("."),
    /// it attempts to resolve it as a static property. Otherwise, it processes it as an object or constructor property.
    ///
    /// @param imports  the list of import statements used to resolve type references during property extraction
    /// @param clazz    the class associated with the FXML properties. Used to find matching methods or constructors
    /// @param property the map entry representing a property, where the key is the property name and the value is the property value
    /// @return an [Optional<FXMLProperty>] containing the extracted property if resolvable, or an empty `Optional` if not
    private Optional<FXMLProperty> extractProperty(List<String> imports, Class<?> clazz, Map.Entry<String, String> property) {
        String propertyName = property.getKey();
        if (propertyName.contains(".")) {
            return getStaticProperty(imports, property);
        } else {
            return getObjectOrConstructorProperty(imports, clazz, property);
        }
    }

    /// Attempts to resolve an FXML property either as an object property using a setter method
    /// or as a constructor property based on the provided class, imports, and property entry.
    /// If multiple setters for a property are found or no valid setter/constructor property
    /// is determinable, the method logs appropriate details and returns an empty Optional.
    ///
    /// @param imports  the list of import statements used to resolve types referenced in property definitions
    /// @param clazz    the class associated with the FXML being processed, used to find matching setter methods or constructors
    /// @param property a map entry representing a property, where the key is the property name and the value is the property value to be set
    /// @return an Optional containing the resolved property as an FXMLProperty object if successful, or an empty Optional if no valid property was found
    private Optional<FXMLProperty> getObjectOrConstructorProperty(List<String> imports, Class<?> clazz, Map.Entry<String, String> property) {
        String propertyName = property.getKey();
        String propertyValue = property.getValue();
        String setterName = Utils.getSetterName(propertyName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        if (setters.isEmpty()) {
            String listGetterName = Utils.getGetterName(propertyName);
            try {
                Type parameterType = Utils.findGetterListAndReturnElementType(clazz, listGetterName);
                return Optional.of(new FXMLObjectProperty(
                        propertyName,
                        listGetterName + "().add",
                        parameterType,
                        propertyValue
                ));
            } catch (NoSuchMethodException _) {
                log.debug("No setter or getter list found for property %s on %s, potentially using constructor".formatted(propertyName, clazz));
                return getNamedConstructorProperty(clazz, propertyName, propertyValue);
            }
        }
        if (setters.size() > 1) {
            log.warn("Multiple %d setters for property %s on %s, skipping".formatted(setters.size(), clazz, propertyName));
            return Optional.empty();
        }
        log.debug("Found setter for property %s on %s: %s".formatted(propertyName, clazz, setterName));
        Method setter = setters.getFirst();
        Type parameterType = setter.getGenericParameterTypes()[0];
        addImport(imports, parameterType);
        return Optional.of(new FXMLObjectProperty(
                propertyName,
                setterName,
                parameterType,
                propertyValue
        ));
    }

    /// Attempts to resolve a static FXML property based on the provided property entry, the list of imports,
    /// and the presence of a static setter method in the associated static class. The method looks for a valid
    /// static setter for the property and ensures the value type compatibility. If a valid static setter is found,
    /// it resolves the property into an [FXMLStaticProperty] object.
    ///
    /// @param imports  the list of import statements used to resolve the types referenced in property definitions
    /// @param property a map entry representing the static property, where the key is the full property name
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 (including class and property name delimited by a dot ".") and the value is the property value to set
    /// @return an [Optional] containing the resolved property as an [FXMLStaticProperty] object if successful, or an empty `Optional` if no valid static setter was found or if there were multiple matching setters
    private Optional<FXMLProperty> getStaticProperty(List<String> imports, Map.Entry<String, String> property) {
        String propertyName = property.getKey();
        String propertyValue = property.getValue();
        Class<?> staticClass = Utils.findType(imports, propertyName.substring(0, propertyName.lastIndexOf('.')));
        String staticSetterName = Utils.getSetterName(propertyName.substring(propertyName.lastIndexOf('.') + 1));
        List<Method> staticSetters = Utils.findStaticSettersForNode(staticClass, staticSetterName);
        if (staticSetters.isEmpty()) {
            log.warn("No static setter found for property %s on %s, skipping".formatted(propertyName, staticClass));
            return Optional.empty();
        }
        if (staticSetters.size() > 1) {
            log.warn("Multiple %d static setters for property %s on %s, skipping".formatted(staticSetters.size(), staticClass, propertyName));
            return Optional.empty();
        }
        log.debug("Found static setter for property %s on %s: %s".formatted(propertyName, staticClass, staticSetterName));
        Method staticSetter = staticSetters.getFirst();
        Type parameterType = staticSetter.getGenericParameterTypes()[1];
        addImport(imports, parameterType);
        return Optional.of(new FXMLStaticProperty(
                propertyName,
                staticClass,
                staticSetterName,
                parameterType,
                propertyValue
        ));
    }

    /// Retrieves a named constructor property from a given class.
    /// The method analyzes public constructors of the specified class to find
    /// parameters that are annotated with `@NamedArg` and match the provided propertyName.
    /// If a matching property is found, it returns an `Optional` containing the created [FXMLProperty].
    ///
    /// @param clazz         The class to inspect for constructors with the desired named property.
    /// @param propertyName  The name of the property to locate among constructor parameters.
    /// @param propertyValue The value for the located property to associate with the resulting FXMLProperty.
    /// @return An [Optional<FXMLProperty>] representing the found named property. Returns an empty Optional if no matching property is found or if ambiguities exist.
    private Optional<FXMLProperty> getNamedConstructorProperty(
            Class<?> clazz,
            String propertyName,
            String propertyValue
    ) {
        List<Type> parameterTypes = Utils.findParameterTypeForConstructors(clazz, propertyName);
        if (parameterTypes.isEmpty()) {
            log.warn("No constructor found that has the property %s on %s, skipping".formatted(propertyName, clazz));
            return Optional.empty();
        }
        if (parameterTypes.size() > 1) {
            log.warn("Multiple %d constructors that has the property %s with different types on %s, skipping".formatted(parameterTypes.size(), clazz, propertyName));
            return Optional.empty();
        }
        return Optional.of(new FXMLConstructorProperty(propertyName, propertyValue, parameterTypes.getFirst()));
    }

    /// Processes the given `Type` object and adds the required import statements to the provided imports list.
    /// Differentiates between parameterized types, class types (primitive and non-primitive),
    /// wildcard types, and other reflective types.
    ///
    /// @param imports the list of import strings to be updated with fully qualified class names based on the type.
    /// @param type    the reflective type to be analyzed and used to determine what imports should be added.
    private void addImport(List<String> imports, Type type) {
        switch (type) {
            case ParameterizedType parameterizedType -> {
                log.debug("Found parameterized type %s".formatted(parameterizedType));
                addImport(imports, parameterizedType.getRawType());
                for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                    addImport(imports, typeArgument);
                }
            }
            case Class<?> clazz -> {
                if (clazz.isPrimitive()) {
                    log.debug("Found primitive type %s".formatted(clazz));
                } else if ("java.lang".equals(clazz.getPackageName())) {
                    log.debug("Found 'java.lang' package type: %s".formatted(clazz));
                } else if (clazz.getEnclosingClass() != null) {
                    log.debug("Found nested class type %s".formatted(clazz));
                    imports.add(clazz.getCanonicalName().replaceAll("\\$", "."));
                } else {
                    log.debug("Found non-primitive type %s outside 'java.lang' package".formatted(clazz));
                    imports.add(clazz.getCanonicalName());
                }
            }
            case WildcardType wildcardType -> {
                log.debug("Found wildcard type %s".formatted(wildcardType));
                for (Type upperBound : wildcardType.getUpperBounds()) {
                    addImport(imports, upperBound);
                }
                for (Type lowerBound : wildcardType.getLowerBounds()) {
                    addImport(imports, lowerBound);
                }
            }
            default -> log.warn("Unknown reflect type: %s for type %s".formatted(type.getClass(), type));
        }

    }

    /// Extracts a stream of [FXMLMethod] instances by analyzing the given [FXMLNode] and its properties.
    /// It processes object properties that reference methods (starting with "#"), as well as recursively
    /// extracts methods from the node's children.
    ///
    /// @param imports the list of imports available in the FXML context, used for method resolution
    /// @param node    the root node to extract methods from, including its properties and child nodes
    /// @return a distinct stream of [FXMLMethod] instances extracted from the provided node and its hierarchy
    private Stream<FXMLMethod> extractMethods(List<String> imports, FXMLNode node) {
        Stream<FXMLMethod> methodReferences = switch (node) {
            case FXMLObjectNode(_, _, Class<?> clazz, List<FXMLProperty> properties, _, List<String> generics) ->
                    getFxmlMethodStream(imports, clazz, properties, generics);
            case FXMLWrapperNode _, FXMLValueNode _, FXMLConstantNode _, FXMLStaticMethod _ -> Stream.empty();
        };
        return Stream.concat(
                methodReferences,
                getChildrenStream(node).flatMap(child -> extractMethods(imports, child))
        ).distinct();
    }

    /// Processes a list of FXML properties to return a stream of FXML methods defined by specific criteria.
    ///
    /// @param imports    a list of import strings that may be used for resolving types while constructing methods
    /// @param clazz      the class object representing the context for the FXML properties and their generics
    /// @param properties a list of FXMLProperty objects to be processed and filtered
    /// @param generics   a list of generic type parameters corresponding to the provided class
    /// @return a stream of FXMLMethod objects constructed from the filtered and processed FXML properties
    private Stream<FXMLMethod> getFxmlMethodStream(
            List<String> imports,
            Class<?> clazz,
            List<FXMLProperty> properties,
            List<String> generics
    ) {
        Map<String, String> namedGenerics = computeNamedGenericsForClass(clazz, generics);
        return properties.stream()
                .gather(CheckAndCast.of(FXMLObjectProperty.class))
                .filter(property -> property.value().startsWith("#"))
                .map(property -> constructFXMLMethod(imports, property, namedGenerics));
    }

    /// Computes a mapping of named generic type parameters for a given class to their corresponding type arguments.
    ///
    /// @param clazz    the class for which the generic type mapping is to be computed
    /// @param generics a list of type arguments corresponding to the class's type parameters
    /// @return a map where the keys are the names of the class's type parameters and the values are the provided type arguments
    /// @throws IllegalStateException if the number of generics provided does not match the number of the class's type parameters
    private Map<String, String> computeNamedGenericsForClass(Class<?> clazz, List<String> generics) {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        Map<String, String> namedGenerics;
        if (generics.isEmpty()) {
            namedGenerics = Stream.of(typeParameters)
                    .collect(Collectors.toMap(TypeVariable::getName, _ -> "?"));
        } else if (typeParameters.length != generics.size()) {
            throw new IllegalStateException(
                    "Invalid number of generics provided for %s: %d, expecting: %d".formatted(
                            clazz, generics.size(), typeParameters.length
                    )
            );
        } else {
            namedGenerics = IntStream.range(0, typeParameters.length)
                    .boxed()
                    .collect(Collectors.toMap(
                            index -> typeParameters[index].getName(),
                            generics::get
                    ));
        }
        return Map.copyOf(namedGenerics);
    }

    /// Retrieves a stream of child [FXMLNode] objects from a given [FXMLNode].
    ///
    /// @param node the [FXMLNode] whose children are to be retrieved. It can be an instance of [FXMLObjectNode], [FXMLWrapperNode], or [FXMLValueNode].
    /// @return a [Stream] of [FXMLNode] objects representing the children of the given node. If the node has no children or is of type [FXMLValueNode], an empty stream is returned.
    private Stream<FXMLNode> getChildrenStream(FXMLNode node) {
        return (switch (node) {
            case FXMLParentNode parentNode -> parentNode.children();
            case FXMLValueNode _, FXMLConstantNode _ -> List.<FXMLNode>of();
        }).stream();
    }

    /// Extracts a stream of [FXMLField] objects from the provided [FXMLNode]
    /// and its child nodes recursively. The [FXMLField] represents fields associated
    /// with the node's type, identifier, and exposure status.
    ///
    /// @param node the [FXMLNode] to extract fields from, including its children
    /// @return a stream of [FXMLField] objects representing the fields of the node and its descendants
    private Stream<FXMLField> extractFields(FXMLNode node) {
        Stream<FXMLField> objectField = switch (node) {
            case FXMLValueNode(boolean internal, String identifier, Class<?> clazz, _) ->
                    Stream.of(new FXMLField(clazz, identifier, internal, List.of()));
            case FXMLObjectNode(boolean internal, String identifier, Class<?> clazz, _, _, List<String> genericTypes) ->
                    Stream.of(new FXMLField(clazz, identifier, internal, genericTypes));
            case FXMLWrapperNode _, FXMLConstantNode _, FXMLStaticMethod _ -> Stream.empty();
        };
        return Stream.concat(objectField, getChildrenStream(node).flatMap(this::extractFields));
    }

    /// Constructs an [FXMLMethod] instance by processing the provided functional interface type
    /// and its associated generic type arguments or method signature. This method determines parameter
    /// and return types for the given property and imports necessary classes.
    ///
    /// @param imports       a list of imports required for fully qualifying the types used in the constructed [FXMLMethod]. The method may add additional imports to this list as needed.
    /// @param property      the `FXMLObjectProperty` defining the interface type and associated details to construct the [FXMLMethod].
    /// @param namedGenerics the list of generic types associated with the functional interface type. This is used to resolve the generic type arguments for the method.
    /// @return an [FXMLMethod] instance representing the processed method signature of the provided functional interface, including parameter and return types.
    /// @throws UnsupportedOperationException if the property type does not represent a functional interface.
    private FXMLMethod constructFXMLMethod(
            List<String> imports,
            FXMLObjectProperty property,
            Map<String, String> namedGenerics
    ) {
        if (property.type() instanceof ParameterizedType parameterizedType && parameterizedType.getRawType() instanceof Class<?> clazz && clazz.isInterface()) {
            TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Map<String, Type> typeParameterNameToType = new HashMap<>();
            for (int i = 0; i < typeParameters.length; i++) {
                TypeVariable<?> typeParameter = typeParameters[i];
                Type typeArgument = typeArguments[i];
                if (typeArgument instanceof WildcardType wildcardType) {
                    Class<?> type = Stream.of(
                                    wildcardType.getLowerBounds(),
                                    wildcardType.getUpperBounds()
                            ).flatMap(Arrays::stream)
                            .filter(Predicate.not(Object.class::equals))
                            .gather(CheckAndCast.of(Class.class))
                            .findFirst()
                            .orElse(Object.class);
                    typeParameterNameToType.put(typeParameter.getName(), type);
                    addImport(imports, type);
                } else {
                    typeParameterNameToType.put(typeParameter.getName(), typeArgument);
                    addImport(imports, typeArgument);
                }
            }

            Method method = Utils.getFunctionalMethod(clazz);
            List<Type> parameterTypes = Stream.of(method.getGenericParameterTypes())
                    .map(Utils.getTypeMapperFunction(log, typeParameterNameToType))
                    .toList();
            Type returnType = method.getGenericReturnType();
            addImport(imports, returnType);
            return new FXMLMethod(
                    property.value().substring(1),
                    parameterTypes,
                    returnType,
                    namedGenerics
            );
        } else if (property.type() instanceof Class<?> clazz && clazz.isInterface()) {
            Method method = Utils.getFunctionalMethod(clazz);
            List<Type> parameterTypes = List.of(method.getGenericParameterTypes());
            parameterTypes.forEach(type -> addImport(imports, type));
            Type genericReturnType = method.getGenericReturnType();
            addImport(imports, genericReturnType);
            return new FXMLMethod(
                    property.value().substring(1),
                    parameterTypes,
                    genericReturnType,
                    namedGenerics
            );
        } else {
            throw new UnsupportedOperationException("Functional interface expected, but got %s".formatted(property.type()));
        }
    }
}
