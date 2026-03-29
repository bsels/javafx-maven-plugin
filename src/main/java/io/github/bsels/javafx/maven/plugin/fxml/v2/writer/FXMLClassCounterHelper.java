package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

/**
 * A helper class designed to assist in counting and managing FXML-related classes.
 * This class serves as a utility for performing operations related to FXML class counting.
 * It provides functionality for FXML class analysis and handling, such as analyzing FXML
 * documents, controller methods, and various FXML-related components.
 */
final class FXMLClassCounterHelper {
    /// Creates and returns a gatherer for aggregating class counts into a map and producing [ClassCount] objects
    /// for downstream processing.
    ///
    /// The gatherer operates sequentially, collecting the full class name and its associated count from input lines
    /// and aggregating them into a map.
    /// Once complete, the gathered data is pushed as [ClassCount] objects to the downstream consumer.
    private static final Gatherer<ClassCount, Map<String, Integer>, ClassCount> CLASS_COUNT_MERGER = Gatherer.ofSequential(
            HashMap::new,
            (state, line, _) -> {
                state.merge(line.fullClassName(), line.count(), Integer::sum);
                return true;
            },
            (state, downstream) ->
                    state.forEach((key, value) -> downstream.push(new ClassCount(key, value)))
    );

    /// A helper class designed to assist in counting and managing FXML-related classes.
    /// This class serves as a utility for performing operations related to FXML class counting.
    /// It does not accept parameters in its constructor and provides functionality related to FXML class analysis and handling.
    FXMLClassCounterHelper() {
    }

