package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/// A helper utility for recursively traversing and processing FXML property structures.
/// This class encapsulates logic to navigate through various types of FXML properties, including collections, maps,
/// and object properties, and apply processing functions to their values.
///
/// This class is designed for internal use and does not offer any customizable state or configuration.
/// It provides a single recursive method to facilitate walking through FXML properties.
final class FXMLPropertyRecursionHelper {

    /// A helper utility for recursively traversing and processing FXML property structures.
    /// This class encapsulates logic to navigate through various types of FXML properties, including collections, maps,
    /// and object properties, and apply processing functions to their values.
    ///
    /// This constructor initializes the helper instance.
    /// The class is designed for internal usage and provides no additional state or configuration during instantiation.
    FXMLPropertyRecursionHelper() {
    }

    /// Recursively traverses a [FXMLProperty] structure and processes its values and nested properties using
    /// the provided value-walking function.
    ///
    /// @param <R>       The type of the stream elements returned by the walking function.
    /// @param <C>       The type of the context object passed during each walk operation.
    /// @param property  The [FXMLProperty] to be traversed.
    /// @param valueWalk A [BiFunction] that processes each [AbstractFXMLValue] in the property and returns a [Stream] of results.
    /// @param context   The context object that is passed to the `valueWalk` function for processing.
    /// @return A [Stream] containing results from traversing the property and processing its values.
    <R, C> Stream<R> walk(FXMLProperty property, BiFunction<AbstractFXMLValue, C, Stream<R>> valueWalk, C context) {
        return switch (property) {
            case FXMLCollectionProperties(_, _, _, List<AbstractFXMLValue> values, List<FXMLProperty> properties) ->
                    Stream.concat(
                            values.stream()
                                    .flatMap(value -> valueWalk.apply(value, context)),
                            properties.stream()
                                    .flatMap(fxmlProperty -> walk(fxmlProperty, valueWalk, context))
                    );
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
            case FXMLMapProperty(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> values) -> values.values()
                    .stream()
                    .flatMap(value -> valueWalk.apply(value, context));
            case FXMLObjectProperty(_, _, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
            case FXMLStaticObjectProperty(_, _, _, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
        };
    }
}
