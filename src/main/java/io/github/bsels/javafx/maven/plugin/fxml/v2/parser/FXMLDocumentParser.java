package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
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
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
    /// Mapping of special JavaFX FXML element names to their corresponding handler methods.
    ///
    /// Mappings:
    /// - `fx:copy`: [FXMLDocumentParser::parseFXCopy]
    /// - `fx:define`: [FXMLDocumentParser::parseFXDefine]
    /// - `fx:include`: [FXMLDocumentParser::parseFXInclude]
    /// - `fx:reference`: [FXMLDocumentParser::parseFXReference]
    /// - `fx:root`: [FXMLDocumentParser::parseFXRoot]
    /// - `fx:script`: [FXMLDocumentParser::parseFXScript]
    private static final Map<String, SpecialFXElementHandler> SPECIAL_FX_ELEMENTS_HANDLERS;
    /// Mapping of element names to [SpecialFXElementHandler] implementations where inline processing is not allowed.
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
    /// An instance of FXMLReader that handles the loading and parsing of FXML files.
    /// This variable is immutable and ensures reliable access to FXML processing functionality within the application.
    private final FXMLReader reader;

    /// Initializes a new [FXMLDocumentParser] instance.
    ///
    /// @param log            The logging instance for diagnostic output
    /// @param defaultCharset The default character set to use for parsing
    /// @throws NullPointerException If `log` or `defaultCharset` is null
    public FXMLDocumentParser(Log log, Charset defaultCharset) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        Objects.requireNonNull(defaultCharset, "`defaultCharset` must not be null");
        this.helper = new FXMLDocumentParserHelper(log, defaultCharset);
        this.reader = new FXMLReader(log);
    }

    /// Parses the specified [ParsedFXML] instance and constructs an [FXMLDocument].
    ///
    /// Logic:
    /// 1. Creates a root [BuildContext] with imports and resource path.
    /// 2. Resolves the controller class if `fx:controller` is present.
    /// 3. Recursively parses the root element.
    /// 4. Verifies that the root value is an [AbstractFXMLObject].
    ///
    /// @param parsedFXML   The [ParsedFXML] object to parse
    /// @param resourcePath The path relative to the resources folder root
    /// @param rootPath     The absolute path to the resource directory root
    /// @return An [FXMLDocument] representing the parsed structure
    /// @throws NullPointerException  If any parameter is null
    /// @throws IllegalStateException If the root element is not an [AbstractFXMLObject]
    public FXMLDocument parse(ParsedFXML parsedFXML, String resourcePath, Path rootPath) {
        Objects.requireNonNull(parsedFXML, "`parsedFXML` must not be null");
        Objects.requireNonNull(resourcePath, "`resourcePath` must not be null");
        Objects.requireNonNull(rootPath, "`rootPath` must not be null");
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
        List<FXMLInterface> interfaces = helper.parseInterfaces(rootStructure.comments(), buildContext);

        FXMLDocument fxmlDocument = new FXMLDocument(
                parsedFXML.className(),
                root,
                interfaces,
                controller,
                parsedFXML.scriptNamespace(),
                buildContext.definitions(),
                buildContext.scripts()
        );
        loadIncludeFXMLDocuments(fxmlDocument, resourcePath, rootPath);
        return fxmlDocument;
    }

    /// Parses an XML structure into an [FXMLValue].
    ///
    /// Logic:
    /// 1. Checks for special FX elements (e.g., `fx:include`).
    /// 2. Resolves the class and FXML identifier.
    /// 3. Identifies the factory method and determines the actual type.
    /// 4. Resolves type mappings and generic information.
    /// 5. Selects the parsing strategy (object, collection, or map).
    ///
    /// @param structure    The parsed XML structure
    /// @param buildContext The build context
    /// @param isRoot       Whether the element is the document root
    /// @return An [Optional] containing the parsed [AbstractFXMLValue]
    /// @throws IllegalStateException If parsing fails
    private Optional<AbstractFXMLValue> parseElement(ParsedXMLStructure structure, BuildContext buildContext, boolean isRoot)
            throws IllegalStateException {
        String nodeName = structure.name();
        if (SPECIAL_FX_ELEMENTS_HANDLERS.containsKey(nodeName)) {
            return parseSpecialFXElements(structure, buildContext, isRoot);
        }
        ClassAndIdentifier classAndIdentifier = helper.resolveClassAndIdentifier(structure, buildContext, isRoot);
        return Optional.of(parseNormalElements(structure, buildContext, classAndIdentifier));
    }

    /// Parses an XML element.
    /// Delegates to [#parseElement(ParsedXMLStructure, BuildContext, boolean)] with `isRoot` as `false`.
    ///
    /// @param structure    The parsed XML structure
    /// @param buildContext The build context
    /// @return An [Optional] containing the parsed [AbstractFXMLValue]
    /// @throws IllegalStateException If parsing fails
    private Optional<AbstractFXMLValue> parseElement(ParsedXMLStructure structure, BuildContext buildContext)
            throws IllegalStateException {
        return parseElement(structure, buildContext, false);
    }


    /// Parses a special FX element.
    ///
    /// @param structure    The parsed XML structure
    /// @param buildContext The build context
    /// @param isRoot       Whether the element is the root
    /// @return The resulting [AbstractFXMLValue]
    /// @throws IllegalStateException If the element is not the root and its name is 'fx:root'
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

    /// Parses an XML structure into an [AbstractFXMLValue] based on the provided context.
    ///
    /// @param structure          The parsed XML structure
    /// @param buildContext       The build context
    /// @param classAndIdentifier Information about the class and identifier
    /// @return An [AbstractFXMLValue] instance
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
        return Optional.of(new FXMLCopy(copyId, new FXMLExposedIdentifier(source)));
    }

    /// Parses an `fx:define` element from the given parsed XML structure and updates the build context with any
    /// extracted definitions.
    /// Returns an empty optional as this method produces no specific value.
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

    /// Parses an `fx:script` element from the given XML structure and updates the build context by adding the extracted
    /// script.
    /// Returns an empty optional as this method produces no specific value.
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
    /// The logic iterates over all child elements of the current structure,
    /// parsing each one into an [AbstractFXMLValue] using [#parseElement(ParsedXMLStructure, BuildContext)].
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
        FXMLType type = context.type();
        FXMLType keyType = FXMLUtils.findMapKeyTypeFromHierarchy(type);
        FXMLType valueType = FXMLUtils.findMapValueTypeFromHierarchy(type);
        Map<FXMLLiteral, AbstractFXMLValue> entries = parseMapEntries(structure, valueType, context.buildContext());
        return new FXMLMap(
                context.classAndIdentifier().identifier(),
                type,
                keyType,
                valueType,
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
    /// @param expectedType The expected type for the map values.
    /// @param buildContext The [BuildContext] used for parsing and resolving values.
    /// @return A [Map] of entry keys to their corresponding [AbstractFXMLValue] objects.
    /// @throws IllegalArgumentException If a child element for a map entry does not have exactly one child representing the value.
    private Map<FXMLLiteral, AbstractFXMLValue> parseMapEntries(ParsedXMLStructure structure, FXMLType expectedType, BuildContext buildContext) {
        Map<String, String> properties = structure.properties();
        Map<FXMLLiteral, AbstractFXMLValue> entries = properties.entrySet()
                .stream()
                .filter(entry -> FXMLUtils.hasNonSkippablePrefix(entry.getKey()) && !entry.getKey().equals("onChange"))
                .collect(Collectors.toMap(
                        entry -> new FXMLLiteral(entry.getKey()),
                        entry -> parseValueString(entry.getValue(), expectedType, buildContext),
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
                    value = parseValueString(
                            textContent.get().stripTrailing(),
                            expectedType,
                            buildContext
                    );
                } else {
                    List<AbstractFXMLValue> grandChildren = child.children()
                            .stream()
                            .map(grandChild -> parseMapElements(grandChild, buildContext, expectedType))
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
            FXMLType expectedType
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
                            property, Map.of(), defaultPropertyValues
                    ))
                    .ifPresent(properties::add);
        }
        return new FXMLObject(classAndIdentifier.identifier(), context.type(), context.factoryMethod(), properties);
    }

    /// Parses attribute properties for a class.
    ///
    /// @param clazz        The class to process
    /// @param buildContext The build context
    /// @param attributes   The map of attribute names to string values
    /// @return A list of [FXMLProperty] objects
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

    /// Returns a function for parsing an attribute into an [FXMLProperty].
    ///
    /// @param buildContext The build context
    /// @param clazz        The class to which the attribute belongs
    /// @return A [Function] that maps an attribute entry to an [Optional] [FXMLProperty]
    private Function<Map.Entry<String, String>, Optional<FXMLProperty>> parseAttributeProperty(
            BuildContext buildContext,
            Class<?> clazz
    ) {
        return attribute -> parseProperty(
                List.of(),
                buildContext,
                clazz,
                attribute,
                (fxmlType, value) -> parseValueString(value, fxmlType, buildContext),
                this::parseObjectProperty,
                (_, _) -> Optional.empty()
        );
    }

    /// Parses a string value as an [FXMLProperty] for the specified [ObjectProperty].
    ///
    /// @param buildContext The build context
    /// @param property     The [ObjectProperty] definition
    /// @param value        The string value to parse
    /// @return An [Optional] containing the parsed [FXMLProperty]
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
                FXMLType collectionValueType = FXMLUtils.findCollectionValueTypeFromHierarchy(property.type());
                parsedValue = parseValueString(value, collectionValueType, buildContext);
            } else {
                log.debug(
                        "Parsing string value property using setter or constructor as collection: %s is not supported.".formatted(
                                property.name()));
                return Optional.empty();
            }
        } else {
            parsedValue = parseValueString(value, property.type(), buildContext);
        }
        return handleObjectPropertyOrCollectionProperties(
                property, Map.of(), List.of(parsedValue)
        );
        // endregion
    }

    /// Returns a function for parsing an XML element into an [FXMLProperty].
    ///
    /// @param defaultPropertyValues A list to collect values for the default property
    /// @param buildContext          The build context
    /// @param clazz                 The class to which the property belongs
    /// @return A [Function] that maps a [ParsedXMLStructure] to an [Optional] [FXMLProperty]
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

    /// Parses a static property element into an [AbstractFXMLValue].
    ///
    /// @param buildContext The build context
    /// @param type         The expected type of the property
    /// @param value        The parsed XML structure
    /// @return The resulting [AbstractFXMLValue]
    /// @throws IllegalArgumentException If multiple child elements are found when one is expected
    private AbstractFXMLValue parseStaticPropertyOfElement(
            BuildContext buildContext,
            FXMLType type,
            ParsedXMLStructure value
    ) throws IllegalArgumentException {
        // region: text values
        Optional<String> textValue = value.textValue();
        if (textValue.isPresent()) {
            return parseValueString(textValue.get().stripTrailing(), type, buildContext);
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

    /// Parses a [ParsedXMLStructure] as an [FXMLProperty] for the specified [ObjectProperty].
    ///
    /// @param buildContext The build context
    /// @param property     The [ObjectProperty] definition
    /// @param value        The [ParsedXMLStructure] representing the property element
    /// @return An [Optional] containing the parsed [FXMLProperty]
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
            FXMLType keyType = FXMLUtils.findMapKeyTypeFromHierarchy(property.type());
            FXMLType valueType = FXMLUtils.findMapValueTypeFromHierarchy(property.type());
            Map<FXMLLiteral, AbstractFXMLValue> entries = parseMapEntries(value, valueType, buildContext);
            Optional<String> onChangeListener = Optional.empty();
            if (ObservableMap.class.isAssignableFrom(rawType) && value.properties().containsKey("onChange")) {
                onChangeListener = Optional.of(value.properties().get("onChange"));
            }
            return Optional.of(new FXMLMapProperty(
                    property.name(),
                    property.methodName().orElseThrow(),
                    property.type(),
                    keyType,
                    valueType,
                    entries,
                    onChangeListener
            ));
        }
        // endregion
        List<AbstractFXMLValue> values = parseChildrenAsValues(buildContext, value);
        return handleObjectPropertyOrCollectionProperties(property, value.properties(), values);
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

    /// Handles object property or collection properties based on the property type.
    ///
    /// @param property     The [ObjectProperty] definition
    /// @param attributes   The XML attributes of the property element
    /// @param values       The list of values to assign to the property
    /// @return An [Optional] containing the resolved [FXMLProperty]
    /// @throws IllegalArgumentException If multiple values are provided for a non-collection property
    private Optional<FXMLProperty> handleObjectPropertyOrCollectionProperties(
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
            Optional<String> onChangeListener = Optional.empty();
            if ((ObservableList.class.isAssignableFrom(rawType) || ObservableSet.class.isAssignableFrom(rawType))
                    && attributes.containsKey("onChange")) {
                onChangeListener = Optional.of(attributes.get("onChange"));
            }
            return Optional.of(new FXMLCollectionProperties(
                            property.name(),
                            property.methodName().orElseThrow(),
                            property.type(),
                            FXMLUtils.findCollectionValueTypeFromHierarchy(property.type()),
                            values,
                            onChangeListener
                    )
            );
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
    ///
    /// @param <T>                   The type of the property value
    /// @param defaultPropertyValues List to collect values for the default property
    /// @param buildContext          The build context
    /// @param clazz                 The class to which the property belongs
    /// @param element               The entry representing the property name and value
    /// @param valueProcessor        Function to process the property value
    /// @param propertyProcessor     Function to handle object properties
    /// @param childHandler          Function to handle child elements
    /// @return An [Optional] containing the parsed [FXMLProperty]
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
            return helper.findStaticSetter(clazz, buildContext, name)
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
    /// @param structure    The parsed XML structure
    /// @param buildContext The build context
    /// @return The parsed [FXMLScript]
    private FXMLScript parseScript(ParsedXMLStructure structure, BuildContext buildContext) {
        Map<String, String> attributes = structure.properties();
        if (attributes.containsKey(FXMLConstants.SOURCE_ATTRIBUTE)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            Charset charset = helper.getCharsetOfElement(structure);
            return new FXMLFileScript(helper.resolveResourcePath(source, buildContext), charset);
        }
        return new FXMLSourceScript(structure.textValue().orElse(""));
    }

    /// Parses a string value into an [AbstractFXMLValue].
    ///
    /// Supported prefixes:
    /// - `%`: Returns [FXMLTranslation]
    /// - `@`: Returns [FXMLResource]
    /// - `#`: Returns [FXMLMethod] via [FXMLDocumentParserHelper#findMethodReferenceType]
    /// - `$`: Returns [FXMLReference] or [FXMLExpression]
    /// - `\`: Returns [FXMLLiteral] (escaped)
    ///
    /// @param value        The string value to parse
    /// @param paramType    The expected type of the value
    /// @param buildContext The build context
    /// @return The corresponding [AbstractFXMLValue]
    private AbstractFXMLValue parseValueString(String value, FXMLType paramType, BuildContext buildContext) {
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
        if (FXMLUtils.isEventHandlerType(FXMLUtils.findRawType(paramType))) {
            return new FXMLInlineScript(value);
        }
        return new FXMLLiteral(value);
    }

    /// Loads and processes included FXML documents associated with the specified [FXMLDocument].
    ///
    /// @param document     The [FXMLDocument] instance
    /// @param resourcePath The base resource path
    /// @param rootPath     The root path of the project
    private void loadIncludeFXMLDocuments(FXMLDocument document, String resourcePath, Path rootPath) {
        loadIncludeFXMLDocuments(document.root(), resourcePath, rootPath);
        document.definitions()
                .forEach(definition -> loadIncludeFXMLDocuments(definition, resourcePath, rootPath));
    }

    /// Recursively loads FXML include documents for the specified value.
    ///
    /// @param value        The [AbstractFXMLValue] to process
    /// @param resourcePath The base resource path
    /// @param rootPath     The root path of the project
    private void loadIncludeFXMLDocuments(AbstractFXMLValue value, String resourcePath, Path rootPath) {
        switch (value) {
            case FXMLInclude(_, String sourceFile, Charset charset, _, FXMLLazyLoadedDocument lazyLoadedDocument) -> {
                try {
                    String substring = sourceFile.substring(1);
                    ParsedFXML parsedFXML = reader.readFXML(rootPath.resolve(substring), charset);
                    lazyLoadedDocument.set(parse(parsedFXML, resourcePath, rootPath));
                } catch (MojoExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            case FXMLCollection(_, _, _, List<AbstractFXMLValue> values) ->
                    values.forEach(value1 -> loadIncludeFXMLDocuments(value1, resourcePath, rootPath));
            case FXMLMap(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) -> entries.values()
                    .forEach(value1 -> loadIncludeFXMLDocuments(value1, resourcePath, rootPath));
            case FXMLObject(_, _, _, List<FXMLProperty> properties) ->
                    properties.forEach(property -> loadIncludeFXMLDocuments(property, resourcePath, rootPath));
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _,
                 FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> {
            }
        }
    }

    /// Recursively loads FXML include documents for the specified property.
    ///
    /// @param property     The [FXMLProperty] to process
    /// @param resourcePath The base resource path
    /// @param rootPath     The root path of the project
    private void loadIncludeFXMLDocuments(FXMLProperty property, String resourcePath, Path rootPath) {
        switch (property) {
            case FXMLCollectionProperties(_, _, _, _, List<AbstractFXMLValue> value, _) ->
                    value.forEach(value1 -> loadIncludeFXMLDocuments(value1, resourcePath, rootPath));
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) ->
                    loadIncludeFXMLDocuments(value, resourcePath, rootPath);
            case FXMLMapProperty(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> value, _) -> value.values()
                    .forEach(value1 -> loadIncludeFXMLDocuments(value1, resourcePath, rootPath));
            case FXMLObjectProperty(_, _, _, AbstractFXMLValue value) ->
                    loadIncludeFXMLDocuments(value, resourcePath, rootPath);
            case FXMLStaticObjectProperty(_, _, _, _, AbstractFXMLValue value) ->
                    loadIncludeFXMLDocuments(value, resourcePath, rootPath);
        }
    }
}