    /// Finds and returns a list of class counts derived from the provided FXML document.
    /// The method aggregates and processes class counts from the root element, controller elements,
    /// and definitions in the FXML document, sorts them in descending order of count, and then by class name.
    ///
    /// @param document the [FXMLDocument] to analyze; must not be null
    /// @return a sorted list of [ClassCount] objects representing the count of occurrences for each class within the provided [FXMLDocument]
    /// @throws NullPointerException if the provided document is null
    public List<ClassCount> findClassCounts(FXMLDocument document) {
        Objects.requireNonNull(document, "`document` must not be null");
        return Stream.concat(
                        Stream.concat(
                                findAbstractFXMLValueClassCount(document.root()),
                                document.controller()
                                        .stream()
                                        .flatMap(this::findControllerClassCounts)
                        ),
                        document.definitions()
                                .stream()
                                .flatMap(this::findAbstractFXMLValueClassCount)
                ).gather(CLASS_COUNT_MERGER)
                .sorted(
                        Comparator.comparingInt(ClassCount::count)
                                .reversed()
                                .thenComparing(ClassCount::fullClassName)
                )
                .toList();
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of class names associated with
    /// the provided [AbstractFXMLValue].
    ///
    /// This method handles multiple cases of [AbstractFXMLValue]:
    /// - For [FXMLCollection], it delegates analysis to [#findFXMLCollectionClassCount].
    /// - For [FXMLMap], it delegates analysis to [#findFXMLMapClassCount].
    /// - For [FXMLMethod], it delegates analysis to [#findFXMLMethodClassCount].
    /// - For [FXMLObject], it delegates analysis to [#findFXMLObjectClassCount].
    /// - For [FXMLValue], it processes the associated [FXMLType].
    /// - For [FXMLConstant], it processes the associated [FXMLClassType].
    /// - For other types, such as [FXMLCopy], [FXMLExpression], [FXMLInclude], and others, an empty stream is returned.
    ///
    /// Each case analyzes relevant components of the value and aggregates discovered class names and their occurrence
    /// counts into a unified stream.
    ///
    /// @param value The [AbstractFXMLValue] to analyze. Must not be `null`.
    /// @return A [Stream] of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count. If no class names are found, the stream will be empty.
    private Stream<ClassCount> findAbstractFXMLValueClassCount(AbstractFXMLValue value) {
        return (switch (value) {
            case FXMLCollection collection -> findFXMLCollectionClassCount(collection);
            case FXMLMap fxmlMap -> findFXMLMapClassCount(fxmlMap);
            case FXMLMethod method -> findFXMLMethodClassCount(method);
            case FXMLObject fxmlObject -> findFXMLObjectClassCount(fxmlObject);
            case FXMLValue(_, FXMLType type, _) -> findFXMLTypeClassCounts(type);
            case FXMLConstant(FXMLClassType clazz, _, _) -> findFXMLTypeClassCounts(clazz);
            case FXMLCopy _, FXMLExpression _, FXMLInclude _, FXMLInlineScript _, FXMLLiteral _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.<ClassCount>empty();
        }).gather(CLASS_COUNT_MERGER);
    }

    /// Recursively computes a stream of [ClassCount] for the given [FXMLProperty].
    /// Traverses the structure of the property and its nested components to aggregate class count data.
    ///
    /// @param property the [FXMLProperty] instance for which the class counts need to be computed
    /// @return a [Stream] of [ClassCount] representing the class count information derived from the property
    private Stream<ClassCount> findFXMLPropertyClassCount(FXMLProperty property) {
        return (switch (property) {
            case FXMLCollectionProperties(
                    _, _, FXMLType type, List<AbstractFXMLValue> value, List<FXMLProperty> properties
            ) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    Stream.concat(
                            value.stream()
                                    .flatMap(this::findAbstractFXMLValueClassCount),
                            properties.stream()
                                    .flatMap(this::findFXMLPropertyClassCount)
                    )
            );
            case FXMLConstructorProperty(_, FXMLType type, AbstractFXMLValue value) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    findAbstractFXMLValueClassCount(value)
            );
            case FXMLMapProperty(_, _, FXMLType type, _, _, Map<String, AbstractFXMLValue> value) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    value.values()
                            .stream()
                            .flatMap(this::findAbstractFXMLValueClassCount)
            );
            case FXMLObjectProperty(_, _, FXMLType type, AbstractFXMLValue value) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    findAbstractFXMLValueClassCount(value)
            );
            case FXMLStaticObjectProperty(_, FXMLClassType clazz, _, FXMLType type, AbstractFXMLValue value) ->
                    Stream.concat(
                            Stream.concat(
                                    findFXMLTypeClassCounts(type),
                                    findFXMLTypeClassCounts(clazz)
                            ),
                            findAbstractFXMLValueClassCount(value)
                    );
        }).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of class names associated with
    /// the provided [FXMLObject].
    ///
    /// The method processes the following components within the [FXMLObject]:
    /// - The type of the object itself.
    /// - Class names derived from the factory method of the object, if present.
    /// - Class names derived from the object's properties.
    ///
    /// All encountered class names and their occurrence counts are aggregated into a unified stream.
    ///
    /// @param object The [FXMLObject] to analyze. Must not be `null`.
    /// @return A [Stream] of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findFXMLObjectClassCount(FXMLObject object) {
        return Stream.concat(
                Stream.concat(
                        findFXMLTypeClassCounts(object.type()),
                        object.factoryMethod()
                                .stream()
                                .flatMap(this::findFXMLFactoryMethodClassCount)
                ),
                object.properties()
                        .stream()
                        .flatMap(this::findFXMLPropertyClassCount)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of class names associated with
    /// the provided [FXMLMap].
    ///
    /// The method processes the following components of the [FXMLMap]:
    /// - The type of the map itself.
    /// - Class names derived from the factory method of the map, if present.
    /// - Class names derived from the values within the map entries.
    ///
    /// Aggregates all encountered class names and their occurrence counts into a unified stream using the
    /// [#CLASS_COUNT_MERGER].
    ///
    /// @param map The [FXMLMap] to analyze. Must not be `null`.
    /// @return A [Stream] of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findFXMLMapClassCount(FXMLMap map) {
        return Stream.concat(
                Stream.concat(
                        findFXMLTypeClassCounts(map.type()),
                        map.factoryMethod()
                                .stream()
                                .flatMap(this::findFXMLFactoryMethodClassCount)
                ),
                map.entries()
                        .values()
                        .stream()
                        .flatMap(this::findAbstractFXMLValueClassCount)

        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of various class names associated
    /// with the provided [FXMLCollection].
    ///
    /// The method processes the following components of the [FXMLCollection]:
    /// - The class type of the collection itself.
    /// - The class names derived from the collection's factory method, if present.
    /// - The class names derived from the values within the collection.
    ///
    /// Aggregates all encountered class names and their occurrence counts into a unified stream.
    ///
    /// @param collection The [FXMLCollection] to analyze. Must not be `null`.
    /// @return A stream of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findFXMLCollectionClassCount(FXMLCollection collection) {
        return Stream.concat(
                Stream.concat(
                        findFXMLTypeClassCounts(collection.type()),
                        collection.factoryMethod()
                                .stream()
                                .flatMap(this::findFXMLFactoryMethodClassCount)
                ),
                collection.values()
                        .stream()
                        .flatMap(this::findAbstractFXMLValueClassCount)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of classes associated with
    /// he provided [FXMLFactoryMethod].
    ///
    /// The method analyzes the class defined in the [FXMLFactoryMethod].
    /// All encountered class names and their occurrence counts are aggregated into a unified stream.
    ///
    /// @param factoryMethod The [FXMLFactoryMethod] to analyze. Must not be `null`.
    /// @return A stream of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findFXMLFactoryMethodClassCount(FXMLFactoryMethod factoryMethod) {
        return findFXMLTypeClassCounts(factoryMethod.clazz());
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of classes
    /// associated with the provided [FXMLMethod].
    ///
    /// The method includes analysis of:
    /// - The return type is of the given [FXMLMethod].
    /// - The parameter types are of the given [FXMLMethod].
    ///
    /// All encountered class names and their occurrence counts are aggregated into a unified stream.
    ///
    /// @param method The [FXMLMethod] to analyze. Must not be `null`.
    /// @return A stream of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findFXMLMethodClassCount(FXMLMethod method) {
        return Stream.concat(
                findFXMLTypeClassCounts(method.returnType()),
                method.parameters()
                        .stream()
                        .flatMap(this::findFXMLTypeClassCounts)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of various class names associated
    /// with the provided [FXMLController].
    ///
    /// The method processes the following components of the [FXMLController]:
    /// - The controller's class type.
    /// - The types of its fields.
    /// - The return and parameter types of its methods.
    ///
    /// Each encountered class name is analyzed, and its occurrence count is aggregated.
    ///
    /// @param controller The [FXMLController] to analyze. Must not be `null`.
    /// @return A stream of [ClassCount] objects, where each object contains a class name and the corresponding number of occurrences.
    private Stream<ClassCount> findControllerClassCounts(FXMLController controller) {
        return Stream.concat(
                Stream.concat(
                        findFXMLTypeClassCounts(controller.controllerClass()),
                        controller.fields()
                                .stream()
                                .map(FXMLControllerField::type)
                                .flatMap(this::findFXMLTypeClassCounts)
                ),
                controller.methods()
                        .stream()
                        .flatMap(this::findClassCountControllerMethod)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects representing the occurrence counts of various class names associated
    /// with the return type and parameter types of the specified [FXMLControllerMethod].
    ///
    /// This method processes the following components:
    /// - The method's return type.
    /// - The types of the method's parameters.
    ///
    /// Class names and their occurrence counts are aggregated into a unified stream for analysis.
    ///
    /// @param method The [FXMLControllerMethod] to analyze. Must not be `null`.
    /// @return A stream of [ClassCount] objects, where each object contains a class name and its corresponding occurrence count.
    private Stream<ClassCount> findClassCountControllerMethod(FXMLControllerMethod method) {
        return Stream.concat(
                findFXMLTypeClassCounts(method.returnType()),
                method.parameterTypes()
                        .stream()
                        .flatMap(this::findFXMLTypeClassCounts)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Analyzes the specified [FXMLType] and generates a stream of [ClassCount] objects,
    /// where each [ClassCount] represents a type name and its associated occurrence count.
    ///
    /// The method handles various forms of [FXMLType], including class types,
    /// generic types (both compiled and uncompiled), and wildcard types.
    /// Depending on the specific `FXMLType`, it recursively processes nested type arguments if applicable,
    /// combining the results to provide a consolidated count of class names.
    ///
    /// @param type The [FXMLType] to process. Must not be `null`.
    /// @return A stream of [ClassCount] objects, representing the class names and their corresponding counts. If the input type does not represent valid classes (e.g., wildcard), the stream will be empty.
    private Stream<ClassCount> findFXMLTypeClassCounts(FXMLType type) {
        return (switch (type) {
            case FXMLClassType(Class<?> clazz) -> Stream.of(new ClassCount(clazz.getName(), 1));
            case FXMLGenericType(Class<?> clazz, List<FXMLType> typeArguments) -> Stream.concat(
                    Stream.of(new ClassCount(clazz.getName(), 1)),
                    typeArguments.stream()
                            .flatMap(this::findFXMLTypeClassCounts)
            );
            case FXMLUncompiledClassType(String className) -> Stream.of(new ClassCount(className, 1));
            case FXMLUncompiledGenericType(String name, List<FXMLType> typeArguments) -> Stream.concat(
                    Stream.of(new ClassCount(name, 1)),
                    typeArguments.stream()
                            .flatMap(this::findFXMLTypeClassCounts)
            );
            case FXMLWildcardType _ -> Stream.<ClassCount>empty();
        }).gather(CLASS_COUNT_MERGER);
    }

    /// Represents an immutable pair consisting of a fully qualified class name and its associated occurrence count.
    ///
    /// This record is used to consolidate and represent data about how many times a specific class name is encountered
    /// during processing within the application.
    /// It ensures that the class name cannot be null upon creation.
    ///
    /// @param fullClassName The fully qualified name of the class. Must not be null.
    /// @param count         The number of occurrences for the specified class name.
    record ClassCount(String fullClassName, int count) {

        /// Constructs a new `ClassCount` record instance.
        /// Ensures the specified class name is not null and the count is non-negative.
        ///
        /// @param fullClassName The full class name associated with this [ClassCount]. Must not be null.
        /// @param count         The occurrence count of the specified class name. Represents how many times this class name is encountered.
        /// @throws NullPointerException     If `fullClassName` is null.
        /// @throws IllegalArgumentException If `count` is negative.
        ClassCount {
            Objects.requireNonNull(fullClassName, "`fullClassName` must not be null");
            if (count >= 0) {
                throw new IllegalArgumentException("`count` must be non-negative");
            }
        }
    }
}
