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
///
/// This class is responsible for identifying and organizing the required class imports for a generated Java source file
/// from an [FXMLDocument].
/// It performs a recursive analysis of the entire FXML document structure, including the root element, definitions,
/// and the controller, to collect all referenced types.
///
/// The helper calculates the occurrence frequency of each class and uses this information to optimize the import
/// statements, potentially using fully qualified names for classes that would otherwise cause name collisions.
/// It also handles the `@Generated` annotation and manages recursive property bindings through a
/// [FXMLPropertyRecursionHelper].
final class FXMLSourceCodeBuilderImportHelper {
    /// A [Gatherer] used to aggregate and merge [ClassCount] objects into a map by their full class name.
    ///
    /// The gatherer maintains a [HashMap] as its internal state, where keys are fully qualified class names and values
    /// are the cumulative counts.
    /// During the integration step, it merges counts using [Integer#sum].
    /// In the finish step, it converts the map entries back into a stream of [ClassCount] objects.
    private static final Gatherer<ClassCount, Map<String, Integer>, ClassCount> CLASS_COUNT_MERGER = Gatherer.ofSequential(
            HashMap::new,
            (state, line, _) -> {
                state.merge(line.fullClassName(), line.count(), Integer::sum);
                return true;
            },
            (state, downstream) ->
                    state.forEach((key, value) -> downstream.push(new ClassCount(key, value)))
    );

    /// A utility for managing and resolving recursive property bindings within the FXML structure.
    ///
    /// This helper is used during the traversal of [FXMLProperty] objects to ensure that nested properties and their
    /// associated values are correctly processed without infinite recursion or missing depth.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Initializes a new [FXMLSourceCodeBuilderImportHelper] instance.
    ///
    /// This constructor sets up the internal [FXMLPropertyRecursionHelper] used for property traversal.
    FXMLSourceCodeBuilderImportHelper() {
        this.propertyRecursionHelper = new FXMLPropertyRecursionHelper();
    }

    /// Finds and organizes the class imports required for the specified [FXMLDocument].
    ///
    /// The process involves several steps:
    /// 1. Retrieve all class names defined within the document (e.g., the root class and any nested definitions).
    /// 2. Perform a comprehensive scan of the document to count the occurrences of every referenced class.
    /// 3. Group the identified classes based on common package/class prefixes to identify potential name collisions.
    /// 4. Sort the groups by total occurrence count in descending order.
    /// 5. Iterate through the groups to decide whether a class should be imported or used with its fully qualified name.
    ///    - If a class name collision is detected with a class defined in the document itself, the fully qualified name
    ///      is used.
    ///    - Otherwise, the class is added to the import list, and a mapping for its simple name is created.
    /// 6. Handle nested classes by mapping them relative to the root document class.
    ///
    /// @param document               The [FXMLDocument] to process
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation in the imports
    /// @return An [Imports] object containing the list of sorted import statements and a mapping of how each class
    /// should be referenced in the source code (either simple name or fully qualified)
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
    /// This method extracts the main class name of the document and then recursively collects class names from the
    /// root element and all definitions within the document.
    ///
    /// @param document The [FXMLDocument] whose class names are to be retrieved
    /// @return A [Stream] of fully qualified class names found in the document structure
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
    ///
    /// This method uses a pattern-matching switch to traverse various FXML value types:
    /// - [FXMLCollection]: Recurses into all values in the collection.
    /// - [FXMLMap]: Recurses into all values stored in the map entries.
    /// - [FXMLObject]: Uses the [propertyRecursionHelper] to walk through all properties and their values.
    /// - [FXMLInclude]: Recurses into the lazily loaded document's class names.
    /// - Other types (literals, references, etc.): Do not contribute document-defined class names.
    ///
    /// @param value The [AbstractFXMLValue] instance to process
    /// @return A stream of extracted fully qualified class names
    private Stream<String> getDocumentClassNames(AbstractFXMLValue value) {
        return switch (value) {
            case FXMLCollection(_, _, _, List<AbstractFXMLValue> values) -> values.stream()
                    .flatMap(this::getDocumentClassNames);
            case FXMLMap(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) -> entries.values()
                    .stream()
                    .flatMap(this::getDocumentClassNames);
            case FXMLObject(_, _, _, List<FXMLProperty> properties) -> propertyRecursionHelper.walk(
                    properties,
                    (v, _) -> getDocumentClassNames(v),
                    null
            );
            case FXMLInclude(_, _, _, _, FXMLLazyLoadedDocument lazyLoadedDocument) ->
                    getDocumentClassNames(lazyLoadedDocument.get());
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _,
                 FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> Stream.empty();
        };
    }

