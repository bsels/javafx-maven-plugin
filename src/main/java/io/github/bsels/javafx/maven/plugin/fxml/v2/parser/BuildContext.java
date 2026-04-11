package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/// Holds state and context during the FXML document building process.
/// Maintains internal state such as identifier counter, imports, definitions, scripts, and the resource path.
///
/// @param internalCounter The counter for generating internal identifiers
/// @param imports         The list of imports
/// @param definitions     The list of definitions
/// @param scripts         The list of scripts
/// @param typeMapping     The map for resolving type variables
/// @param resourcePath    The path of the FXML file relative to the resources folder root
record BuildContext(
        AtomicInteger internalCounter,
        List<String> imports,
        List<AbstractFXMLValue> definitions,
        List<FXMLScript> scripts,
        Map<String, FXMLType> typeMapping,
        String resourcePath
) {

    /// Initializes a new [BuildContext] record instance.
    ///
    /// @param internalCounter The counter for generating internal identifiers
    /// @param imports         The list of imports
    /// @param definitions     The list of definitions
    /// @param scripts         The list of scripts
    /// @param typeMapping     The map for resolving type variables
    /// @param resourcePath    The path of the FXML file relative to the resources folder root
    /// @throws NullPointerException If any parameter is null
    public BuildContext {
        Objects.requireNonNull(internalCounter, "`internalCounter` must not be null");
        Objects.requireNonNull(imports, "`imports` must not be null");
        Objects.requireNonNull(definitions, "`definitions` must not be null");
        Objects.requireNonNull(scripts, "`scripts` must not be null");
        Objects.requireNonNull(typeMapping, "`typeMapping` must not be null");
        Objects.requireNonNull(resourcePath, "`resourcePath` must not be null");
        resourcePath = resourcePath.endsWith("/") ? resourcePath : resourcePath + "/";
    }

    /// Initializes a new [BuildContext] instance with the provided imports and resource path.
    ///
    /// @param imports      The list of imports
    /// @param resourcePath The path of the FXML file relative to the resources folder root
    public BuildContext(List<String> imports, String resourcePath) {
        this(
                new AtomicInteger(),
                imports,
                new ArrayList<>(),
                new ArrayList<>(),
                new LinkedHashMap<>(),
                resourcePath
        );
    }

    /// Initializes a new [BuildContext] by copying the properties of an existing [BuildContext]
    /// and replacing the `typeMapping` with the provided mapping.
    ///
    /// @param original    The original [BuildContext] instance
    /// @param typeMapping The new map for resolving type variables
    /// @throws NullPointerException If `original` or `typeMapping` is null
    public BuildContext(
            BuildContext original,
            Map<String, FXMLType> typeMapping
    ) {
        Objects.requireNonNull(original, "`original` must not be null");
        Objects.requireNonNull(typeMapping, "`typeMapping` must not be null");
        this(
                original.internalCounter,
                original.imports,
                original.definitions,
                original.scripts,
                new LinkedHashMap<>(typeMapping),
                original.resourcePath
        );
    }

    /// Generates the next internal identifier.
    ///
    /// @return The next incremental identifier
    int nextInternalId() {
        return internalCounter.getAndIncrement();
    }
}
