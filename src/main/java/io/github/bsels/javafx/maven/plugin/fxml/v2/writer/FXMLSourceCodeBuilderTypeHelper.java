package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
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

final class FXMLSourceCodeBuilderTypeHelper {
    private static final FXMLClassType OBJECT_TYPE = new FXMLClassType(Object.class);
    private static final FXMLClassType STRING_TYPE = new FXMLClassType(String.class);

    FXMLSourceCodeBuilderTypeHelper() {
    }

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

    public FXMLClassType getTypeForMapEntry(FXMLClassType type) {
        return OBJECT_TYPE.equals(type) ? STRING_TYPE : type;
    }

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

    public String encodeString(String translationKey) {
        return ObjectMapperProvider.encodeObject(translationKey);
    }

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

    private String createBaseTypeSourceCode(SourceCodeGeneratorContext context, String className) {
        return context.imports()
                .inlineClassNames()
                .getOrDefault(className, className);
    }

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

    private FXMLType findTypeRecursion(Map<String, Wrapper> typeMap, Wrapper value) {
        return switch (value) {
            case Wrapper.FXMLTypeWrapper(FXMLType type) -> type;
            case Wrapper.ReferenceWrapper(String reference) -> Optional.ofNullable(typeMap.get(reference))
                    .map(nestedValue -> findTypeRecursion(typeMap, nestedValue))
                    .orElseThrow();
        };
    }

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
                            new Wrapper.FXMLTypeWrapper(document.get().root().type())
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
                            .flatMap(this::createIdentifierToTypeMapEntry),
                    Stream.of(Map.entry(identifier.toString(), new Wrapper.FXMLTypeWrapper(type)))
            );
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.empty();
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType type, _) -> identifier.stream()
                    .map(FXMLIdentifier::toString)
                    .map(id -> Map.entry(id, new Wrapper.FXMLTypeWrapper(type)));
        };
    }

    private Stream<Map.Entry<String, Wrapper>> createIdentifierToTypeMapEntry(FXMLProperty property) {
        return switch (property) {
            case FXMLCollectionProperties(_, _, _, List<AbstractFXMLValue> value, List<FXMLProperty> properties) ->
                    Stream.concat(
                            value.stream()
                                    .flatMap(this::createIdentifierToTypeMapEntry),
                            properties.stream()
                                    .flatMap(this::createIdentifierToTypeMapEntry)
                    );
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> createIdentifierToTypeMapEntry(value);
            case FXMLMapProperty(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> value) -> value.values()
                    .stream()
                    .flatMap(this::createIdentifierToTypeMapEntry);
            case FXMLObjectProperty(_, _, _, AbstractFXMLValue value) -> createIdentifierToTypeMapEntry(value);
            case FXMLStaticObjectProperty(_, _, _, _, AbstractFXMLValue value) -> createIdentifierToTypeMapEntry(value);
        };
    }

    private sealed interface Wrapper {
        record FXMLTypeWrapper(FXMLType type) implements Wrapper {

            public FXMLTypeWrapper {
                Objects.requireNonNull(type, "`type` must not be null");
            }
        }

        record ReferenceWrapper(String reference) implements Wrapper {

            public ReferenceWrapper {
                Objects.requireNonNull(reference, "`reference` must not be null");
            }
        }
    }
}