    /// Finds and returns a list of class counts derived from the specified [FXMLDocument].
    ///
    /// This method performs an exhaustive search for all types referenced in the document:
    /// 1. It scans the root element's entire structure for types.
    /// 2. It inspects the controller class and its fields/methods for referenced types.
    /// 3. It scans all document definitions for types.
    /// 4. Optionally, it adds the `@Generated` annotation to the count.
    /// All collected [ClassCount] objects are then merged using the [CLASS_COUNT_MERGER] to produce a final list of
    /// unique classes and their total occurrence counts.
    ///
    /// @param document               The [FXMLDocument] to analyze
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation in the analysis
    /// @return A list of unique [ClassCount] objects representing all referenced classes and their frequencies
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

    /// Builds a [GroupedClassCount] based on the specified map entry.
    ///
    /// This method takes a map entry representing a class group (identified by a prefix) and its associated classes.
    /// It calculates the total frequency of all classes within that group and constructs a new [GroupedClassCount].
    ///
    /// @param entry A map entry where the key is the group identifier (prefix) and the value is a list of [ClassCount]
    /// objects belonging to that group
    /// @return A [GroupedClassCount] object summarizing the group's name, total count, and individual classes
    private GroupedClassCount buildGroupedClassCount(Map.Entry<String, List<ClassCount>> entry) {
        List<ClassCount> classCountsForGroup = entry.getValue();
        int count = classCountsForGroup.stream()
                .mapToInt(ClassCount::count)
                .sum();
        return new GroupedClassCount(entry.getKey(), count, classCountsForGroup);
    }

    /// Groups a list of [ClassCount] objects based on class name prefixes.
    ///
    /// This method organizes classes into groups to help identify potential naming conflicts (e.g., classes from
    /// different packages with the same simple name).
    /// It iterates through each class count and either:
    /// - Adds it to an existing group if a matching prefix is found.
    /// - Creates a new group and potentially merges existing groups that are sub-prefixes of the new one.
    ///
    /// @param classCounts The list of [ClassCount] objects to be grouped
    /// @return An immutable map where keys are group identifiers (prefixes) and values are lists of [ClassCount] objects
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

