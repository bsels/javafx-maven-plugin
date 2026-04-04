package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A helper class for handling [FXMLType]s and their conversion to Java source code.
/// It provides methods for creating type mappings, encoding literals, and generating class names.
final class FXMLSourceCodeBuilderTypeHelper {
    /// The [FXMLClassType] for [Object].
    private static final FXMLClassType OBJECT_TYPE = new FXMLClassType(Object.class);
    /// The [FXMLClassType] for [String].
    private static final FXMLClassType STRING_TYPE = new FXMLClassType(String.class);

    /// Helper instance used for managing and resolving recursive property bindings when working with FXML.
    /// This ensures proper handling of nested property structures and prevents infinite recursion during the binding
    /// and lookup process.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Constructs a new `FXMLSourceCodeBuilderTypeHelper` instance.
    FXMLSourceCodeBuilderTypeHelper() {
        this.propertyRecursionHelper = new FXMLPropertyRecursionHelper();
    }

    /// Creates a mapping from FXML identifiers to their corresponding [FXMLType]s for the given document.
    ///
    /// @param document The [FXMLDocument] to process.
    /// @return A map associating FXML identifiers with their resolved types.
    public Map<String, FXMLType> createIdentifierToTypeMap(FXMLDocument document) {
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
    /// @throws IllegalArgumentException If the literal is invalid for the given type.
    public String encodeLiteral(SourceCodeGeneratorContext context, String value, FXMLType type) {
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
    /// @param translationKey The string to encode.
    /// @return The Java source code representation of the string.
    public String encodeString(String translationKey) {
        return ObjectMapperProvider.encodeObject(translationKey);
    }

    /// Converts an [FXMLType] to its Java source code representation.
    ///
    /// @param context The [SourceCodeGeneratorContext].
    /// @param type    The [FXMLType] to convert.
    /// @return The Java source code representation of the type.
    public String typeToSourceCode(SourceCodeGeneratorContext context, FXMLType type) {
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> createBaseTypeSourceCode(context, clazz.getName());
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, clazz.getName(), generics);
            case FXMLUncompiledClassType(String className) -> createBaseTypeSourceCode(context, className);
            case FXMLUncompiledGenericType(String className, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, className, generics);
            case FXMLWildcardType _ -> "?";
        };
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
    /// @throws java.util.NoSuchElementException If the reference cannot be resolved.
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
            case FXMLCopy(FXMLIdentifier identifier, String name) ->
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
                    properties.stream()
                            .flatMap(property -> propertyRecursionHelper.walk(
                                    property,
                                    (v, _) -> createIdentifierToTypeMapEntry(v),
                                    null
                            )),
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
