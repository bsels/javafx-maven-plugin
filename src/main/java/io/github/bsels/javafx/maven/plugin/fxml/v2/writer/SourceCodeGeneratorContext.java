package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/// Represents the context used during Java source code generation for an FXML document.
/// It maintains the generated source code parts, imports, features, and other necessary metadata.
///
/// @param sourceCode          A map containing `StringBuilder` instances for each [SourcePart].
/// @param resourceBundle      The resource bundle expression used for translations.
/// @param imports             The [Imports] needed for the generated source code.
/// @param fieldDefinitions    A list of field definition strings.
/// @param features            A set of [Feature]s identified as necessary for the generated code.
/// @param identifierToTypeMap A map from FXML identifiers to their corresponding [FXMLType]s.
/// @param seenNestedFXMLFiles A set of paths to FXML files that have already been processed as nested `includes`.
/// @param seenFXMLMethods     A set of FXML methods that have already been processed.
/// @param packageName         The package name for the generated Java class.
record SourceCodeGeneratorContext(
        Map<SourcePart, StringBuilder> sourceCode,
        String resourceBundle,
        Imports imports,
        List<String> fieldDefinitions,
        Set<Feature> features,
        Map<String, FXMLType> identifierToTypeMap,
        Set<String> seenNestedFXMLFiles,
        Set<FXMLMethod> seenFXMLMethods,
        Optional<String> packageName
) {

    /// Constructs a `SourceCodeGeneratorContext` and validates that all parameters are non-null.
    ///
    /// @param sourceCode          A map containing `StringBuilder` instances for each [SourcePart].
    /// @param resourceBundle      The resource bundle expression used for translations.
    /// @param imports             The [Imports] needed for the generated source code.
    /// @param fieldDefinitions    A list of field definition strings.
    /// @param features            A set of [Feature]s identified as necessary for the generated code.
    /// @param identifierToTypeMap A map from FXML identifiers to their corresponding [FXMLType]s.
    /// @param seenNestedFXMLFiles A set of paths to FXML files that have already been processed as nested `includes`.
    /// @param seenFXMLMethods     A set of FXML methods that have already been processed.
    /// @param packageName         The package name for the generated Java class.
    /// @throws NullPointerException If any of the parameters is null.
    public SourceCodeGeneratorContext {
        Objects.requireNonNull(sourceCode, "`sourceCode` must not be null");
        Objects.requireNonNull(resourceBundle, "`resourceBundle` must not be null");
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(fieldDefinitions, "`fieldDefinitions` must not be null");
        Objects.requireNonNull(features, "`features` must not be null");
        Objects.requireNonNull(identifierToTypeMap, "`identifierToTypeMap` must not be null");
        Objects.requireNonNull(seenNestedFXMLFiles, "`seenNestedFXMLFiles` must not be null");
        Objects.requireNonNull(seenFXMLMethods, "`seenFXMLMethods` must not be null");
        Objects.requireNonNull(packageName, "`packageName` must not be null");
    }

    /// Creates a new `SourceCodeGeneratorContext` with default empty collections.
    ///
    /// @param imports             The [Imports] for the generated source code.
    /// @param resourceBundle      The initial resource bundle expression.
    /// @param identifierToTypeMap A map from FXML identifiers to their corresponding [FXMLType]s.
    /// @param packageName         The package name for the generated Java class.
    public SourceCodeGeneratorContext(
            Imports imports,
            String resourceBundle,
            Map<String, FXMLType> identifierToTypeMap,
            String packageName
    ) {
        Map<SourcePart, StringBuilder> sourceCode = createSourceCodeBuilders();
        this(
                sourceCode,
                resourceBundle,
                imports,
                new ArrayList<>(),
                new HashSet<>(),
                identifierToTypeMap,
                new HashSet<>(),
                new HashSet<>(),
                Optional.ofNullable(packageName)
        );
    }

    /// Creates a map of [StringBuilder]s for each [SourcePart].
    ///
    /// @return An unmodifiable map containing a `StringBuilder` for each `SourcePart`.
    private static Map<SourcePart, StringBuilder> createSourceCodeBuilders() {
        Map<SourcePart, StringBuilder> sourceCode = new EnumMap<>(SourcePart.class);
        for (SourcePart part : SourcePart.values()) {
            sourceCode.put(part, new StringBuilder());
        }
        return Collections.unmodifiableMap(sourceCode);
    }

    /// Retrieves the [StringBuilder] for the specified [SourcePart].
    ///
    /// @param part The `SourcePart` for which to get the `StringBuilder`.
    /// @return The `StringBuilder` associated with the given `SourcePart`.
    public StringBuilder sourceCode(SourcePart part) {
        return sourceCode.get(part);
    }

    /// Checks if a [Feature] is present in this context.
    ///
    /// @param feature The `Feature` to check.
    /// @return `true` if the feature is present, `false` otherwise.
    public boolean hasFeature(Feature feature) {
        return features.contains(feature);
    }

    /// Adds a [Feature] to this context.
    ///
    /// @param feature The `Feature` to add.
    public void addFeature(Feature feature) {
        features.add(feature);
    }

    /// Creates a copy of this context with a new resource bundle.
    /// The copied context uses fresh `StringBuilder` instances for its source code.
    ///
    /// @param resourceBundle The new resource bundle expression.
    /// @return A new `SourceCodeGeneratorContext` with the updated resource bundle.
    public SourceCodeGeneratorContext with(String resourceBundle) {
        return new SourceCodeGeneratorContext(
                createSourceCodeBuilders(),
                resourceBundle,
                imports,
                fieldDefinitions,
                features,
                identifierToTypeMap,
                seenNestedFXMLFiles,
                new HashSet<>(),
                packageName
        );
    }
}
