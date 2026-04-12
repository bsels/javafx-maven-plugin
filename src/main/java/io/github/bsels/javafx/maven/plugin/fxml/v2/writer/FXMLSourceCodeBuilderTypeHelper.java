package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
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
import io.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider;
import javafx.beans.NamedArg;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_CONTROLLER_FIELD;
import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_RESOURCE_BUNDLE;
import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_STRING_TO_FILE_METHOD;
import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_STRING_TO_PATH_METHOD;
import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_STRING_TO_URI_METHOD;
import static io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder.INTERNAL_STRING_TO_URL_METHOD;

/// Helper for handling [FXMLType]s and their conversion to Java source code.
/// Provides methods for creating type mappings, encoding literals, and generating class names.
final class FXMLSourceCodeBuilderTypeHelper {
    /// The [FXMLClassType] for [Object].
    private static final FXMLClassType OBJECT_TYPE = new FXMLClassType(Object.class);
    /// The [FXMLClassType] for [String].
    private static final FXMLClassType STRING_TYPE = new FXMLClassType(String.class);

    /// Cache for storing and reusing [FXMLConstructor]s associated with a [Class].
    private final Map<Class<?>, List<FXMLConstructor>> constructorCache;
    /// Cache for storing and reusing [FXMLConstructor]s associated with an [FXMLFactoryMethod].
    private final Map<FXMLFactoryMethod, List<FXMLConstructor>> factoryMethodCache;

    /// Helper for managing and resolving recursive property bindings.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Initializes a new [FXMLSourceCodeBuilderTypeHelper] instance.
    FXMLSourceCodeBuilderTypeHelper() {
        this.propertyRecursionHelper = new FXMLPropertyRecursionHelper();
        this.constructorCache = new HashMap<>();
        this.factoryMethodCache = new HashMap<>();
    }

    /// Returns the default value of an [FXMLType] as a string representation.
    ///
    /// @param type The [FXMLType] whose default value is to be determined.
    /// @return A string representation of the default value corresponding to the [FXMLType].
    /// @throws NullPointerException If `type` is null.
    public String defaultTypeValue(FXMLType type) throws NullPointerException {
        Objects.requireNonNull(type, "`type` must not be null");
        Class<?> rawClass = FXMLUtils.findRawType(type);
        if (boolean.class.equals(rawClass)) {
            return "false";
        }
        if (char.class.equals(rawClass)) {
            return "'\0'";
        }
        if (byte.class.equals(rawClass)) {
            return "(byte) 0";
        }
        if (short.class.equals(rawClass)) {
            return "(short) 0";
        }
        if (int.class.equals(rawClass)) {
            return "0";
        }
        if (long.class.equals(rawClass)) {
            return "0L";
        }
        if (float.class.equals(rawClass)) {
            return "0f";
        }
        if (double.class.equals(rawClass)) {
            return "0.0";
        }
        return "null";
    }

