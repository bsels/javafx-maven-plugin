package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/// Helper for recursively traversing and processing FXML property structures.
/// Navigates through various types of FXML properties, including collections, maps, and object properties,
/// and applies processing functions to their values.
final class FXMLPropertyRecursionHelper {

    /// Initializes a new [FXMLPropertyRecursionHelper] instance.
    FXMLPropertyRecursionHelper() {
    }

    /// Recursively traverses a [FXMLProperty] structure and processes its values.
    ///
    /// @param <R>       The type of the stream elements returned by the walking function
    /// @param <C>       The type of the context object passed during each walk operation
    /// @param property  The [FXMLProperty] to be traversed
    /// @param valueWalk A [BiFunction] that processes each [AbstractFXMLValue]
    /// @param context   The context object passed to the `valueWalk` function
    /// @return A [Stream] containing results from traversing the property and processing its values
    /// @throws NullPointerException if `property` or `valueWalk` is null
    <R, C> Stream<R> walk(FXMLProperty property, BiFunction<AbstractFXMLValue, C, Stream<R>> valueWalk, C context)
            throws NullPointerException {
        Objects.requireNonNull(property, "`property` must not be null");
        Objects.requireNonNull(valueWalk, "`valueWalk` must not be null");
        return switch (property) {
            case FXMLCollectionProperties(_, _, _, _, List<AbstractFXMLValue> values, List<FXMLProperty> properties) ->
                    Stream.concat(
                            values.stream()
                                    .flatMap(value -> valueWalk.apply(value, context)),
                            walk(properties, valueWalk, context)
                    );
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
            case FXMLMapProperty(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> values) -> values.values()
                    .stream()
                    .flatMap(value -> valueWalk.apply(value, context));
            case FXMLObjectProperty(_, _, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
            case FXMLStaticObjectProperty(_, _, _, _, AbstractFXMLValue value) -> valueWalk.apply(value, context);
        };
    }

    /// Recursively traverses a collection of [FXMLProperty] structures and processes their values.
    ///
    /// @param <R>        The type of the stream elements returned by the walking function
    /// @param <C>        The type of the context object passed during each walk operation
    /// @param properties The collection of [FXMLProperty] objects to be traversed
    /// @param valueWalk  A [BiFunction] that processes each [AbstractFXMLValue]
    /// @param context    The context object passed to the `valueWalk` function
    /// @return A [Stream] containing results from traversing the properties and processing their values
    /// @throws NullPointerException if `properties` or `valueWalk` is null
    <R, C> Stream<R> walk(
            Collection<FXMLProperty> properties, BiFunction<AbstractFXMLValue, C, Stream<R>> valueWalk, C context
    ) throws NullPointerException {
        Objects.requireNonNull(properties, "`properties` must not be null");
        Objects.requireNonNull(valueWalk, "`valueWalk` must not be null");
        return properties.stream()
                .flatMap(fxmlProperty -> walk(fxmlProperty, valueWalk, context));
    }
}
