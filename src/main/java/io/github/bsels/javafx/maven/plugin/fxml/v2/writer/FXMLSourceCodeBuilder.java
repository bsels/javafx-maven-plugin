package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
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
import org.apache.maven.plugin.logging.Log;

import javax.annotation.processing.Generated;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class FXMLSourceCodeBuilder {
    private static final String INTERNAL_RESOURCE_BUNDLE = "$INTERNAL$RESOURCE$BUNDLE$";
    private static final String INTERNAL_STRING_TO_FILE_METHOD = "$internalMethod$stringToFile$";
    private static final String INTERNAL_STRING_TO_PATH_METHOD = "$internalMethod$stringToPath$";
    private static final String INTERNAL_STRING_TO_URI_METHOD = "$internalMethod$stringToURI$";
    private static final String INTERNAL_STRING_TO_URL_METHOD = "$internalMethod$stringToURL$";
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "boolean", "byte", "char", "short", "int", "long", "float", "double"
    );
    private final Log log;
    private final FXMLSourceCodeBuilderImportHelper builderImportHelper;
    private final FXMLSourceCodeBuilderTypeHelper typeHelper;
    private final boolean addGeneratedAnnotation;
    private final ZonedDateTime buildTime;

    public FXMLSourceCodeBuilder(Log log, boolean addGeneratedAnnotation) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.builderImportHelper = new FXMLSourceCodeBuilderImportHelper();
        this.typeHelper = new FXMLSourceCodeBuilderTypeHelper();
        this.addGeneratedAnnotation = addGeneratedAnnotation;
        this.buildTime = ZonedDateTime.now(ZoneOffset.UTC);
    }

    public String generateSourceCode(FXMLDocument document, String packageName) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        log.debug("Generating source code for FXML document with classname: %s".formatted(document.className()));
        List<ClassCount> classCountList = builderImportHelper.findClassCounts(document, addGeneratedAnnotation);
        Imports imports = builderImportHelper.findImports(classCountList, document.className());
        SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(imports);

        addPackageLine(context, packageName);
        addImports(context);
        addFields(context, document);
        addConstructorEpilogue(context, document);

        // The class declaration should be done last
        addClassDeclaration(context, document);
        return generateSourceCode(context, document.className(), addGeneratedAnnotation);
    }

    private String generateSourceCode(SourceCodeGeneratorContext context, String className, boolean addGeneratedAnnotation) {
        StringBuilder sourceCode = new StringBuilder()
                .append(context.sourceCode(SourcePart.PACKAGE))
                .append(context.sourceCode(SourcePart.IMPORTS));

        if (addGeneratedAnnotation) {
            sourceCode.append('@')
                    .append(typeHelper.typeToSourceCode(context, FXMLType.of(Generated.class)))
                    .append("(\n    value=\"")
                    .append(FXMLSourceCodeBuilder.class.getName())
                    .append("\",\n    date=\"")
                    .append(buildTime)
                    .append("\"\n)\n");
        }

        sourceCode.append(context.sourceCode(SourcePart.CLASS_DECLARATION)).append(" {\n");
        if (context.hasFeature(Feature.RESOURCE_BUNDLE)) {
            // TODO: Assign the resource bundle
            sourceCode.append("private static final java.util.ResourceBundle ")
                    .append(INTERNAL_RESOURCE_BUNDLE)
                    .append(" = null;\n\n");
        }
        sourceCode.append(context.sourceCode(SourcePart.FIELDS).toString().indent(4));

        if (context.hasFeature(Feature.ABSTRACT_CLASS)) {
            sourceCode.append("protected".indent(4).stripTrailing());
        } else {
            sourceCode.append("public".indent(4).stripTrailing());
        }

        return sourceCode.append(" ").append(className).append("() {\n")
                .append(context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString().indent(8))
                .append("super();".indent(8))
                .append(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString().indent(8))
                .append("}\n".indent(4))
                .append(context.sourceCode(SourcePart.METHODS).toString().indent(4))
                .append("}\n")
                .toString();
    }

    private void addImports(SourceCodeGeneratorContext context) {
        List<String> importList = context.imports()
                .imports()
                .stream()
                .filter(Predicate.not(PRIMITIVE_TYPES::contains))
                .sorted()
                .toList();
        StringBuilder sourceCode = context.sourceCode(SourcePart.IMPORTS);
        for (String importStatement : importList) {
            sourceCode.append("import ").append(importStatement).append(";\n");
        }
        sourceCode.append("\n");
    }

    private void addPackageLine(SourceCodeGeneratorContext context, String packageName) {
        if (packageName != null) {
            context.sourceCode(SourcePart.PACKAGE).append("package ").append(packageName).append(";\n\n");
        }
    }

    private void addFields(SourceCodeGeneratorContext context, FXMLDocument document) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.FIELDS);
        document.controller()
                .ifPresent(c -> sourceCode
                        .append("private final ")
                        .append(typeHelper.typeToSourceCode(context, c.controllerClass()))
                        .append(" $internalController$;\n"));
        addFields(context, document.root());
        document.definitions()
                .forEach(d -> addFields(context, d));
        sourceCode
                .append("\n");
    }

    private void addFields(SourceCodeGeneratorContext context, AbstractFXMLValue value) {
        switch (value) {
            case FXMLCollection(FXMLIdentifier identifier, FXMLType type, _, List<AbstractFXMLValue> values) -> {
                identifierToField(context, identifier, type);
                values.forEach(v -> addFields(context, v));
            }
            case FXMLCopy(FXMLIdentifier identifier, String name) -> {
                identifierToField(context, identifier, FXMLType.of(Object.class)); // TODO: handle copy type
            }
            case FXMLInclude(FXMLIdentifier identifier, String sourceFile, _, _) -> {
                identifierToField(context, identifier, FXMLType.of(Object.class)); // TODO: handle include type
                // TODO: Check for linked controller
            }
            case FXMLMap(
                    FXMLIdentifier identifier, FXMLType type, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> {
                identifierToField(context, identifier, type);
                entries.values().forEach(v -> addFields(context, v));
            }
            case FXMLObject(FXMLIdentifier identifier, FXMLType type, _, List<FXMLProperty> properties) -> {
                identifierToField(context, identifier, type);
                properties.forEach(p -> addFields(context, p));
            }
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLReference _, FXMLResource _, FXMLTranslation _ -> {
            }
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType type, _) ->
                    identifier.ifPresent(id -> identifierToField(context, id, type));
        }
    }

    private void addFields(SourceCodeGeneratorContext context, FXMLProperty property) {
        switch (property) {
            case FXMLCollectionProperties(_, _, _, List<AbstractFXMLValue> value, List<FXMLProperty> properties) -> {
                value.forEach(v -> addFields(context, v));
                properties.forEach(p -> addFields(context, p));
            }
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> addFields(context, value);
            case FXMLMapProperty(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> value) ->
                    value.values().forEach(v -> addFields(context, v));
            case FXMLObjectProperty(_, _, _, AbstractFXMLValue value) -> addFields(context, value);
            case FXMLStaticObjectProperty(_, _, _, _, AbstractFXMLValue value) -> addFields(context, value);
        }
    }

    private void identifierToField(SourceCodeGeneratorContext context, FXMLIdentifier identifier, FXMLType type) {
        switch (identifier) {
            case FXMLExposedIdentifier(String name) -> context.sourceCode(SourcePart.FIELDS)
                    .append("protected final ")
                    .append(typeHelper.typeToSourceCode(context, type))
                    .append(" ").append(name).append(";\n");
            case FXMLInternalIdentifier id -> context.sourceCode(SourcePart.FIELDS)
                    .append("private final ")
                    .append(typeHelper.typeToSourceCode(context, type))
                    .append(" ").append(id).append(";\n");
            case FXMLNamedRootIdentifier _, FXMLRootIdentifier _ -> {
            }
        }
    }

    private void addClassDeclaration(SourceCodeGeneratorContext context, FXMLDocument document) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CLASS_DECLARATION);
        sourceCode.append("public ");
        if (context.hasFeature(Feature.ABSTRACT_CLASS)) {
            sourceCode.append("abstract ");
        }
        sourceCode.append("class ")
                .append(document.className())
                .append(" extends ")
                .append(typeHelper.typeToSourceCode(context, document.root().type()))
        // TODO: Add interfaces
        ;
    }

    private void addConstructorEpilogue(SourceCodeGeneratorContext context, FXMLDocument document) {
        addConstructorEpilogue(context, document.root());
        document.definitions()
                .forEach(d -> addConstructorEpilogue(context, d));
    }

    private void addConstructorEpilogue(SourceCodeGeneratorContext context, AbstractFXMLValue value) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE);
        switch (value) {
            case FXMLCollection(
                    FXMLIdentifier identifier,
                    _,
                    _,
                    List<AbstractFXMLValue> values
            ) -> {
                values.forEach(v -> sourceCode.append(identifier.toString())
                        .append(".add(")
                        .append(encodeFXMLValue(context, v, FXMLType.of(Object.class)))
                        .append(");\n"));
                values.forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLMap(
                    FXMLIdentifier identifier,
                    _,
                    FXMLClassType keyType,
                    FXMLClassType valueType,
                    _,
                    Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> {
                entries.forEach((k, v) -> sourceCode.append(identifier.toString())
                        .append(".put(")
                        .append(encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(keyType)))
                        .append(", ")
                        .append(encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(valueType)))
                        .append(");\n"));
                entries.values().forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLObject(
                    FXMLIdentifier identifier,
                    _,
                    _,
                    List<FXMLProperty> properties
            ) -> {
                properties.forEach(p -> addConstructorEpilogue(context, identifier.toString(), p));
            }
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInclude _, FXMLInlineScript _, FXMLLiteral _,
                 FXMLMethod _, FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> {
            }
        }
    }

    private void addConstructorEpilogue(SourceCodeGeneratorContext context, String identifier, FXMLProperty property) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE);
        switch (property) {
            case FXMLCollectionProperties(
                    _,
                    String getter,
                    FXMLType type,
                    List<AbstractFXMLValue> value,
                    List<FXMLProperty> properties
            ) -> {
                String collectionSelection = "%s.%s()".formatted(identifier, getter);
                value.forEach(v -> sourceCode.append(collectionSelection)
                        .append(".add(")
                        .append(encodeFXMLValue(context, v, type))
                        .append(");\n"));
                value.forEach(v -> addConstructorEpilogue(context, v));
                properties.forEach(p -> addConstructorEpilogue(context, collectionSelection, p));
            }
            case FXMLConstructorProperty _ -> {
                // Added in the constructor prologue during the constructor call
            }
            case FXMLMapProperty(
                    _,
                    String getter,
                    _,
                    FXMLClassType rawKeyClass,
                    FXMLClassType rawValueClass,
                    Map<FXMLLiteral, AbstractFXMLValue> value
            ) -> {
                value.forEach((k, v) -> sourceCode.append(identifier)
                        .append('.')
                        .append(getter)
                        .append("().put(")
                        .append(encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(rawKeyClass)))
                        .append(", ")
                        .append(encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(rawValueClass)))
                        .append(");\n"));
                value.values().forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLObjectProperty(
                    _,
                    String setter,
                    FXMLType type,
                    AbstractFXMLValue value
            ) -> {
                sourceCode.append(identifier)
                        .append('.')
                        .append(setter)
                        .append("(")
                        .append(encodeFXMLValue(context, value, type))
                        .append(");\n");
                addConstructorEpilogue(context, value);
            }
            case FXMLStaticObjectProperty(
                    _,
                    FXMLClassType clazz,
                    String setter,
                    FXMLType type,
                    AbstractFXMLValue value
            ) -> {
                sourceCode.append(typeHelper.typeToSourceCode(context, clazz))
                        .append('.')
                        .append(setter)
                        .append("(")
                        .append(identifier)
                        .append(", ")
                        .append(encodeFXMLValue(context, value, type))
                        .append(");\n");
                addConstructorEpilogue(context, value);
            }
        }
    }

    private String encodeFXMLValue(SourceCodeGeneratorContext context, AbstractFXMLValue value, FXMLType type) {
        return switch (value) {
            case AbstractFXMLObject object -> object.identifier().toString();
            case FXMLConstant(FXMLClassType clazz, String identifier, _) ->
                    "%s.%s".formatted(typeHelper.typeToSourceCode(context, clazz), identifier);
            case FXMLCopy(FXMLIdentifier identifier, _) -> identifier.toString();
            case FXMLExpression fxmlExpression ->
                    throw new UnsupportedOperationException("Expression values are not supported yet");
            case FXMLInclude(FXMLIdentifier identifier, _, _, _) -> identifier.toString();
            case FXMLInlineScript fxmlInlineScript ->
                    throw new UnsupportedOperationException("Inline script values are not supported yet");
            case FXMLLiteral(String literal) -> typeHelper.encodeLiteral(context, literal, type);
            case FXMLMethod(String name, _, _) -> "this::%s".formatted(name);
            case FXMLReference(String name) -> name;
            case FXMLResource(String resource) -> {
                resource = typeHelper.encodeString(resource);
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
                        typeHelper.encodeString(translationKey)
                );
            }
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType t, String v) ->
                    identifier.map(FXMLIdentifier::toString).orElseGet(() -> typeHelper.encodeLiteral(context, v, t));
        };
    }
}
