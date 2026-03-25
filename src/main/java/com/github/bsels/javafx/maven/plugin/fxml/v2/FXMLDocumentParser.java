package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLFileScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLResource;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLTranslation;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import com.github.bsels.javafx.maven.plugin.utils.InternalClassNotFoundException;
import com.github.bsels.javafx.maven.plugin.utils.Utils;
import javafx.beans.DefaultProperty;
import javafx.event.EventHandler;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Parses a [ParsedFXML] raw XML structure into a [FXMLDocument] V2 model.
///
/// This parser handles all standard FXML constructs including object instantiation,
/// property attributes, static properties, `fx:root`, `fx:id`, `fx:constant`,
/// `fx:value`, `fx:include`, `fx:copy`, `fx:reference`, `fx:script`, and inline scripts.
///
/// The parsing process follows these main steps:
/// 1. Initializes a [BuildContext] with imports from the [ParsedFXML].
/// 2. Identifies the controller class if specified via the `fx:controller` attribute.
/// 3. Parses the root XML structure recursively into a hierarchy of [AbstractFXMLValue] objects.
/// 4. Collects and organizes all identified definitions, scripts, and type mappings into the final [FXMLDocument].
public final class FXMLDocumentParser {
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

    /// A mapping of special JavaFX FXML element names to their corresponding handler methods.
    /// This map is used to associate specific predefined FXML element constants with
    /// their respective parser functions, enabling dynamic handling and processing
    /// during FXML document parsing.
    ///
    /// Key-Value pairs:
    /// - Keys: String constants representing special FXML element names, defined in `FXMLConstants`.
    /// - Values: Method references implementing the parsing logic for each corresponding FXML element.
    ///
    /// This map is immutable and initialized with predefined mappings for processing the following special FXML elements:
    /// - `fx:copy`: Maps to the `FXMLDocumentParser::parseFXCopy` handler.
    /// - `fx:define`: Maps to the `FXMLDocumentParser::parseFXDefine` handler.
    /// - `fx:include`: Maps to the `FXMLDocumentParser::parseFXInclude` handler.
    /// - `fx:reference`: Maps to the `FXMLDocumentParser::parseFXReference` handler.
    /// - `fx:root`: Maps to the `FXMLDocumentParser::parseFXRoot` handler.
    /// - `fx:script`: Maps to the `FXMLDocumentParser::parseFXScript` handler.
    private static final Map<String, SpecialFXElementHandler> SPECIAL_FX_ELEMENTS_HANDLERS;

    static {
        SPECIAL_FX_ELEMENTS_HANDLERS = Map.ofEntries(
                Map.entry(FXMLConstants.FX_COPY_ELEMENT, FXMLDocumentParser::parseFXCopy),
                Map.entry(FXMLConstants.FX_DEFINE_ELEMENT, FXMLDocumentParser::parseFXDefine),
                Map.entry(FXMLConstants.FX_INCLUDE_ELEMENT, FXMLDocumentParser::parseFXInclude),
                Map.entry(FXMLConstants.FX_REFERENCE_ELEMENT, FXMLDocumentParser::parseFXReference),
                Map.entry(FXMLConstants.FX_ROOT_ELEMENT, FXMLDocumentParser::parseFXRoot),
                Map.entry(FXMLConstants.FX_SCRIPT_ELEMENT, FXMLDocumentParser::parseFXScript)
        );
    }

    /// Provides a logger instance for recording runtime information, debugging, and error messages.
    ///
    /// This logger is immutable and initialized once via the constructor, ensuring consistent logging
    /// throughout the application. It is used to report warnings for unresolvable types or properties
    /// and to provide debug information during the parsing process.
    private final Log log;

