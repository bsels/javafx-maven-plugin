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
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLConstant;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLCopy;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLExpression;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInclude;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
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
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
    /// @param log the logging instance used for diagnostic output. Must not be null.
    /// @throws NullPointerException if log is null.
    public FXMLDocumentParser {
        Objects.requireNonNull(log, "`log` must not be null");
    }

    /// Parses the given [ParsedFXML] into a [FXMLDocument].
    ///
    /// Definitions and scripts encountered at any nesting level are collected and stored
    /// grouped in the returned [FXMLDocument].
    ///
    /// @param parsedFXML the raw-parsed FXML structure to convert. Must not be null.
    /// @return the parsed [FXMLDocument] representing the V2 model.
    /// @throws NullPointerException     if parsedFXML is null.
    /// @throws IllegalArgumentException if the FXML structure is invalid.
    public FXMLDocument parse(ParsedFXML parsedFXML) {
        Objects.requireNonNull(parsedFXML, "`parsedFXML` must not be null");

        List<String> imports = new ArrayList<>(parsedFXML.imports());
        ParsedXMLStructure rootStructure = parsedFXML.root();
        AtomicInteger internalCounter = new AtomicInteger();

        Optional<String> controller = Optional.ofNullable(
                rootStructure.properties()
                        .get(FXMLConstants.FX_CONTROLLER_ATTRIBUTE)
        );

        List<FXMLObject> definitions = new ArrayList<>();
        List<FXMLScript> scripts = new ArrayList<>();

        FXMLObject root = convertObject(imports, rootStructure, internalCounter, true, definitions, scripts);

        return new FXMLDocument(root, controller, parsedFXML.scriptNamespace(), imports, definitions, scripts);
    }

    /// Converts a [ParsedXMLStructure] node into an [FXMLObject].
    ///
    /// Definitions and scripts encountered at any nesting level are accumulated into the
    /// provided global `definitions` and `scripts` lists.
    ///
    /// @param imports         the list of imports used for class resolution.
    /// @param structure       the XML structure node to convert.
    /// @param internalCounter the counter used to generate unique internal identifiers.
    /// @param isRoot          whether this node is the document root element.
    /// @param definitions     the global list to accumulate all `fx:define` objects into.
    /// @param scripts         the global list to accumulate all `fx:script` elements into.
    /// @return the converted [FXMLObject].
    private FXMLObject convertObject(
            List<String> imports,
            ParsedXMLStructure structure,
            AtomicInteger internalCounter,
            boolean isRoot,
            List<FXMLObject> definitions,
            List<FXMLScript> scripts
    ) {
        Map<String, String> attributes = structure.properties();
        String nodeName = structure.name();

        Class<?> clazz;
        FXMLIdentifier identifier;
        Optional<String> factoryMethod = Optional.empty();

        if (FXMLConstants.FX_ROOT_ELEMENT.equals(nodeName)) {
            String typeName = attributes.get("type");
            if (typeName == null) {
                throw new IllegalArgumentException("fx:root must have a 'type' attribute");
            }
            clazz = Utils.findType(imports, typeName);
            identifier = FXMLRootIdentifier.INSTANCE;
            log.debug("Parsed fx:root with type: %s".formatted(clazz.getName()));
        } else {
            clazz = Utils.findType(imports, nodeName);
            if (isRoot) {
                identifier = FXMLRootIdentifier.INSTANCE;
            } else if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
                identifier = new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE));
            } else {
                identifier = new FXMLInternalIdentifier(internalCounter.getAndIncrement());
            }
        }

        if (attributes.containsKey(FXMLConstants.FX_FACTORY_ATTRIBUTE)) {
            factoryMethod = Optional.of(attributes.get(FXMLConstants.FX_FACTORY_ATTRIBUTE));
        }

        List<String> generics = extractGenericsFromComments(structure.comments());
        List<FXMLProperty<?>> properties = new ArrayList<>();

        // Process XML attributes as properties (skip fx: namespace and xmlns attributes)
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("fx:") || key.startsWith("xmlns")) {
                continue;
            }
            Optional<FXMLProperty<?>> prop = convertAttributeProperty(imports, clazz, key, entry.getValue());
            prop.ifPresent(properties::add);
        }

        // Process child elements
        for (ParsedXMLStructure child : structure.children()) {
            String childName = child.name();

            if (FXMLConstants.FX_DEFINE_ELEMENT.equals(childName)) {
                // fx:define block — children are object definitions, collected globally
                for (ParsedXMLStructure defChild : child.children()) {
                    definitions.add(convertObject(imports, defChild, internalCounter, false, definitions, scripts));
                }
            } else if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(childName)) {
                // fx:script — collected globally
                scripts.add(convertScript(child));
            } else if (FXMLConstants.FX_INCLUDE_ATTRIBUTE.equals(childName)
                    || FXMLConstants.FX_REFERENCE_ATTRIBUTE.equals(childName)
                    || FXMLConstants.FX_COPY_ATTRIBUTE.equals(childName)) {
                // Special fx: value elements used as children
                AbstractFXMLValue fxValue = convertValue(imports, child, internalCounter, definitions, scripts);
                String defaultPropName = resolveDefaultPropertyName(clazz, "children");
                String getterName = Utils.getGetterName(defaultPropName);
                try {
                    Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
                    addValueToMultipleProperty(properties, defaultPropName, getterName, elementType, fxValue);
                } catch (NoSuchMethodException _) {
                    log.debug("No default list property found on %s for %s, skipping".formatted(clazz.getSimpleName(), childName));
                }
            } else if (childName.contains(".")) {
                // Property element: either "ClassName.propertyName" (static) or "propertyName" (instance)
                int dotIndex = childName.lastIndexOf('.');
                String ownerPart = childName.substring(0, dotIndex);
                String propName = childName.substring(dotIndex + 1);

                if (Character.isUpperCase(ownerPart.charAt(0))) {
                    // Static property element
                    Class<?> staticClass = Utils.findType(imports, ownerPart);
                    String staticSetterName = Utils.getSetterName(propName);
                    Optional<FXMLProperty<?>> prop = convertStaticPropertyElement(
                            imports, staticClass, staticSetterName, propName, child, internalCounter, definitions, scripts
                    );
                    prop.ifPresent(properties::add);
                } else {
                    // Instance property element (e.g., <Button.padding>)
                    Optional<FXMLProperty<?>> prop = convertInstancePropertyElement(
                            imports, clazz, propName, child, internalCounter, definitions, scripts
                    );
                    prop.ifPresent(properties::add);
                }
            } else {
                // Regular child object — treat as a child value in the default property
                FXMLObject childObject = convertObject(imports, child, internalCounter, false, definitions, scripts);
                String defaultPropName = resolveDefaultPropertyName(clazz, "children");
                String getterName = Utils.getGetterName(defaultPropName);
                try {
                    Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
                    addValueToMultipleProperty(properties, defaultPropName, getterName, elementType, childObject);
                } catch (NoSuchMethodException _) {
                    log.debug("No default list property found on %s for child %s, skipping".formatted(clazz.getSimpleName(), childName));
                }
            }
        }

        return new FXMLObject(identifier, clazz, factoryMethod, generics, properties);
    }

    /// Converts an XML attribute into an [FXMLProperty], handling both static and instance properties.
    ///
    /// @param imports       the list of imports for class resolution.
    /// @param clazz         the class owning the property.
    /// @param attributeName the attribute name (may contain a dot for static properties).
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the property, or empty if it could not be resolved.
    private Optional<FXMLProperty<?>> convertAttributeProperty(
            List<String> imports,
            Class<?> clazz,
            String attributeName,
            String value
    ) {
        if (attributeName.contains(".")) {
            return convertStaticAttributeProperty(imports, attributeName, value);
        } else {
            return convertInstanceAttributeProperty(imports, clazz, attributeName, value);
        }
    }

    /// Converts a static attribute property (e.g., `GridPane.rowIndex="0"`) into an [FXMLStaticSingleProperty].
    ///
    /// @param imports       the list of imports for class resolution.
    /// @param attributeName the full attribute name including the class prefix.
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the static property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> convertStaticAttributeProperty(
            List<String> imports,
            String attributeName,
            String value
    ) {
        int dotIndex = attributeName.lastIndexOf('.');
        String className = attributeName.substring(0, dotIndex);
        String propName = attributeName.substring(dotIndex + 1);
        Class<?> staticClass;
        try {
            staticClass = Utils.findType(imports, className);
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
        FXMLValue fxmlValue = new FXMLValue(Optional.empty(), Utils.getClassType(paramType), value);
        return Optional.of(new FXMLStaticSingleProperty(propName, staticClass, staticSetterName, paramType, fxmlValue));
    }

    /// Converts an instance attribute property (e.g., `text="Hello"`) into an [FXMLSingleProperty].
    ///
    /// @param imports       the list of imports for class resolution.
    /// @param clazz         the class owning the property.
    /// @param attributeName the attribute name.
    /// @param value         the attribute value string.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> convertInstanceAttributeProperty(
            List<String> imports,
            Class<?> clazz,
            String attributeName,
            String value
    ) {
        AbstractFXMLValue fxmlValue = parseValueString(value);
        String setterName = Utils.getSetterName(attributeName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        if (!setters.isEmpty()) {
            if (setters.size() > 1) {
                log.warn("Multiple setters '%s' found on '%s', skipping".formatted(setterName, clazz.getName()));
                return Optional.empty();
            }
            Method setter = setters.getFirst();
            Type paramType = setter.getGenericParameterTypes()[0];
            return Optional.of(new FXMLSingleProperty(attributeName, Optional.of(setterName), paramType, fxmlValue));
        }
        // Try list getter
        String getterName = Utils.getGetterName(attributeName);
        try {
            Type elementType = Utils.findGetterListAndReturnElementType(clazz, getterName);
            return Optional.of(new FXMLSingleProperty(attributeName, Optional.of(getterName + "().add"), elementType, fxmlValue));
        } catch (NoSuchMethodException _) {
            log.debug("No setter or list getter found for '%s' on '%s', skipping".formatted(attributeName, clazz.getName()));
            return Optional.empty();
        }
    }

    /// Resolves the default property name for a class using the [DefaultProperty] annotation.
    ///
    /// If the class (or any of its superclasses) is annotated with [DefaultProperty], the
    /// annotated property name is returned. Otherwise, the provided fallback name is returned.
    ///
    /// @param clazz    the class to inspect for a [DefaultProperty] annotation.
    /// @param fallback the property name to use if no annotation is found.
    /// @return the default property name.
    private String resolveDefaultPropertyName(Class<?> clazz, String fallback) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            DefaultProperty annotation = current.getAnnotation(DefaultProperty.class);
            if (annotation != null) {
                return annotation.value();
            }
            current = current.getSuperclass();
        }
        return fallback;
    }

    /// Converts a static property child element into an [FXMLProperty].
    ///
    /// @param imports          the list of imports for class resolution.
    /// @param staticClass      the class defining the static property.
    /// @param staticSetterName the name of the static setter method.
    /// @param propName         the property name.
    /// @param child            the child XML structure containing the property values.
    /// @param internalCounter  the counter for generating internal identifiers.
    /// @param definitions      the global list to accumulate all `fx:define` objects into.
    /// @param scripts          the global list to accumulate all `fx:script` elements into.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> convertStaticPropertyElement(
            List<String> imports,
            Class<?> staticClass,
            String staticSetterName,
            String propName,
            ParsedXMLStructure child,
            AtomicInteger internalCounter,
            List<FXMLObject> definitions,
            List<FXMLScript> scripts
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
        List<AbstractFXMLValue> values = convertChildrenToValues(imports, child, internalCounter, definitions, scripts);
        if (values.size() == 1) {
            return Optional.of(new FXMLStaticSingleProperty(propName, staticClass, staticSetterName, paramType, values.getFirst()));
        }
        return Optional.of(new FXMLStaticMultipleProperties(propName, staticClass, staticSetterName, paramType, values));
    }

    /// Converts an instance property child element into an [FXMLProperty].
    ///
    /// @param imports         the list of imports for class resolution.
    /// @param clazz           the class owning the property.
    /// @param propName        the property name.
    /// @param child           the child XML structure containing the property values.
    /// @param internalCounter the counter for generating internal identifiers.
    /// @param definitions     the global list to accumulate all `fx:define` objects into.
    /// @param scripts         the global list to accumulate all `fx:script` elements into.
    /// @return an [Optional] containing the property, or empty if unresolvable.
    private Optional<FXMLProperty<?>> convertInstancePropertyElement(
            List<String> imports,
            Class<?> clazz,
            String propName,
            ParsedXMLStructure child,
            AtomicInteger internalCounter,
            List<FXMLObject> definitions,
            List<FXMLScript> scripts
    ) {
        String setterName = Utils.getSetterName(propName);
        List<Method> setters = Utils.findObjectSetters(clazz, setterName);
        List<AbstractFXMLValue> values = convertChildrenToValues(imports, child, internalCounter, definitions, scripts);

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

        // Try list getter
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
    /// @param imports         the list of imports for class resolution.
    /// @param propertyElement the property element whose children are to be converted.
    /// @param internalCounter the counter for generating internal identifiers.
    /// @param definitions     the global list to accumulate all `fx:define` objects into.
    /// @param scripts         the global list to accumulate all `fx:script` elements into.
    /// @return the list of converted values.
    private List<AbstractFXMLValue> convertChildrenToValues(
            List<String> imports,
            ParsedXMLStructure propertyElement,
            AtomicInteger internalCounter,
            List<FXMLObject> definitions,
            List<FXMLScript> scripts
    ) {
        List<AbstractFXMLValue> values = new ArrayList<>();
        for (ParsedXMLStructure child : propertyElement.children()) {
            values.add(convertValue(imports, child, internalCounter, definitions, scripts));
        }
        return values;
    }

    /// Converts a [ParsedXMLStructure] node into an [AbstractFXMLValue].
    ///
    /// @param imports         the list of imports for class resolution.
    /// @param structure       the XML structure node to convert.
    /// @param internalCounter the counter for generating internal identifiers.
    /// @param definitions     the global list to accumulate all `fx:define` objects into.
    /// @param scripts         the global list to accumulate all `fx:script` elements into.
    /// @return the converted [AbstractFXMLValue].
    private AbstractFXMLValue convertValue(
            List<String> imports,
            ParsedXMLStructure structure,
            AtomicInteger internalCounter,
            List<FXMLObject> definitions,
            List<FXMLScript> scripts
    ) {
        String nodeName = structure.name();
        Map<String, String> attributes = structure.properties();

        if (FXMLConstants.FX_INCLUDE_ATTRIBUTE.equals(nodeName)) {
            String src = attributes.get(FXMLConstants.SOURCE);
            if (src == null) {
                throw new IllegalArgumentException("fx:include must have a 'source' attribute");
            }
            return new FXMLInclude(src);
        }

        if (FXMLConstants.FX_REFERENCE_ATTRIBUTE.equals(nodeName)) {
            String source = attributes.get(FXMLConstants.SOURCE);
            if (source == null) {
                throw new IllegalArgumentException("fx:reference must have a 'source' attribute");
            }
            return new FXMLReference(source);
        }

        if (FXMLConstants.FX_COPY_ATTRIBUTE.equals(nodeName)) {
            String source = attributes.get(FXMLConstants.SOURCE);
            if (source == null) {
                throw new IllegalArgumentException("fx:copy must have a 'source' attribute");
            }
            return new FXMLCopy(source);
        }

        if (FXMLConstants.FX_SCRIPT_ELEMENT.equals(nodeName)) {
            String scriptContent = attributes.get("#text");
            if (scriptContent != null && !scriptContent.isBlank()) {
                return new FXMLInlineScript(scriptContent);
            }
            throw new IllegalArgumentException("fx:script used as value must have inline content");
        }

        Class<?> clazz = Utils.findType(imports, nodeName);

        if (attributes.containsKey(FXMLConstants.FX_CONSTANT_ATTRIBUTE)) {
            String constantName = attributes.get(FXMLConstants.FX_CONSTANT_ATTRIBUTE);
            Type constantType = resolveConstantType(clazz, constantName);
            return new FXMLConstant(clazz, constantName, constantType);
        }

        if (attributes.containsKey(FXMLConstants.FX_VALUE_ATTRIBUTE)) {
            String val = attributes.get(FXMLConstants.FX_VALUE_ATTRIBUTE);
            Optional<FXMLIdentifier> id = resolveOptionalIdentifier(attributes, internalCounter);
            return new FXMLValue(id, clazz, val);
        }

        return convertObject(imports, structure, internalCounter, false, definitions, scripts);
    }

    /// Converts a [ParsedXMLStructure] `fx:script` element into an [FXMLScript].
    ///
    /// @param structure the XML structure representing the script element.
    /// @return the converted [FXMLScript].
    private FXMLScript convertScript(ParsedXMLStructure structure) {
        Map<String, String> attributes = structure.properties();
        if (attributes.containsKey(FXMLConstants.SOURCE)) {
            String source = attributes.get(FXMLConstants.SOURCE);
            String charsetName = attributes.getOrDefault(FXMLConstants.CHARSET, StandardCharsets.UTF_8.name());
            Charset charset = Charset.forName(charsetName);
            return new FXMLFileScript(Path.of(source), charset);
        }
        String scriptContent = attributes.get("#text");
        if (scriptContent != null && !scriptContent.isBlank()) {
            return new FXMLSourceScript(scriptContent);
        }
        return new FXMLSourceScript("");
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
        if (value.startsWith(FXMLConstants.TRANSLATION_PREFIX)) {
            return new FXMLTranslation(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.LOCATION_PREFIX)) {
            return new FXMLResource(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.METHOD_REFERENCE_PREFIX)) {
            return new FXMLMethod(value.substring(1), List.of(), void.class, Map.of());
        }
        if (value.startsWith(FXMLConstants.EXPRESSION_PREFIX)) {
            return new FXMLExpression(value.substring(1));
        }
        if (value.startsWith(FXMLConstants.ESCAPE_PREFIX)) {
            return new FXMLValue(Optional.empty(), String.class, value.substring(1));
        }
        return new FXMLValue(Optional.empty(), String.class, value);
    }

    /// Resolves the [Type] of a constant field on the given class.
    ///
    /// @param clazz        the class defining the constant.
    /// @param constantName the name of the constant field.
    /// @return the [Type] of the constant field.
    /// @throws IllegalArgumentException if the field does not exist or is not static.
    private Type resolveConstantType(Class<?> clazz, String constantName) {
        try {
            Field field = clazz.getField(constantName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Field '%s' on '%s' is not static".formatted(constantName, clazz.getName()));
            }
            return field.getGenericType();
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("No such constant field '%s' on '%s'".formatted(constantName, clazz.getName()), e);
        }
    }

    /// Resolves an optional [FXMLIdentifier] from the given attributes map.
    ///
    /// If the attributes contain an [FXMLConstants#FX_ID_ATTRIBUTE] entry, an
    /// [FXMLExposedIdentifier] is returned. Otherwise, an empty [Optional] is returned.
    ///
    /// @param attributes      the XML attributes map.
    /// @param internalCounter the counter for generating internal identifiers.
    /// @return an [Optional] containing the identifier, or empty if no `fx:id` is present.
    private Optional<FXMLIdentifier> resolveOptionalIdentifier(
            Map<String, String> attributes,
            AtomicInteger internalCounter
    ) {
        if (attributes.containsKey(FXMLConstants.FX_ID_ATTRIBUTE)) {
            return Optional.of(new FXMLExposedIdentifier(attributes.get(FXMLConstants.FX_ID_ATTRIBUTE)));
        }
        return Optional.empty();
    }

    /// Extracts generic type names from XML comments (e.g., `<!-- generic 0: java.util.List<String> -->`).
    ///
    /// @param comments the list of XML comments on the element.
    /// @return the list of extracted generic type strings.
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

    /// Adds a value to an existing [FXMLMultipleProperties] entry, or creates a new one.
    ///
    /// @param properties  the current list of properties to update.
    /// @param propName    the property name.
    /// @param getterName  the getter method name for the list property.
    /// @param elementType the element type of the list.
    /// @param value       the value to add.
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
}
