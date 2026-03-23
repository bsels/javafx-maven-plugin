package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMultipleProperties;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLSingleProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticMultipleProperties;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticSingleProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLFileScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCollection;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMap;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLReference;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLResource;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLTranslation;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import com.github.bsels.javafx.maven.plugin.utils.Utils;
import javafx.beans.DefaultProperty;
import javafx.event.EventHandler;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/// Parses a [ParsedFXML] raw XML structure into a [FXMLDocument] V2 model.
///
/// This parser handles all standard FXML constructs including object instantiation,
/// property attributes, static properties, `fx:root`, `fx:id`, `fx:constant`,
/// `fx:value`, `fx:include`, `fx:copy`, `fx:reference`, `fx:script`, and inline scripts.
///
/// @param log the logging instance used for diagnostic output. Must not be null.
public record FXMLDocumentParser(Log log) {

    /// Compact constructor to validate the log dependency.
    ///
    /// @param log The logging instance used for diagnostic output.
    /// @throws NullPointerException if `log` is `null`.
    public FXMLDocumentParser {
        Objects.requireNonNull(log, "`log` must not be null");
    }

    /// Parses the provided [ParsedFXML] instance and constructs an [FXMLDocument].
    ///
    /// @param parsedFXML The [ParsedFXML] object to be parsed.
    /// @return An [FXMLDocument] that represents the parsed structure of the given [ParsedFXML].
    /// @throws NullPointerException if `parsedFXML` is `null`.
    public FXMLDocument parse(ParsedFXML parsedFXML) {
        Objects.requireNonNull(parsedFXML, "`parsedFXML` must not be null");
        ParsedXMLStructure rootStructure = parsedFXML.root();
        BuildContext buildContext = new BuildContext(parsedFXML.imports());

        Optional<Class<?>> controller = Optional.ofNullable(
                rootStructure.properties().get(FXMLConstants.FX_CONTROLLER_ATTRIBUTE)
        ).map(name -> Utils.findType(buildContext.imports(), name));

        AbstractFXMLObject root = parseObject(rootStructure, buildContext, true);

        return new FXMLDocument(
                root,
                controller,
                parsedFXML.scriptNamespace(),
                buildContext.imports(),
                buildContext.definitions(),
                buildContext.scripts()
        );
    }