    /// Compact constructor to validate the log dependency.
    ///
    /// It ensures that the required [Log] instance is not `null` before assignment.
    ///
    /// @param log The logging instance used for diagnostic output.
    /// @throws NullPointerException if `log` is `null`.
    public FXMLDocumentParser(Log log) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
    }

    /// Parses the provided [ParsedFXML] instance and constructs an [FXMLDocument].
    ///
    /// The logic follows these steps:
    /// 1. Ensures `parsedFXML` is not `null`.
    /// 2. Creates a root [BuildContext] using the imports from the FXML file.
    /// 3. Resolves the controller class if the `fx:controller` attribute is present on the root element.
    /// 4. Recursively parses the root element using [#parseElement(ParsedXMLStructure, BuildContext, boolean)].
    /// 5. Verifies that the resulting root value is an instance of [AbstractFXMLObject].
    /// 6. Returns a new [FXMLDocument] containing all gathered information.
    ///
    /// @param parsedFXML The [ParsedFXML] object to be parsed.
    /// @return An [FXMLDocument] that represents the parsed structure of the given [ParsedFXML].
    /// @throws NullPointerException  if `parsedFXML` is `null`.
    /// @throws IllegalStateException if the root element does not parse to an [AbstractFXMLObject].
    public FXMLDocument parse(ParsedFXML parsedFXML) {
        Objects.requireNonNull(parsedFXML, "`parsedFXML` must not be null");
        ParsedXMLStructure rootStructure = parsedFXML.root();
        BuildContext buildContext = new BuildContext(parsedFXML.imports());

        Optional<Class<?>> controller = Optional.ofNullable(
                rootStructure.properties().get(FXMLConstants.FX_CONTROLLER_ATTRIBUTE)
        ).map(name -> Utils.findType(buildContext.imports(), name));

        Optional<AbstractFXMLValue> rootValue = parseElement(rootStructure, buildContext, true);
        if (rootValue.isEmpty() || !(rootValue.get() instanceof AbstractFXMLObject root)) {
            throw new IllegalStateException(
                    "Root object must be an instance of AbstractFXMLObject, but was %s".formatted(rootValue.map(
                            value -> value.getClass().getName()
                    ).orElse("null"))
            );
        }

        return new FXMLDocument(
                parsedFXML.className(),
                root,
                controller,
                parsedFXML.scriptNamespace(),
                buildContext.imports(),
                buildContext.definitions(),
                buildContext.scripts()
        );
    }

    /// Parses an XML structure into an FXML value.
    ///
    /// The logic involves:
    /// 1. Checking if the node name corresponds to a special FX element (like `fx:include`).
    /// 2. Resolving the Java class and FXML identifier for the element.
    /// 3. Identifying any factory method specified by `fx:factory`.
    /// 4. Determining the actual type, considering the factory method's return type.
    /// 5. Resolving type mappings for generics based on the class hierarchy.
    /// 6. Extracting generic type information from XML comments.
    /// 7. Selecting the appropriate parsing strategy based on whether the class is a [Collection], [Map], or a single object.
    ///
    /// @param structure    The parsed XML structure.
    /// @param buildContext The context used during the building process.
    /// @param isRoot       Whether the object is the root of the FXML document.
    /// @return An [AbstractFXMLValue] representing the parsed XML structure.
    /// @throws IllegalStateException if the parsing fails or if nested generics cannot be parsed.
    private Optional<AbstractFXMLValue> parseElement(ParsedXMLStructure structure, BuildContext buildContext, boolean isRoot)
            throws IllegalStateException {
        String nodeName = structure.name();
        if (SPECIAL_FX_ELEMENTS_HANDLERS.containsKey(nodeName)) {
            return parseSpecialFXElements(structure, buildContext, isRoot);
        }
        ClassAndIdentifier classAndIdentifier = resolveClassAndIdentifier(structure, buildContext, isRoot);
        return Optional.of(parseNormalElements(structure, buildContext, classAndIdentifier));
    }

    /// Parses the given special FX element and returns its corresponding abstract FXML value.
    ///
    /// The logic looks up the handler for the element's name in the [#SPECIAL_FX_ELEMENTS_HANDLERS] map
    /// and applies it to the current structure and build context.
    ///
    /// @param structure    The parsed XML structure representing the special FX element to be processed.
    /// @param buildContext The context in which the build is occurring, providing necessary utilities and state.
    /// @param isRoot       Indicates whether the current element is the root of the FXML document.
    /// @return The abstract FXML value resulting from processing the special FX element.
    /// @throws IllegalStateException if the element is not the root and its name is 'fx:root'.
    private Optional<AbstractFXMLValue> parseSpecialFXElements(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            boolean isRoot
    ) {
        String name = structure.name();
        if (FXMLConstants.FX_ROOT_ELEMENT.equals(name) && !isRoot) {
            throw new IllegalStateException("%s must be the root element of the FXML document".formatted(name));
        }
        return SPECIAL_FX_ELEMENTS_HANDLERS.get(name)
                .handle(this, structure, buildContext)
                .map(Function.identity());
    }

    /// Parses the given XML structure into an `AbstractFXMLValue` based on the provided context and class information.
    /// This method handles different types of objects, collections, and maps and resolves the appropriate type mappings
    /// and factory methods if present.
    ///
    /// @param structure          The parsed XML structure containing properties, comments, and other elements.
    /// @param buildContext       The current build context used for resolving type mappings and other configurations.
    /// @param classAndIdentifier The class and identifier information used to determine the type of object to parse and instantiate.
    /// @return An [AbstractFXMLValue] representing the parsed XML components, configured based on the input structure and context.
    private AbstractFXMLValue parseNormalElements(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier
    ) {
        Map<String, String> properties = structure.properties();
        Class<?> clazz = classAndIdentifier.clazz();
        String factoryMethodName = properties.get(FXMLConstants.FX_FACTORY_ATTRIBUTE);
        Optional<FXMLFactoryMethod> factoryMethod = Optional.ofNullable(factoryMethodName)
                .map(method -> new FXMLFactoryMethod(clazz, method));

        Type actualType = clazz;
        if (factoryMethodName != null) {
            actualType = findFactoryMethodReturnType(clazz, factoryMethodName);
        }

        Map<String, FXMLType> typeMapping = resolveTypeMapping(Utils.getClassType(actualType), buildContext);
        BuildContext localContext = new BuildContext(buildContext, typeMapping);

        FXMLType fxmlType = buildFXMLType(
                actualType,
                extractGenericsFromComments(structure.comments()),
                localContext,
                localContext.typeMapping()
        );
        Class<?> actualClass = Utils.getClassType(actualType);
        ParseContext context = new ParseContext(structure, localContext, classAndIdentifier, fxmlType, factoryMethod);
        if (Collection.class.isAssignableFrom(actualClass)) {
            return parseCollection(context);
        } else if (Map.class.isAssignableFrom(actualClass)) {
            return parseMap(context);
        } else {
            return parseSingle(context);
        }
    }

    /// Parses an `fx:copy` element into an [FXMLCopy] value.
    ///
    /// The logic:
    /// 1. Extracts the `source` attribute, ensuring it is present.
    /// 2. Resolves an optional FXML identifier (`fx:id`); if absent, generates a new internal identifier.
    /// 3. Returns a new [FXMLCopy] instance.
    ///
    /// @param structure    The parsed XML structure representing the `fx:copy` element.
    /// @param buildContext The context used during the building process.
    /// @return An [FXMLCopy] representing the parsed element.
    /// @throws IllegalArgumentException if the `source` attribute is missing.
    private Optional<FXMLCopy> parseFXCopy(ParsedXMLStructure structure, BuildContext buildContext) {
        Map<String, String> properties = structure.properties();
        String source = properties.get(FXMLConstants.SOURCE_ATTRIBUTE);
        if (source == null) {
            throw new IllegalArgumentException("`source` attribute is required for fx:copy");
        }
        FXMLIdentifier copyId = resolveOptionalIdentifier(properties)
                .orElseGet(() -> new FXMLInternalIdentifier(buildContext.nextInternalId()));
        return Optional.of(new FXMLCopy(copyId, source));
    }

    /// Parses an `fx:define` element from the given parsed XML structure and updates the build context
    /// with any extracted definitions. Returns an empty optional as this method produces no specific value.
    ///
    /// @param structure    the parsed XML structure containing the FXDefine element and its children
    /// @param buildContext the build context used to store extracted definitions
    /// @return an empty [Optional] as the method does not produce a concrete result
    private Optional<AbstractFXMLValue> parseFXDefine(ParsedXMLStructure structure, BuildContext buildContext) {
        structure.children()
                .stream()
                .map(child -> parseElement(child, buildContext, false))
                .gather(Utils.optional())
                .forEach(buildContext.definitions()::add);
        return Optional.empty();
    }

    /// Parses an `fx:include` element into an [FXMLInclude] value.
    ///
    /// The logic:
    /// 1. Extracts the `source` attribute, ensuring it is present.
    /// 2. Resolves an optional FXML identifier (`fx:id`); if absent, generates a new internal identifier.
    /// 3. Returns a new [FXMLInclude] instance.
    ///
    /// @param structure    The parsed XML structure representing the `fx:include` element.
    /// @param buildContext The context used during the building process.
    /// @return An [FXMLInclude] representing the parsed element.
    /// @throws IllegalArgumentException if the `source` attribute is missing.
    private Optional<FXMLInclude> parseFXInclude(ParsedXMLStructure structure, BuildContext buildContext) {
        Map<String, String> properties = structure.properties();
        String source = properties.get(FXMLConstants.SOURCE_ATTRIBUTE);
        if (source == null) {
            throw new IllegalArgumentException("`source` attribute is required for fx:include");
        }
        FXMLIdentifier includeId = resolveOptionalIdentifier(properties)
                .orElseGet(() -> new FXMLInternalIdentifier(buildContext.nextInternalId()));
        return Optional.of(new FXMLInclude(includeId, source));
    }

    /// Parses an `fx:root` element from the given XML structure and creates an appropriate representation of it,
    /// wrapped in an [Optional].
    /// The method validates that the `fx:root` element has a type attribute
    /// and determines the corresponding class using the provided build context.
    ///
    /// @param structure    the parsed XML structure containing the `fx:root` element to be processed
    /// @param buildContext the build context providing imports and other contextual information necessary for resolving types and building objects
    /// @return an [Optional] containing an AbstractFXMLValue representation of the `fx:root` element
    /// @throws IllegalStateException if the `fx:root` element does not have a "type" attribute
    private Optional<AbstractFXMLValue> parseFXRoot(ParsedXMLStructure structure, BuildContext buildContext) {
        String typeName = structure.properties()
                .get(FXMLConstants.TYPE_ATTRIBUTE);
        if (typeName == null) {
            throw new IllegalStateException("fx:root must have a 'type' attribute");
        }
        Class<?> clazz = Utils.findType(buildContext.imports(), typeName);
        log.debug("Parsing fx:root with type: %s".formatted(clazz.getName()));
        ClassAndIdentifier classAndIdentifier = new ClassAndIdentifier(clazz, FXMLRootIdentifier.INSTANCE);
        return Optional.of(parseNormalElements(structure, buildContext, classAndIdentifier));
    }

    /// Parses an `fx:reference` element into an [FXMLReference] value.
    ///
    /// The logic extracts the `source` attribute and returns a new [FXMLReference] instance.
    ///
    /// @param structure The parsed XML structure representing the `fx:reference` element.
    /// @param ignored   The build context (ignored for this element).
    /// @return An [FXMLReference] representing the parsed element.
    /// @throws IllegalArgumentException if the `source` attribute is missing.
    private Optional<FXMLReference> parseFXReference(ParsedXMLStructure structure, BuildContext ignored) {
        String source = structure.properties()
                .get(FXMLConstants.SOURCE_ATTRIBUTE);
        if (source == null) {
            throw new IllegalArgumentException("`source` attribute is required for fx:reference");
        }
        return Optional.of(new FXMLReference(source));
    }

    private Optional<AbstractFXMLValue> parseFXScript(ParsedXMLStructure structure, BuildContext buildContext) {
        buildContext.scripts()
                .add(parseScript(structure));
        return Optional.empty();
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLCollection] object.
    ///
    /// The logic iterates over all child elements of the current structure, parsing each one
    /// into an [AbstractFXMLValue] using [#parseElement(ParsedXMLStructure, BuildContext, boolean)].
    /// These values are then collected into a list and used to create an [FXMLCollection].
    ///
    /// @param context The [ParseContext] containing the parsed XML structure, build context, class and identifier, and FXML type.
    /// @return An [FXMLCollection] object constructed from the parsed XML structure, generics, factory method, and child values.
    private FXMLCollection parseCollection(ParseContext context) {
        List<AbstractFXMLValue> values = context.structure()
                .children()
                .stream()
                .map(child -> parseElement(child, context.buildContext(), false))
                .gather(Utils.optional())
                .toList();
        return new FXMLCollection(
                context.classAndIdentifier().identifier(),
                context.type(),
                context.factoryMethod(),
                values
        );
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLMap] object.
    ///
    /// The logic involves:
    /// 1. Collecting map entries from attributes that don't have skippable prefixes (like `fx:`).
    /// 2. Iterating through child elements:
    ///    - If the child is `fx:define`, it is parsed as definitions.
    ///    - If the child is `fx:script`, it is added to the script list.
    ///    - Otherwise, if it has no skippable prefix, it's treated as a map entry where the element name is the key.
    ///      The child must have exactly one grand-child element representing its value.
    ///
    /// @param context The parsing context containing the necessary information for map parsing.
    /// @return An [FXMLMap] object constructed from the parsed XML structure.
    /// @throws IllegalArgumentException if a map entry element does not have exactly one child element.
    private FXMLMap parseMap(ParseContext context) {
        ParsedXMLStructure structure = context.structure();
        BuildContext buildContext = context.buildContext();
        FXMLType type = context.type();
        Class<?> mapValueType = findMapValueType(type);
        Map<String, String> properties = structure.properties();
        Map<String, AbstractFXMLValue> entries = properties.entrySet()
                .stream()
                .filter(entry -> !hasSkippablePrefix(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> parseValueString(entry.getValue(), mapValueType),
                        Utils.duplicateThrowException(),
                        HashMap::new
                ));
        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();
            if (!hasSkippablePrefix(childName)) {
                List<AbstractFXMLValue> grandChildren = child.children()
                        .stream()
                        .map(grandChild -> parseMapElements(grandChild, buildContext, mapValueType))
                        .gather(Utils.optional())
                        .toList();
                if (grandChildren.size() != 1) {
                    throw new IllegalArgumentException(
                            "Map entry element `%s` must have exactly one child element representing the value".formatted(
                                    childName));
                }
                entries.put(childName, grandChildren.getFirst());
            }
        }
        return new FXMLMap(
                context.classAndIdentifier().identifier(),
                type,
                context.factoryMethod(),
                entries
        );
    }

    /// Parses map elements from the provided XML structure and attempts to convert them into an appropriate value.
    /// The method processes text values or XML elements to produce a result matching the expected type.
    ///
    /// @param structure    The parsed XML structure containing the elements to be processed.
    /// @param buildContext The context used for building and resolving dependencies during parsing.
    /// @param expectedType The expected type of the resulting object, used for type conversion.
    /// @return An [Optional] containing the parsed [AbstractFXMLValue] if successful, or an empty [Optional] if no valid parsing result is found.
    private Optional<AbstractFXMLValue> parseMapElements(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            Class<?> expectedType
    ) {
        return structure.textValue()
                .map(value -> parseValueString(value, expectedType))
                .or(() -> parseElement(structure, buildContext, false));
    }

    /// Finds the value type of [Map] from the given [FXMLType].
    ///
    /// The logic:
    /// 1. For non-generic types ([FXMLClassType], [FXMLWildcardType], [FXMLUncompiledClassType], [FXMLUncompiledGenericType]),
    ///    returns [Object] as the value type.
    /// 2. For [FXMLGenericType], builds an initial type mapping from the class's own type parameters to the provided
    ///    generic arguments, then traverses the full class hierarchy via [#resolveTypeMappingInternal(Type, Map, Set)]
    ///    to propagate type variable bindings down to the [Map] interface's type parameters.
    ///    The value type (`V`, at index 1 of [Map]'s type parameters) is then looked up in the resolved mapping
    ///    and its raw class is returned.
    ///
    /// @param type The [FXMLType] representing the map type.
    /// @return The raw [Class] of the map's value type, or [Object] if it cannot be determined.
    private Class<?> findMapValueType(FXMLType type) {
        return switch (type) {
            case FXMLClassType _, FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ ->
                    Object.class;
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) -> {
                // Build initial mapping from clazz's own type parameters to the provided generics
                Map<String, FXMLType> mapping = new LinkedHashMap<>();
                TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
                for (int i = 0; i < typeParameters.length && i < generics.size(); i++) {
                    mapping.put(typeParameters[i].getName(), generics.get(i));
                }
                // Traverse the class hierarchy to propagate type variable bindings to Map's type parameters
                Set<Type> visited = new HashSet<>();
                resolveTypeMappingInternal(clazz, mapping, visited);
                // Map's type parameters are K (index 0) and V (index 1)
                String valueTypeParamName = Map.class.getTypeParameters()[1].getName();
                FXMLType valueType = mapping.getOrDefault(valueTypeParamName, FXMLType.of(Object.class));
                yield findRawType(valueType);
            }
        };
    }

    /// Determines the raw type of the given [FXMLType] instance.
    ///
    /// @param type the [FXMLType] instance whose raw type is to be identified
    /// @return the raw [Class] type corresponding to the FXMLType instance; returns `Object.class` for unsupported or wildcard types
    private Class<?> findRawType(FXMLType type) {
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> clazz;
            case FXMLGenericType(Class<?> clazz, List<FXMLType> _) -> clazz;
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ -> Object.class;
        };
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLObject] or a value.
    ///
    /// The logic first tries to parse the element as a constant (`fx:constant`) or a value (`fx:value`)
    /// using [#parseConstantOrValue(ParsedXMLStructure, BuildContext, ClassAndIdentifier)].
    /// If that fails, it proceeds to parse it as a plain FXML object.
    ///
    /// @param context The [ParseContext] containing the parsed XML structure, build context, class and identifier, and FXML type.
    /// @return An [AbstractFXMLValue] constructed from the parsed XML structure.
    private AbstractFXMLValue parseSingle(ParseContext context) {
        return parseConstantOrValue(context.structure(), context.buildContext(), context.classAndIdentifier())
                .orElseGet(() -> parsePlainObject(context));
    }

    /// Parses a plain FXML object from the provided parsing context.
    ///
    /// The logic involves:
    /// 1. Parsing attribute properties that don't have skippable prefixes.
    /// 2. Iterating through child elements:
    ///    - Handling `fx:define` and `fx:script`.
    ///    - Handling static properties (e.g., `GridPane.rowIndex`).
    ///    - Handling instance properties by looking for setters or constructor parameters.
    ///    - Handling collection and map properties.
    ///    - Falling back to the default property if no setter or getter is found.
    ///
    /// @param context The parsing context containing necessary metadata for parsing, including the structure, build context, and class-related information.
    /// @return An `FXMLObject` instance constructed based on the parsed structure, properties, and child elements defined in the context.
    private FXMLObject parsePlainObject(ParseContext context) {
        ParsedXMLStructure structure = context.structure();
        BuildContext buildContext = context.buildContext();
        ClassAndIdentifier classAndIdentifier = context.classAndIdentifier();
        Class<?> clazz = classAndIdentifier.clazz();
        Map<String, String> attributes = structure.properties();
        List<FXMLProperty> properties = attributes.entrySet()
                .stream()
                .filter(entry -> !hasSkippablePrefix(entry.getKey()))
                .map(entry -> parseAttributeProperty(buildContext, clazz, entry.getKey(), entry.getValue()))
                .gather(Utils.optional())
                .collect(Collectors.toCollection(ArrayList::new));

        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();
            if (childName.contains(".")) {
                int dotIndex = childName.lastIndexOf('.');
                String ownerPart = childName.substring(0, dotIndex);
                String propName = childName.substring(dotIndex + 1);
                if (Character.isUpperCase(ownerPart.charAt(0))) {
                    Class<?> staticClazz = Utils.findType(buildContext.imports(), ownerPart);
                    String setterName = Utils.getSetterName(propName);
                    parseStaticPropertyElement(buildContext, staticClazz, setterName, propName, child)
                            .ifPresent(properties::add);
                } else {
                    parseInstancePropertyElement(buildContext, clazz, propName, child).ifPresent(properties::add);
                }
            } else {
                String setterName = Utils.getSetterName(childName);
                List<Method> setters = Utils.findObjectSetters(clazz, setterName);
                String getterName = Utils.getGetterName(childName);
                if (!setters.isEmpty() || !Utils.findParameterTypeForConstructors(clazz, childName).isEmpty()) {
                    parseInstancePropertyElement(buildContext, clazz, childName, child).ifPresent(properties::add);
                } else {
                    boolean handled = false;
                    try {
                        Utils.findGetterListAndReturnElementType(clazz, getterName);
                        parseInstancePropertyElement(buildContext, clazz, childName, child).ifPresent(properties::add);
                        handled = true;
                    } catch (NoSuchMethodException _) {
                        // not a collection getter
                    }
                    if (!handled) {
                        try {
                            Utils.findGetterMapAndReturnValueType(clazz, getterName);
                            parseInstancePropertyElement(
                                    buildContext,
                                    clazz,
                                    childName,
                                    child
                            ).ifPresent(properties::add);
                            handled = true;
                        } catch (NoSuchMethodException _) {
                            // not a map getter
                        }
                    }
                    if (!handled) {
                        Optional<AbstractFXMLValue> childValueOptional = parseElement(child, buildContext, false);
                        Optional<String> defaultPropName = resolveDefaultPropertyName(clazz);
                        if (defaultPropName.isPresent() && childValueOptional.isPresent()) {
                            String defaultGetterName = Utils.getGetterName(defaultPropName.get());
                            AbstractFXMLValue childValue = childValueOptional.get();
                            try {
                                Method getter = clazz.getMethod(defaultGetterName);
                                Utils.findGetterListAndReturnElementType(clazz, defaultGetterName);
                                addValueToMultipleProperty(
                                        buildContext,
                                        properties,
                                        defaultPropName.get(),
                                        defaultGetterName,
                                        getter.getGenericReturnType(),
                                        childValue
                                );
                            } catch (NoSuchMethodException _) {
                                log.debug("No default list property found on %s for child %s, skipping".formatted(
                                        clazz.getSimpleName(),
                                        childName
                                ));
                            }
                        } else {
                            log.debug("No setter, getter, or default property found on %s for child %s, skipping".formatted(
                                    clazz.getSimpleName(),
                                    childName
                            ));
                        }
                    }
                }
            }
        }

        return new FXMLObject(classAndIdentifier.identifier(), context.type(), context.factoryMethod(), properties);
    }

    /// Parses a constant or value attribute from the provided XML structure and returns an appropriate instance.
    ///
    /// The logic:
    /// 1. Checks for the `fx:constant` attribute; if present, resolves the constant's type and returns an [FXMLConstant].
    /// 2. Checks for the `fx:value` attribute; if present, returns an [FXMLValue].
    /// 3. Returns an empty [Optional] if neither attribute is present.
    ///
    /// @param structure          The [ParsedXMLStructure] containing the XML attributes and metadata to parse.
    /// @param buildContext       The [BuildContext] used during the parsing process to resolve types and dependencies.
    /// @param classAndIdentifier The [ClassAndIdentifier] holding the class and identifier used for resolution.
    /// @return An [Optional] containing an [AbstractFXMLValue] representing either an [FXMLConstant] or [FXMLValue], or an empty [Optional] if neither are found in the given structure.
    private Optional<AbstractFXMLValue> parseConstantOrValue(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier
    ) {
        Class<?> clazz = classAndIdentifier.clazz();
        Map<String, String> properties = structure.properties();
        if (properties.containsKey(FXMLConstants.FX_CONSTANT_ATTRIBUTE)) {
            String constantName = properties.get(FXMLConstants.FX_CONSTANT_ATTRIBUTE);
            FXMLType constantType = buildFXMLType(
                    resolveConstantType(clazz, constantName),
                    List.of(),
                    buildContext,
                    Map.of()
            );
            return Optional.of(new FXMLConstant(clazz, constantName, constantType));
        }
        if (properties.containsKey(FXMLConstants.FX_VALUE_ATTRIBUTE)) {
            String value = properties.get(FXMLConstants.FX_VALUE_ATTRIBUTE);
            return Optional.of(new FXMLValue(
                    Optional.of(classAndIdentifier.identifier()),
                    FXMLType.of(clazz),
                    value
            ));
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
    /// @param clazz             The class declaring the factory method.
    /// @param factoryMethodName The name of the factory method.
    /// @return The return [Type] of the factory method.
    /// @throws IllegalArgumentException if the factory method cannot be found.
    private Type findFactoryMethodReturnType(Class<?> clazz, String factoryMethodName) {
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

    /// Resolves the default property name for a class using the [DefaultProperty] annotation.
    ///
    /// The logic traverses the class hierarchy (including superclasses) to find the first
    /// occurrence of the [DefaultProperty] annotation and returns its value.
    ///
    /// @param clazz The class to inspect for a [DefaultProperty] annotation.
    /// @return The default property name if found, or an empty [Optional] if not found.
    private Optional<String> resolveDefaultPropertyName(Class<?> clazz) {
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

    /// Builds an [FXMLType] from a [Type] and a list of generic type name strings.
    ///
    /// The logic:
    /// 1. If `generics` is not empty, it parses each generic string and returns an [FXMLType] with these arguments.
    /// 2. Otherwise, it switches on the provided [Type]:
    ///    - For [Class]: Resolves type mapping and returns [FXMLClassType] or [FXMLGenericType].
    ///    - For [ParameterizedType]: Recursively resolves type arguments.
    ///    - For [TypeVariable]: Looks up the name in `typeMapping`.
    ///    - For [WildcardType]: Resolves upper or lower bounds.
    ///
    /// @param type         The base type.
    /// @param generics     The list of generic type name strings extracted from XML comments.
    /// @param buildContext The build context used for class resolution.
    /// @param typeMapping  A map for resolving [TypeVariable]s.
    /// @return The corresponding [FXMLType].
    private FXMLType buildFXMLType(Type type, List<String> generics, BuildContext buildContext, Map<String, FXMLType> typeMapping) {
        if (!generics.isEmpty()) {
            Class<?> clazz = Utils.getClassType(type);
            List<FXMLType> typeArgs = generics.stream()
                    .map(generic -> parseGenericString(generic.strip(), buildContext))
                    .toList();
            return FXMLType.of(clazz, typeArgs);
        }

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
                        .map(arg -> buildFXMLType(arg, List.of(), buildContext, typeMapping))
                        .toList();
                yield FXMLType.of(rawClass, typeArgs);
            }
            case TypeVariable<?> tv -> typeMapping.getOrDefault(tv.getName(), FXMLType.wildcard());
            case WildcardType wt -> {
                Type[] upperBounds = wt.getUpperBounds();
                Type[] lowerBounds = wt.getLowerBounds();
                if (upperBounds.length > 0 && upperBounds[0] != Object.class) {
                    yield buildFXMLType(upperBounds[0], List.of(), buildContext, typeMapping);
                } else if (lowerBounds.length > 0) {
                    yield buildFXMLType(lowerBounds[0], List.of(), buildContext, typeMapping);
                } else {
                    yield FXMLType.wildcard();
                }
            }
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
    private Map<String, FXMLType> resolveTypeMapping(Class<?> clazz, BuildContext buildContext) {
        Map<String, FXMLType> mapping = new LinkedHashMap<>(buildContext.typeMapping());
        Set<Type> visited = new HashSet<>();
        resolveTypeMappingInternal(clazz, mapping, visited);
        return mapping;
    }

    /// Recursively resolves the type mapping for a given type and updates the mapping.
    ///
    /// The logic handles:
    /// - [Class]: Recursively calls for superclass and interfaces.
    /// - [ParameterizedType]: Maps actual type arguments to the raw class's type parameters.
    /// It avoids infinite recursion by tracking visited types.
    ///
    /// @param type    The type to resolve.
    /// @param mapping The mapping to update.
    /// @param visited The set of visited types to avoid infinite recursion.
    private void resolveTypeMappingInternal(Type type, Map<String, FXMLType> mapping, Set<Type> visited) {
        if (type == null || type == Object.class || !visited.add(type)) {
            return;
        }

        if (type instanceof Class<?> clazz) {
            Type superclass = clazz.getGenericSuperclass();
            if (superclass != null) {
                resolveTypeMappingInternal(superclass, mapping, visited);
            }
            for (Type genericInterface : clazz.getGenericInterfaces()) {
                resolveTypeMappingInternal(genericInterface, mapping, visited);
            }
        } else if (type instanceof ParameterizedType pt) {
            Class<?> rawClass = (Class<?>) pt.getRawType();
            TypeVariable<?>[] typeParameters = rawClass.getTypeParameters();
            Type[] actualTypeArguments = pt.getActualTypeArguments();
            for (int i = 0; i < typeParameters.length && i < actualTypeArguments.length; i++) {
                Type arg = actualTypeArguments[i];
                switch (arg) {
                    case TypeVariable<?> tv -> {
                        FXMLType resolved = mapping.get(tv.getName());
                        if (resolved != null) {
                            mapping.put(typeParameters[i].getName(), resolved);
                        }
                    }
                    case Class<?> argClass -> mapping.put(typeParameters[i].getName(), FXMLType.of(argClass));
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
                        mapping.put(typeParameters[i].getName(), FXMLType.of(argRawClass, argTypeArgs));
                    }
                    case null, default -> mapping.put(typeParameters[i].getName(), FXMLType.wildcard());
                }
            }
            resolveTypeMappingInternal(rawClass, mapping, visited);
        }
    }

    /// Recursively parses a single generic type string into an [FXMLType].
    ///
    /// The logic:
    /// 1. Uses the [#NESTED_GENERICS] pattern to match the input string.
    /// 2. Extracts the raw type name and nested generic arguments.
    /// 3. Recursively parses nested arguments via [#parseNestedGenerics(String, BuildContext)].
    /// 4. Attempts to resolve the raw class; if not found, creates an uncompiled FXML type.
    ///
    /// @param genericString The generic type string to parse (e.g., `Map<String, List<Integer>>`).
    /// @param buildContext  The build context used for class resolution.
    /// @return The corresponding [FXMLType], potentially nested.
    /// @throws IllegalArgumentException if the generic string is invalid.
    private FXMLType parseGenericString(String genericString, BuildContext buildContext) {
        Matcher matcher = NESTED_GENERICS.matcher(genericString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid generic type string: %s".formatted(genericString));
        }
        String rawType = matcher.group("rawType").strip();
        String nestedGenerics = matcher.group("generics");
        List<FXMLType> nestedTypeArgs = parseNestedGenerics(nestedGenerics, buildContext);
        try {
            Class<?> resolvedClass = Utils.findType(buildContext.imports(), rawType);
            return FXMLType.of(resolvedClass, nestedTypeArgs);
        } catch (InternalClassNotFoundException _) {
            return FXMLType.of(rawType, nestedTypeArgs);
        }
    }

    /// Parses a comma-separated list of nested generic type strings into a list of [FXMLType] objects.
    ///
    /// The logic iteratively matches the [#NESTED_GENERICS] pattern against the input string,
    /// extracting and recursively parsing each type argument until no more matches are found.
    /// It builds the list in reverse order (due to the regex structure) and returns the correct sequence.
    ///
    /// @param nestedGenerics The string containing comma-separated nested generic type arguments.
    /// @param buildContext   The build context used for class resolution.
    /// @return A list of [FXMLType] objects representing the parsed nested type arguments.
    private List<FXMLType> parseNestedGenerics(String nestedGenerics, BuildContext buildContext) {
        if (nestedGenerics == null || nestedGenerics.isBlank()) {
            return List.of();
        }
        List<FXMLType> result = new LinkedList<>();
        String remaining = nestedGenerics;
        while (remaining != null) {
            Matcher matcher = NESTED_GENERICS.matcher(remaining);
            if (!matcher.find()) {
                log.warn("Could not parse nested generic type string: %s".formatted(remaining));
                break;
            }
            String rawType = matcher.group("rawType").strip();
            String deeper = matcher.group("generics");
            List<FXMLType> deeperArgs = parseNestedGenerics(deeper, buildContext);
            FXMLType type;
            try {
                Class<?> resolvedClass = Utils.findType(buildContext.imports(), rawType);
                type = FXMLType.of(resolvedClass, deeperArgs);
            } catch (InternalClassNotFoundException _) {
                type = FXMLType.of(rawType, deeperArgs);
            }
            result.addFirst(type);
            remaining = matcher.group("first");
        }
        return List.copyOf(result);
    }

    /// Parses an XML structure into an FXML value.
    ///
    /// The logic delegates to [#parseValue(ParsedXMLStructure, BuildContext, Type)] with a `null` expected type.
    ///
    /// @param structure    The parsed XML structure.
    /// @param buildContext The context used during the building process.
    /// @return The parsed [AbstractFXMLValue].
    private AbstractFXMLValue parseValue(ParsedXMLStructure structure, BuildContext buildContext) {
        return parseValue(structure, buildContext, null);
    }

    /// Parses an XML structure into an FXML value, with an optional expected type.
    ///
    /// The logic handles:
    /// - `fx:include`, `fx:reference`, `fx:copy`: Direct delegation to respective parsing logic.
    /// - `fx:script`: Handles inline scripts.
    /// - Classes: Resolves the type and handles `fx:constant` or `fx:value` attributes.
    /// - Primitive literals: If the structure has no children and a primitive type is expected.
    /// - Recursion: Falls back to [#parseElement(ParsedXMLStructure, BuildContext, boolean)].
    ///
    /// @param structure    The parsed XML structure.
    /// @param buildContext The context used during the building process.
    /// @param expectedType The expected [Type] of the value, used for primitive literals.
    /// @return The parsed [AbstractFXMLValue].
    /// @throws IllegalArgumentException if `fx:include`, `fx:reference`, or `fx:copy` is missing the `source` attribute, or if `fx:script` is missing inline content.
    private AbstractFXMLValue parseValue(ParsedXMLStructure structure, BuildContext buildContext, Type expectedType) {
        String nodeName = structure.name();
        Map<String, String> attributes = structure.properties();

        if (FXMLConstants.FX_INCLUDE_ELEMENT.equals(nodeName)) {
            String src = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            if (src == null) {
                throw new IllegalArgumentException("`source` attribute is required for fx:include");
            }
            FXMLIdentifier includeId = resolveOptionalIdentifier(attributes)
                    .orElseGet(() -> new FXMLInternalIdentifier(buildContext.nextInternalId()));
            return new FXMLInclude(includeId, src);
        }

        if (FXMLConstants.FX_REFERENCE_ELEMENT.equals(nodeName)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            if (source == null) {
                throw new IllegalArgumentException("`source` attribute is required for fx:reference");
            }
            return new FXMLReference(source);
        }

        if (FXMLConstants.FX_COPY_ELEMENT.equals(nodeName)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            if (source == null) {
                throw new IllegalArgumentException("`source` attribute is required for fx:copy");
            }
            FXMLIdentifier copyId = resolveOptionalIdentifier(attributes)
                    .orElseGet(() -> new FXMLInternalIdentifier(buildContext.nextInternalId()));
            return new FXMLCopy(copyId, source);
        }

        if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(nodeName)) {
            String scriptContent = structure.getTextValue();
            if (!scriptContent.isBlank()) {
                return new FXMLInlineScript(scriptContent);
            }
            throw new IllegalArgumentException("`fx:script` used as value must have inline content");
        }

        Optional<Class<?>> clazzOpt = Utils.findTypeOptional(buildContext.imports(), nodeName);

        if (clazzOpt.isPresent()) {
            Class<?> clazz = clazzOpt.get();
            if (attributes.containsKey(FXMLConstants.FX_CONSTANT_ATTRIBUTE)) {
                String constantName = attributes.get(FXMLConstants.FX_CONSTANT_ATTRIBUTE);
                FXMLType constantType = FXMLType.of(Utils.getClassType(resolveConstantType(clazz, constantName)));
                return new FXMLConstant(clazz, constantName, constantType);
            }

            if (attributes.containsKey(FXMLConstants.FX_VALUE_ATTRIBUTE)) {
                String val = attributes.get(FXMLConstants.FX_VALUE_ATTRIBUTE);
                Optional<FXMLIdentifier> id = resolveOptionalIdentifier(attributes);
                return new FXMLValue(id, FXMLType.of(clazz), val);
            }
        }

        if (expectedType != null && Utils.isPrimitiveOrWrapper(Utils.getClassType(expectedType)) && structure.children().isEmpty()) {
            return new FXMLLiteral(structure.getTextValue());
        }

        return parseElement(structure, buildContext, false)
                // TODO
                .orElseThrow(() -> new IllegalArgumentException("Could not parse value: %s".formatted(structure)));
    }

    /// Determines if the given string has a skippable prefix.
    ///
    /// The logic checks if the string starts with `fx:` or `xmlns:`.
    ///
    /// @param key The string to check for skippable prefixes.
    /// @return `true` if the string starts with a skippable prefix; `false` otherwise.
    private boolean hasSkippablePrefix(String key) {
        return key.startsWith(FXMLConstants.FX_PREFIX) || key.startsWith(FXMLConstants.XML_NAMESPACE_PREFIX);
    }

    /// Parses an XML structure into an [FXMLScript].
    ///
    /// The logic:
    /// 1. Checks for a `source` attribute; if present, returns an [FXMLFileScript].
    /// 2. If no `source` is present, returns an [FXMLSourceScript] with the inline content.
    ///
    /// @param structure The parsed XML structure representing the `fx:script` element.
    /// @return The parsed [FXMLScript].
    private FXMLScript parseScript(ParsedXMLStructure structure) {
        Map<String, String> attributes = structure.properties();
        if (attributes.containsKey(FXMLConstants.SOURCE_ATTRIBUTE)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            String charsetName = attributes.getOrDefault(
                    FXMLConstants.CHARSET_ATTRIBUTE,
                    StandardCharsets.UTF_8.name()
            );
            Charset charset = Charset.forName(charsetName);
            return new FXMLFileScript(source, charset);
        }
        return new FXMLSourceScript(structure.getTextValue());
    }

    /// Resolves and returns a [ClassAndIdentifier] object based on the given node name, attributes, and context.
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
    private ClassAndIdentifier resolveClassAndIdentifier(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            boolean isRoot
    ) throws IllegalStateException, IllegalArgumentException {
        FXMLIdentifier identifier;
        Class<?> clazz;
        clazz = Utils.findType(buildContext.imports(), structure.name());
        Map<String, String> attributes = structure.properties();
        if (isRoot) {
            identifier = FXMLRootIdentifier.INSTANCE;
        } else if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            identifier = new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE));
        } else {
            identifier = new FXMLInternalIdentifier(buildContext.nextInternalId());
        }
        log.debug("Parsing object with type: %s".formatted(clazz.getName()));
        return new ClassAndIdentifier(clazz, identifier);
    }

    /// Converts an XML attribute into an [FXMLProperty], handling both static and instance properties.
    ///
    /// The logic checks if the attribute name contains a dot (indicating a static property)
    /// and delegates to the appropriate parsing method.
    ///
    /// @param buildContext  The build context for class resolution.
    /// @param clazz         The class owning the property.
    /// @param attributeName The attribute name (may contain a dot for static properties).
    /// @param value         The attribute value string.
    /// @return An [Optional] containing the property, or empty if it could not be resolved.
    private Optional<FXMLProperty> parseAttributeProperty(
            BuildContext buildContext,
            Class<?> clazz,
            String attributeName,
            String value
    ) {
        if (attributeName.contains(".")) {
            return parseStaticAttributeProperty(buildContext, attributeName, value);
        } else {
            return parseInstanceAttributeProperty(buildContext, clazz, attributeName, value);
        }
    }

    /// Converts a static attribute property (e.g., `GridPane.rowIndex="0"`) into an [FXMLStaticObjectProperty].
    ///
    /// The logic:
    /// 1. Splits the attribute name into class name and property name.
    /// 2. Resolves the static class via imports.
    /// 3. Searches for a corresponding static setter on that class.
    /// 4. Parses the value string into an [FXMLValue] with the correct type.
    ///
    /// @param buildContext  The build context for class resolution.
    /// @param attributeName The full attribute name including the class prefix.
    /// @param value         The attribute value string.
    /// @return An [Optional] containing the static property, or empty if unresolvable.
    private Optional<FXMLProperty> parseStaticAttributeProperty(
            BuildContext buildContext,
            String attributeName,
            String value
    ) {
        int dotIndex = attributeName.lastIndexOf('.');
        String className = attributeName.substring(0, dotIndex);
        String propName = attributeName.substring(dotIndex + 1);
        Class<?> staticClass;
        try {
            staticClass = Utils.findType(buildContext.imports(), className);
        } catch (Exception e) {
            log.warn("Could not resolve static property class '%s', skipping attribute '%s'".formatted(
                    className,
                    attributeName
            ));
            return Optional.empty();
        }
        String setterName = Utils.getSetterName(propName);
        List<Method> setters = Utils.findStaticSettersForNode(staticClass, setterName);
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
        FXMLType fxmlType = buildFXMLType(paramType, List.of(), buildContext, buildContext.typeMapping());
        FXMLValue fxmlValue = new FXMLValue(Optional.empty(), fxmlType, value);
        return Optional.of(new FXMLStaticObjectProperty(propName, staticClass, setterName, fxmlType, fxmlValue));
    }

    /// Converts an instance attribute property (e.g., `text="Hello"`) into an [FXMLProperty].
    ///
    /// The logic attempts resolution in order:
    /// 1. Instance setter: Returns [FXMLObjectProperty].
    /// 2. Constructor parameter: Returns [FXMLConstructorProperty].
    /// 3. Collection getter: Returns [FXMLCollectionProperties].
    /// 4. Map getter: Returns [FXMLMapProperty].
    ///
    /// @param buildContext  The build context for class resolution.
    /// @param clazz         The class owning the property.
    /// @param attributeName The attribute name.
    /// @param value         The attribute value string.
    /// @return An [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty> parseInstanceAttributeProperty(
            BuildContext buildContext,
            Class<?> clazz,
            String attributeName,
            String value
    ) {
        String setterName = Utils.getSetterName(attributeName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        if (!setters.isEmpty()) {
            if (setters.size() > 1) {
                log.warn("Multiple setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
                return Optional.empty();
            }
            Method setter = setters.getFirst();
            Type paramType = setter.getGenericParameterTypes()[0];
            AbstractFXMLValue fxmlValue = parseValueString(value, Utils.getClassType(paramType));
            FXMLType fxmlType = buildFXMLType(paramType, List.of(), buildContext, buildContext.typeMapping());
            return Optional.of(new FXMLObjectProperty(attributeName, setterName, fxmlType, fxmlValue));
        }
        List<Type> constructorParamTypes = Utils.findParameterTypeForConstructors(clazz, attributeName);
        if (!constructorParamTypes.isEmpty()) {
            if (constructorParamTypes.size() > 1) {
                log.warn("Multiple constructor parameters found for '%s' on '%s', skipping".formatted(
                        attributeName,
                        clazz.getName()
                ));
                return Optional.empty();
            }
            Type paramType = constructorParamTypes.getFirst();
            AbstractFXMLValue fxmlValue = parseValueString(value, Utils.getClassType(paramType));
            FXMLType fxmlType = buildFXMLType(paramType, List.of(), buildContext, buildContext.typeMapping());
            return Optional.of(new FXMLConstructorProperty(attributeName, fxmlType, fxmlValue));
        }
        String getterName = Utils.getGetterName(attributeName);
        try {
            Method getter = clazz.getMethod(getterName);
            AbstractFXMLValue fxmlValue = parseValueString(value);
            FXMLType fxmlCollectionType = buildFXMLType(
                    getter.getGenericReturnType(),
                    List.of(),
                    buildContext,
                    buildContext.typeMapping()
            );
            return Optional.of(new FXMLCollectionProperties(
                    attributeName,
                    getterName,
                    fxmlCollectionType,
                    List.of(fxmlValue),
                    List.of()
            ));
        } catch (NoSuchMethodException _) {
            // not a collection getter, try map getter
        }
        try {
            Method getter = clazz.getMethod(getterName);
            AbstractFXMLValue fxmlValue = parseValueString(value);
            FXMLType fxmlMapType = buildFXMLType(
                    getter.getGenericReturnType(),
                    List.of(),
                    buildContext,
                    buildContext.typeMapping()
            );
            return Optional.of(new FXMLMapProperty(
                    attributeName,
                    getterName,
                    fxmlMapType,
                    Map.of(attributeName, fxmlValue)
            ));
        } catch (NoSuchMethodException _) {
            log.debug("No getter, list getter, or map getter found for '%s' on '%s', skipping".formatted(
                    attributeName,
                    clazz.getName()
            ));
            return Optional.empty();
        }
    }

    /// Converts a static property child element into an [FXMLProperty].
    ///
    /// The logic:
    /// 1. Finds the unique static setter for the property on the specified class.
    /// 2. Parses the child element's content into a list of values via [#parseChildrenToValues(BuildContext, ParsedXMLStructure, Type)].
    /// 3. Ensures only a single value is present and returns an [FXMLStaticObjectProperty].
    ///
    /// @param buildContext The build context for class resolution.
    /// @param clazz        The class defining the static property.
    /// @param setterName   The name of the static setter method.
    /// @param propName     The property name.
    /// @param child        The child XML structure containing the property values.
    /// @return An [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty> parseStaticPropertyElement(
            BuildContext buildContext,
            Class<?> clazz,
            String setterName,
            String propName,
            ParsedXMLStructure child
    ) {
        List<Method> setters = Utils.findStaticSettersForNode(clazz, setterName);
        if (setters.isEmpty()) {
            log.warn("No static setter '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
            return Optional.empty();
        }
        if (setters.size() > 1) {
            log.warn("Multiple static setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
            return Optional.empty();
        }
        Method setter = setters.getFirst();
        Type paramType = setter.getGenericParameterTypes()[1];
        List<AbstractFXMLValue> values = parseChildrenToValues(buildContext, child, paramType);
        FXMLType fxmlType = buildFXMLType(paramType, List.of(), buildContext, buildContext.typeMapping());
        if (values.size() == 1) {
            return Optional.of(new FXMLStaticObjectProperty(propName, clazz, setterName, fxmlType, values.getFirst()));
        }
        log.warn("Multiple values for static property '%s' on '%s' are not supported, skipping".formatted(
                propName,
                clazz.getName()
        ));
        return Optional.empty();
    }

    /// Converts an instance property child element into an [FXMLProperty].
    ///
    /// The logic attempts resolution in order:
    /// 1. Setter or Constructor parameter: parses children or text value and returns [FXMLObjectProperty], [FXMLCollectionProperties], or [FXMLConstructorProperty].
    /// 2. Collection getter: returns [FXMLCollectionProperties].
    /// 3. Map getter: returns [FXMLMapProperty].
    ///
    /// @param buildContext The build context for class resolution.
    /// @param clazz        The class owning the property.
    /// @param propName     The property name.
    /// @param child        The child XML structure containing the property values.
    /// @return An [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty> parseInstancePropertyElement(
            BuildContext buildContext,
            Class<?> clazz,
            String propName,
            ParsedXMLStructure child
    ) {
        String setterName = Utils.getSetterName(propName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        List<Type> constructorParamTypes = Utils.findParameterTypeForConstructors(clazz, propName);

        Type paramType = null;
        if (!setters.isEmpty()) {
            if (setters.size() > 1) {
                log.warn("Multiple setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
                return Optional.empty();
            }
            paramType = setters.getFirst().getGenericParameterTypes()[0];
        } else if (!constructorParamTypes.isEmpty()) {
            if (constructorParamTypes.size() > 1) {
                log.warn("Multiple constructor parameters found for '%s' on '%s', skipping".formatted(
                        propName,
                        clazz.getName()
                ));
                return Optional.empty();
            }
            paramType = constructorParamTypes.getFirst();
        }

        if (paramType != null) {
            List<AbstractFXMLValue> values = parseChildrenToValues(buildContext, child, paramType);
            if (values.isEmpty() && !child.getTextValue().isEmpty()) {
                values = List.of(parseValueString(child.getTextValue().strip(), Utils.getClassType(paramType)));
            }
            FXMLType fxmlType = buildFXMLType(paramType, List.of(), buildContext, buildContext.typeMapping());
            if (!setters.isEmpty()) {
                if (values.size() == 1) {
                    return Optional.of(new FXMLObjectProperty(propName, setterName, fxmlType, values.getFirst()));
                }
                return Optional.of(new FXMLCollectionProperties(propName, setterName, fxmlType, values, List.of()));
            } else {
                if (values.size() == 1) {
                    return Optional.of(new FXMLConstructorProperty(propName, fxmlType, values.getFirst()));
                }
                log.warn("Multiple values for constructor property '%s' on '%s' are not supported, skipping".formatted(
                        propName,
                        clazz.getName()
                ));
                return Optional.empty();
            }
        }

        String getterName = Utils.getGetterName(propName);
        List<AbstractFXMLValue> values = parseChildrenToValues(buildContext, child, null);
        try {
            Method getter = clazz.getMethod(getterName);
            FXMLType fxmlCollectionType = buildFXMLType(
                    getter.getGenericReturnType(),
                    List.of(),
                    buildContext,
                    buildContext.typeMapping()
            );
            return Optional.of(new FXMLCollectionProperties(
                    propName,
                    getterName,
                    fxmlCollectionType,
                    values,
                    List.of()
            ));
        } catch (NoSuchMethodException _) {
            // not a collection getter, try map getter
        }
        try {
            Method getter = clazz.getMethod(getterName);
            Map<String, AbstractFXMLValue> entries = parseChildrenToMapEntries(buildContext, child);
            FXMLType fxmlMapType = buildFXMLType(
                    getter.getGenericReturnType(),
                    List.of(),
                    buildContext,
                    buildContext.typeMapping()
            );
            return Optional.of(new FXMLMapProperty(propName, getterName, fxmlMapType, entries));
        } catch (NoSuchMethodException _) {
            log.debug("No getter, list getter, or map getter found for property element '%s' on '%s', skipping".formatted(
                    propName,
                    clazz.getName()
            ));
            return Optional.empty();
        }
    }

    /// Converts the children of a property element into a list of [AbstractFXMLValue].
    ///
    /// The logic iterates over all children of the property element and parses each as a value
    /// using [#parseValue(ParsedXMLStructure, BuildContext, Type)], applying the expected parameter type.
    ///
    /// @param buildContext    The build context for class resolution.
    /// @param propertyElement The property element whose children are to be converted.
    /// @param paramType       The expected parameter type for the property.
    /// @return The list of converted values.
    private List<AbstractFXMLValue> parseChildrenToValues(
            BuildContext buildContext,
            ParsedXMLStructure propertyElement,
            Type paramType
    ) {
        List<AbstractFXMLValue> values = new ArrayList<>();
        for (ParsedXMLStructure child : propertyElement.children()) {
            values.add(parseValue(child, buildContext, paramType));
        }
        return values;
    }

    /// Converts the attributes and children of a map property element into a map of string keys to [AbstractFXMLValue] values.
    ///
    /// The logic:
    /// 1. Collects entries from attributes using [#parseValueString(String)].
    /// 2. Collects entries from child elements using [#parseValue(ParsedXMLStructure, BuildContext)].
    /// Child elements with the same name as an attribute will override the attribute's entry.
    ///
    /// @param buildContext    The build context for class resolution.
    /// @param propertyElement The property element whose attributes and children are to be converted.
    /// @return The map of attribute/child names to parsed values.
    private Map<String, AbstractFXMLValue> parseChildrenToMapEntries(
            BuildContext buildContext,
            ParsedXMLStructure propertyElement
    ) {
        Map<String, AbstractFXMLValue> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> attribute : propertyElement.properties().entrySet()) {
            entries.put(attribute.getKey(), parseValueString(attribute.getValue()));
        }
        for (ParsedXMLStructure child : propertyElement.children()) {
            entries.put(child.name(), parseValue(child, buildContext));
        }
        return entries;
    }

    /// Parses an attribute value string into an [AbstractFXMLValue].
    ///
    /// The logic delegates to [#parseValueString(String, Class)] with a `null` parameter type.
    ///
    /// @param value The raw attribute value string.
    /// @return The corresponding [AbstractFXMLValue].
    private AbstractFXMLValue parseValueString(String value) {
        return parseValueString(value, null);
    }

    /// Parses an attribute value string into an [AbstractFXMLValue], with awareness of the expected getter
    /// parameter type and build context.
    ///
    /// The logic handles various FXML prefixes:
    /// - `%`: Returns [FXMLTranslation].
    /// - `@`: Returns [FXMLResource].
    /// - `#`: Returns [FXMLMethod] via [#resolveMethodReference(String, Class)].
    /// - `$`: Returns [FXMLExpression].
    /// - `\`: Returns [FXMLLiteral] (escaped).
    /// - No prefix: Returns [FXMLInlineScript] if `paramType` is an `EventHandler`, otherwise returns [FXMLLiteral].
    ///
    /// @param value     The raw attribute value string.
    /// @param paramType The expected getter parameter type, used to detect functional interfaces, may be `null`.
    /// @return The corresponding [AbstractFXMLValue].
    private AbstractFXMLValue parseValueString(String value, Class<?> paramType) {
        if (value.startsWith(FXMLConstants.TRANSLATION_PREFIX)) {
            return new FXMLTranslation(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.LOCATION_PREFIX)) {
            return new FXMLResource(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.METHOD_REFERENCE_PREFIX)) {
            return resolveMethodReference(value.substring(1), paramType);
        }
        if (value.startsWith(FXMLConstants.EXPRESSION_PREFIX)) {
            return new FXMLExpression(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.ESCAPE_PREFIX)) {
            return new FXMLLiteral(value.substring(1));
        }
        if (paramType != null && isEventHandlerType(paramType)) {
            return new FXMLInlineScript(value);
        }
        return new FXMLLiteral(value);
    }

    /// Resolves a method reference value string into an [FXMLMethod].
    ///
    /// The logic:
    /// 1. If `paramType` is a functional interface, it attempts to resolve the return type from its single abstract method.
    /// 2. If it's not a functional interface, it defaults to `void`.
    /// 3. Returns a new [FXMLMethod] instance.
    ///
    /// @param methodName The method name (without the `#` prefix).
    /// @param paramType  The expected getter parameter type; may be `null`.
    /// @return The corresponding [FXMLMethod].
    private FXMLMethod resolveMethodReference(String methodName, Class<?> paramType) {
        // TODO Improve handling of method references generics
        FXMLType returnType;
        if (paramType != null && isFunctionalInterface(paramType)) {
            try {
                Method functionalMethod = Utils.getFunctionalMethod(paramType);
                returnType = FXMLType.of(functionalMethod.getReturnType());
            } catch (IllegalStateException _) {
                returnType = FXMLType.of(void.class);
            }
        } else {
            returnType = FXMLType.of(void.class);
        }
        return new FXMLMethod(methodName, List.of(), returnType);
    }

    /// Checks whether the given class is or extends `javafx.event.EventHandler`.
    ///
    /// @param clazz The class to check.
    /// @return `true` if the class is assignable to `javafx.event.EventHandler`; `false` otherwise.
    private boolean isEventHandlerType(Class<?> clazz) {
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
    private boolean isFunctionalInterface(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return false;
        }
        if (clazz.isAnnotationPresent(FunctionalInterface.class)) {
            return true;
        }
        long abstractMethodCount = Stream.of(clazz.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
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
    private Type resolveConstantType(Class<?> clazz, String constantName) {
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

    /// Resolves an optional [FXMLIdentifier] from the given attributes map.
    ///
    /// The logic checks if the `fx:id` attribute is present and returns an [FXMLExposedIdentifier] if so.
    ///
    /// @param attributes The XML attributes map.
    /// @return An [Optional] containing the identifier, or empty if no `fx:id` is present.
    private Optional<FXMLIdentifier> resolveOptionalIdentifier(
            Map<String, String> attributes
    ) {
        if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            return Optional.of(new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)));
        }
        return Optional.empty();
    }

    /// Extracts generic type names from XML comments (e.g., `<!-- generic 0: java.util.List<String> -->`).
    ///
    /// The logic iterates through comments, looking for those starting with `generic `,
    /// then extracts the type definition following the first colon.
    ///
    /// @param comments The list of XML comments on the element.
    /// @return The list of extracted generic type strings.
    private List<String> extractGenericsFromComments(List<String> comments) {
        // TODO: Be more strict with generic comment parsing
        List<String> generics = new ArrayList<>();
        for (String comment : comments) {
            String stripped = comment.strip();
            if (stripped.startsWith("generic ")) {
                int colonIndex = stripped.indexOf(':');
                if (colonIndex >= 0) {
                    generics.add(stripped.substring(colonIndex + 1).strip());
                }
            }
        }
        return generics;
    }

    /// Adds a value to an existing [FXMLCollectionProperties] entry or creates a new one.
    ///
    /// The logic:
    /// 1. Searches the `properties` list for an existing [FXMLCollectionProperties] with the same name.
    /// 2. If found, appends the new value to the existing collection.
    /// 3. If not found, creates a new [FXMLCollectionProperties] and adds it to the list.
    ///
    /// @param buildContext   The build context for class resolution.
    /// @param properties     The current list of properties to update.
    /// @param propName       The property name.
    /// @param getterName     The getter method name for the list property.
    /// @param collectionType The type of the collection itself.
    /// @param value          The value to add.
    private void addValueToMultipleProperty(
            BuildContext buildContext,
            List<FXMLProperty> properties,
            String propName,
            String getterName,
            Type collectionType,
            AbstractFXMLValue value
    ) {
        FXMLType fxmlCollectionType = buildFXMLType(
                collectionType,
                List.of(),
                buildContext,
                buildContext.typeMapping()
        );
        for (int i = 0; i < properties.size(); i++) {
            FXMLProperty existing = properties.get(i);
            if (existing instanceof FXMLCollectionProperties mp && mp.name().equals(propName)) {
                List<AbstractFXMLValue> newValues = new ArrayList<>(mp.value());
                newValues.add(value);
                properties.set(
                        i,
                        new FXMLCollectionProperties(propName, getterName, fxmlCollectionType, newValues, List.of())
                );
                return;
            }
        }
        List<AbstractFXMLValue> values = new ArrayList<>();
        values.add(value);
        properties.add(new FXMLCollectionProperties(propName, getterName, fxmlCollectionType, values, List.of()));
    }

    /// Represents a functional interface designed to handle and process special FXML elements encountered during
    /// the parsing and building of an FXML document.
    /// The interface defines a single abstract method intended to handle custom processing logic for specific FXML
    /// elements, allowing for the generation of corresponding values or actions.
    ///
    /// Implementations of this interface provide a mechanism to extend or customize the default behavior of
    /// FXML document parsing.
    ///
    /// The handler receives relevant contextual information including the document parser instance,
    /// the segment of the FXML document to be processed, and the build context,
    /// enabling comprehensive and flexible handling of special FXML elements.
    ///
    /// This interface follows the contract of a functional interface and can be implemented using a lambda expression
    /// or method reference.
    @FunctionalInterface
    private interface SpecialFXElementHandler {
        /// Handles processing of a special FXML element based on the provided inputs.
        ///
        /// @param instance     The [FXMLDocumentParser] instance responsible for parsing the document.
        /// @param structure    The [ParsedXMLStructure] representing the current segment of the FXML document.
        /// @param buildContext The [BuildContext] providing contextual information during the building process.
        /// @return An [Optional] containing an [AbstractFXMLValue] if the processing completes successfully, or an empty [Optional] if no value could be generated.
        Optional<? extends AbstractFXMLValue> handle(
                FXMLDocumentParser instance,
                ParsedXMLStructure structure,
                BuildContext buildContext
        );
    }

    /// Holds a class and its associated FXML identifier.
    ///
    /// This record is used to associate a Java class with its identifier in the FXML document.
    ///
    /// @param clazz      The class type.
    /// @param identifier The FXML identifier.
    private record ClassAndIdentifier(Class<?> clazz, FXMLIdentifier identifier) {
        /// Compact constructor to validate the class and identifier.
        ///
        /// The logic ensures that both the `clazz` and `identifier` are not `null`.
        ///
        /// @param clazz      The class type.
        /// @param identifier The FXML identifier.
        /// @throws NullPointerException if `clazz` or `identifier` is `null`.
        private ClassAndIdentifier {
            Objects.requireNonNull(clazz, "`clazz` must not be null");
            Objects.requireNonNull(identifier, "`identifier` must not be null");
        }
    }

    /// Holds the state and context during the FXML document building process.
    ///
    /// This record maintains the internal state such as identifiers counter, imports,
    /// definitions, and scripts during the parsing process.
    ///
    /// @param internalCounter The counter for generating internal identifiers.
    /// @param imports         The list of imports.
    /// @param definitions     The list of definitions.
    /// @param scripts         The list of scripts.
    /// @param typeMapping     The map for resolving type variables.
    private record BuildContext(
            AtomicInteger internalCounter,
            List<String> imports,
            List<AbstractFXMLValue> definitions,
            List<FXMLScript> scripts,
            Map<String, FXMLType> typeMapping
    ) {

        /// Compact constructor to validate the build context components.
        ///
        /// The logic ensures that all components of the build context are not `null`.
        ///
        /// @param internalCounter The counter for generating internal identifiers.
        /// @param imports         The list of imports.
        /// @param definitions     The list of definitions.
        /// @param scripts         The list of scripts.
        /// @param typeMapping     The map for resolving type variables.
        /// @throws NullPointerException if any parameter is `null`.
        public BuildContext {
            Objects.requireNonNull(internalCounter, "`internalCounter` must not be null");
            Objects.requireNonNull(imports, "`imports` must not be null");
            Objects.requireNonNull(definitions, "`definitions` must not be null");
            Objects.requireNonNull(scripts, "`scripts` must not be null");
            Objects.requireNonNull(typeMapping, "`typeMapping` must not be null");
        }

        /// Constructs a new build context with the provided imports.
        ///
        /// The logic initializes a new build context with default empty lists and a new atomic counter.
        ///
        /// @param imports The list of imports.
        public BuildContext(List<String> imports) {
            this(new AtomicInteger(), imports, new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>());
        }

        /// Constructs a new `BuildContext` by copying the properties of an existing `BuildContext`
        /// and replacing the `typeMapping` with the provided mapping.
        ///
        /// The logic performs a shallow copy of the other fields from the `original` context.
        ///
        /// @param original    The original `BuildContext` instance.
        /// @param typeMapping The new map for resolving type variables.
        /// @throws NullPointerException if `original` or `typeMapping` is `null`.
        public BuildContext(
                BuildContext original,
                Map<String, FXMLType> typeMapping
        ) {
            Objects.requireNonNull(original, "`original` must not be null");
            Objects.requireNonNull(typeMapping, "`typeMapping` must not be null");
            this(original.internalCounter, original.imports, original.definitions, original.scripts, typeMapping);
        }

        /// Generates the next internal identifier for tracking purposes.
        ///
        /// The logic increments the internal atomic counter and returns the value.
        ///
        /// @return The next incremental identifier as an integer.
        private int nextInternalId() {
            return internalCounter.getAndIncrement();
        }
    }

    /// Represents the parsing context for an FXML document.
    ///
    /// This record encapsulates all necessary information required for parsing and building FXML structures,
    /// ensuring proper linking between parsed elements and their associated metadata.
    ///
    /// @param structure          The parsed structure of the FXML document.
    /// @param buildContext       The context used during the construction phase.
    /// @param classAndIdentifier Carries information about the involved class and its identifier.
    /// @param type               Specifies the type of the FXML element being processed.
    /// @param factoryMethod      Represents the factory method, if any, that may be invoked.
    private record ParseContext(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier,
            FXMLType type,
            Optional<FXMLFactoryMethod> factoryMethod
    ) {

        /// Constructor for the `ParseContext` record.
        ///
        /// The logic ensures that all required components are not `null`.
        ///
        /// @param structure          The parsed structure of the FXML document.
        /// @param buildContext       The context used during the building.
        /// @param classAndIdentifier The class and identifier details.
        /// @param type               The type of the FXML object being parsed.
        /// @param factoryMethod      The factory method associated with the FXML object.
        /// @throws NullPointerException if any of the parameters (except `factoryMethod`) are null.
        private ParseContext {
            Objects.requireNonNull(structure, "`structure` must not be null");
            Objects.requireNonNull(buildContext, "`buildContext` must not be null");
            Objects.requireNonNull(classAndIdentifier, "`classAndIdentifier` must not be null");
            Objects.requireNonNull(type, "`type` must not be null");
            Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        }
    }


}
