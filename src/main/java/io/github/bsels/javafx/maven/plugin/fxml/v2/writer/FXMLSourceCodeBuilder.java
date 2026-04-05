package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.CheckAndCast;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLObject;
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
import io.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.processing.Generated;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A builder for generating Java source code from an [FXMLDocument].
/// This class handles the conversion of FXML elements, properties, and values into a corresponding Java class
/// that can be instantiated to create the defined object graph.
public final class FXMLSourceCodeBuilder {
    /// Internal constant for the resource bundle field name.
    private static final String INTERNAL_RESOURCE_BUNDLE = "$INTERNAL$RESOURCE$BUNDLE$";
    /// Internal constant for the string-to-file conversion method name.
    private static final String INTERNAL_STRING_TO_FILE_METHOD = "$internalMethod$stringToFile$";
    /// Internal constant for the string-to-path conversion method name.
    private static final String INTERNAL_STRING_TO_PATH_METHOD = "$internalMethod$stringToPath$";
    /// Internal constant for the string-to-uri conversion method name.
    private static final String INTERNAL_STRING_TO_URI_METHOD = "$internalMethod$stringToURI$";
    /// Internal constant for the string-to-url conversion method name.
    private static final String INTERNAL_STRING_TO_URL_METHOD = "$internalMethod$stringToURL$";
    /// Internal constant for the controller field name.
    private static final String INTERNAL_CONTROLLER_FIELD = "$internalField$controller$";
    /// Internal constant for constructor variable prefix.
    private static final String CONSTRUCTOR_VARIABLE_PREFIX = "$$";
    /// A set of Java primitive types.
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "boolean", "byte", "char", "short", "int", "long", "float", "double"
    );
    /// The method body for internal string-to-file conversion.
    private static final String INTERNAL_STRING_TO_FILE_METHOD_BODY = """
            private java.io.File %s(String value) {
                try {
                    return new java.io.File(
                            getClass()
                                    .getResource(value)
                                    .toURI()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            """.formatted(INTERNAL_STRING_TO_FILE_METHOD);
    /// The method body for internal string-to-path conversion.
    private static final String INTERNAL_STRING_TO_PATH_METHOD_BODY = """
            private java.nio.file.Path %s(String value) {
                try {
                    return java.nio.file.Paths.get(
                            getClass()
                                    .getResource(value)
                                    .toURI()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            """.formatted(INTERNAL_STRING_TO_PATH_METHOD);
    /// The method body for internal string-to-URI conversion.
    private static final String INTERNAL_STRING_TO_URI_METHOD_BODY = """
            private java.net.URI %s(String value) {
                try {
                    return getClass()
                            .getResource(value)
                            .toURI();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            """.formatted(INTERNAL_STRING_TO_URI_METHOD);
    /// The method body for internal string-to-URL conversion.
    private static final String INTERNAL_STRING_TO_URL_METHOD_BODY = """
            private java.net.URL %s(String value) {
                return java.util.Objects.requireNonNull(
                        getClass()
                                .getResource(value)
                );
            }
            """.formatted(INTERNAL_STRING_TO_URL_METHOD);
    /// The logger for outputting generation information and debugging messages.
    private final Log log;
    /// Helper for managing and adding imports to the generated source code.
    private final FXMLSourceCodeBuilderImportHelper builderImportHelper;
    /// Helper for handling type conversions and literal encoding.
    private final FXMLSourceCodeBuilderTypeHelper typeHelper;
    /// Flag indicating whether the `@Generated` annotation should be added.
    private final boolean addGeneratedAnnotation;
    /// The time at which the build started, used for the `@Generated` annotation.
    private final ZonedDateTime buildTime;
    /// The default resource bundle to use for translations.
    private final String defaultResourceBundle;
    /// Helper instance used for managing and resolving recursive property bindings when working with FXML.
    /// This ensures proper handling of nested property structures and prevents infinite recursion during the binding
    /// and lookup process.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Constructs a new `FXMLSourceCodeBuilder`.
    ///
    /// @param log                    The Maven plugin logger.
    /// @param defaultResourceBundle  The default resource bundle to use for translations.
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation.
    /// @throws NullPointerException If `log` or `defaultResourceBundle` is null.
    public FXMLSourceCodeBuilder(Log log, String defaultResourceBundle, boolean addGeneratedAnnotation)
            throws NullPointerException {
        this.log = Objects.requireNonNull(log, "`log` must not be null");
        this.defaultResourceBundle = Objects.requireNonNull(
                defaultResourceBundle,
                "`defaultResourceBundle` must not be null"
        );
        this.builderImportHelper = new FXMLSourceCodeBuilderImportHelper();
        this.typeHelper = new FXMLSourceCodeBuilderTypeHelper();
        this.propertyRecursionHelper = new FXMLPropertyRecursionHelper();
        this.addGeneratedAnnotation = addGeneratedAnnotation;
        this.buildTime = ZonedDateTime.now(ZoneOffset.UTC);
    }

    /// Generates the Java source code for the given [FXMLDocument].
    ///
    /// @param document    The [FXMLDocument] to process.
    /// @param packageName The package name for the generated class.
    /// @return The generated Java source code as a string.
    /// @throws NullPointerException If `document` is null.
    public String generateSourceCode(FXMLDocument document, String packageName) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        log.debug("Generating source code for FXML document with classname: %s".formatted(document.className()));

        Map<String, FXMLType> identifierToTypeMap = typeHelper.createIdentifierToTypeMap(document);
        SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                builderImportHelper.findImports(document, addGeneratedAnnotation),
                defaultResourceBundle,
                identifierToTypeMap
        );

        return internalGenerateSourceCode(document, context, packageName, true);
    }

    /// Internal method to generate source code, allowing for recursive processing of nested FXML documents.
    ///
    /// @param document       The [FXMLDocument] being processed.
    /// @param context        The current [SourceCodeGeneratorContext].
    /// @param packageName    The package name for the class.
    /// @param isRootDocument Whether this is the root FXML document or an included one.
    /// @return The generated Java source code.
    private String internalGenerateSourceCode(
            FXMLDocument document,
            SourceCodeGeneratorContext context,
            String packageName,
            boolean isRootDocument
    ) {

        addPackageLine(context, packageName);
        addImports(context);
        addFields(context, document);
        addConstructorPrologue(context, document);
        addConstructorEpilogue(context, document);
        addInnerClass(document, context);
        // TODO: Add methods

        // The class declaration should be done last
        addClassDeclaration(context, document, isRootDocument);
        return generateSourceCode(context, document.className(), isRootDocument, addGeneratedAnnotation);
    }

    /// Assembles the final source code from the gathered [SourceCodeGeneratorContext].
    ///
    /// @param context                The [SourceCodeGeneratorContext].
    /// @param className              The name of the class being generated.
    /// @param isRootDocument         Whether this is the root FXML document.
    /// @param addGeneratedAnnotation Whether to include the `@Generated` annotation.
    /// @return The assembled Java source code.
    private String generateSourceCode(
            SourceCodeGeneratorContext context,
            String className,
            boolean isRootDocument,
            boolean addGeneratedAnnotation
    ) {
        StringBuilder sourceCode = new StringBuilder();
        if (isRootDocument) {
            sourceCode.append(context.sourceCode(SourcePart.PACKAGE))
                    .append(context.sourceCode(SourcePart.IMPORTS));
        }

        if (isRootDocument && addGeneratedAnnotation) {
            sourceCode.append('@')
                    .append(typeHelper.typeToSourceCode(context, FXMLType.of(Generated.class)))
                    .append("(\n    value=\"")
                    .append(FXMLSourceCodeBuilder.class.getName())
                    .append("\",\n    date=\"")
                    .append(buildTime)
                    .append("\"\n)\n");
        }

        sourceCode.append(context.sourceCode(SourcePart.CLASS_DECLARATION)).append(" {\n");
        if (context.hasFeature(Feature.RESOURCE_BUNDLE)) {
            sourceCode.append("private static final java.util.ResourceBundle ")
                    .append(INTERNAL_RESOURCE_BUNDLE)
                    .append(" = ")
                    .append(context.resourceBundle())
                    .append(";\n\n");
        }
        sourceCode.append(context.sourceCode(SourcePart.FIELDS).toString().indent(4));

        if (context.hasFeature(Feature.ABSTRACT_CLASS)) {
            sourceCode.append("protected".indent(4).stripTrailing());
        } else {
            sourceCode.append("public".indent(4).stripTrailing());
        }

        sourceCode.append(" ").append(className).append("() {\n")
                .append(context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE).toString().indent(8))
                .append(context.sourceCode(SourcePart.CONSTRUCTOR_SUPER_CALL).toString().indent(8))
                .append(context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE).toString().indent(8))
                .append("}\n".indent(4))
                .append('\n')
                .append(context.sourceCode(SourcePart.METHODS).toString().indent(4));

        if (context.hasFeature(Feature.STRING_TO_URL_METHOD)) {
            sourceCode.append(INTERNAL_STRING_TO_URL_METHOD_BODY.indent(4))
                    .append('\n');
        }
        if (context.hasFeature(Feature.STRING_TO_URI_METHOD)) {
            sourceCode.append(INTERNAL_STRING_TO_URI_METHOD_BODY.indent(4))
                    .append('\n');
        }
        if (context.hasFeature(Feature.STRING_TO_PATH_METHOD)) {
            sourceCode.append(INTERNAL_STRING_TO_PATH_METHOD_BODY.indent(4))
                    .append('\n');
        }
        if (context.hasFeature(Feature.STRING_TO_FILE_METHOD)) {
            sourceCode.append(INTERNAL_STRING_TO_FILE_METHOD_BODY.indent(4))
                    .append('\n');
        }

        if (isRootDocument) {
            sourceCode.append(context.sourceCode(SourcePart.NESTED_TYPES).toString().indent(4))
                    .append("}\n");
        } else {
            sourceCode.append("}\n\n")
                    .append(context.sourceCode(SourcePart.NESTED_TYPES));
        }

        return sourceCode.toString();
    }

    /// Checks if a given Java type is a primitive type.
    ///
    /// @param type the type to check.
    /// @return `true` if the type is primitive, `false` otherwise.
    private boolean isPrimitive(String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    /// Adds import statements to the source code context.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    private void addImports(SourceCodeGeneratorContext context) {
        List<String> importList = context.imports()
                .imports()
                .stream()
                .filter(Predicate.not(this::isPrimitive))
                .sorted()
                .toList();
        StringBuilder sourceCode = context.sourceCode(SourcePart.IMPORTS);
        for (String importStatement : importList) {
            sourceCode.append("import ").append(importStatement).append(";\n");
        }
        sourceCode.append("\n");
    }

    /// Adds the package declaration to the source code context.
    ///
    /// @param context     The current [SourceCodeGeneratorContext].
    /// @param packageName The package name to declare.
    private void addPackageLine(SourceCodeGeneratorContext context, String packageName) {
        if (packageName != null) {
            context.sourceCode(SourcePart.PACKAGE).append("package ").append(packageName).append(";\n\n");
        }
    }

    /// Adds field declarations to the source code context for the given FXML document.
    ///
    /// @param context  The current [SourceCodeGeneratorContext].
    /// @param document The [FXMLDocument] whose fields should be added.
    private void addFields(SourceCodeGeneratorContext context, FXMLDocument document) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.FIELDS);
        Stream.concat(
                        document.controller()
                                .stream()
                                .map(c -> "private final %s %s;\n".formatted(
                                        typeHelper.typeToSourceCode(context, c.controllerClass()),
                                        INTERNAL_CONTROLLER_FIELD
                                )),
                        Stream.concat(
                                addFields(document.root(), context),
                                document.definitions()
                                        .stream()
                                        .flatMap(d -> addFields(d, context))
                        )
                )
                .sorted()
                .reduce(sourceCode, StringBuilder::append, StringBuilder::append)
                .append("\n");
    }

    /// Adds a field declaration to the current source code context for an FXML value.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] to process for fields.
    /// @return A stream of field declaration strings.
    private Stream<String> addFields(AbstractFXMLValue value, SourceCodeGeneratorContext context) {
        return switch (value) {
            case FXMLCollection(FXMLIdentifier identifier, FXMLType type, _, List<AbstractFXMLValue> values) ->
                    Stream.concat(
                            identifierToField(context, identifier, type),
                            values.stream().flatMap(v -> addFields(v, context))
                    );
            case FXMLCopy(FXMLIdentifier identifier, FXMLExposedIdentifier(String name)) ->
                    identifierToField(context, identifier, context.identifierToTypeMap().get(name));
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, FXMLLazyLoadedDocument document) -> {
                FXMLDocument fxmlDocument = document.get();
                yield Stream.concat(
                        identifierToField(context, identifier, new FXMLUncompiledClassType(fxmlDocument.className())),
                        fxmlDocument.controller()
                                .stream()
                                .flatMap(controller -> identifierToField(
                                                context,
                                                new FXMLExposedIdentifier(identifier + "Controller"),
                                                controller.controllerClass()
                                        )
                                )
                );
            }
            case FXMLMap(
                    FXMLIdentifier identifier, FXMLType type, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> Stream.concat(
                    identifierToField(context, identifier, type),
                    entries.values().stream().flatMap(v -> addFields(v, context))
            );
            case FXMLObject(FXMLIdentifier identifier, FXMLType type, _, List<FXMLProperty> properties) ->
                    Stream.concat(
                            identifierToField(context, identifier, type),
                            properties.stream().flatMap(
                                    property -> propertyRecursionHelper.walk(property, this::addFields, context)
                            )
                    );
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.empty();
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType type, _) -> identifier.stream()
                    .flatMap(id -> identifierToField(context, id, type));
        };
    }

    /// Converts an [FXMLIdentifier] and [FXMLType] into a field declaration string.
    ///
    /// @param context    The current [SourceCodeGeneratorContext].
    /// @param identifier The [FXMLIdentifier] for the field.
    /// @param type       The [FXMLType] for the field.
    /// @return A stream containing the field declaration string, or an empty stream if no field is needed.
    private Stream<String> identifierToField(SourceCodeGeneratorContext context, FXMLIdentifier identifier, FXMLType type) {
        return switch (identifier) {
            case FXMLExposedIdentifier(String name) ->
                    Stream.of("protected final %s %s;\n".formatted(typeHelper.typeToSourceCode(context, type), name));
            case FXMLInternalIdentifier id ->
                    Stream.of("private final %s %s;\n".formatted(typeHelper.typeToSourceCode(context, type), id));
            case FXMLNamedRootIdentifier _, FXMLRootIdentifier _ -> Stream.empty();
        };
    }

    /// Adds the class declaration to the source code context.
    ///
    /// @param context        The current [SourceCodeGeneratorContext].
    /// @param document       The [FXMLDocument] being declared.
    /// @param isRootDocument Whether this is the root FXML document.
    /// @throws UnsupportedOperationException If an abstract inner class is requested for an included FXML.
    private void addClassDeclaration(SourceCodeGeneratorContext context, FXMLDocument document, boolean isRootDocument) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CLASS_DECLARATION);
        if (isRootDocument) {
            sourceCode.append("public ");
            if (context.hasFeature(Feature.ABSTRACT_CLASS)) {
                sourceCode.append("abstract ");
            }
        } else {
            sourceCode.append("private static ");
            if (context.hasFeature(Feature.ABSTRACT_CLASS)) {
                throw new UnsupportedOperationException(
                        "Abstract inner classes for included FXML documents are not supported."
                );
            }
        }
        sourceCode.append("class ")
                .append(document.className())
                .append(" extends ")
                .append(typeHelper.typeToSourceCode(context, document.root().type()))
        // TODO: Add interfaces
        ;
    }

    /// Adds the constructor prologue to the generated source code.
    ///
    /// @param context  The current [SourceCodeGeneratorContext].
    /// @param document The [FXMLDocument] to process.
    private void addConstructorPrologue(SourceCodeGeneratorContext context, FXMLDocument document) {
        List<Constructions> constructions = Stream.concat(
                        findConstructions(document.root(), context),
                        document.definitions()
                                .stream()
                                .flatMap(d -> findConstructions(d, context))
                )
                .collect(Collectors.toCollection(LinkedList::new));
        Set<FXMLIdentifier> constructorDependencies = constructions.stream()
                .map(Constructions::dependencies)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Constructions superCall = constructions.removeFirst();
        List<Constructions> orderedConstructions = resolveConstructionOrder(
                constructions);
        prepareArgumentsLists(
                context.sourceCode(SourcePart.CONSTRUCTOR_SUPER_CALL)
                        .append("super("),
                superCall,
                context
        ).append(");");
        addFieldCreationPrologue(context, orderedConstructions, constructorDependencies);
    }

    /// Adds field creation logic to the constructor prologue.
    ///
    /// @param context                 The current [SourceCodeGeneratorContext].
    /// @param orderedConstructions    The list of constructions in their resolved order.
    /// @param constructorDependencies The internal constructor dependencies.
    private void addFieldCreationPrologue(
            SourceCodeGeneratorContext context,
            List<Constructions> orderedConstructions,
            Set<FXMLIdentifier> constructorDependencies
    ) {
        for (Constructions construction : orderedConstructions) {
            switch (construction) {
                case AbstractFXMLObjectAndDependencies(AbstractFXMLObject object, _, _) -> {
                    String typeString = typeHelper.typeToSourceCode(context, object.type());
                    addPrologue(
                            context,
                            constructorDependencies,
                            typeString,
                            object.identifier(),
                            s -> prepareArgumentsLists(
                                    object.factoryMethod()
                                            .map(
                                                    method -> s.append(typeHelper.typeToSourceCode(
                                                                    context,
                                                                    method.clazz()
                                                            ))
                                                            .append('.')
                                                            .append(method.method()))
                                            .orElseGet(() -> s.append("new ").append(typeString))
                                            .append('('),
                                    construction,
                                    context
                            ).append(")")
                    );
                }
                case FXMLCopyAndDependencies(FXMLCopy(FXMLIdentifier identifier, FXMLExposedIdentifier source), _) -> {
                    FXMLType type = context.identifierToTypeMap().get(source.name());
                    String typeString = typeHelper.typeToSourceCode(context, type);
                    addPrologue(
                            context,
                            constructorDependencies,
                            typeString,
                            identifier,
                            s -> prepareArgumentsLists(
                                    s.append("new ").append(typeString).append('('),
                                    construction,
                                    context
                            ).append(')')

                    );
                }
                case FXMLIncludeAndDependencies(
                        FXMLInclude(FXMLIdentifier identifier, _, _, _, FXMLLazyLoadedDocument document), _
                ) -> {
                    FXMLUncompiledClassType type = new FXMLUncompiledClassType(document.get().className());
                    String typeString = typeHelper.typeToSourceCode(context, type);
                    addPrologue(
                            context,
                            constructorDependencies,
                            typeString,
                            identifier,
                            s -> s.append("new ").append(typeString).append("()")
                    );
                }
                case FXMLValueConstruction(
                        FXMLValue(Optional<FXMLIdentifier> id, FXMLType type, String value), _
                ) -> id.ifPresent(identifier -> addPrologue(
                        context,
                        constructorDependencies,
                        typeHelper.typeToSourceCode(context, type),
                        identifier,
                        s -> s.append(typeHelper.encodeLiteral(context, value, type))
                ));
            }
        }
    }

    /// Adds the prologue section for the source code generation process.
    /// This includes initializing the specified identifier and optionally binding it to a field if it is marked
    /// as a dependency.
    ///
    /// @param context                 The context of the source code generator, used to retrieve the target source code section.
    /// @param constructorDependencies A set of identifiers that are dependencies in the constructor.
    /// @param typeString              The type of the identifier being added to the prologue (e.g., class name or primitive type).
    /// @param identifier              The FXML identifier to be initialized in the prologue.
    /// @param sourceCodeConsumer      A consumer that appends additional source code logic to the initialization.
    private void addPrologue(
            SourceCodeGeneratorContext context,
            Set<FXMLIdentifier> constructorDependencies,
            String typeString,
            FXMLIdentifier identifier,
            Consumer<StringBuilder> sourceCodeConsumer
    ) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE);
        boolean isDependency = constructorDependencies.contains(identifier);
        if (isDependency) {
            sourceCode.append(typeString)
                    .append(' ')
                    .append(CONSTRUCTOR_VARIABLE_PREFIX);
        }
        sourceCode.append(identifier)
                .append(" = ");
        sourceCodeConsumer.accept(sourceCode);
        sourceCode.append(";\n");
        if (isDependency) {
            bindPrologueVariableToField(sourceCode, identifier);
        }
    }

    /// Binds a prologue variable to its corresponding field.
    ///
    /// @param sourceCode     The [StringBuilder] containing the source code.
    /// @param fxmlIdentifier The [FXMLIdentifier] of the field to bind.
    private void bindPrologueVariableToField(StringBuilder sourceCode, FXMLIdentifier fxmlIdentifier) {
        sourceCode.append(fxmlIdentifier)
                .append(" = ")
                .append(CONSTRUCTOR_VARIABLE_PREFIX)
                .append(fxmlIdentifier)
                .append(";\n");
    }

    /// Resolves the construction order of FXML objects based on their dependencies.
    ///
    /// @param constructions The list of constructions to reorder.
    /// @return A list of constructions in an order that satisfies all dependencies.
    /// @throws IllegalArgumentException If cyclic dependencies are detected.
    private List<Constructions> resolveConstructionOrder(List<Constructions> constructions)
            throws IllegalArgumentException {
        List<Constructions> orderedConstructions = new ArrayList<>();
        Set<FXMLIdentifier> seenIdentifiers = new HashSet<>();
        int oldSize = -1;
        int newSize = 0;
        while (!constructions.isEmpty() && (oldSize != newSize)) {
            Iterator<Constructions> iterator = constructions.iterator();
            oldSize = seenIdentifiers.size();
            while (iterator.hasNext()) {
                Constructions next = iterator.next();
                if (seenIdentifiers.containsAll(next.dependencies())) {
                    iterator.remove();
                    orderedConstructions.add(next);
                    switch (next) {
                        case AbstractFXMLObjectAndDependencies(AbstractFXMLObject object, _, _) ->
                                seenIdentifiers.add(object.identifier());
                        case FXMLCopyAndDependencies(FXMLCopy copy, _) -> seenIdentifiers.add(copy.identifier());
                        case FXMLIncludeAndDependencies(FXMLInclude include, _) ->
                                seenIdentifiers.add(include.identifier());
                        case FXMLValueConstruction(FXMLValue(Optional<FXMLIdentifier> identifier, _, _), _) ->
                                identifier.ifPresent(seenIdentifiers::add);
                    }
                }
            }
            newSize = seenIdentifiers.size();
        }
        if (!constructions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cyclic dependencies detected in FXML document: %s".formatted(constructions)
            );
        }
        return orderedConstructions;
    }

    /// Prepares the argument list for a construction call.
    ///
    /// @param builder       The [StringBuilder] to append the argument list to.
    /// @param constructions The construction for which to prepare arguments.
    /// @param context       The current [SourceCodeGeneratorContext].
    /// @return The same [StringBuilder] instance with the argument list appended.
    private StringBuilder prepareArgumentsLists(
            StringBuilder builder,
            Constructions constructions,
            SourceCodeGeneratorContext context
    ) {
        return switch (constructions) {
            case AbstractFXMLObjectAndDependencies(
                    AbstractFXMLObject object,
                    List<FXMLConstructorProperty> properties,
                    _
            ) -> {
                FXMLConstructor constructor = object.factoryMethod()
                        .map(method -> typeHelper.findFactoryMethodConstructor(method, properties))
                        .orElseGet(() -> typeHelper.findMinimalConstructor(
                                FXMLUtils.findRawType(object.type()),
                                properties
                        ));
                Map<String, FXMLConstructorProperty> propertyMap = properties.stream()
                        .collect(Collectors.toMap(FXMLConstructorProperty::name, Function.identity()));
                boolean first = true;
                for (ConstructorProperty property : constructor.properties()) {
                    if (!first) {
                        builder.append(", ");
                    }
                    first = false;
                    FXMLConstructorProperty fxmlConstructorProperty = propertyMap.get(property.name());
                    if (fxmlConstructorProperty == null) {
                        builder.append(
                                property.defaultValue()
                                        .map(v -> encodeFXMLValue(context, v, property.type()))
                                        .orElseGet(() -> typeHelper.defaultTypeValue(property.type()))
                        );
                    } else {
                        builder.append(encodeFXMLValue(
                                context, CONSTRUCTOR_VARIABLE_PREFIX, fxmlConstructorProperty.value(), property.type()
                        ));
                    }
                }
                yield builder;
            }
            case FXMLCopyAndDependencies(FXMLCopy(_, FXMLExposedIdentifier(String name)), _) ->
                    builder.append("%s%s".formatted(CONSTRUCTOR_VARIABLE_PREFIX, name));
            case FXMLIncludeAndDependencies _, FXMLValueConstruction _ -> builder;
        };
    }

    /// Identifies dependencies for the construction of an FXML object.
    ///
    /// @param object The [AbstractFXMLObject] to analyze.
    /// @return An [AbstractFXMLObjectAndDependencies] containing the object, its constructor properties, and its dependencies.
    private AbstractFXMLObjectAndDependencies findDependenciesForObjectConstruction(AbstractFXMLObject object) {
        return switch (object) {
            case FXMLCollection fxmlCollection ->
                    new AbstractFXMLObjectAndDependencies(fxmlCollection, List.of(), List.of());
            case FXMLMap fxmlMap -> new AbstractFXMLObjectAndDependencies(fxmlMap, List.of(), List.of());
            case FXMLObject fxmlObject -> {
                List<FXMLConstructorProperty> constructorProperties = fxmlObject.properties()
                        .stream()
                        .gather(CheckAndCast.of(FXMLConstructorProperty.class))
                        .toList();
                List<FXMLIdentifier> dependencies = constructorProperties.stream()
                        .map(FXMLConstructorProperty::value)
                        .map(this::findIdentifierForValue)
                        .gather(Utils.optional())
                        .toList();
                yield new AbstractFXMLObjectAndDependencies(fxmlObject, constructorProperties, dependencies);
            }
        };
    }

    /// Finds the identifier associated with an FXML value, if any.
    ///
    /// @param value The [AbstractFXMLValue] to inspect.
    /// @return An [Optional] containing the [FXMLIdentifier] if found, or empty otherwise.
    private Optional<FXMLIdentifier> findIdentifierForValue(AbstractFXMLValue value) {
        return switch (value) {
            case AbstractFXMLObject abstractFXMLObject -> Optional.of(abstractFXMLObject.identifier());
            case FXMLCopy(FXMLIdentifier identifier, _) -> Optional.of(identifier);
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, _) -> Optional.of(identifier);
            case FXMLReference(String name) -> Optional.of(new FXMLExposedIdentifier(name));
            case FXMLValue(Optional<FXMLIdentifier> identifier, _, _) -> identifier;
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLResource _,
                 FXMLTranslation _ -> Optional.empty();
        };
    }

    /// Recursively finds all constructions and their dependencies for an FXML value.
    ///
    /// @param value   The [AbstractFXMLValue] to analyze.
    /// @param context The current [SourceCodeGeneratorContext].
    /// @return A stream of [Constructions] representing the necessary construction steps.
    private Stream<Constructions> findConstructions(AbstractFXMLValue value, SourceCodeGeneratorContext context) {
        return switch (value) {
            case FXMLCollection fxmlCollection -> Stream.concat(
                    Stream.of(findDependenciesForObjectConstruction(fxmlCollection)),
                    fxmlCollection.values()
                            .stream()
                            .flatMap(v -> findConstructions(v, context))
            );
            case FXMLMap fxmlMap -> Stream.concat(
                    Stream.of(findDependenciesForObjectConstruction(fxmlMap)),
                    fxmlMap.entries()
                            .values()
                            .stream()
                            .flatMap(v -> findConstructions(v, context))
            );
            case FXMLObject fxmlObject -> Stream.concat(
                    Stream.of(findDependenciesForObjectConstruction(fxmlObject)),
                    fxmlObject.properties()
                            .stream()
                            .flatMap(property -> propertyRecursionHelper.walk(
                                    property,
                                    this::findConstructions,
                                    context
                            ))
            );
            case FXMLCopy copy -> Stream.of(new FXMLCopyAndDependencies(copy, List.of(copy.source())));
            case FXMLInclude include -> Stream.of(new FXMLIncludeAndDependencies(include, List.of()));
            case FXMLValue fxmlValue -> fxmlValue.identifier()
                    .stream()
                    .map(_ -> new FXMLValueConstruction(fxmlValue, List.of()));
            case FXMLMethod _, FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.empty();
        };
    }

    /// Adds the constructor epilogue (object initialization and property settings) for all definitions in a document.
    ///
    /// @param context  The current [SourceCodeGeneratorContext].
    /// @param document The [FXMLDocument] to process.
    private void addConstructorEpilogue(SourceCodeGeneratorContext context, FXMLDocument document) {
        addConstructorEpilogue(context, document.root());
        document.definitions()
                .forEach(d -> addConstructorEpilogue(context, d));
    }

    /// Adds the constructor epilogue for a specific FXML value.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] to process.
    private void addConstructorEpilogue(SourceCodeGeneratorContext context, AbstractFXMLValue value) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE);
        switch (value) {
            case FXMLCollection(
                    FXMLIdentifier identifier,
                    _,
                    _,
                    List<AbstractFXMLValue> values
            ) -> {
                values.forEach(v -> sourceCode.append(identifier.toString())
                        .append(".add(")
                        .append(encodeFXMLValue(context, v, FXMLType.of(Object.class)))
                        .append(");\n"));
                values.forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLMap(
                    FXMLIdentifier identifier,
                    _,
                    FXMLClassType keyType,
                    FXMLClassType valueType,
                    _,
                    Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> {
                entries.forEach((k, v) -> sourceCode.append(identifier.toString())
                        .append(".put(")
                        .append(encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(keyType)))
                        .append(", ")
                        .append(encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(valueType)))
                        .append(");\n"));
                entries.values().forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLObject(
                    FXMLIdentifier identifier,
                    _,
                    _,
                    List<FXMLProperty> properties
            ) -> properties.forEach(p -> addConstructorEpilogue(context, identifier.toString(), p));
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInclude _, FXMLInlineScript _, FXMLLiteral _,
                 FXMLMethod _, FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> {
            }
        }
    }

    /// Adds the constructor epilogue for a specific FXML property.
    ///
    /// @param context    The current [SourceCodeGeneratorContext].
    /// @param identifier The identifier of the object the property belongs to.
    /// @param property   The [FXMLProperty] to process.
    private void addConstructorEpilogue(SourceCodeGeneratorContext context, String identifier, FXMLProperty property) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE);
        switch (property) {
            case FXMLCollectionProperties(
                    _,
                    String getter,
                    FXMLType type,
                    List<AbstractFXMLValue> value,
                    List<FXMLProperty> properties
            ) -> {
                String collectionSelection = "%s.%s()".formatted(identifier, getter);
                value.forEach(v -> sourceCode.append(collectionSelection)
                        .append(".add(")
                        .append(encodeFXMLValue(context, v, type))
                        .append(");\n"));
                value.forEach(v -> addConstructorEpilogue(context, v));
                properties.forEach(p -> addConstructorEpilogue(context, collectionSelection, p));
            }
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> addConstructorEpilogue(context, value);
            case FXMLMapProperty(
                    _,
                    String getter,
                    _,
                    FXMLClassType rawKeyClass,
                    FXMLClassType rawValueClass,
                    Map<FXMLLiteral, AbstractFXMLValue> value
            ) -> {
                value.forEach((k, v) -> sourceCode.append(identifier)
                        .append('.')
                        .append(getter)
                        .append("().put(")
                        .append(encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(rawKeyClass)))
                        .append(", ")
                        .append(encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(rawValueClass)))
                        .append(");\n"));
                value.values().forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLObjectProperty(
                    _,
                    String setter,
                    FXMLType type,
                    AbstractFXMLValue value
            ) -> {
                sourceCode.append(identifier)
                        .append('.')
                        .append(setter)
                        .append("(")
                        .append(encodeFXMLValue(context, value, type))
                        .append(");\n");
                addConstructorEpilogue(context, value);
            }
            case FXMLStaticObjectProperty(
                    _,
                    FXMLClassType clazz,
                    String setter,
                    FXMLType type,
                    AbstractFXMLValue value
            ) -> {
                sourceCode.append(typeHelper.typeToSourceCode(context, clazz))
                        .append('.')
                        .append(setter)
                        .append("(")
                        .append(identifier)
                        .append(", ")
                        .append(encodeFXMLValue(context, value, type))
                        .append(");\n");
                addConstructorEpilogue(context, value);
            }
        }
    }

    /// Encodes an FXML value for use in Java source code.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] to encode.
    /// @param type    The expected [FXMLType] of the value.
    /// @return The Java source code representation of the value.
    /// @throws UnsupportedOperationException If the value type is not yet supported.
    /// @throws IllegalArgumentException      If the value cannot be converted to the required type.
    private String encodeFXMLValue(SourceCodeGeneratorContext context, AbstractFXMLValue value, FXMLType type) {
        return encodeFXMLValue(context, "", value, type);
    }

    /// Encodes an FXML value for use in Java source code.
    ///
    /// @param context          The current [SourceCodeGeneratorContext].
    /// @param identifierPrefix The prefix to use for generated identifiers.
    /// @param value            The [AbstractFXMLValue] to encode.
    /// @param type             The expected [FXMLType] of the value.
    /// @return The Java source code representation of the value.
    /// @throws UnsupportedOperationException If the value type is not yet supported.
    /// @throws IllegalArgumentException      If the value cannot be converted to the required type.
    private String encodeFXMLValue(
            SourceCodeGeneratorContext context,
            String identifierPrefix,
            AbstractFXMLValue value,
            FXMLType type
    ) {
        return switch (value) {
            case AbstractFXMLObject object -> "%s%s".formatted(identifierPrefix, object.identifier());
            case FXMLConstant(FXMLClassType clazz, String identifier, _) ->
                    "%s.%s".formatted(typeHelper.typeToSourceCode(context, clazz), identifier);
            case FXMLCopy(FXMLIdentifier identifier, _) -> "%s%s".formatted(identifierPrefix, identifier);
            case FXMLExpression _ -> throw new UnsupportedOperationException("Expression values are not supported yet");
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, _) -> "%s%s".formatted(identifierPrefix, identifier);
            case FXMLInlineScript _ ->
                    throw new UnsupportedOperationException("Inline script values are not supported yet");
            case FXMLLiteral(String literal) -> typeHelper.encodeLiteral(context, literal, type);
            case FXMLMethod(String name, _, _) -> "this::%s".formatted(name);
            case FXMLReference(String name) -> name;
            case FXMLResource(String resource) -> {
                resource = typeHelper.encodeString(resource);
                if (type instanceof FXMLClassType(Class<?> clazz)) {
                    if (clazz.isAssignableFrom(String.class)) {
                        yield resource;
                    }
                    if (URI.class.equals(clazz)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_URI_METHOD, resource);
                    }
                    if (URL.class.equals(clazz)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_URL_METHOD, resource);
                    }
                    if (clazz.isAssignableFrom(Path.class)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_PATH_METHOD, resource);
                    }
                    if (clazz.isAssignableFrom(File.class)) {
                        yield "%s(%s)".formatted(INTERNAL_STRING_TO_FILE_METHOD, resource);
                    }
                }
                throw new IllegalArgumentException(
                        "Resources can only be used with `java.lang.String`, `java.nio.file.Path`, `java.net.URI`, or `java.net.URL`");
            }
            case FXMLTranslation(String translationKey) -> {
                context.addFeature(Feature.RESOURCE_BUNDLE);
                yield "%s.getString(%s)".formatted(
                        INTERNAL_RESOURCE_BUNDLE,
                        typeHelper.encodeString(translationKey)
                );
            }
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType t, String v) ->
                    identifier.map(FXMLIdentifier::toString)
                            .map(i -> "%s%s".formatted(identifierPrefix, i))
                            .orElseGet(() -> typeHelper.encodeLiteral(context, v, t));
        };
    }

    /// Adds inner class declarations to the source code context for included FXML documents.
    ///
    /// @param document The [FXMLDocument] whose included documents should be processed.
    /// @param context  The current [SourceCodeGeneratorContext].
    private void addInnerClass(FXMLDocument document, SourceCodeGeneratorContext context) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.NESTED_TYPES);
        Stream.concat(
                        addInnerClass(document.root(), context),
                        document.definitions()
                                .stream()
                                .flatMap(value -> addInnerClass(value, context))
                )
                .reduce(sourceCode, StringBuilder::append, StringBuilder::append)
                .append("\n");
    }

    /// Recursively identifies and generates inner classes for nested FXML includes within an FXML value.
    ///
    /// @param value   The [AbstractFXMLValue] to process for nested includes.
    /// @param context The current [SourceCodeGeneratorContext].
    /// @return A stream of generated source code strings for inner classes.
    private Stream<String> addInnerClass(AbstractFXMLValue value, SourceCodeGeneratorContext context) {
        return switch (value) {
            case FXMLCollection(_, _, _, List<AbstractFXMLValue> values) -> values.stream()
                    .flatMap(v -> addInnerClass(v, context));
            case FXMLMap(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) -> entries.values()
                    .stream()
                    .flatMap(v -> addInnerClass(v, context));
            case FXMLObject(_, _, _, List<FXMLProperty> properties) -> properties.stream()
                    .flatMap(property -> propertyRecursionHelper.walk(property, this::addInnerClass, context));
            case FXMLInclude(_, String sourceFile, _, Optional<String> resources, FXMLLazyLoadedDocument document) -> {
                if (context.seenNestedFXMLFiles().add(sourceFile)) {
                    yield Stream.of(internalGenerateSourceCode(
                            document.get(),
                            context.with(
                                    resources.map(typeHelper::encodeString)
                                            .map("java.util.ResourceBundle.getBundle(%s)"::formatted)
                                            .orElse(context.resourceBundle())
                            ),
                            null,
                            false
                    ));
                } else {
                    yield Stream.empty();
                }
            }
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _,
                 FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> Stream.empty();
        };
    }

    /// Represents an FXML construction step that may have dependencies.
    private sealed interface Constructions {
        /// Returns the identifiers this construction depends on.
        ///
        /// @return A list of [FXMLIdentifier] dependencies.
        List<FXMLIdentifier> dependencies();
    }

    /// Represents an FXML object construction and its dependencies.
    ///
    /// @param object                The [AbstractFXMLObject] being constructed.
    /// @param constructorProperties The properties passed to the constructor.
    /// @param dependencies          The identifiers this object depends on.
    private record AbstractFXMLObjectAndDependencies(
            AbstractFXMLObject object,
            List<FXMLConstructorProperty> constructorProperties,
            List<FXMLIdentifier> dependencies
    ) implements Constructions {

        /// Validates the object and initializes the lists.
        ///
        /// @param object                The [AbstractFXMLObject] being constructed.
        /// @param constructorProperties The properties passed to the constructor.
        /// @param dependencies          The identifiers this object depends on.
        private AbstractFXMLObjectAndDependencies {
            Objects.requireNonNull(object, "object");
            constructorProperties = List.copyOf(Objects.requireNonNullElseGet(constructorProperties, List::of));
            dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
        }
    }

    /// Represents an FXML copy construction and its dependencies.
    ///
    /// @param copy         The [FXMLCopy] being constructed.
    /// @param dependencies The identifiers this copy depends on.
    private record FXMLCopyAndDependencies(
            FXMLCopy copy,
            List<FXMLIdentifier> dependencies
    ) implements Constructions {

        /// Validates the copy and initializes the dependency list.
        ///
        /// @param copy         The [FXMLCopy] being constructed.
        /// @param dependencies The identifiers this copy depends on.
        private FXMLCopyAndDependencies {
            Objects.requireNonNull(copy, "copy");
            dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
        }
    }

    /// Represents an FXML include construction and its dependencies.
    ///
    /// @param include      The [FXMLInclude] being constructed.
    /// @param dependencies The identifiers this includes depend on.
    private record FXMLIncludeAndDependencies(
            FXMLInclude include,
            List<FXMLIdentifier> dependencies
    ) implements Constructions {

        /// Validates the include and initializes the dependency list.
        ///
        /// @param include      The [FXMLInclude] being constructed.
        /// @param dependencies The identifiers this includes depend on.
        private FXMLIncludeAndDependencies {
            Objects.requireNonNull(include, "include");
            dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
        }
    }

    /// Represents an FXML value construction and its dependencies.
    ///
    /// @param value        The [FXMLValue] being constructed.
    /// @param dependencies The identifiers this value construction depends on.
    private record FXMLValueConstruction(
            FXMLValue value,
            List<FXMLIdentifier> dependencies
    ) implements Constructions {

        /// Validates the value and initializes the dependency list.
        ///
        /// @param value        The [FXMLValue] being constructed.
        /// @param dependencies The identifiers this value construction depends on.
        private FXMLValueConstruction {
            Objects.requireNonNull(value, "value");
            dependencies = List.copyOf(Objects.requireNonNullElseGet(dependencies, List::of));
        }
    }
}