    /// Creates a new group for the given class count and merges it with existing groups that share a prefix relationship.
    ///
    /// This method is called when a class does not fit into any existing prefix group.
    /// It:
    /// 1. Identifies all existing groups whose prefix is actually a prefix of the new class name.
    /// 2. Removes those groups from the map and collects their [ClassCount] objects.
    /// 3. Adds the new class count to the collected list.
    /// 4. Re-inserts the combined list under the new class name prefix.
    ///
    /// @param classCount         The [ClassCount] object triggering the new group creation
    /// @param groupedClassCounts The map containing current prefix groups to be updated
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
    /// This method recursively analyzes an FXML value to find all referenced types:
    /// - [FXMLCollection]: Delegates to [findFXMLCollectionClassCount].
    /// - [FXMLMap]: Delegates to [findFXMLMapClassCount].
    /// - [FXMLMethod]: Delegates to [findFXMLMethodClassCount].
    /// - [FXMLObject]: Delegates to [findFXMLObjectClassCount].
    /// - [FXMLValue]: Extracts types from the value's type definition.
    /// - [FXMLConstant]: Extracts types from the constant's class.
    /// - [FXMLInclude]: Recurses into the included document's root, controller, and definitions.
    /// - Other types: Return an empty stream as they don't contain additional type references.
    ///
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param value The [AbstractFXMLValue] to analyze
    /// @return A merged stream of [ClassCount] objects found within the value
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
    /// This method extracts type references from different property types:
    /// - [FXMLCollectionProperties]: Analyzes the collection type, child values, and nested properties.
    /// - [FXMLConstructorProperty]: Analyzes the property type and its value.
    /// - [FXMLMapProperty]: Analyzes the map type and all values within the map.
    /// - [FXMLObjectProperty]: Analyzes the object type and its value.
    /// - [FXMLStaticObjectProperty]: Analyzes the property type, the static class owning the property, and the value.
    ///
    /// All identified counts are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param property The [FXMLProperty] for which class counts should be computed
    /// @return A merged stream of [ClassCount] objects
    private Stream<ClassCount> findFXMLPropertyClassCount(FXMLProperty property) {
        return (switch (property) {
            case FXMLCollectionProperties(
                    _, _, FXMLType type, _, List<AbstractFXMLValue> value, _
            ) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    value.stream()
                            .flatMap(this::findAbstractFXMLValueClassCount)
            );
            case FXMLConstructorProperty(_, FXMLType type, AbstractFXMLValue value) -> Stream.concat(
                    findFXMLTypeClassCounts(type),
                    findAbstractFXMLValueClassCount(value)
            );
            case FXMLMapProperty(_, _, FXMLType type, _, _, Map<FXMLLiteral, AbstractFXMLValue> value, _) -> Stream.concat(
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
    /// This method extracts type references from the object's own type, its factory method (if any),
    /// and all of its properties.
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param object The [FXMLObject] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method extracts type references from the map's type, its factory method (if any),
    /// and all values stored in its entries.
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param map The [FXMLMap] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method extracts type references from the collection's type, its factory method (if any),
    /// and all values contained within the collection.
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param collection The [FXMLCollection] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method extracts type references from the class that defines the factory method.
    ///
    /// @param factoryMethod The [FXMLFactoryMethod] to analyze
    /// @return A stream of [ClassCount] objects from the factory method's owner class
    private Stream<ClassCount> findFXMLFactoryMethodClassCount(FXMLFactoryMethod factoryMethod) {
        return findFXMLTypeClassCounts(factoryMethod.clazz());
    }

    /// Computes a stream of [ClassCount] objects for the specified [FXMLMethod].
    ///
    /// This method extracts type references from the method's return type and all of its parameter types.
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param method The [FXMLMethod] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method extracts type references from the controller class itself, all of its fields (via their types),
    /// and all of its methods (via return and parameter types).
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param controller The [FXMLController] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method extracts type references from the controller method's return type and all of its parameter types.
    /// The results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param method The [FXMLControllerMethod] to analyze
    /// @return A merged stream of [ClassCount] objects
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
    /// This method handles different type implementations:
    /// - [FXMLClassType]: Extracts the canonical name of the Java class.
    /// - [FXMLGenericType]: Extracts the canonical name of the raw class and recurses into all type arguments.
    /// - [FXMLUncompiledClassType]: Extracts the class name string directly.
    /// - [FXMLUncompiledGenericType]: Extracts the raw class name and recurses into all type arguments.
    /// - [FXMLWildcardType]: Returns an empty stream.
    ///
    /// All results are merged using [CLASS_COUNT_MERGER].
    ///
    /// @param type The [FXMLType] to process
    /// @return A merged stream of [ClassCount] objects representing all types found within the given type definition
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

    /// Checks if the given prefix is a logical prefix of the specified full class name.
    ///
    /// The comparison is performed on a part-by-part basis (separated by dots) to ensure that partial name matches
    /// (e.g., `com.foo` matching `com.foobar`) are correctly rejected.
    ///
    /// @param fullClassName The fully qualified class name to check
    /// @param prefix        The prefix to verify
    /// @return `true` if the prefix matches the start of the class name part-by-part; `false` otherwise
    private boolean isPrefix(String fullClassName, String prefix) {
        List<String> fullClassNameParts = splitClassName(fullClassName);
        List<String> prefixParts = splitClassName(prefix);
        return fullClassNameParts.size() >= prefixParts.size()
                && IntStream.range(0, prefixParts.size())
                .allMatch(i -> prefixParts.get(i).equals(fullClassNameParts.get(i)));
    }

    /// Splits a fully qualified class name into its individual parts using the dot character as a delimiter.
    ///
    /// For example, `java.util.List` is split into `["java", "util", "List"]`.
    ///
    /// @param className The fully qualified class name to split
    /// @return A list of strings containing the individual parts of the class name
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
    /// This method returns everything after the last dot in the string.
    /// If no dot is present, the entire string is returned.
    ///
    /// @param className The fully qualified name of the class
    /// @return The simple class name (e.g., "List" for "java.util.List")
    private String getSimpleClassName(String className) {
        int lastDotIndex = className.lastIndexOf('.');
        return lastDotIndex == -1 ? className : className.substring(lastDotIndex + 1);
    }
}
