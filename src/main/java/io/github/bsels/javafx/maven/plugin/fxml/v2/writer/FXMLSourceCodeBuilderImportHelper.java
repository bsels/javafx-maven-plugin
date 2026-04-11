package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
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

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// Helper for counting and managing FXML-related classes.
/// Utility for performing operations related to FXML class counting and analysis.
final class FXMLSourceCodeBuilderImportHelper {
    /// Creates a gatherer for aggregating class counts into a map and producing [ClassCount] objects.
    private static final Gatherer<ClassCount, Map<String, Integer>, ClassCount> CLASS_COUNT_MERGER = Gatherer.ofSequential(
            HashMap::new,
            (state, line, _) -> {
                state.merge(line.fullClassName(), line.count(), Integer::sum);
                return true;
            },
            (state, downstream) ->
                    state.forEach((key, value) -> downstream.push(new ClassCount(key, value)))
    );

    /// Helper for managing and resolving recursive property bindings.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Initializes a new [FXMLSourceCodeBuilderImportHelper] instance.
    FXMLSourceCodeBuilderImportHelper() {
        this.propertyRecursionHelper = new FXMLPropertyRecursionHelper();
    }

    /// Finds and organizes the class imports required for the specified [FXMLDocument].
    ///
    /// @param document               The [FXMLDocument] to process
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation
    /// @return An [Imports] object containing the list of import statements and a mapping of inline class names
    /// @throws NullPointerException If `document` is null
    public Imports findImports(FXMLDocument document, boolean addGeneratedAnnotation) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        String rootDocumentClassName = document.className();
        List<String> documentClassNames = getDocumentClassNames(document).toList();
        List<ClassCount> classCounts = findClassCounts(document, addGeneratedAnnotation);
        Map<String, List<ClassCount>> groupedClassCounts = groupClassCountsBasedOnClassPrefix(classCounts);
        List<GroupedClassCount> groupedClassCountList = groupedClassCounts.entrySet()
                .stream()
                .map(this::buildGroupedClassCount)
                .sorted(Comparator.comparingInt(GroupedClassCount::count).reversed())
                .toList();
        List<String> imports = new ArrayList<>();
        Map<String, String> inlineClassNames = new HashMap<>();
        for (GroupedClassCount groupedClassCount : groupedClassCountList) {
            String groupClassName = groupedClassCount.group();
            String simpleClassName = getSimpleClassName(groupClassName);
            boolean simpleClassNameAlreadyImported = imports.stream()
                    .map(this::getSimpleClassName)
                    .anyMatch(simpleClassName::equals);
            if (
                    simpleClassNameAlreadyImported &&
                            documentClassNames.stream().anyMatch(className -> className.endsWith(simpleClassName))
            ) {
                groupedClassCount.classes()
                        .stream()
                        .map(ClassCount::fullClassName)
                        .forEach(fullClassName -> inlineClassNames.put(fullClassName, fullClassName));
            } else {
                imports.add(groupClassName);
                groupedClassCount.classes()
                        .stream()
                        .map(ClassCount::fullClassName)
                        .forEach(className -> inlineClassNames.put(
                                className,
                                className.substring(className.indexOf(simpleClassName))
                        ));
            }
        }
        documentClassNames.stream()
                .filter(Predicate.not(rootDocumentClassName::equals))
                .forEach(className -> inlineClassNames.put(
                        className,
                        "%s.%s".formatted(rootDocumentClassName, className)
                ));
        return new Imports(imports, inlineClassNames);
    }

    /// Retrieves a stream of class names associated with the specified [FXMLDocument].
    ///
    /// @param document The [FXMLDocument] whose class names are to be retrieved
    /// @return A [Stream] of class names
    private Stream<String> getDocumentClassNames(FXMLDocument document) {
        return Stream.concat(
                Stream.of(document.className()),
                Stream.concat(
                        getDocumentClassNames(document.root()),
                        document.definitions()
                                .stream()
                                .flatMap(this::getDocumentClassNames)
                )
        );
    }

    /// Extracts class names from an [AbstractFXMLValue] instance.
    /// Traverses various FXML structures recursively to collect relevant class names.
    ///
    /// @param value The [AbstractFXMLValue] instance to process
    /// @return A stream of extracted class names
    private Stream<String> getDocumentClassNames(AbstractFXMLValue value) {
        return switch (value) {
            case FXMLCollection(_, _, _, List<AbstractFXMLValue> values) -> values.stream()
                    .flatMap(this::getDocumentClassNames);
            case FXMLMap(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) -> entries.values()
                    .stream()
                    .flatMap(this::getDocumentClassNames);
            case FXMLObject(_, _, _, List<FXMLProperty> properties) -> properties.stream()
                    .flatMap(property -> propertyRecursionHelper.walk(
                            property,
                            (v, _) -> getDocumentClassNames(v),
                            null
                    ));
            case FXMLInclude(_, _, _, _, FXMLLazyLoadedDocument lazyLoadedDocument) ->
                    getDocumentClassNames(lazyLoadedDocument.get());
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _,
                 FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> Stream.empty();
        };
    }

    /// Finds and returns a list of class counts derived from the specified [FXMLDocument].
    /// Aggregates and processes class counts from the root element, controller, and definitions.
    ///
    /// @param document               The [FXMLDocument] to analyze
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation in class counts
    /// @return A list of [ClassCount] objects representing the occurrence count for each class
    /// @throws NullPointerException If `document` is null
    private List<ClassCount> findClassCounts(FXMLDocument document, boolean addGeneratedAnnotation)
            throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        final Stream<ClassCount> generationAnnotation;
        if (addGeneratedAnnotation) {
            generationAnnotation = Stream.of(new ClassCount(Generated.class.getCanonicalName(), 1));
        } else {
            generationAnnotation = Stream.empty();
        }
        return Stream.concat(
                        Stream.concat(
                                findAbstractFXMLValueClassCount(document.root()),
                                document.controller()
                                        .stream()
                                        .flatMap(this::findControllerClassCounts)
                        ),
                        Stream.concat(
                                document.definitions()
                                        .stream()
                                        .flatMap(this::findAbstractFXMLValueClassCount),
                                generationAnnotation
                        )
                )
                .gather(CLASS_COUNT_MERGER)
                .toList();
    }

    /// Builds a [GroupedClassCount] based on the specified entry containing group information.
    ///
    /// @param entry A map entry where the key is the group identifier and the value is a list of [ClassCount] objects
    /// @return A [GroupedClassCount] object
    private GroupedClassCount buildGroupedClassCount(Map.Entry<String, List<ClassCount>> entry) {
        List<ClassCount> classCountsForGroup = entry.getValue();
        int count = classCountsForGroup.stream()
                .mapToInt(ClassCount::count)
                .sum();
        return new GroupedClassCount(entry.getKey(), count, classCountsForGroup);
    }

    /// Groups a list of [ClassCount] objects based on class name prefixes.
    ///
    /// @param classCounts The list of [ClassCount] objects to be grouped
    /// @return A map where keys are class name prefixes and values are lists of [ClassCount] objects
    private Map<String, List<ClassCount>> groupClassCountsBasedOnClassPrefix(List<ClassCount> classCounts) {
        Map<String, List<ClassCount>> groupedClassCounts = new HashMap<>();
        for (ClassCount classCount : classCounts) {
            String className = classCount.fullClassName();
            groupedClassCounts.keySet()
                    .stream()
                    .filter(prefix -> isPrefix(className, prefix))
                    .findFirst()
                    .map(groupedClassCounts::get)
                    .ifPresentOrElse(
                            list -> list.add(classCount),
                            () -> createNewAndMergeExistingClassCountPrefixes(classCount, groupedClassCounts)
                    );
        }
        return Map.copyOf(groupedClassCounts);
    }

    /// Creates a new group for the given class count and merges it with existing groups.
    ///
    /// @param classCount         The [ClassCount] object to be grouped and merged
    /// @param groupedClassCounts Map where keys are class name prefixes and values are lists of [ClassCount] objects
    private void createNewAndMergeExistingClassCountPrefixes(
            ClassCount classCount,
            Map<String, List<ClassCount>> groupedClassCounts
    ) {
        String className = classCount.fullClassName();
        List<String> keys = groupedClassCounts.keySet()
                .stream()
                .filter(key -> isPrefix(key, className))
                .toList();
        List<ClassCount> classes = keys.stream()
                .map(groupedClassCounts::remove)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        classes.add(classCount);
        groupedClassCounts.put(className, classes);
    }

    /// Computes a stream of [ClassCount] objects for the specified [AbstractFXMLValue].
    ///
    /// @param value The [AbstractFXMLValue] to analyze
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findAbstractFXMLValueClassCount(AbstractFXMLValue value) {
        return (switch (value) {
            case FXMLCollection collection -> findFXMLCollectionClassCount(collection);
            case FXMLMap fxmlMap -> findFXMLMapClassCount(fxmlMap);
            case FXMLMethod method -> findFXMLMethodClassCount(method);
            case FXMLObject fxmlObject -> findFXMLObjectClassCount(fxmlObject);
            case FXMLValue(_, FXMLType type, _) -> findFXMLTypeClassCounts(type);
            case FXMLConstant(FXMLClassType clazz, _, _) -> findFXMLTypeClassCounts(clazz);
            case FXMLInclude(_, _, _, _, FXMLLazyLoadedDocument lazyLoadedDocument) -> Stream.concat(
                    Stream.concat(
                            findAbstractFXMLValueClassCount(lazyLoadedDocument.get().root()),
                            lazyLoadedDocument.get().controller()
                                    .stream()
                                    .flatMap(this::findControllerClassCounts)
                    ),
                    lazyLoadedDocument.get().definitions()
                            .stream()
                            .flatMap(this::findAbstractFXMLValueClassCount)
            );
            case FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLReference _, FXMLResource _,
                 FXMLTranslation _ -> Stream.<ClassCount>empty();
        }).gather(CLASS_COUNT_MERGER);
    }

    /// Recursively computes a stream of [ClassCount] for the specified [FXMLProperty].
    ///
    /// @param property The [FXMLProperty] for which class counts should be computed
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findFXMLPropertyClassCount(FXMLProperty property) {
        return (switch (property) {
            case FXMLCollectionProperties(
                    _, _, FXMLType type, _, List<AbstractFXMLValue> value, List<FXMLProperty> properties
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
            case FXMLMapProperty(_, _, FXMLType type, _, _, Map<FXMLLiteral, AbstractFXMLValue> value) -> Stream.concat(
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

    /// Computes a stream of [ClassCount] objects for the specified [FXMLObject].
    ///
    /// @param object The [FXMLObject] to analyze
    /// @return A stream of [ClassCount] objects
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

    /// Computes a stream of [ClassCount] objects for the specified [FXMLMap].
    ///
    /// @param map The [FXMLMap] to analyze
    /// @return A stream of [ClassCount] objects
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

    /// Computes a stream of [ClassCount] objects for the specified [FXMLCollection].
    ///
    /// @param collection The [FXMLCollection] to analyze
    /// @return A stream of [ClassCount] objects
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

    /// Computes a stream of [ClassCount] objects for the specified [FXMLFactoryMethod].
    ///
    /// @param factoryMethod The [FXMLFactoryMethod] to analyze
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findFXMLFactoryMethodClassCount(FXMLFactoryMethod factoryMethod) {
        return findFXMLTypeClassCounts(factoryMethod.clazz());
    }

    /// Computes a stream of [ClassCount] objects for the specified [FXMLMethod].
    ///
    /// @param method The [FXMLMethod] to analyze
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findFXMLMethodClassCount(FXMLMethod method) {
        return Stream.concat(
                findFXMLTypeClassCounts(method.returnType()),
                method.parameters()
                        .stream()
                        .flatMap(this::findFXMLTypeClassCounts)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Computes a stream of [ClassCount] objects for the specified [FXMLController].
    ///
    /// @param controller The [FXMLController] to analyze
    /// @return A stream of [ClassCount] objects
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

    /// Computes a stream of [ClassCount] objects for the specified [FXMLControllerMethod].
    ///
    /// @param method The [FXMLControllerMethod] to analyze
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findClassCountControllerMethod(FXMLControllerMethod method) {
        return Stream.concat(
                findFXMLTypeClassCounts(method.returnType()),
                method.parameterTypes()
                        .stream()
                        .flatMap(this::findFXMLTypeClassCounts)
        ).gather(CLASS_COUNT_MERGER);
    }

    /// Analyzes the specified [FXMLType] and generates a stream of [ClassCount] objects.
    ///
    /// @param type The [FXMLType] to process
    /// @return A stream of [ClassCount] objects
    private Stream<ClassCount> findFXMLTypeClassCounts(FXMLType type) {
        return (switch (type) {
            case FXMLClassType(Class<?> clazz) -> Stream.of(new ClassCount(clazz.getCanonicalName(), 1));
            case FXMLGenericType(Class<?> clazz, List<FXMLType> typeArguments) -> Stream.concat(
                    Stream.of(new ClassCount(clazz.getCanonicalName(), 1)),
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

    /// Checks if the given prefix is a prefix of the specified full class name.
    ///
    /// @param fullClassName The full class name to check
    /// @param prefix        The prefix to verify
    /// @return `true` if the prefix matches; `false` otherwise
    private boolean isPrefix(String fullClassName, String prefix) {
        List<String> fullClassNameParts = splitClassName(fullClassName);
        List<String> prefixParts = splitClassName(prefix);
        return fullClassNameParts.size() >= prefixParts.size()
                && IntStream.range(0, prefixParts.size())
                .allMatch(i -> prefixParts.get(i).equals(fullClassNameParts.get(i)));
    }

    /// Splits a fully qualified class name into its individual parts.
    ///
    /// @param className The fully qualified class name to split
    /// @return A list of parts
    private List<String> splitClassName(String className) {
        List<String> parts = new ArrayList<>();
        int lastDotIndex = -1;
        int length = className.length();
        for (int i = 0; i < length; i++) {
            if (className.charAt(i) == '.') {
                parts.add(className.substring(lastDotIndex + 1, i));
                lastDotIndex = i;
            }
        }
        parts.add(className.substring(lastDotIndex + 1));
        return List.copyOf(parts);
    }

    /// Extracts the simple class name from a fully qualified class name.
    ///
    /// @param className The fully qualified name of the class
    /// @return The simple class name
    private String getSimpleClassName(String className) {
        int lastDotIndex = className.lastIndexOf('.');
        return lastDotIndex == -1 ? className : className.substring(lastDotIndex + 1);
    }
}
