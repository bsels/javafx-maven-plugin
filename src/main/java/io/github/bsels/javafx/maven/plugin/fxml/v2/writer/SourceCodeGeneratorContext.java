package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

record SourceCodeGeneratorContext(
        Map<SourcePart, StringBuilder> sourceCode,
        String resourceBundle,
        Imports imports,
        List<String> fieldDefinitions,
        Set<Feature> features,
        Map<String, FXMLType> identifierToTypeMap,
        Set<String> seenNestedFXMLFiles
) {

    public SourceCodeGeneratorContext {
        Objects.requireNonNull(sourceCode, "`sourceCode` must not be null");
        Objects.requireNonNull(resourceBundle, "`resourceBundle` must not be null");
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(fieldDefinitions, "`fieldDefinitions` must not be null");
        Objects.requireNonNull(features, "`features` must not be null");
        Objects.requireNonNull(identifierToTypeMap, "`identifierToTypeMap` must not be null");
        Objects.requireNonNull(seenNestedFXMLFiles, "`seenNestedFXMLFiles` must not be null");
    }

    public SourceCodeGeneratorContext(Imports imports, String resourceBundle, Map<String, FXMLType> identifierToTypeMap) {
        Map<SourcePart, StringBuilder> sourceCode = createSourceCodeBuilders();
        this(
                sourceCode,
                resourceBundle,
                imports,
                new ArrayList<>(),
                new HashSet<>(),
                identifierToTypeMap,
                new HashSet<>()
        );
    }

    private static Map<SourcePart, StringBuilder> createSourceCodeBuilders() {
        Map<SourcePart, StringBuilder> sourceCode = new EnumMap<>(SourcePart.class);
        for (SourcePart part : SourcePart.values()) {
            sourceCode.put(part, new StringBuilder());
        }
        return Collections.unmodifiableMap(sourceCode);
    }

    public StringBuilder sourceCode(SourcePart part) {
        return sourceCode.get(part);
    }

    public boolean hasFeature(Feature feature) {
        return features.contains(feature);
    }

    public void addFeature(Feature feature) {
        features.add(feature);
    }

    public SourceCodeGeneratorContext with(String resourceBundle) {
        return new SourceCodeGeneratorContext(
                createSourceCodeBuilders(),
                resourceBundle,
                imports,
                fieldDefinitions,
                features,
                identifierToTypeMap,
                seenNestedFXMLFiles
        );
    }
}