    /// Parses an XML structure into an FXML object.
    ///
    /// @param structure    The parsed XML structure.
    /// @param buildContext The context used during the building process.
    /// @param isRoot       Whether the object is the root of the FXML document.
    /// @return An [AbstractFXMLObject] representing the parsed XML structure.
    /// @throws IllegalStateException if the parsing fails.
    private AbstractFXMLObject parseObject(ParsedXMLStructure structure, BuildContext buildContext, boolean isRoot)
            throws IllegalStateException {
        Map<String, String> properties = structure.properties();
        String nodeName = structure.name();
        ClassAndIdentifier classAndIdentifier = resolveClassAndIdentifier(nodeName, properties, buildContext, isRoot);
        if (Collection.class.isAssignableFrom(classAndIdentifier.clazz())) {
            return parseCollection(structure, buildContext, classAndIdentifier);
        } else if (Map.class.isAssignableFrom(classAndIdentifier.clazz())) {
            return parseMap(structure, buildContext, classAndIdentifier);
        } else {
            return parsePlainObject(structure, buildContext, classAndIdentifier);
        }
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLCollection] object.
    ///
    /// @param structure          The [ParsedXMLStructure] representing the XML to be parsed.
    /// @param buildContext       The [BuildContext] that provides the context during the parsing process.
    /// @param classAndIdentifier The [ClassAndIdentifier] containing the class type and identifier for the collection.
    /// @return An [FXMLCollection] object constructed from the parsed XML structure, generics, factory method, and child values.
    private FXMLCollection parseCollection(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier
    ) {
        Map<String, String> properties = structure.properties();
        FXMLType type = buildFXMLType(classAndIdentifier.clazz(), extractGenericsFromComments(structure.comments()), buildContext);
        Optional<String> factoryMethod = Optional.ofNullable(properties.get(FXMLConstants.FX_FACTORY_ATTRIBUTE));
        List<AbstractFXMLValue> values = structure.children()
                .stream()
                .map(parseValueChild(buildContext))
                .gather(Utils.optional(AbstractFXMLValue.class))
                .toList();
        return new FXMLCollection(
                classAndIdentifier.identifier(),
                type,
                factoryMethod,
                values
        );
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLMap] object.
    ///
    /// Entries are collected from two sources:
    /// - Attributes on the map element (skipping `fx:` and `xmlns` prefixes as well as `fx:factory`),
    ///   each parsed via [#parseValueString(String)].
    /// - Child elements, where the element name is used as the key and the child is parsed via
    ///   [#parseValue(ParsedXMLStructure, BuildContext)].
    ///
    /// @param structure          The [ParsedXMLStructure] representing the XML to be parsed.
    /// @param buildContext       The [BuildContext] that provides the context during the parsing process.
    /// @param classAndIdentifier The [ClassAndIdentifier] containing the class type and identifier for the map.
    /// @return An [FXMLMap] object constructed from the parsed XML structure.
    private FXMLMap parseMap(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier
    ) {
        Map<String, String> properties = structure.properties();
        FXMLType type = buildFXMLType(classAndIdentifier.clazz(), extractGenericsFromComments(structure.comments()), buildContext);
        Optional<String> factoryMethod = Optional.ofNullable(properties.get(FXMLConstants.FX_FACTORY_ATTRIBUTE));
        Map<String, AbstractFXMLValue> entries = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (hasSkippablePrefix(key) || FXMLConstants.FX_FACTORY_ATTRIBUTE.equals(key)) {
                continue;
            }
            entries.put(key, parseValueString(entry.getValue()));
        }
        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();
            if (FXMLConstants.FX_DEFINE_ELEMENT.equals(childName)) {
                child.children().stream()
                        .map(defChild -> parseObject(defChild, buildContext, false))
                        .forEach(buildContext.definitions()::add);
            } else if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(childName)) {
                buildContext.scripts().add(parseScript(child));
            } else if (!hasSkippablePrefix(childName)) {
                entries.put(childName, parseValue(child, buildContext));
            }
        }
        return new FXMLMap(
                classAndIdentifier.identifier(),
                type,
                factoryMethod,
                entries
        );
    }

    /// Parses the provided [ParsedXMLStructure] and constructs an [FXMLObject].
    ///
    /// @param structure          The [ParsedXMLStructure] representing the XML to be parsed.
    /// @param buildContext       The [BuildContext] that provides the context during the parsing process.
    /// @param classAndIdentifier The [ClassAndIdentifier] containing the class type and identifier for the object.
    /// @return An [FXMLObject] constructed from the parsed XML structure.
    private FXMLObject parsePlainObject(
            ParsedXMLStructure structure,
            BuildContext buildContext,
            ClassAndIdentifier classAndIdentifier
    ) {
        Class<?> clazz = classAndIdentifier.clazz();
        FXMLType type = buildFXMLType(clazz, extractGenericsFromComments(structure.comments()), buildContext);
        Map<String, String> attributes = structure.properties();
        Optional<String> factoryMethod = Optional.ofNullable(attributes.get(FXMLConstants.FX_FACTORY_ATTRIBUTE));
        List<FXMLProperty<?>> properties = new ArrayList<>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (hasSkippablePrefix(key)) {
                continue;
            }
            parseAttributeProperty(buildContext, clazz, key, entry.getValue()).ifPresent(properties::add);
        }

        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();
            if (FXMLConstants.FX_DEFINE_ELEMENT.equals(childName)) {
                child.children().stream()
                        .map(defChild -> parseObject(defChild, buildContext, false))
                        .forEach(buildContext.definitions()::add);
            } else if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(childName)) {
                buildContext.scripts().add(parseScript(child));
            } else if (FXMLConstants.FX_INCLUDE_ELEMENT.equals(childName)
                    || FXMLConstants.FX_REFERENCE_ELEMENT.equals(childName)
                    || FXMLConstants.FX_COPY_ELEMENT.equals(childName)) {
                AbstractFXMLValue fxValue = parseValue(child, buildContext);
                Optional<String> defaultPropName = resolveDefaultPropertyName(clazz);
                String getterName = Utils.getGetterName(defaultPropName.orElseThrow());
                try {
                    Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
                    addValueToMultipleProperty(properties, defaultPropName.orElseThrow(), getterName, elementType, fxValue);
                } catch (NoSuchMethodException _) {
                    log.debug("No default list property found on %s for %s, skipping".formatted(clazz.getSimpleName(), childName));
                }
            } else if (childName.contains(".")) {
                int dotIndex = childName.lastIndexOf('.');
                String ownerPart = childName.substring(0, dotIndex);
                String propName = childName.substring(dotIndex + 1);
                if (Character.isUpperCase(ownerPart.charAt(0))) {
                    Class<?> staticClass = Utils.findType(buildContext.imports(), ownerPart);
                    String staticSetterName = Utils.getSetterName(propName);
                    parseStaticPropertyElement(buildContext, staticClass, staticSetterName, propName, child)
                            .ifPresent(properties::add);
                } else {
                    parseInstancePropertyElement(buildContext, clazz, propName, child).ifPresent(properties::add);
                }
            } else if (!hasSkippablePrefix(childName)) {
                AbstractFXMLObject childObject = parseObject(child, buildContext, false);
                Optional<String> defaultPropName = resolveDefaultPropertyName(clazz);
                String getterName = Utils.getGetterName(defaultPropName.orElseThrow());
                try {
                    Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
                    addValueToMultipleProperty(properties, defaultPropName.orElseThrow(), getterName, elementType, (AbstractFXMLValue) childObject);
                } catch (NoSuchMethodException _) {
                    log.debug("No default list property found on %s for child %s, skipping".formatted(clazz.getSimpleName(), childName));
                }
            }
        }

        return new FXMLObject(classAndIdentifier.identifier(), type, factoryMethod, properties);
    }

    /// Resolves the default property name for a class using the [DefaultProperty] annotation.
    ///
    /// If the class (or any of its superclasses) is annotated with [DefaultProperty],
    /// the annotated property name is returned.
    ///
    /// @param clazz the class to inspect for a [DefaultProperty] annotation.
    /// @return the default property name if found, or an empty [Optional] if not found.
    private Optional<String> resolveDefaultPropertyName(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null) {
            DefaultProperty annotation = current.getAnnotation(DefaultProperty.class);
            if (annotation != null) {
                return Optional.of(annotation.value());
            }
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    /// Builds an [FXMLType] from a class and a list of generic type name strings.
    ///
    /// If the generics list is empty, returns a [FXMLClassType]. Otherwise, parses each generic
    /// string into an [FXMLType] and returns an [FXMLGenericType].
    ///
    /// @param clazz        The base class.
    /// @param generics     The list of generic type name strings extracted from XML comments.
    /// @param buildContext The build context used for class resolution.
    /// @return The corresponding [FXMLType].
    private FXMLType buildFXMLType(Class<?> clazz, List<String> generics, BuildContext buildContext) {
        if (generics.isEmpty()) {
            return new FXMLClassType(clazz);
        }
        List<FXMLType> typeArgs = generics.stream()
                .<FXMLType>map(generic -> new FXMLClassType(Utils.findType(buildContext.imports(), generic)))
                .toList();
        return new FXMLGenericType(clazz, typeArgs);
    }

    /// Parses a child XML structure into an [AbstractFXMLValue].
    ///
    /// @param buildContext the build context for parsing
    /// @return a function that parses a child XML structure into an [AbstractFXMLValue]
    private Function<ParsedXMLStructure, Optional<AbstractFXMLValue>> parseValueChild(BuildContext buildContext) {
        return child -> parseValueChild(child, buildContext);
    }

    /// Parses a value node from the XML structure and processes it based on its type.
    /// Handles special cases such as "fx:define", "fx:script", or nodes with skippable prefixes.
    ///
    /// @param structure    the parsed XML structure representing the value node
    /// @param buildContext the context used during building, containing definitions, scripts, and other relevant data
    /// @return an [Optional] containing the parsed [AbstractFXMLValue], or an empty [Optional] if the node is processed without producing a value (e.g., "fx:define" or "fx:script" nodes)
    private Optional<AbstractFXMLValue> parseValueChild(ParsedXMLStructure structure, BuildContext buildContext) {
        String nodeName = structure.name();
        if (FXMLConstants.FX_DEFINE_ELEMENT.equals(nodeName)) {
            structure.children()
                    .stream()
                    .map(child -> parseObject(child, buildContext, false))
                    .forEach(buildContext.definitions()::add);
            return Optional.empty();
        } else if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(nodeName)) {
            buildContext.scripts()
                    .add(parseScript(structure));
            return Optional.empty();
        } else if (hasSkippablePrefix(nodeName)) {
            return Optional.empty();
        } else {
            return Optional.of(parseValue(structure, buildContext));
        }
    }

    /// Parses an XML structure into an FXML value.
    ///
    /// Handles `fx:include`, `fx:reference`, `fx:copy`, `fx:script` (inline), `fx:constant`,
    /// `fx:value`, and plain object nodes.
    ///
    /// @param structure    The parsed XML structure.
    /// @param buildContext The context used during the building process.
    /// @return The parsed [AbstractFXMLValue].
    private AbstractFXMLValue parseValue(ParsedXMLStructure structure, BuildContext buildContext) {
        String nodeName = structure.name();
        Map<String, String> attributes = structure.properties();

        if (FXMLConstants.FX_INCLUDE_ELEMENT.equals(nodeName)) {
            String src = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            if (src == null) {
                throw new IllegalArgumentException("`source` attribute is required for fx:include");
            }
            FXMLIdentifier includeId = resolveOptionalIdentifier(attributes)
                    .orElseGet(() -> new FXMLInternalIdentifier(buildContext.internalCounter().getAndIncrement()));
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
                    .orElseGet(() -> new FXMLInternalIdentifier(buildContext.internalCounter().getAndIncrement()));
            return new FXMLCopy(copyId, source);
        }

        if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(nodeName)) {
            String scriptContent = structure.getTextValue();
            if (!scriptContent.isBlank()) {
                return new FXMLInlineScript(scriptContent);
            }
            throw new IllegalArgumentException("`fx:script` used as value must have inline content");
        }

        Class<?> clazz = Utils.findType(buildContext.imports(), nodeName);

        if (attributes.containsKey(FXMLConstants.FX_CONSTANT_ATTRIBUTE)) {
            String constantName = attributes.get(FXMLConstants.FX_CONSTANT_ATTRIBUTE);
            FXMLType constantType = new FXMLClassType(Utils.getClassType(resolveConstantType(clazz, constantName)));
            return new FXMLConstant(clazz, constantName, constantType);
        }

        if (attributes.containsKey(FXMLConstants.FX_VALUE_ATTRIBUTE)) {
            String val = attributes.get(FXMLConstants.FX_VALUE_ATTRIBUTE);
            Optional<FXMLIdentifier> id = resolveOptionalIdentifier(attributes);
            return new FXMLValue(id, new FXMLClassType(clazz), val);
        }

        return parseObject(structure, buildContext, false);
    }

    /// Determines if the given string has a skippable prefix.
    ///
    /// @param key The string to check for skippable prefixes.
    /// @return `true` if the string starts with a skippable prefix; `false` otherwise.
    private boolean hasSkippablePrefix(String key) {
        return key.startsWith(FXMLConstants.FX_PREFIX) || key.startsWith(FXMLConstants.XML_NAMESPACE_PREFIX);
    }

    /// Parses an XML structure into an [FXMLScript].
    ///
    /// If the structure has a `source` attribute, returns an [FXMLFileScript] with the resolved
    /// path and charset. Otherwise, returns an [FXMLSourceScript] with the inline script content.
    ///
    /// @param structure The parsed XML structure representing the `fx:script` element.
    /// @return The parsed [FXMLScript].
    private FXMLScript parseScript(ParsedXMLStructure structure) {
        Map<String, String> attributes = structure.properties();
        if (attributes.containsKey(FXMLConstants.SOURCE_ATTRIBUTE)) {
            String source = attributes.get(FXMLConstants.SOURCE_ATTRIBUTE);
            String charsetName = attributes.getOrDefault(FXMLConstants.CHARSET_ATTRIBUTE, StandardCharsets.UTF_8.name());
            Charset charset = Charset.forName(charsetName);
            return new FXMLFileScript(source, charset);
        }
        String scriptContent = structure.getTextValue();
        if (!scriptContent.isBlank()) {
            return new FXMLSourceScript(scriptContent);
        }
        return new FXMLSourceScript(scriptContent);
    }

    /// Resolves and returns a [ClassAndIdentifier] object based on the given node name, attributes, and context.
    /// This method identifies the class type and identifier for the specified node in an FXML document.
    ///
    /// @param nodeName     The name of the node being processed.
    /// @param attributes   A map of attributes associated with the node.
    /// @param buildContext The context in which the FXML document is being built.
    /// @param isRoot       Whether the node is the root of the FXML document.
    /// @return A [ClassAndIdentifier] containing the resolved class type and identifier for the node.
    /// @throws IllegalStateException    if the node is labeled as `fx:root` but is not the document root, or if `fx:root` is missing the required `type` attribute.
    /// @throws IllegalArgumentException if the class type cannot be resolved.
    private ClassAndIdentifier resolveClassAndIdentifier(
            String nodeName,
            Map<String, String> attributes,
            BuildContext buildContext,
            boolean isRoot
    ) throws IllegalStateException, IllegalArgumentException {
        FXMLIdentifier identifier;
        Class<?> clazz;
        if (FXMLConstants.FX_ROOT_ELEMENT.equals(nodeName)) {
            if (!isRoot) {
                throw new IllegalStateException("Root object must be the document root");
            }
            String typeName = attributes.get(FXMLConstants.TYPE_ATTRIBUTE);
            if (typeName == null) {
                throw new IllegalStateException("fx:root must have a 'type' attribute");
            }
            clazz = Utils.findType(buildContext.imports(), typeName);
            identifier = FXMLRootIdentifier.INSTANCE;
            log.debug("Parsing fx:root with type: %s".formatted(clazz.getName()));
        } else {
            clazz = Utils.findType(buildContext.imports(), nodeName);
            if (isRoot) {
                identifier = FXMLRootIdentifier.INSTANCE;
            } else if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
                identifier = new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE));
            } else {
                identifier = new FXMLInternalIdentifier(buildContext.internalCounter().getAndIncrement());
            }
            log.debug("Parsing object with type: %s".formatted(clazz.getName()));
        }
        return new ClassAndIdentifier(clazz, identifier);
    }

    /// Converts an XML attribute into an [FXMLProperty], handling both static and instance properties.
    ///
    /// @param buildContext  the build context for class resolution.
    /// @param clazz         the class owning the property.
    /// @param attributeName the attribute name (may contain a dot for static properties).
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the property, or empty if it could not be resolved.
    private Optional<FXMLProperty<?>> parseAttributeProperty(
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

    /// Converts a static attribute property (e.g., `GridPane.rowIndex="0"`) into an [FXMLStaticSingleProperty].
    ///
    /// @param buildContext  the build context for class resolution.
    /// @param attributeName the full attribute name including the class prefix.
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the static property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> parseStaticAttributeProperty(
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
            log.warn("Could not resolve static property class '%s', skipping attribute '%s'".formatted(className, attributeName));
            return Optional.empty();
        }
        String staticSetterName = Utils.getSetterName(propName);
        List<Method> setters = Utils.findStaticSettersForNode(staticClass, staticSetterName);
        if (setters.isEmpty()) {
            log.warn("No static setter '%s' found on '%s', skipping".formatted(staticSetterName, staticClass.getName()));
            return Optional.empty();
        }
        if (setters.size() > 1) {
            log.warn("Multiple static setters '%s' found on '%s', skipping".formatted(staticSetterName, staticClass.getName()));
            return Optional.empty();
        }
        Method setter = setters.getFirst();
        Type paramType = setter.getGenericParameterTypes()[1];
        FXMLValue fxmlValue = new FXMLValue(Optional.empty(), new FXMLClassType(Utils.getClassType(paramType)), value);
        return Optional.of(new FXMLStaticSingleProperty(propName, staticClass, staticSetterName, paramType, fxmlValue));
    }

    /// Converts an instance attribute property (e.g., `text="Hello"`) into an [FXMLSingleProperty].
    ///
    /// If the setter's parameter type is `EventHandler`, the value may be an inline script (no prefix),
    /// an expression (`$`), or a method reference (`#`), all of which are handled accordingly.
    ///
    /// @param buildContext  the build context for class resolution.
    /// @param clazz         the class owning the property.
    /// @param attributeName the attribute name.
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> parseInstanceAttributeProperty(
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
            AbstractFXMLValue fxmlValue = parseValueString(value, Utils.getClassType(paramType), buildContext);
            return Optional.of(new FXMLSingleProperty(attributeName, Optional.of(setterName), paramType, fxmlValue));
        }
        String getterName = Utils.getGetterName(attributeName);
        try {
            Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
            AbstractFXMLValue fxmlValue = parseValueString(value);
            return Optional.of(new FXMLSingleProperty(attributeName, Optional.of(getterName + "().add"), elementType, fxmlValue));
        } catch (NoSuchMethodException _) {
            log.debug("No setter or list getter found for '%s' on '%s', skipping".formatted(attributeName, clazz.getName()));
            return Optional.empty();
        }
    }

    /// Converts a static property child element into an [FXMLProperty].
    ///
    /// @param buildContext     the build context for class resolution.
    /// @param staticClass      the class defining the static property.
    /// @param staticSetterName the name of the static setter method.
    /// @param propName         the property name.
    /// @param child            the child XML structure containing the property values.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> parseStaticPropertyElement(
            BuildContext buildContext,
            Class<?> staticClass,
            String staticSetterName,
            String propName,
            ParsedXMLStructure child
    ) {
        List<Method> setters = Utils.findStaticSettersForNode(staticClass, staticSetterName);
        if (setters.isEmpty()) {
            log.warn("No static setter '%s' found on '%s', skipping".formatted(staticSetterName, staticClass.getName()));
            return Optional.empty();
        }
        if (setters.size() > 1) {
            log.warn("Multiple static setters '%s' found on '%s', skipping".formatted(staticSetterName, staticClass.getName()));
            return Optional.empty();
        }
        Method setter = setters.getFirst();
        Type paramType = setter.getGenericParameterTypes()[1];
        List<AbstractFXMLValue> values = parseChildrenToValues(buildContext, child);
        if (values.size() == 1) {
            return Optional.of(new FXMLStaticSingleProperty(propName, staticClass, staticSetterName, paramType, values.getFirst()));
        }
        return Optional.of(new FXMLStaticMultipleProperties(propName, staticClass, staticSetterName, paramType, values));
    }

    /// Converts an instance property child element into an [FXMLProperty].
    ///
    /// @param buildContext the build context for class resolution.
    /// @param clazz        the class owning the property.
    /// @param propName     the property name.
    /// @param child        the child XML structure containing the property values.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> parseInstancePropertyElement(
            BuildContext buildContext,
            Class<?> clazz,
            String propName,
            ParsedXMLStructure child
    ) {
        String setterName = Utils.getSetterName(propName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        List<AbstractFXMLValue> values = parseChildrenToValues(buildContext, child);

        if (!setters.isEmpty()) {
            if (setters.size() > 1) {
                log.warn("Multiple setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
                return Optional.empty();
            }
            Method setter = setters.getFirst();
            Type paramType = setter.getGenericParameterTypes()[0];
            if (values.size() == 1) {
                return Optional.of(new FXMLSingleProperty(propName, Optional.of(setterName), paramType, values.getFirst()));
            }
            return Optional.of(new FXMLMultipleProperties(propName, Optional.of(setterName), paramType, values));
        }

        String getterName = Utils.getGetterName(propName);
        try {
            Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
            return Optional.of(new FXMLMultipleProperties(propName, Optional.of(getterName + "().add"), elementType, values));
        } catch (NoSuchMethodException _) {
            log.debug("No setter or list getter found for property element '%s' on '%s', skipping".formatted(propName, clazz.getName()));
            return Optional.empty();
        }
    }

    /// Converts the children of a property element into a list of [AbstractFXMLValue].
    ///
    /// @param buildContext   the build context for class resolution.
    /// @param propertyElement the property element whose children are to be converted.
    /// @return the list of converted values.
    private List<AbstractFXMLValue> parseChildrenToValues(
            BuildContext buildContext,
            ParsedXMLStructure propertyElement
    ) {
        List<AbstractFXMLValue> values = new ArrayList<>();
        for (ParsedXMLStructure child : propertyElement.children()) {
            values.add(parseValue(child, buildContext));
        }
        return values;
    }

    /// Parses an attribute value string into an [AbstractFXMLValue].
    ///
    /// The following prefixes are handled:
    /// - [FXMLConstants#TRANSLATION_PREFIX] (`%`) — translation key, returns [FXMLTranslation].
    /// - [FXMLConstants#LOCATION_PREFIX] (`@`) — resource/location reference, returns [FXMLResource].
    /// - [FXMLConstants#METHOD_REFERENCE_PREFIX] (`#`) — method reference, returns [FXMLMethod].
    /// - [FXMLConstants#EXPRESSION_PREFIX] (`$`) — binding expression, returns [FXMLExpression].
    /// - [FXMLConstants#ESCAPE_PREFIX] (`\`) — escaped literal, strips the prefix and returns [FXMLValue].
    /// - No prefix — plain string value, returns [FXMLValue].
    ///
    /// @param value the raw attribute value string.
    /// @return the corresponding [AbstractFXMLValue].
    private AbstractFXMLValue parseValueString(String value) {
        return parseValueString(value, null, null);
    }

    /// Parses an attribute value string into an [AbstractFXMLValue], with awareness of the expected setter
    /// parameter type and build context.
    ///
    /// When the expected `paramType` is a functional interface (e.g., `EventHandler`), a method reference
    /// (`#handler`) without a prefix is treated as a method reference rather than a plain string literal.
    /// The return type of the method reference is resolved from the functional interface's method signature.
    ///
    /// The following prefixes are handled:
    /// - [FXMLConstants#TRANSLATION_PREFIX] (`%`) — translation key, returns [FXMLTranslation].
    /// - [FXMLConstants#LOCATION_PREFIX] (`@`) — resource/location reference, returns [FXMLResource].
    /// - [FXMLConstants#METHOD_REFERENCE_PREFIX] (`#`) — method reference, returns [FXMLMethod].
    /// - [FXMLConstants#EXPRESSION_PREFIX] (`$`) — binding expression, returns [FXMLExpression].
    /// - [FXMLConstants#ESCAPE_PREFIX] (`\`) — escaped literal, strips the prefix and returns [FXMLValue].
    /// - No prefix with `paramType` assignable to [EventHandler] — treated as inline script, returns [FXMLInlineScript].
    /// - No prefix — plain string value, returns [FXMLValue].
    ///
    /// @param value        the raw attribute value string.
    /// @param paramType    the expected setter parameter type, used to detect functional interfaces; may be `null`.
    /// @param buildContext the build context used for type resolution; may be `null`.
    /// @return the corresponding [AbstractFXMLValue].
    private AbstractFXMLValue parseValueString(String value, Class<?> paramType, BuildContext buildContext) {
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
            return new FXMLValue(Optional.empty(), new FXMLClassType(String.class), value.substring(1));
        }
        if (paramType != null && isEventHandlerType(paramType)) {
            return new FXMLInlineScript(value);
        }
        return new FXMLValue(Optional.empty(), new FXMLClassType(String.class), value);
    }

    /// Resolves a method reference value string into an [FXMLMethod].
    ///
    /// If `paramType` is a functional interface, the return type is resolved from its single abstract method.
    /// Otherwise, `void` is used as the return type.
    ///
    /// @param methodName the method name (without the `#` prefix).
    /// @param paramType  the expected setter parameter type; may be `null`.
    /// @return the corresponding [FXMLMethod].
    private FXMLMethod resolveMethodReference(String methodName, Class<?> paramType) {
        FXMLType returnType;
        if (paramType != null && isFunctionalInterface(paramType)) {
            try {
                Method functionalMethod = Utils.getFunctionalMethod(paramType);
                returnType = new FXMLClassType(functionalMethod.getReturnType());
            } catch (IllegalStateException _) {
                returnType = new FXMLClassType(void.class);
            }
        } else {
            returnType = new FXMLClassType(void.class);
        }
        return new FXMLMethod(methodName, List.of(), returnType);
    }

    /// Checks whether the given class is or extends `javafx.event.EventHandler`.
    ///
    /// Uses class name comparison to avoid a hard compile-time dependency on the JavaFX event module.
    ///
    /// @param clazz the class to check.
    /// @return `true` if the class is assignable to `javafx.event.EventHandler`; `false` otherwise.
    private boolean isEventHandlerType(Class<?> clazz) {
        return EventHandler.class.isAssignableFrom(clazz);
    }

    /// Checks whether the given class is a functional interface.
    ///
    /// Used to determine the return type of method reference from the functional interface's
    /// single abstract method. A functional interface is an interface annotated with
    /// [@FunctionalInterface] or one that has exactly one abstract method.
    ///
    /// @param clazz the class to check.
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
    /// @param clazz        The class defining the constant.
    /// @param constantName The name of the constant field.
    /// @return The [Type] of the constant field.
    /// @throws IllegalArgumentException if the field does not exist or is not static.
    private Type resolveConstantType(Class<?> clazz, String constantName) {
        try {
            Field field = clazz.getField(constantName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Field `%s` on `%s` is not static".formatted(constantName, clazz.getName()));
            }
            return field.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No such constant field `%s` on `%s`".formatted(constantName, clazz.getName()), e);
        }
    }

    /// Resolves an optional [FXMLIdentifier] from the given attributes map.
    ///
    /// If the attributes contain an [FXMLConstants#FX_ID_ATTRIBUTE] entry, an
    /// [FXMLExposedIdentifier] is returned. Otherwise, an empty [Optional] is returned.
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
    /// @param comments The list of XML comments on the element.
    /// @return The list of extracted generic type strings.
    private List<String> extractGenericsFromComments(List<String> comments) {
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

    /// Adds a value to an existing [FXMLMultipleProperties] entry or creates a new one.
    ///
    /// @param properties  The current list of properties to update.
    /// @param propName    The property name.
    /// @param getterName  The getter method name for the list property.
    /// @param elementType The element type of the list.
    /// @param value       The value to add.
    private void addValueToMultipleProperty(
            List<FXMLProperty<?>> properties,
            String propName,
            String getterName,
            Type elementType,
            AbstractFXMLValue value
    ) {
        String accessorName = getterName + "().add";
        for (int i = 0; i < properties.size(); i++) {
            FXMLProperty<?> existing = properties.get(i);
            if (existing instanceof FXMLMultipleProperties mp && mp.name().equals(propName)) {
                List<AbstractFXMLValue> newValues = new ArrayList<>(mp.value());
                newValues.add(value);
                properties.set(i, new FXMLMultipleProperties(propName, Optional.of(accessorName), elementType, newValues));
                return;
            }
        }
        List<AbstractFXMLValue> values = new ArrayList<>();
        values.add(value);
        properties.add(new FXMLMultipleProperties(propName, Optional.of(accessorName), elementType, values));
    }

    /// Holds a class and its associated FXML identifier.
    ///
    /// @param clazz      The class type.
    /// @param identifier The FXML identifier.
    private record ClassAndIdentifier(Class<?> clazz, FXMLIdentifier identifier) {
        /// Compact constructor to validate the class and identifier.
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
    /// @param internalCounter The counter for generating internal identifiers.
    /// @param imports         The list of imports.
    /// @param definitions     The list of definitions.
    /// @param scripts         The list of scripts.
    private record BuildContext(
            AtomicInteger internalCounter,
            List<String> imports,
            List<AbstractFXMLObject> definitions,
            List<FXMLScript> scripts
    ) {

        /// Compact constructor to validate the build context components.
        ///
        /// @param internalCounter The counter for generating internal identifiers.
        /// @param imports         The list of imports.
        /// @param definitions     The list of definitions.
        /// @param scripts         The list of scripts.
        /// @throws NullPointerException if `internalCounter`, `imports`, `definitions`, or `scripts` is `null`.
        public BuildContext {
            Objects.requireNonNull(internalCounter, "`internalCounter` must not be null");
            Objects.requireNonNull(imports, "`imports` must not be null");
            Objects.requireNonNull(definitions, "`definitions` must not be null");
            Objects.requireNonNull(scripts, "`scripts` must not be null");
        }

        /// Constructs a new build context with the provided imports.
        ///
        /// @param imports The list of imports.
        public BuildContext(List<String> imports) {
            this(new AtomicInteger(), imports, new ArrayList<>(), new ArrayList<>());
        }
    }
}
