package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLFileScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLResource;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLTranslation;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    /// A mapping of special JavaFX FXML element names to their corresponding handler methods.
    /// This map is used to associate specific predefined FXML element constants with
    /// their respective parser functions, enabling dynamic handling and processing
    /// during FXML document parsing.
    ///
    /// Key-Value pairs:
    /// - Keys: String constants representing special FXML element names, defined in `FXMLConstants`.
    /// - Values: Method references implement the parsing logic for each corresponding FXML element.
    ///
    /// This map is immutable and initialized with predefined mappings for processing the following special FXML elements:
    /// - `fx:copy`: Maps to the `FXMLDocumentParser::parseFXCopy` handler.
    /// - `fx:define`: Maps to the `FXMLDocumentParser::parseFXDefine` handler.
    /// - `fx:include`: Maps to the `FXMLDocumentParser::parseFXInclude` handler.
    /// - `fx:reference`: Maps to the `FXMLDocumentParser::parseFXReference` handler.
    /// - `fx:root`: Maps to the `FXMLDocumentParser::parseFXRoot` handler.
    /// - `fx:script`: Maps to the `FXMLDocumentParser::parseFXScript` handler.
    private static final Map<String, SpecialFXElementHandler> SPECIAL_FX_ELEMENTS_HANDLERS;
    /// A static, unmodifiable map holding mappings between element names and their corresponding
    /// [SpecialFXElementHandler] implementations for scenarios where elements are not allowed to be processed inline.
    ///
    /// This map is intended to provide special handling logic for certain FX elements that require unique or predefined
    /// behavior.
    /// Entries in the map associate element identifiers with handlers responsible for implementing this behavior.
    ///
    /// The contents of this map are immutable and need to be initialized during application startup or when the class
    /// is loaded.
    private static final Map<String, SpecialFXElementHandler> NO_INLINE_FX_ELEMENTS_HANDLERS;

    static {
        SPECIAL_FX_ELEMENTS_HANDLERS = Map.ofEntries(
                Map.entry(FXMLConstants.FX_COPY_ELEMENT, FXMLDocumentParser::parseFXCopy),
                Map.entry(FXMLConstants.FX_DEFINE_ELEMENT, FXMLDocumentParser::parseFXDefine),
                Map.entry(FXMLConstants.FX_INCLUDE_ELEMENT, FXMLDocumentParser::parseFXInclude),
                Map.entry(FXMLConstants.FX_REFERENCE_ELEMENT, FXMLDocumentParser::parseFXReference),
                Map.entry(FXMLConstants.FX_ROOT_ELEMENT, FXMLDocumentParser::parseFXRoot),
                Map.entry(FXMLConstants.FX_SCRIPT_ELEMENT, FXMLDocumentParser::parseFXScript)
        );
        NO_INLINE_FX_ELEMENTS_HANDLERS = Map.ofEntries(
                Map.entry(FXMLConstants.FX_DEFINE_ELEMENT, FXMLDocumentParser::parseFXDefine),
                Map.entry(FXMLConstants.FX_SCRIPT_ELEMENT, FXMLDocumentParser::parseFXScript)
        );
    }

    /// Provides a logger instance for recording runtime information, debugging, and error messages.
    ///
    /// This logger is immutable and initialized once via the constructor, ensuring consistent logging
    /// throughout the application. It is used to report warnings for unresolvable types or properties
    /// and to provide debug information during the parsing process.
    private final Log log;
    /// A utility object that helps with parsing FXML documents.
    /// This helper provides methods and functionality specific to processing and interpreting FXML content within
    /// the application.
    private final FXMLDocumentParserHelper helper;

    /// Compact constructor to validate the log dependency.
    ///
    /// It ensures that the required [Log] instance is not `null` before assignment.
    ///
    /// @param log            The logging instance used for diagnostic output.
    /// @param defaultCharset The default character set to use for parsing.
    /// @throws NullPointerException if `log` or `defaultCharset` is `null`.
    public FXMLDocumentParser(Log log, Charset defaultCharset) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        Objects.requireNonNull(defaultCharset, "`defaultCharset` must not be null");
        this.helper = new FXMLDocumentParserHelper(log, defaultCharset);
    }

    /// Parses the provided [ParsedFXML] instance and constructs an [FXMLDocument].
    ///
    /// The logic follows these steps:
    /// 1. Ensures `parsedFXML` is not `null`.
    /// 2. Ensures `resourcePath` is not `null`.
    /// 3. Creates a root [BuildContext] using the imports from the FXML file and the given resource path.
    /// 4. Resolves the controller class if the `fx:controller` attribute is present on the root element.
    /// 5. Recursively parses the root element using [#parseElement(ParsedXMLStructure, BuildContext, boolean)].
    /// 6. Verifies that the resulting root value is an instance of [AbstractFXMLObject].
    /// 7. Returns a new [FXMLDocument] containing all gathered information.
    ///
    /// @param parsedFXML   The [ParsedFXML] object to be parsed.
    /// @param resourcePath The path of the FXML file relative to the resources folder root. A single `/` denotes the root of the resource directory.
    /// @return An [FXMLDocument] that represents the parsed structure of the given [ParsedFXML].
    /// @throws NullPointerException  if `parsedFXML` or `resourcePath` is `null`.
    /// @throws IllegalStateException if the root element does not parse to an [AbstractFXMLObject].
    public FXMLDocument parse(ParsedFXML parsedFXML, String resourcePath) {
        Objects.requireNonNull(parsedFXML, "`parsedFXML` must not be null");
        Objects.requireNonNull(resourcePath, "`resourcePath` must not be null");
        ParsedXMLStructure rootStructure = parsedFXML.root();
        BuildContext buildContext = new BuildContext(parsedFXML.imports(), resourcePath);

        Map<String, String> properties = rootStructure.properties();
        Optional<FXMLController> controller = Optional.ofNullable(properties.get(FXMLConstants.FX_CONTROLLER_ATTRIBUTE))
                .map(name -> Utils.findType(buildContext.imports(), name))
                .map(controllerClass -> helper.introspectControllerClass(controllerClass, buildContext));

        Optional<AbstractFXMLValue> rootValue = parseElement(rootStructure, buildContext, true);
        if (rootValue.isEmpty() || !(rootValue.get() instanceof AbstractFXMLObject root)) {
            throw new IllegalStateException(
                    "Root object must be an instance of object, collection, or map, but was %s".formatted(
                            rootStructure.name()
                    )
            );
        }

        return new FXMLDocument(
                parsedFXML.className(),
                root,
                controller,
                parsedFXML.scriptNamespace(),
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
        ClassAndIdentifier classAndIdentifier = helper.resolveClassAndIdentifier(structure, buildContext, isRoot);
        return Optional.of(parseNormalElements(structure, buildContext, classAndIdentifier));
    }

    /// Parses an XML element and converts it into an AbstractFXMLValue, if possible.
    ///
    /// The logic delegates to [#parseElement(ParsedXMLStructure, BuildContext, boolean)] with `isRoot` set to `false`.
    ///
    /// @param structure    the parsed XML structure to analyze and process
    /// @param buildContext the context used for building and maintaining state during parsing
    /// @return an [Optional] containing the parsed [AbstractFXMLValue], or an empty [Optional] for certain special elements (e.g., `fx:define`, `fx:script`)
    /// @throws IllegalStateException if the parsing process encounters an unexpected or illegal state
    private Optional<AbstractFXMLValue> parseElement(ParsedXMLStructure structure, BuildContext buildContext)
            throws IllegalStateException {
        return parseElement(structure, buildContext, false);
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
                .map(method -> new FXMLFactoryMethod(new FXMLClassType(clazz), method));

        Type actualType = clazz;
        if (factoryMethodName != null) {
            actualType = FXMLUtils.findFactoryMethodReturnType(clazz, factoryMethodName);
        }
        FXMLType fxmlType = helper.constructGenericType(
                Utils.getClassType(actualType),
                structure.comments(),
                buildContext
        );
        Map<String, FXMLType> typeMapping = helper.resolveTypeMapping(Utils.getClassType(actualType), buildContext);
        BuildContext localContext = new BuildContext(buildContext, typeMapping);
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
        FXMLIdentifier copyId = helper.resolveOptionalIdentifier(properties)
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
                .map(child -> parseElement(child, buildContext))
                .gather(Utils.optional())
                .forEach(buildContext.definitions()::add);
        return Optional.empty();
    }

    /// Parses an `fx:include` element into an [FXMLInclude] value.
    ///
    /// The logic:
    /// 1. Extracts the `source` attribute, ensuring it is present.
    /// 2. Resolves an optional FXML identifier (`fx:id`); if absent, generates a new internal identifier.
    /// 3. Resolves the `source` and optional `resources` paths relative to the resource directory.
    /// 4. Returns a new [FXMLInclude] instance.
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
        FXMLIdentifier includeId = helper.resolveOptionalIdentifier(properties)
                .orElseGet(() -> new FXMLInternalIdentifier(buildContext.nextInternalId()));
        Charset charset = helper.getCharsetOfElement(structure);
        Optional<String> resources = Optional.ofNullable(properties.get(FXMLConstants.RESOURCES_ATTRIBUTE))
                .map(r -> helper.resolveResourcePath(r, buildContext));
        source = helper.resolveResourcePath(source, buildContext);
        return Optional.of(new FXMLInclude(includeId, source, charset, resources, new FXMLLazyLoadedDocument()));
    }

    /// Parses an `fx:root` element from the given XML structure and creates an appropriate representation of it,
    /// wrapped in an [Optional].
    /// The method validates that the `fx:root` element has a type attribute
    /// and determines the corresponding class using the provided build context.
    ///
    /// @param structure    the parsed XML structure containing the `fx:root` element to be processed
    /// @param buildContext the build context providing imports and other contextual information necessary for resolving types and building objects
    /// @return an [Optional] containing an AbstractFXMLValue representation of the `fx:root` element
    /// @throws IllegalArgumentException if the `fx:root` element does not have a "type" attribute
    private Optional<AbstractFXMLValue> parseFXRoot(ParsedXMLStructure structure, BuildContext buildContext) {
        Map<String, String> properties = structure.properties();
        String typeName = properties.get(FXMLConstants.TYPE_ATTRIBUTE);
        if (typeName == null) {
            throw new IllegalArgumentException("fx:root must have a 'type' attribute");
        }
        Class<?> clazz = Utils.findType(buildContext.imports(), typeName);
        log.debug("Parsing fx:root with type: %s".formatted(clazz.getName()));
        FXMLIdentifier identifier = Optional.ofNullable(properties.get(FXMLConstants.FX_ID_ATTRIBUTE))
                .map(FXMLNamedRootIdentifier::new)
                .map(Function.<FXMLIdentifier>identity())
                .orElse(FXMLRootIdentifier.INSTANCE);
        ClassAndIdentifier classAndIdentifier = new ClassAndIdentifier(clazz, identifier);
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

    /// Parses an `fx:script` element from the given XML structure and updates the build context
    /// by adding the extracted script. Returns an empty optional as this method produces no specific value.
    ///
    /// @param structure    The parsed XML structure containing the `fx:script` element.
    /// @param buildContext The context used to store the extracted script.
    /// @return An empty [Optional] as the method does not produce a concrete result.
    private Optional<AbstractFXMLValue> parseFXScript(ParsedXMLStructure structure, BuildContext buildContext) {
        buildContext.scripts()
                .add(parseScript(structure, buildContext));
        return Optional.empty();
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLCollection] object.
    ///
    /// The logic iterates over all child elements of the current structure, parsing each one
    /// into an [AbstractFXMLValue] using [#parseElement(ParsedXMLStructure, BuildContext)].
    /// These values are then collected into a list and used to create an [FXMLCollection].
    ///
    /// @param context The [ParseContext] containing the parsed XML structure, build context, class and identifier, and FXML type.
    /// @return An [FXMLCollection] object constructed from the parsed XML structure, generics, factory method, and child values.
    private FXMLCollection parseCollection(ParseContext context) {
        List<AbstractFXMLValue> values = parseChildrenAsValues(context.buildContext(), context.structure());
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
        Map.Entry<FXMLType, FXMLType> mapKeyAndValueTypes = FXMLUtils.findMapKeyAndValueTypes(type);
        Class<?> rawKeyClass = FXMLUtils.findRawType(mapKeyAndValueTypes.getKey());
        Class<?> rawValueType = FXMLUtils.findRawType(mapKeyAndValueTypes.getValue());
        Map<FXMLLiteral, AbstractFXMLValue> entries = parseMapEntries(structure, rawValueType, buildContext);
        return new FXMLMap(
                context.classAndIdentifier().identifier(),
                type,
                new FXMLClassType(rawKeyClass),
                new FXMLClassType(rawValueType),
                context.factoryMethod(),
                entries
        );
    }

    /// Parses map entries from the provided XML structure.
    ///
    /// The method extracts entries from both XML attributes and child elements of the structure.
    /// Attribute properties with non-skippable prefixes are treated as simple map entries.
    /// Child elements with non-skippable prefixes are expected to have exactly one child representing the value.
    ///
    /// @param structure    The [ParsedXMLStructure] containing the map entries to parse.
    /// @param rawValueType The expected raw class type for the map values.
    /// @param buildContext The [BuildContext] used for parsing and resolving values.
    /// @return A [Map] of entry keys to their corresponding [AbstractFXMLValue] objects.
    /// @throws IllegalArgumentException If a child element for a map entry does not have exactly one child representing the value.
    private Map<FXMLLiteral, AbstractFXMLValue> parseMapEntries(ParsedXMLStructure structure, Class<?> rawValueType, BuildContext buildContext) {
        Map<String, String> properties = structure.properties();
        Map<FXMLLiteral, AbstractFXMLValue> entries = properties.entrySet()
                .stream()
                .filter(entry -> FXMLUtils.hasNonSkippablePrefix(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> new FXMLLiteral(entry.getKey()),
                        entry -> parseValueString(entry.getValue(), rawValueType, buildContext),
                        Utils.duplicateThrowException(),
                        HashMap::new
                ));
        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();
            if (NO_INLINE_FX_ELEMENTS_HANDLERS.containsKey(childName)) {
                NO_INLINE_FX_ELEMENTS_HANDLERS.get(childName)
                        .handle(this, child, buildContext);
            } else if (FXMLUtils.hasNonSkippablePrefix(childName)) {
                Optional<String> textContent = child.textValue();
                AbstractFXMLValue value;
                if (textContent.isPresent()) {
                    value = parseValueString(textContent.get().stripTrailing(), rawValueType, buildContext);
                } else {
                    List<AbstractFXMLValue> grandChildren = child.children()
                            .stream()
                            .map(grandChild -> parseMapElements(grandChild, buildContext, rawValueType))
                            .gather(Utils.optional())
                            .toList();
                    if (grandChildren.size() != 1) {
                        throw new IllegalArgumentException(
                                "Map entry element `%s` must have exactly one child element representing the value".formatted(
                                        childName));
                    }
                    value = grandChildren.getFirst();
                }
                entries.put(new FXMLLiteral(childName), value);
            }
        }
        return entries;
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
                .map(value -> parseValueString(value, expectedType, buildContext))
                .or(() -> parseElement(structure, buildContext));
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
            FXMLType constantType = helper.buildFXMLType(
                    FXMLUtils.resolveConstantType(clazz, constantName),
                    buildContext
            );
            return Optional.of(new FXMLConstant(new FXMLClassType(clazz), constantName, constantType));
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
        List<FXMLProperty> properties = parseAttributesProperties(clazz, buildContext, attributes);

        List<AbstractFXMLValue> defaultPropertyValues = new ArrayList<>();
        structure.children()
                .stream()
                .map(parseElementProperty(defaultPropertyValues, buildContext, clazz))
                .gather(Utils.optional())
                .forEach(properties::add);
        if (!defaultPropertyValues.isEmpty()) {
            FXMLUtils.resolveDefaultPropertyName(clazz)
                    .flatMap(name -> helper.findObjectProperty(context.buildContext(), clazz, name))
                    .flatMap(property -> handleObjectPropertyOrCollectionProperties(
                            context.buildContext(), property, Map.of(), defaultPropertyValues
                    ))
                    .ifPresent(properties::add);
        }
        return new FXMLObject(classAndIdentifier.identifier(), context.type(), context.factoryMethod(), properties);
    }

    /// Parses all attribute properties for a given class from a map of attributes.
    ///
    /// Only attributes with non-skippable prefixes are processed. Each such attribute is converted
    /// into an [FXMLProperty] using the attribute parsing logic.
    ///
    /// @param clazz        The [Class] for which the attributes are being parsed.
    /// @param buildContext The [BuildContext] used for property resolution and parsing.
    /// @param attributes   A [Map] of attribute names to their string values.
    /// @return A [List] of [FXMLProperty] objects representing the parsed attributes.
    private List<FXMLProperty> parseAttributesProperties(
            Class<?> clazz,
            BuildContext buildContext,
            Map<String, String> attributes
    ) {
        return attributes.entrySet()
                .stream()
                .filter(entry -> FXMLUtils.hasNonSkippablePrefix(entry.getKey()))
                .map(parseAttributeProperty(buildContext, clazz))
                .gather(Utils.optional())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /// Returns a function that parses a single attribute entry into an [Optional] [FXMLProperty].
    ///
    /// The returned function uses [parseProperty] to handle the attribute, delegating value parsing
    /// to [parseValueString] and object property parsing to [parseObjectProperty].
    ///
    /// @param buildContext The [BuildContext] used for property resolution and parsing.
    /// @param clazz        The [Class] to which the attribute property belongs.
    /// @return A [Function] that maps an attribute entry to an [Optional] [FXMLProperty].
    private Function<Map.Entry<String, String>, Optional<FXMLProperty>> parseAttributeProperty(
            BuildContext buildContext,
            Class<?> clazz
    ) {
        return attribute -> parseProperty(
                List.of(),
                buildContext,
                clazz,
                attribute,
                (fxmlType, value) -> parseValueString(value, FXMLUtils.findRawType(fxmlType), buildContext),
                this::parseObjectProperty,
                (_, _) -> Optional.empty()
        );
    }

    /// Parses an object property from a string value.
    ///
    /// This method handles different types of properties:
    /// - Maps: Currently not supported for string values.
    /// - Collections: Supported if the property is a getter (read-only collection).
    /// - Basic properties: Supported through setters or constructors.
    ///
    /// @param buildContext The [BuildContext] used for parsing and resolution.
    /// @param property     The [ObjectProperty] definition being parsed.
    /// @param value        The string value to be assigned to the property.
    /// @return An [Optional] containing the parsed [FXMLProperty], or empty if the property type or method type is not supported.
    private Optional<FXMLProperty> parseObjectProperty(BuildContext buildContext, ObjectProperty property, String value) {
        Class<?> rawType = FXMLUtils.findRawType(property.type());
        // region: map
        if (Map.class.isAssignableFrom(rawType)) {
            log.debug("Parsing string value property as map: %s is not supported.".formatted(property.name()));
            return Optional.empty();
        }
        // endregion
        // region: collection and objects
        AbstractFXMLValue parsedValue;
        if (Collection.class.isAssignableFrom(rawType)) {
            if (property.methodType() == ObjectProperty.MethodType.GETTER) {
                Class<?> collectionValueType = FXMLUtils.findRawType(FXMLUtils.findCollectionValueType(property.type()));
                parsedValue = parseValueString(value, collectionValueType, buildContext);
            } else {
                log.debug(
                        "Parsing string value property using setter or constructor as collection: %s is not supported.".formatted(
                                property.name()));
                return Optional.empty();
            }
        } else {
            parsedValue = parseValueString(value, rawType, buildContext);
        }
        return handleObjectPropertyOrCollectionProperties(
                buildContext, property, Map.of(), List.of(parsedValue)
        );
        // endregion
    }

    /// Returns a function that parses a [ParsedXMLStructure] into an [Optional] [FXMLProperty].
    ///
    /// This is typically used for parsing FXML elements that represent properties of an object.
    /// It delegates to [parseProperty] using various handlers for static properties, object properties,
    /// and nested elements.
    ///
    /// @param defaultPropertyValues A list to collect values for the default property if the element does not match a specific property.
    /// @param buildContext          The [BuildContext] used for property resolution and parsing.
    /// @param clazz                 The [Class] to which the element property belongs.
    /// @return A [Function] that maps a [ParsedXMLStructure] to an [Optional] [FXMLProperty].
    private Function<ParsedXMLStructure, Optional<FXMLProperty>> parseElementProperty(
            List<AbstractFXMLValue> defaultPropertyValues,
            BuildContext buildContext,
            Class<?> clazz
    ) {
        return xml -> parseProperty(
                defaultPropertyValues,
                buildContext,
                clazz,
                Map.entry(xml.name(), xml),
                (type, value) -> parseStaticPropertyOfElement(buildContext, type, value),
                this::parseObjectProperty,
                this::parseElement
        );
    }

    /// Parses a static property of an element from a [ParsedXMLStructure].
    ///
    /// This method handles both simple text values and complex child elements. If the structure
    /// contains a text value, it is parsed according to the expected type. If it contains
    /// child elements, it expects exactly one child representing the value.
    ///
    /// @param buildContext The [BuildContext] used for parsing.
    /// @param type         The [FXMLType] of the static property.
    /// @param value        The [ParsedXMLStructure] representing the property's value.
    /// @return An [AbstractFXMLValue] representing the parsed value.
    /// @throws IllegalArgumentException If the structure contains multiple child elements, when a single value is expected.
    private AbstractFXMLValue parseStaticPropertyOfElement(
            BuildContext buildContext,
            FXMLType type,
            ParsedXMLStructure value
    ) throws IllegalArgumentException {
        // region: text values
        Optional<String> textValue = value.textValue();
        if (textValue.isPresent()) {
            return parseValueString(textValue.get().stripTrailing(), FXMLUtils.findRawType(type), buildContext);
        }
        // endregion
        // region: child elements
        List<AbstractFXMLValue> values = parseChildrenAsValues(buildContext, value);
        if (values.size() == 1) {
            return values.getFirst();
        }
        throw new IllegalArgumentException("Expected single value for the static property");
        // endregion
    }

    /// Parses an object property from a [ParsedXMLStructure].
    ///
    /// This method handles properties defined as XML elements. It supports:
    /// - Text values within the element.
    /// - Map properties (via getters).
    /// - Collection properties or single object properties defined by child elements.
    ///
    /// @param buildContext The [BuildContext] used for parsing and resolution.
    /// @param property     The [ObjectProperty] definition being parsed.
    /// @param value        The [ParsedXMLStructure] representing the property element.
    /// @return An [Optional] containing the parsed [FXMLProperty], or empty if not supported or no values found.
    private Optional<FXMLProperty> parseObjectProperty(
            BuildContext buildContext,
            ObjectProperty property,
            ParsedXMLStructure value
    ) {
        // region: text values
        Optional<String> textValue = value.textValue();
        if (textValue.isPresent()) {
            return parseObjectProperty(buildContext, property, textValue.get().stripTrailing());
        }
        // endregion
        Class<?> rawType = FXMLUtils.findRawType(property.type());
        // region: map
        if (Map.class.isAssignableFrom(rawType) && property.methodType() == ObjectProperty.MethodType.GETTER) {
            Map.Entry<FXMLType, FXMLType> mapKeyAndValueTypes = FXMLUtils.findMapKeyAndValueTypes(property.type());
            Class<?> rawKeyType = FXMLUtils.findRawType(mapKeyAndValueTypes.getKey());
            Class<?> rawValueType = FXMLUtils.findRawType(mapKeyAndValueTypes.getValue());
            Map<FXMLLiteral, AbstractFXMLValue> entries = parseMapEntries(value, rawValueType, buildContext);
            return Optional.of(new FXMLMapProperty(
                    property.name(),
                    property.methodName().orElseThrow(),
                    property.type(),
                    new FXMLClassType(rawKeyType),
                    new FXMLClassType(rawValueType),
                    entries
            ));
        }
        // endregion
        List<AbstractFXMLValue> values = parseChildrenAsValues(buildContext, value);
        return handleObjectPropertyOrCollectionProperties(buildContext, property, value.properties(), values);
    }

    /// Parses the children of the given XML structure into a list of AbstractFXMLValue instances.
    ///
    /// @param buildContext the context for building and maintaining state during the parsing process
    /// @param value        the parsed XML structure containing the children to be processed
    /// @return a list of AbstractFXMLValue instances resulting from parsing the children elements
    private List<AbstractFXMLValue> parseChildrenAsValues(BuildContext buildContext, ParsedXMLStructure value) {
        return value.children()
                .stream()
                .map(child -> parseElement(child, buildContext))
                .gather(Utils.optional())
                .toList();
    }

    /// Handles the resolution of an object property or collection properties from a list of values.
    ///
    /// Depending on the raw type of the property and its method type (GETTER, SETTER, or CONSTRUCTOR),
    /// this method constructs an [FXMLCollectionProperties], [FXMLObjectProperty], or [FXMLConstructorProperty].
    ///
    /// @param buildContext The [BuildContext] used for property resolution.
    /// @param property     The [ObjectProperty] definition being handled.
    /// @param attributes   A [Map] of attributes associated with the property element.
    /// @param values       A [List] of [AbstractFXMLValue] objects representing the property's content.
    /// @return An [Optional] containing the resolved [FXMLProperty] or empty if no values are provided or the configuration is unsupported.
    /// @throws IllegalArgumentException If multiple values are provided for a non-collection property.
    private Optional<FXMLProperty> handleObjectPropertyOrCollectionProperties(
            BuildContext buildContext,
            ObjectProperty property,
            Map<String, String> attributes,
            List<AbstractFXMLValue> values
    ) {
        Class<?> rawType = FXMLUtils.findRawType(property.type());
        if (values.isEmpty()) {
            log.debug("No values found for property: %s".formatted(property.name()));
            return Optional.empty();
        }
        // region: collection
        if (Collection.class.isAssignableFrom(rawType) && property.methodType() == ObjectProperty.MethodType.GETTER) {
            return Optional.of(new FXMLCollectionProperties(
                    property.name(),
                    property.methodName().orElseThrow(),
                    property.type(),
                    values,
                    parseAttributesProperties(rawType, buildContext, attributes)
            ));
        }
        // endregion
        if (values.size() > 1) {
            log.debug("Multiple values found for property: %s".formatted(property.name()));
            throw new IllegalArgumentException("Multiple values found for property: %s".formatted(property.name()));
        }
        // region: basic property
        return switch (property.methodType()) {
            case GETTER -> {
                log.debug("Parsing string value property using getter: %s is not supported for non-collections.".formatted(
                        property.name()));
                yield Optional.empty();
            }
            case SETTER -> Optional.of(new FXMLObjectProperty(
                    property.name(),
                    property.methodName().orElseThrow(),
                    property.type(),
                    values.getFirst()
            ));
            case CONSTRUCTOR -> Optional.of(new FXMLConstructorProperty(
                    property.name(),
                    property.type(),
                    values.getFirst()
            ));
        };
        // endregion
    }

    /// Parses a property and processes it based on its type and context.
    /// This method handles both static setter properties and object properties
    /// and delegates additional processing to provided functional handlers.
    ///
    /// @param defaultPropertyValues a list to which default property values can be added during processing
    /// @param buildContext          the context for the current build, which provides additional metadata and helpers
    /// @param clazz                 the class type associated with the property being parsed
    /// @param element               a key-value pair representing the property name and its value
    /// @param valueProcessor        a function to process the given value of a specific FXML type
    /// @param propertyProcessor     a function to process object properties and attempt to create an [FXMLProperty]
    /// @param childHandler          a function to handle child elements during property parsing, which can result in new default property values being added
    /// @param <T>                   the type of the property value being processed
    /// @return an [Optional] containing the parsed [FXMLProperty], or `Optional.empty()` if no property was created
    private <T> Optional<FXMLProperty> parseProperty(
            List<AbstractFXMLValue> defaultPropertyValues,
            BuildContext buildContext,
            Class<?> clazz,
            Map.Entry<String, T> element,
            BiFunction<FXMLType, T, AbstractFXMLValue> valueProcessor,
            PropertyHandler<T> propertyProcessor,
            BiFunction<T, BuildContext, Optional<AbstractFXMLValue>> childHandler
    ) {
        String name = element.getKey();
        T value = element.getValue();
        if (name.contains(".")) {
            return helper.findStaticSetter(buildContext, name)
                    .map(staticSetter -> new FXMLStaticObjectProperty(
                            staticSetter.name(),
                            new FXMLClassType(staticSetter.staticClass()),
                            staticSetter.setter(),
                            staticSetter.fxmlType(),
                            valueProcessor.apply(staticSetter.fxmlType(), value)
                    ));
        }
        Optional<ObjectProperty> objectProperty = helper.findObjectProperty(buildContext, clazz, name);
        if (objectProperty.isPresent()) {
            return propertyProcessor.apply(buildContext, objectProperty.get(), value);
        }
        childHandler.apply(value, buildContext)
                .ifPresent(defaultPropertyValues::add);
        return Optional.empty();
    }

    /// Parses an XML structure into an [FXMLScript].
    ///
    /// The logic:
    /// 1. Checks for a `source` attribute; if present, resolves the path relative to the resource directory
    ///    and returns an [FXMLFileScript].
    /// 2. If no `source` is present, returns an [FXMLSourceScript] with the inline content.
    ///
    /// @param structure    The parsed XML structure representing the `fx:script` element.
    /// @param buildContext The [BuildContext] used to resolve the script file path.
    /// @return The parsed [FXMLScript].
    private FXMLScript parseScript(ParsedXMLStructure structure, BuildContext buildContext) {
        Map<String, String> attributes = structure.properties();
        if (attributes.containsKey(FXMLConstants.SOURCE_ATTRIBUTE)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            Charset charset = helper.getCharsetOfElement(structure);
            return new FXMLFileScript(helper.resolveResourcePath(source, buildContext), charset);
        }
        return new FXMLSourceScript(structure.textValue().orElse(""));
    }

    /// Parses an attribute value string into an [AbstractFXMLValue], with awareness of the expected getter
    /// parameter type and build context.
    ///
    /// The logic handles various FXML prefixes:
    /// - `%`: Returns [FXMLTranslation].
    /// - `@`: Returns [FXMLResource].
    /// - `#`: Returns [FXMLMethod] via [FXMLDocumentParserHelper#findMethodReferenceType(String, Class, BuildContext)].
    /// - `$`: Returns [FXMLReference].
    /// - `${...}`: Returns [FXMLExpression] if the expression is valid, otherwise throws [IllegalArgumentException].
    /// - `\`: Returns [FXMLLiteral] (escaped).
    /// - No prefix: Returns [FXMLInlineScript] if `paramType` is an `EventHandler`, otherwise returns [FXMLLiteral].
    ///
    /// @param value        The raw attribute value string.
    /// @param paramType    The expected getter parameter type, used to detect functional interfaces.
    /// @param buildContext The context used during the building process.
    /// @return The corresponding [AbstractFXMLValue].
    private AbstractFXMLValue parseValueString(String value, Class<?> paramType, BuildContext buildContext) {
        if (value.startsWith(FXMLConstants.TRANSLATION_PREFIX)) {
            return new FXMLTranslation(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.LOCATION_PREFIX)) {
            return new FXMLResource(helper.resolveResourcePath(value.substring(1), buildContext));
        }
        if (value.startsWith(FXMLConstants.METHOD_REFERENCE_PREFIX)) {
            return helper.findMethodReferenceType(value.substring(1), paramType, buildContext);
        }
        if (value.startsWith(FXMLConstants.EXPRESSION_PREFIX)) {
            if (value.startsWith(FXMLConstants.EXPRESSION_START) && value.endsWith(FXMLConstants.EXPRESSION_END)) {
                return new FXMLExpression(value.substring(2, value.length() - 1));
            }
            return new FXMLReference(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.ESCAPE_PREFIX)) {
            return new FXMLLiteral(value.substring(1));
        }
        if (FXMLUtils.isEventHandlerType(paramType)) {
            return new FXMLInlineScript(value);
        }
        return new FXMLLiteral(value);
    }
}
