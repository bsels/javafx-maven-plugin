package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

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
        Imports imports,
        List<String> fieldDefinitions,
        Set<Feature> features
) {

    public SourceCodeGeneratorContext {
        Objects.requireNonNull(sourceCode, "`sourceCode` must not be null");
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(fieldDefinitions, "`fieldDefinitions` must not be null");
        Objects.requireNonNull(features, "`features` must not be null");
    }

    public SourceCodeGeneratorContext(Imports imports) {
        Map<SourcePart, StringBuilder> sourceCode = new EnumMap<>(SourcePart.class);
        for (SourcePart part : SourcePart.values()) {
            sourceCode.put(part, new StringBuilder());
        }
        sourceCode = Collections.unmodifiableMap(sourceCode);
        this(sourceCode, imports, new ArrayList<>(), new HashSet<>());
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
}
