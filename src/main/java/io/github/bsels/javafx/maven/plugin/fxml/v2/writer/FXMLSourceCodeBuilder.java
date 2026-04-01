package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FXMLSourceCodeBuilder {
    private final Log log;
    private final FXMLSourceCodeBuilderImportHelper builderImportHelper;
    private final boolean addGeneratedAnnotation;

    public FXMLSourceCodeBuilder(Log log, boolean addGeneratedAnnotation) {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.builderImportHelper = new FXMLSourceCodeBuilderImportHelper();
        this.addGeneratedAnnotation = addGeneratedAnnotation;
    }

    public String generateSourceCode(FXMLDocument document, String packageName) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        log.debug("Generating source code for FXML document with classname: %s".formatted(document.className()));
        List<ClassCount> classCountList = builderImportHelper.findClassCounts(document);
        Imports imports = builderImportHelper.findImports(classCountList);
        SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(imports);

        addPackageLine(context, packageName);
        addImports(context);

        return generateSourceCode(context, document.className());
    }

    private String generateSourceCode(SourceCodeGeneratorContext context, String className) {
        StringBuilder sourceCode = new StringBuilder()
                .append(context.sourceCode(SourcePart.PACKAGE))
                .append(context.sourceCode(SourcePart.IMPORTS))
                .append(context.sourceCode(SourcePart.CLASS_DECLARATION)).append(" {\n")
                .append(context.sourceCode(SourcePart.FIELDS).toString().indent(4));

        if (context.abstractClass().get()) {
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

    private String typeToSourceCode(SourceCodeGeneratorContext context, FXMLType type) {
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
                .get(className);
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
                .append(">")
                .toString();
    }

    private enum SourcePart {
        PACKAGE,
        IMPORTS,
        CLASS_DECLARATION,
        FIELDS,
        CONSTRUCTOR_PROLOGUE,
        CONSTRUCTOR_EPILOGUE,
        METHODS
    }

    private record SourceCodeGeneratorContext(
            Map<SourcePart, StringBuilder> sourceCode,
            Imports imports,
            List<String> fieldDefinitions,
            AtomicBoolean abstractClass
    ) {

        public SourceCodeGeneratorContext {
            Objects.requireNonNull(sourceCode, "`sourceCode` must not be null");
            Objects.requireNonNull(imports, "`imports` must not be null");
            Objects.requireNonNull(fieldDefinitions, "`fieldDefinitions` must not be null");
            Objects.requireNonNull(abstractClass, "`abstractClass` must not be null");
        }

        public SourceCodeGeneratorContext(Imports imports) {
            Map<SourcePart, StringBuilder> sourceCode = new EnumMap<>(SourcePart.class);
            for (SourcePart part : SourcePart.values()) {
                sourceCode.put(part, new StringBuilder());
            }
            sourceCode = Collections.unmodifiableMap(sourceCode);
            this(sourceCode, imports, new ArrayList<>(), new AtomicBoolean(false));
        }

        public StringBuilder sourceCode(SourcePart part) {
            return sourceCode.get(part);
        }
    }
}