    /// Creates a mapping from FXML identifiers to their corresponding [FXMLType]s for the specified document.
    ///
    /// @param document The [FXMLDocument] to process.
    /// @return A map associating FXML identifiers with their resolved types.
    /// @throws NullPointerException If `document` is null.
    public Map<String, FXMLType> createIdentifierToTypeMap(FXMLDocument document) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        Map<String, Wrapper> identifierTypeMap = Stream.concat(
                        Stream.of(document.root()),
                        document.definitions()
                                .stream()
                )
                .flatMap(this::createIdentifierToTypeMapEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return identifierTypeMap.entrySet()
                .stream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> findTypeRecursion(identifierTypeMap, entry.getValue())
                                ),
                                Map::copyOf
                        )
                );
    }

    /// Returns the appropriate type for a map entry.
    /// If the type is [Object], it defaults to [String].
    ///
    /// @param type The [FXMLClassType] to convert.
    /// @return The resolved [FXMLClassType].
    public FXMLClassType getTypeForMapEntry(FXMLClassType type) {
        return OBJECT_TYPE.equals(type) ? STRING_TYPE : type;
    }

    /// Encodes a literal value for use in Java source code.
    ///
    /// @param context The [SourceCodeGeneratorContext].
    /// @param value   The literal value as a string.
    /// @param type    The [FXMLType] of the literal.
    /// @return The Java source code representation of the literal.
    /// @throws NullPointerException     If any input is null.
    /// @throws IllegalArgumentException If the literal is invalid for the given type.
    public String encodeLiteral(SourceCodeGeneratorContext context, String value, FXMLType type)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(context, "`context` must not be null");
        Objects.requireNonNull(value, "`value` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        if (type instanceof FXMLClassType(Class<?> clazz)) {
            if (clazz.isAssignableFrom(String.class)) {
                return encodeString(value);
            }
            if (clazz.isEnum()) {
                return "%s.%s".formatted(typeToSourceCode(context, type), value);
            }
            if (char.class.equals(clazz) || Character.class.equals(clazz)) {
                String valueString = value.strip();
                if (valueString.length() == 3) {
                    return "'%s'".formatted(valueString.charAt(1));
                }
                throw new IllegalArgumentException("Character literals must be exactly 1 character long");
            }
            if (double.class.equals(clazz) || Double.class.equals(clazz)) {
                if ("-Infinity".equalsIgnoreCase(value)) {
                    return "Double.NEGATIVE_INFINITY";
                }
                if ("Infinity".equalsIgnoreCase(value)) {
                    return "Double.POSITIVE_INFINITY";
                }
                if ("NaN".equalsIgnoreCase(value)) {
                    return "Double.NaN";
                }
                return value;
            }
            if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                if ("-Infinity".equalsIgnoreCase(value)) {
                    return "Float.NEGATIVE_INFINITY";
                }
                if ("Infinity".equalsIgnoreCase(value)) {
                    return "Float.POSITIVE_INFINITY";
                }
                if ("NaN".equalsIgnoreCase(value)) {
                    return "Float.NaN";
                }
                return "%sf".formatted(value);
            }
            if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
                if ("true".equalsIgnoreCase(value)) {
                    return "true";
                }
                return "false";
            }
            if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
                return "(byte) %s".formatted(value);
            }
            if (short.class.equals(clazz) || Short.class.equals(clazz)) {
                return "(short) %s".formatted(value);
            }
            if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
                return value;
            }
            if (long.class.equals(clazz) || Long.class.equals(clazz)) {
                return "%sL".formatted(value);
            }
        }
        return "%s.valueOf(%s)".formatted(typeToSourceCode(context, type), encodeString(value));
    }

    /// Encodes a string for use in Java source code.
    ///
    /// @param string The string to encode.
    /// @return The Java source code representation of the string.
    public String encodeString(String string) {
        return ObjectMapperProvider.encodeObject(string);
    }

    /// Encodes an FXML value for use in Java source code.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] to encode.
    /// @param type    The expected [FXMLType] of the value.
    /// @return The Java source code representation of the value.
    /// @throws UnsupportedOperationException If the value type is not yet supported.
    /// @throws IllegalArgumentException      If the value cannot be converted to the required type.
    public String encodeFXMLValue(SourceCodeGeneratorContext context, AbstractFXMLValue value, FXMLType type) {
        return encodeFXMLValue(context, "", value, type);
    }

    /// Encodes an FXML value for use in Java source code.
    ///
    /// @param context          The current [SourceCodeGeneratorContext].
    /// @param identifierPrefix The prefix to use for generated identifiers.
    /// @param value            The [AbstractFXMLValue] to encode.
    /// @param type             The expected [FXMLType] of the value.
    /// @return The Java source code representation of the value.
    /// @throws UnsupportedOperationException If the value type is not yet supported.
    /// @throws IllegalArgumentException      If the value cannot be converted to the required type.
    public String encodeFXMLValue(
            SourceCodeGeneratorContext context,
            String identifierPrefix,
            AbstractFXMLValue value,
            FXMLType type
    ) {
        return switch (value) {
            case AbstractFXMLObject object -> createIdentifier(identifierPrefix, object.identifier());
            case FXMLConstant(FXMLClassType clazz, String identifier, _) ->
                    "%s.%s".formatted(typeToSourceCode(context, clazz), identifier);
            case FXMLCopy(FXMLIdentifier identifier, _) -> createIdentifier(identifierPrefix, identifier);
            case FXMLExpression _ -> throw new UnsupportedOperationException("Expression values are not supported yet");
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, _) -> createIdentifier(identifierPrefix, identifier);
            case FXMLInlineScript _ ->
                    throw new UnsupportedOperationException("Inline script values are not supported yet");
            case FXMLLiteral(String literal) -> encodeLiteral(context, literal, type);
            case FXMLMethod(String name, _, _) -> "this::%s".formatted(name);
            case FXMLReference(String name) -> name;
            case FXMLResource(String resource) -> {
                resource = encodeString(resource);
                if (type instanceof FXMLClassType(Class<?> clazz)) {
                    if (clazz.isAssignableFrom(String.class)) {
                        yield resource;
                    }
                    if (URI.class.equals(clazz)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_URI_METHOD, resource);
                    }
                    if (URL.class.equals(clazz)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_URL_METHOD, resource);
                    }
                    if (clazz.isAssignableFrom(Path.class)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_PATH_METHOD, resource);
                    }
                    if (clazz.isAssignableFrom(File.class)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_FILE_METHOD, resource);
                    }
                }
                throw new IllegalArgumentException(
                        "Resources can only be used with `java.lang.String`, `java.nio.file.Path`, `java.net.URI`, or `java.net.URL`");
            }
            case FXMLTranslation(String translationKey) -> {
                context.addFeature(Feature.RESOURCE_BUNDLE);
                yield "%s.getString(%s)".formatted(
                        INTERNAL_RESOURCE_BUNDLE,
                        encodeString(translationKey)
                );
            }
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType t, String v) ->
                    identifier.map(i -> createIdentifier(identifierPrefix, i))
                            .orElseGet(() -> encodeLiteral(context, v, t));
        };
    }

    /// Creates a unique identifier string based on the provided prefix and object.
    ///
    /// @param identifierPrefix The prefix to be used in the identifier.
    /// @param object           The FXMLIdentifier object used to generate the identifier string.
    /// @return a string representation of the identifier, either from the object itself if it is an instance of FXMLInternalIdentifier, or composed using the prefix and the object.
    private String createIdentifier(String identifierPrefix, FXMLIdentifier object) {
        if (object instanceof FXMLInternalIdentifier internalIdentifier) {
            return internalIdentifier.toString();
        }
        return "%s%s".formatted(identifierPrefix, object);
    }

    /// Converts an [FXMLType] to its Java source code representation.
    ///
    /// @param context The [SourceCodeGeneratorContext].
    /// @param type    The [FXMLType] to convert.
    /// @return The Java source code representation of the type.
    /// @throws NullPointerException if `context` or `type` is null
    public String typeToSourceCode(SourceCodeGeneratorContext context, FXMLType type) throws NullPointerException {
        Objects.requireNonNull(context, "`context` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> createBaseTypeSourceCode(context, clazz.getCanonicalName());
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, clazz.getCanonicalName(), generics);
            case FXMLUncompiledClassType(String className) -> createBaseTypeSourceCode(context, className);
            case FXMLUncompiledGenericType(String className, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, className, generics);
            case FXMLWildcardType _ -> "?";
        };
    }

    /// Finds the best matching constructor for the given class and its property names.
    ///
    /// @param clazz      The [Class] to inspect. Must not be null.
    /// @param properties The list of [FXMLConstructorProperty] to match. Must not be null.
    /// @return The [FXMLConstructor] with the lowest number of properties that satisfy the criteria.
    /// @throws NullPointerException     If any input is null.
    /// @throws IllegalArgumentException If no matching constructor is found.
    public FXMLConstructor findMinimalConstructor(Class<?> clazz, List<FXMLConstructorProperty> properties)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(clazz, "`clazz` must not be null");
        Objects.requireNonNull(properties, "`properties` must not be null");
        List<String> names = properties.stream()
                .map(FXMLConstructorProperty::name)
                .toList();
        List<FXMLConstructor> constructors = getNamedParameterConstructors(clazz);

        return constructors.stream()
                .filter(constructor -> filterOnConstructorPropertyNames(constructor, names))
                .min(Comparator.comparing(FXMLConstructor::properties, Comparator.comparingInt(List::size)))
                .orElseThrow(() -> new IllegalArgumentException("No matching constructor found for properties: %s".formatted(
                        properties)));
    }

    /// Finds the best matching factory method for the given [FXMLFactoryMethod] and property names.
    ///
    /// @param factoryMethod The [FXMLFactoryMethod] to inspect. Must not be null.
    /// @param properties    The list of [FXMLConstructorProperty] to match. Must not be null.
    /// @return The [FXMLConstructor] with the fewest properties satisfying the criteria.
    /// @throws NullPointerException     If any input is null.
    /// @throws IllegalArgumentException If no matching factory method is found.
    public FXMLConstructor findFactoryMethodConstructor(
            FXMLFactoryMethod factoryMethod,
            List<FXMLConstructorProperty> properties
    ) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(factoryMethod, "`factoryMethod` must not be null");
        Objects.requireNonNull(properties, "`properties` must not be null");
        List<String> names = properties.stream()
                .map(FXMLConstructorProperty::name)
                .toList();
        return getNamedParameterConstructors(factoryMethod)
                .stream()
                .filter(constructor -> filterOnConstructorPropertyNames(constructor, names))
                .min(Comparator.comparing(FXMLConstructor::properties, Comparator.comparingInt(List::size)))
                .orElseThrow(() -> new IllegalArgumentException("No matching factory method found for properties: %s".formatted(
                        properties)));
    }

    /// Renders the source code for a specific [FXMLMethod].
    ///
    /// @param context    The [SourceCodeGeneratorContext] used for code generation.
    /// @param controller The [FXMLController] associated with the FXML document. Can be null.
    /// @param interfaces The [FXMLInterface]s declared in the FXML document. Can be empty.
    /// @param method     The [FXMLMethod] to be rendered.
    /// @return The generated source code for the method as a [Stream] of [String].
    /// @throws NullPointerException if `controller`, or `method` is null.
    public Stream<String> renderMethod(
            SourceCodeGeneratorContext context,
            FXMLController controller,
            List<FXMLInterface> interfaces,
            FXMLMethod method
    ) throws NullPointerException {
        Objects.requireNonNull(context, "`context` must not be null");
        Objects.requireNonNull(interfaces, "`interfaces` must not be null");
        Objects.requireNonNull(method, "`method` must not be null");
        String name = method.name();
        Optional<FXMLControllerMethod> controllerMethod = findMethodInController(controller, method);
        StringBuilder sourceCode = new StringBuilder();
        if (controllerMethod.isEmpty()) {
            // TODO: Check for interface methods
            context.addFeature(Feature.ABSTRACT_CLASS);
        }
        sourceCode.append("protected ")
                .append(controllerMethod.map(_ -> "").orElse("abstract "))
                .append(typeToSourceCode(context, method.returnType()))
                .append(' ')
                .append(name)
                .append('(');
        List<FXMLType> parameters = method.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sourceCode.append(", ");
            }
            sourceCode.append(typeToSourceCode(context, parameters.get(i)))
                    .append(' ')
                    .append("param")
                    .append(i);
        }
        sourceCode.append(')');
        if (controllerMethod.isPresent()) {
            FXMLControllerMethod fxmlMethod = controllerMethod.get();
            sourceCode.append(" {\n");
            switch (fxmlMethod.visibility()) {
                case PUBLIC -> createDirectCallMethodBody(sourceCode, method);
                case PROTECTED, PACKAGE_PRIVATE -> {
                    FXMLClassType type = controller.controllerClass();
                    if (context.packageName().map(type.clazz().getPackageName()::equals).orElse(false)) {
                        createDirectCallMethodBody(sourceCode, method);
                    } else {
                        createReflectionCallMethodBody(context, sourceCode, type, fxmlMethod);
                    }
                }
                case PRIVATE ->
                        createReflectionCallMethodBody(context, sourceCode, controller.controllerClass(), fxmlMethod);
            }
            sourceCode.append("}");
        } else {
            sourceCode.append(';');
        }
        return Stream.of(sourceCode.append("\n\n").toString());
    }

    /// Renders the source code for mapping an FXML identifier to its corresponding controller field.
    ///
    /// This method determines the appropriate mapping strategy based on the field'\''s visibility:
    /// - [Visibility#PUBLIC]: Direct assignment.
    /// - [Visibility#PROTECTED] or [Visibility#PACKAGE_PRIVATE]: Direct assignment if within the same package,
    ///   otherwise reflection.
    /// - [Visibility#PRIVATE]: Reflective access.
    ///
    /// @param context    The [SourceCodeGeneratorContext] used for code generation.
    /// @param controller The [FXMLController] whose fields are being mapped.
    /// @param field      The specific [FXMLControllerField] to be mapped.
    /// @return A string containing the generated Java source code for the field mapping.
    /// @throws NullPointerException If `context`, `controller`, or `field` is null.
    public String renderControllerFieldMapping(
            SourceCodeGeneratorContext context, FXMLController controller, FXMLControllerField field
    ) throws NullPointerException {
        Objects.requireNonNull(context, "`context` must not be null");
        Objects.requireNonNull(controller, "`controller` must not be null");
        Objects.requireNonNull(field, "`field` must not be null");
        return switch (field.visibility()) {
            case PUBLIC -> renderDirectControllerFieldMapping(field);
            case PROTECTED, PACKAGE_PRIVATE -> {
                FXMLClassType type = controller.controllerClass();
                if (context.packageName().map(type.clazz().getPackageName()::equals).orElse(false)) {
                    yield renderDirectControllerFieldMapping(field);
                } else {
                    yield renderReflectionControllerFieldMapping(context, type, field);
                }
            }
            case PRIVATE -> renderReflectionControllerFieldMapping(context, controller.controllerClass(), field);
        };
    }

    /// Renders a direct assignment from an FXML identifier to a public or accessible controller field.
    ///
    /// @param field The [FXMLControllerField] for which a direct assignment is generated.
    /// @return The generated Java source code line for direct field assignment.
    private String renderDirectControllerFieldMapping(FXMLControllerField field) {
        return "%1$s.%2$s = %2$s;".formatted(INTERNAL_CONTROLLER_FIELD, field.name());
    }

    /// Renders the source code to assign an FXML identifier to a controller field using reflection.
    ///
    /// This is used for private fields or fields that are otherwise inaccessible from the generated class.
    ///
    /// @param context   The [SourceCodeGeneratorContext] used for code generation.
    /// @param classType The [FXMLClassType] of the controller.
    /// @param field     The [FXMLControllerField] to be mapped via reflection.
    /// @return A string containing the Java source code block for reflective field assignment.
    private String renderReflectionControllerFieldMapping(
            SourceCodeGeneratorContext context, FXMLClassType classType, FXMLControllerField field
    ) {
        return """
                try {
                    java.lang.reflect.Field $reflectionField$ = %1$s.class.getDeclaredField("%2$s");
                    $reflectionField$.setAccessible(true);
                    $reflectionField$.set(%3$s, %2$s);
                } catch (java.lang.NoSuchFieldException | java.lang.IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                """.formatted(typeToSourceCode(context, classType), field.name(), INTERNAL_CONTROLLER_FIELD);
    }

    /// Generates the body of a method that uses reflection to call a controller method.
    /// This is used when the controller method is not public or otherwise requires reflective access.
    ///
    /// @param context    The [SourceCodeGeneratorContext] used for code generation.
    /// @param sourceCode The [StringBuilder] to which the generated code is appended.
    /// @param clazz      The [FXMLClassType] of the controller.
    /// @param method     The [FXMLControllerMethod] to be called via reflection.
    private void createReflectionCallMethodBody(
            SourceCodeGeneratorContext context,
            StringBuilder sourceCode,
            FXMLClassType clazz,
            FXMLControllerMethod method
    ) {
        List<FXMLType> parameters = method.parameterTypes();
        sourceCode.append("    try {\n")
                .append("        java.lang.reflect.Method method = ")
                .append(typeToSourceCode(context, clazz))
                .append(".class.getDeclaredMethod(")
                .append(encodeString(method.name()));
        for (FXMLType parameter : parameters) {
            sourceCode.append(", ")
                    .append(typeToSourceCode(context, FXMLType.of(FXMLUtils.findRawType(parameter))))
                    .append(".class");
        }
        sourceCode.append(");\n")
                .append("        method.setAccessible(true);\n        ");
        if (!void.class.equals(FXMLUtils.findRawType(method.returnType()))) {
            sourceCode.append("return (")
                    .append(typeToSourceCode(context, method.returnType()))
                    .append(") ");
        }
        sourceCode.append("method.invoke(")
                .append(INTERNAL_CONTROLLER_FIELD);
        for (int i = 0; i < parameters.size(); i++) {
            sourceCode.append(", param")
                    .append(i);
        }
        sourceCode.append(");\n")
                .append("    } catch (java.lang.NoSuchMethodException | java.lang.IllegalAccessException | ")
                .append("java.lang.reflect.InvocationTargetException e) {\n")
                .append("        throw new RuntimeException(e);\n")
                .append("    }\n");
    }

    /// Generates the body of a method that directly calls a public controller method.
    ///
    /// @param sourceCode The [StringBuilder] to which the generated code is appended.
    /// @param fxmlMethod The [FXMLMethod] representing the call to be made.
    private void createDirectCallMethodBody(StringBuilder sourceCode, FXMLMethod fxmlMethod) {
        if (!void.class.equals(FXMLUtils.findRawType(fxmlMethod.returnType()))) {
            sourceCode.append("    return ");
        }
        sourceCode.append(INTERNAL_CONTROLLER_FIELD)
                .append('.')
                .append(fxmlMethod.name())
                .append('(');
        int parameterCount = fxmlMethod.parameters().size();
        for (int i = 0; i < parameterCount; i++) {
            if (i > 0) {
                sourceCode.append(", ");
            }
            sourceCode.append("param")
                    .append(i);
        }
        sourceCode.append(");\n");
    }

    /// Attempts to find a matching method in the given [FXMLController] for a specified [FXMLMethod].
    ///
    /// @param controller The [FXMLController] to search in.
    /// @param method     The [FXMLMethod] containing the target method's name and signature.
    /// @return An [Optional] containing the [FXMLControllerMethod] if a match is found, otherwise an empty [Optional].
    private Optional<FXMLControllerMethod> findMethodInController(FXMLController controller, FXMLMethod method) {
        final String name = method.name();
        final Optional<FXMLControllerMethod> controllerMethod;
        if (controller != null) {
            Class<?> returnType = FXMLUtils.findRawType(method.returnType());
            List<Class<?>> parameterTypes = method.parameters()
                    .stream().map(FXMLUtils::findRawType)
                    .collect(Collectors.toList());
            controllerMethod = controller.methods()
                    .stream()
                    .filter(m -> name.equals(m.name()))
                    .filter(m -> m.parameterTypes().size() == parameterTypes.size())
                    .filter(m -> FXMLUtils.findRawType(m.returnType()).isAssignableFrom(returnType))
                    .filter(m -> checkParameterTypes(m, parameterTypes))
                    .findFirst();
        } else {
            controllerMethod = Optional.empty();
        }
        return controllerMethod;
    }

    /// Checks if the parameters of an [FXMLControllerMethod] are compatible with the provided list of [Class] types.
    ///
    /// @param m              The [FXMLControllerMethod] to check.
    /// @param parameterTypes The list of parameter types to compare against.
    /// @return `true` if all parameter types are compatible, `false` otherwise.
    private boolean checkParameterTypes(FXMLControllerMethod m, List<Class<?>> parameterTypes) {
        for (int i = 0; i < parameterTypes.size(); i++) {
            List<FXMLType> fxmlTypes = m.parameterTypes();
            if (!parameterTypes.get(i).isAssignableFrom(FXMLUtils.findRawType(fxmlTypes.get(i)))) {
                return false;
            }
        }
        return true;
    }

    /// Retrieves the list of constructors with [@NamedArg] annotations for the given factory method from the cache
    /// or computes them if not already cached.
    ///
    /// @param factoryMethod The factory method for which to retrieve constructors.
    /// @return A list of [FXMLConstructor] for the given factory method.
    private List<FXMLConstructor> getNamedParameterConstructors(FXMLFactoryMethod factoryMethod) {
        return factoryMethodCache.computeIfAbsent(factoryMethod, this::constructNamedParameterConstructors);
    }

    /// Computes the list of constructors (static factory methods) that use [@NamedArg] annotations
    /// for the provided [FXMLFactoryMethod].
    ///
    /// @param factoryMethod The [FXMLFactoryMethod] for which to find static factory methods.
    /// @return A list of [FXMLConstructor] objects corresponding to matching static methods.
    private List<FXMLConstructor> constructNamedParameterConstructors(FXMLFactoryMethod factoryMethod) {
        return Stream.of(
                        factoryMethod.clazz()
                                .clazz()
                                .getMethods()
                )
                .filter(m -> m.getName().equals(factoryMethod.method()))
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> Stream.of(m.getParameters()).allMatch(p -> p.isAnnotationPresent(NamedArg.class)))
                .map(this::constructFxmlConstructor)
                .toList();
    }

    /// Filters a constructor based on whether it contains all the required property names.
    ///
    /// @param constructor The [FXMLConstructor] to evaluate.
    /// @param names       The list of property names that must be present in the constructor.
    /// @return `true` if all property names are present, `false` otherwise.
    private boolean filterOnConstructorPropertyNames(FXMLConstructor constructor, List<String> names) {
        Set<String> constructorNames = constructor.properties()
                .stream()
                .map(ConstructorProperty::name)
                .collect(Collectors.toSet());
        return constructorNames.containsAll(names);
    }

    /// Retrieves the list of constructors that use [@NamedArg] for a class, using a cache for efficiency.
    ///
    /// @param clazz The class to inspect for constructors.
    /// @return A list of [FXMLConstructor] for the specified class.
    private List<FXMLConstructor> getNamedParameterConstructors(Class<?> clazz) {
        return constructorCache.computeIfAbsent(clazz, this::computeNamedParameterConstructors);
    }

    /// Computes and collects all constructors of a class whose parameters are all annotated with [@NamedArg].
    ///
    /// @param clazz The class whose constructors are to be computed.
    /// @return A list of matching [FXMLConstructor] objects.
    private List<FXMLConstructor> computeNamedParameterConstructors(Class<?> clazz) {
        return Stream.of(clazz.getConstructors())
                .filter(
                        constructor -> Stream.of(constructor.getParameters())
                                .allMatch(parameter -> parameter.isAnnotationPresent(NamedArg.class))
                )
                .map(this::constructFxmlConstructor)
                .toList();
    }

    /// Constructs an [FXMLConstructor] instance from a Java [Constructor].
    ///
    /// @param constructor The Java reflection [Constructor] object to be converted.
    /// @return An [FXMLConstructor] instance.
    private FXMLConstructor constructFxmlConstructor(Constructor<?> constructor) {
        List<ConstructorProperty> properties = Stream.of(constructor.getParameters())
                .map(this::constructConstructorProperty)
                .toList();
        return new FXMLConstructor(properties);
    }

    /// Constructs an [FXMLConstructor] instance from a Java [Method] (representing a factory method).
    ///
    /// @param method The Java reflection [Method] object to be converted.
    /// @return An [FXMLConstructor] instance.
    private FXMLConstructor constructFxmlConstructor(Method method) {
        List<ConstructorProperty> properties = Stream.of(method.getParameters())
                .map(this::constructConstructorProperty)
                .toList();
        return new FXMLConstructor(properties);
    }

    /// Creates a [ConstructorProperty] from a Java [Parameter] annotated with [@NamedArg].
    ///
    /// @param parameter The Java reflection [Parameter] to convert.
    /// @return A [ConstructorProperty] object.
    private ConstructorProperty constructConstructorProperty(Parameter parameter) {
        NamedArg namedArg = parameter.getAnnotation(NamedArg.class);
        return new ConstructorProperty(
                namedArg.value(),
                FXMLType.of(parameter.getType()),
                Optional.ofNullable(namedArg.defaultValue())
                        .filter(Predicate.not(String::isEmpty))
                        .map(FXMLLiteral::new)
        );
    }

    /// Creates the Java source code for a base type.
    ///
    /// @param context   The [SourceCodeGeneratorContext].
    /// @param className The fully qualified class name.
    /// @return The simple or fully qualified class name, depending on imports.
    private String createBaseTypeSourceCode(SourceCodeGeneratorContext context, String className) {
        return context.imports()
                .inlineClassNames()
                .getOrDefault(className, className);
    }

    /// Creates the Java source code for a generic type.
    ///
    /// @param context   The [SourceCodeGeneratorContext].
    /// @param className The fully qualified class name.
    /// @param generics  The list of [FXMLType] generic arguments.
    /// @return The Java source code for the generic type.
    private String createGenericTypeSourceCode(
            SourceCodeGeneratorContext context,
            String className,
            List<FXMLType> generics
    ) {
        Imports imports = context.imports();
        Map<String, String> inlineClassNames = imports.inlineClassNames();
        StringBuilder s = new StringBuilder()
                .append(inlineClassNames.get(className))
                .append("<");
        for (FXMLType genericType : generics) {
            s.append(typeToSourceCode(context, genericType))
                    .append(", ");
        }
        return s.deleteCharAt(s.length() - 2)
                .deleteCharAt(s.length() - 1)
                .append(">")
                .toString();
    }

    /// Recursively finds the [FXMLType] for a given [Wrapper] value in the type map.
    ///
    /// @param typeMap The map of identifiers to [Wrapper] values.
    /// @param value   The [Wrapper] value to resolve.
    /// @return The resolved [FXMLType].
    /// @throws NoSuchElementException If the reference cannot be resolved.
    private FXMLType findTypeRecursion(Map<String, Wrapper> typeMap, Wrapper value) {
        return switch (value) {
            case Wrapper.FXMLTypeWrapper(FXMLType type) -> type;
            case Wrapper.ReferenceWrapper(String reference) -> Optional.ofNullable(typeMap.get(reference))
                    .map(nestedValue -> findTypeRecursion(typeMap, nestedValue))
                    .orElseThrow();
        };
    }

    /// Creates a stream of mapping entries from FXML identifiers to their [Wrapper] values for a given FXML value.
    ///
    /// @param value The [AbstractFXMLValue] to process.
    /// @return A stream of identifier-to-wrapper entries.
    private Stream<Map.Entry<String, Wrapper>> createIdentifierToTypeMapEntry(AbstractFXMLValue value) {
        return switch (value) {
            case FXMLCollection(
                    FXMLIdentifier identifier,
                    FXMLType type,
                    _,
                    List<AbstractFXMLValue> values
            ) -> Stream.concat(
                    values.stream()
                            .flatMap(this::createIdentifierToTypeMapEntry),
                    Stream.of(Map.entry(identifier.toString(), new Wrapper.FXMLTypeWrapper(type)))
            );
            case FXMLCopy(FXMLIdentifier identifier, FXMLExposedIdentifier(String name)) ->
                    Stream.of(Map.entry(identifier.toString(), new Wrapper.ReferenceWrapper(name)));
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, FXMLLazyLoadedDocument document) ->
                    Stream.of(Map.entry(
                            identifier.toString(),
                            new Wrapper.FXMLTypeWrapper(new FXMLUncompiledClassType(document.get().className()))
                    ));
            case FXMLMap(
                    FXMLIdentifier identifier,
                    FXMLType type,
                    _,
                    _,
                    _,
                    Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> Stream.concat(
                    entries.values()
                            .stream()
                            .flatMap(this::createIdentifierToTypeMapEntry),
                    Stream.of(Map.entry(identifier.toString(), new Wrapper.FXMLTypeWrapper(type)))
            );
            case FXMLObject(
                    FXMLIdentifier identifier,
                    FXMLType type,
                    _,
                    List<FXMLProperty> properties
            ) -> Stream.concat(
                    propertyRecursionHelper.walk(properties, (v, _) -> createIdentifierToTypeMapEntry(v), null),
                    Stream.of(Map.entry(identifier.toString(), new Wrapper.FXMLTypeWrapper(type)))
            );
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.empty();
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType type, _) -> identifier.stream()
                    .map(FXMLIdentifier::toString)
                    .map(id -> Map.entry(id, new Wrapper.FXMLTypeWrapper(type)));
        };
    }

    /// A sealed interface used as a container for either an [FXMLType] or a reference to another identifier.
    private sealed interface Wrapper {
        /// A wrapper for an [FXMLType].
        ///
        /// @param type The [FXMLType] being wrapped.
        record FXMLTypeWrapper(FXMLType type) implements Wrapper {

            /// Constructs an `FXMLTypeWrapper` and ensures the type is not null.
            ///
            /// @param type The [FXMLType] being wrapped.
            public FXMLTypeWrapper {
                Objects.requireNonNull(type, "`type` must not be null");
            }
        }

        /// A wrapper for a reference to another FXML identifier.
        ///
        /// @param reference The name of the FXML identifier being referenced.
        record ReferenceWrapper(String reference) implements Wrapper {

            /// Constructs a `ReferenceWrapper` and ensures the reference name is not null.
            ///
            /// @param reference The name of the FXML identifier being referenced.
            public ReferenceWrapper {
                Objects.requireNonNull(reference, "`reference` must not be null");
            }
        }
    }
}
