package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLLazyLoadedDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLController;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLInterface;
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
import io.github.bsels.javafx.maven.plugin.utils.CheckAndCast;
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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// A builder for generating Java source code from an [FXMLDocument].
/// This class handles the conversion of FXML elements, properties, and values into a corresponding Java class
/// that can be instantiated to create the defined object graph.
/// It coordinates the overall generation process by orchestrating various steps such as package declaration,
/// import organization, field definition, constructor building, method generation,
/// and inner class creation for included FXML documents.
public final class FXMLSourceCodeBuilder {
    /// The name of the FXML controller's initialization method.
    static final String INITIALIZE_METHOD = "initialize";
    /// Internal controller field name used within the generated Java class to hold the controller instance.
    static final String INTERNAL_CONTROLLER_FIELD = "$internalField$controller$";
    /// Internal resource bundle field name used for translations in the generated Java class.
    static final String INTERNAL_RESOURCE_BUNDLE = "$INTERNAL$RESOURCE$BUNDLE$";
    /// Internal method name for converting a string value to a [File] instance.
    static final String INTERNAL_STRING_TO_FILE_METHOD = "$internalMethod$stringToFile$";
    /// Internal method name for converting a string value to a [Path] instance.
    static final String INTERNAL_STRING_TO_PATH_METHOD = "$internalMethod$stringToPath$";
    /// Internal method name for converting a string value to a [URI] instance.
    static final String INTERNAL_STRING_TO_URI_METHOD = "$internalMethod$stringToURI$";
    /// Internal method name for converting a string value to a [URL] instance.
    static final String INTERNAL_STRING_TO_URL_METHOD = "$internalMethod$stringToURL$";
    /// The [FXMLClassType] representation of the [URL] class.
    private static final FXMLClassType URL_TYPE = new FXMLClassType(URL.class);
    /// The [FXMLClassType] representation of the [ResourceBundle] class.
    private static final FXMLClassType RESOURCE_BUNDLE_TYPE = new FXMLClassType(ResourceBundle.class);
    /// Prefix for internal constructor variables used to avoid naming collisions during object initialization.
    private static final String CONSTRUCTOR_VARIABLE_PREFIX = "$$";
    /// The name of the internal method generated to bind FXML identifiers to controller fields.
    ///
    /// This method is called at the end of the constructor when [Feature#BIND_CONTROLLER] is enabled.
    private static final String CONTROLLER_BIND_METHOD_NAME = "$bindController$";
    /// The template body for the internal string-to-file conversion method.
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
    /// Internal string-to-path conversion method body.
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
    /// Internal string-to-URI conversion method body.
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
    /// The template body for the internal string-to-URL conversion method.
    private static final String INTERNAL_STRING_TO_URL_METHOD_BODY = """
            private java.net.URL %s(String value) {
                return java.util.Objects.requireNonNull(
                        getClass()
                                .getResource(value)
                );
            }
            """.formatted(INTERNAL_STRING_TO_URL_METHOD);
    /// Suffix appended to FXML identifiers to denote their associated controller fields.
    private static final String CONTROLLER_SUFFIX = "Controller";
    /// Logger for outputting generation information and debugging messages.
    private final Log log;

    /// Helper for managing and adding imports to the generated source code.
    private final FXMLSourceCodeBuilderImportHelper builderImportHelper;

    /// Helper for handling type conversions and literal encoding.
    private final FXMLSourceCodeBuilderTypeHelper typeHelper;

    /// Flag indicating whether the `@Generated` annotation should be added to the class.
    private final boolean addGeneratedAnnotation;

    /// The time when the generation process started, used for the `@Generated` annotation timestamp.
    private final ZonedDateTime buildTime;

    /// The default resource bundle to be used for FXML translations if none is specified.
    private final String defaultResourceBundle;

    /// Helper instance for managing and resolving recursive property bindings.
    /// Ensures proper handling of nested property structures and prevents infinite recursion.
    private final FXMLPropertyRecursionHelper propertyRecursionHelper;

    /// Initializes a new [FXMLSourceCodeBuilder] instance with the specified configuration.
    ///
    /// @param log                    The Maven plugin logger for diagnostic output.
    /// @param defaultResourceBundle  The name of the default resource bundle for translations.
    /// @param addGeneratedAnnotation Whether to include the standard `@Generated` annotation.
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

    /// Generates the Java source code for the specified [FXMLDocument] and package.
    /// This method serves as the main entry point for the code generation process.
    /// It first analyzes the document to identify all required types and imports,
    /// then initiates the internal recursive generation flow.
    ///
    /// @param document    The [FXMLDocument] describing the object graph to convert.
    /// @param packageName The package name where the generated class will reside.
    /// @return The complete Java source code as a formatted string.
    /// @throws NullPointerException If `document` is null.
    public String generateSourceCode(FXMLDocument document, String packageName) throws NullPointerException {
        Objects.requireNonNull(document, "`document` must not be null");
        log.debug("Generating source code for FXML document with classname: %s".formatted(document.className()));

        Map<String, FXMLType> identifierToTypeMap = typeHelper.createIdentifierToTypeMap(document);
        SourceCodeGeneratorContext context = new SourceCodeGeneratorContext(
                builderImportHelper.findImports(document, addGeneratedAnnotation),
                defaultResourceBundle,
                identifierToTypeMap,
                packageName
        );

        return internalGenerateSourceCode(document, context, true);
    }

    /// Generates source code internally by orchestrating various code sections in the correct order.
    /// This method allows for recursive processing of nested FXML documents by managing the shared
    /// [SourceCodeGeneratorContext].
    ///
    /// @param document       The [FXMLDocument] currently being processed.
    /// @param context        The shared [SourceCodeGeneratorContext] used to accumulate source code parts.
    /// @param isRootDocument Whether the current document is the top-level root document.
    /// @return The generated Java source code for the current document level.
    private String internalGenerateSourceCode(
            FXMLDocument document,
            SourceCodeGeneratorContext context,
            boolean isRootDocument
    ) {
        addPackageLine(context);
        addImports(context);
        addFields(context, document);
        addConstructorPrologue(context, document);
        addConstructorEpilogue(context, document);
        addInnerClass(document, context);
        addMethods(document, context);
        addControllerInitializeMethod(context, document);
        bindControllerFields(context, document);

        // The class declaration should be done last as it may depend on gathered features
        addClassDeclaration(context, document, isRootDocument);
        return generateSourceCode(context, document.className(), isRootDocument);
    }

    /// Assembles the final Java source code string from the accumulated parts in the context.
    /// This method handles the overall class structure, including annotations, class declaration, field definitions,
    /// the constructor, and methods.
    ///
    /// @param context        The [SourceCodeGeneratorContext] containing all gathered source code segments.
    /// @param className      The simple name of the Java class being generated.
    /// @param isRootDocument Whether the current document is the top-level root document.
    /// @return The final assembled Java source code.
    private String generateSourceCode(
            SourceCodeGeneratorContext context,
            String className,
            boolean isRootDocument
    ) {
        if (context.hasFeature(Feature.BIND_CONTROLLER)) {
            context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE)
                    .append(CONTROLLER_BIND_METHOD_NAME)
                    .append("();\n");
            context.sourceCode(SourcePart.METHODS)
                    .append("private void ")
                    .append(CONTROLLER_BIND_METHOD_NAME)
                    .append("() {\n")
                    .append(context.sourceCode(SourcePart.CONTROLLER_FIELDS).toString().indent(4))
                    .append(context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION).toString().indent(4))
                    .append("}\n\n");
        }
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
            sourceCode.append("    private static final java.util.ResourceBundle ")
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

    /// Adds the controller's `initialize` method call to the generated source code.
    ///
    /// This method checks if the controller has an `initialize` method and generates
    /// the appropriate call (direct or via reflection) based on the method's visibility.
    ///
    /// @param context  The [SourceCodeGeneratorContext] used for code generation.
    /// @param document The [FXMLDocument] containing the controller information.
    private void addControllerInitializeMethod(SourceCodeGeneratorContext context, FXMLDocument document) {
        Optional<FXMLController> controllerOptional = document.controller();
        if (controllerOptional.isEmpty()) {
            return;
        }
        FXMLController controller = controllerOptional.get();
        List<FXMLControllerMethod> initializeMethods = controller.methods()
                .stream()
                .filter(method -> INITIALIZE_METHOD.equals(method.name()))
                .toList();
        Optional<FXMLControllerMethod> initializeMethodOptional = initializeMethods.stream()
                .filter(method -> method.parameterTypes().size() == 2)
                .filter(method -> URL_TYPE.equals(method.parameterTypes().getFirst()))
                .filter(method -> RESOURCE_BUNDLE_TYPE.equals(method.parameterTypes().get(1)))
                .findFirst()
                .or(
                        () -> initializeMethods.stream()
                                .filter(method -> method.parameterTypes().isEmpty())
                                .findFirst()
                );
        FXMLClassType controllerClass = controller.controllerClass();
        if (initializeMethodOptional.isEmpty()) {
            log.info("No suitable initialize method found for controller: %s".formatted(controllerClass));
            return;
        }
        FXMLControllerMethod initializeMethod = initializeMethodOptional.get();
        context.addFeature(Feature.BIND_CONTROLLER);
        context.sourceCode(SourcePart.CONTROLLER_INITIALIZATION)
                .append(typeHelper.renderControllerInitialization(context, controllerClass, initializeMethod));
    }

    /// Adds sorted import statements to the source code context.
    /// Filters out primitive types and ensures the imports are lexicographically ordered for better readability of
    /// the generated code.
    ///
    /// @param context The current [SourceCodeGeneratorContext] where imports will be added.
    private void addImports(SourceCodeGeneratorContext context) {
        List<String> importList = context.imports()
                .imports()
                .stream()
                .filter(Predicate.not(typeHelper::isPrimitive))
                .sorted()
                .toList();
        StringBuilder sourceCode = context.sourceCode(SourcePart.IMPORTS);
        for (String importStatement : importList) {
            sourceCode.append("import ").append(importStatement).append(";\n");
        }
        sourceCode.append("\n");
    }

    /// Adds the package declaration to the source code context if a package name is defined.
    ///
    /// @param context The current [SourceCodeGeneratorContext] where the package line will be added.
    private void addPackageLine(SourceCodeGeneratorContext context) {
        context.packageName()
                .ifPresent(packageName -> context.sourceCode(SourcePart.PACKAGE)
                        .append("package ")
                        .append(packageName)
                        .append(";\n\n"));
    }

    /// Adds all necessary field declarations to the source code context for a given FXML document.
    /// This includes the internal controller field and fields for all FXML elements with identifiers (fx:id) found in
    /// the root node and definitions.
    ///
    /// @param context  The current [SourceCodeGeneratorContext] where fields will be accumulated.
    /// @param document The [FXMLDocument] whose elements should be mapped to fields.
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

    /// Recursively identifies and adds field declarations for an FXML value and its nested children.
    /// Different types of FXML values (collections, includes, maps, objects) are handled  according to their specific
    /// structural requirements.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] to process for potential fields.
    /// @return A stream of field declaration strings (e.g., "private final Button myButton;").
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
                                                new FXMLExposedIdentifier(identifier + CONTROLLER_SUFFIX),
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
                            propertyRecursionHelper.walk(properties, this::addFields, context)
                    );
            case FXMLConstant _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> Stream.empty();
            case FXMLValue(Optional<FXMLIdentifier> identifier, FXMLType type, _) -> identifier.stream()
                    .flatMap(id -> identifierToField(context, id, type));
        };
    }

    /// Maps an [FXMLIdentifier] and [FXMLType] to a specific Java field declaration.
    /// Handles exposed identifiers (protected fields for fx:id), internal identifiers (private fields),
    /// and ignores root identifiers which do not require separate fields.
    ///
    /// @param context    The current [SourceCodeGeneratorContext].
    /// @param identifier The identifier (fx:id) that defines the field name and visibility.
    /// @param type       The Java type of the field.
    /// @return A stream containing the field declaration string, or an empty stream if no field is needed.
    private Stream<String> identifierToField(SourceCodeGeneratorContext context, FXMLIdentifier identifier, FXMLType type) {
        return switch (identifier) {
            case FXMLExposedIdentifier(String name) ->
                    Stream.of("protected final %s %s;\n".formatted(typeHelper.typeToSourceCode(context, type), name));
            case FXMLInternalIdentifier _, FXMLNamedRootIdentifier _, FXMLRootIdentifier _ -> Stream.empty();
        };
    }

    /// Constructs the class declaration line, including modifiers, class name, and inheritance/interfaces.
    /// The class is marked as `abstract` if it has abstract methods from implemented interfaces
    /// or if it was explicitly configured as an abstract class.
    ///
    /// @param context        The current [SourceCodeGeneratorContext].
    /// @param document       The [FXMLDocument] defining the class properties.
    /// @param isRootDocument Whether this class is the main root class or a nested inner class.
    /// @throws UnsupportedOperationException If an abstract inner class is requested for an included FXML,
    ///                                       which is currently not supported.
    private void addClassDeclaration(SourceCodeGeneratorContext context, FXMLDocument document, boolean isRootDocument) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CLASS_DECLARATION);
        boolean hasAbstractInterfaceMethods = document.interfaces()
                .stream()
                .map(FXMLInterface::methods)
                .flatMap(List::stream)
                .anyMatch(FXMLControllerMethod::isAbstract);
        if (isRootDocument) {
            sourceCode.append("public ");
            if (context.hasFeature(Feature.ABSTRACT_CLASS) || hasAbstractInterfaceMethods) {
                sourceCode.append("abstract ");
            }
        } else {
            sourceCode.append("private static ");
            if (context.hasFeature(Feature.ABSTRACT_CLASS) || hasAbstractInterfaceMethods) {
                throw new UnsupportedOperationException(
                        "Abstract inner classes for included FXML documents are not supported."
                );
            }
        }
        sourceCode.append("class ")
                .append(document.className())
                .append(" extends ")
                .append(typeHelper.typeToSourceCode(context, document.root().type()));
        if (!document.interfaces().isEmpty()) {
            sourceCode.append(" implements ");
            for (FXMLInterface fxmlInterface : document.interfaces()) {
                sourceCode.append(typeHelper.typeToSourceCode(context, fxmlInterface.type()))
                        .append(", ");
            }
            sourceCode.delete(sourceCode.length() - 2, sourceCode.length());
        }
    }

    /// Adds the constructor prologue, which includes controller initialization and object construction.
    /// This method resolves the dependency order for all objects defined in the FXML to ensure they are created before
    /// being referenced by other objects.
    ///
    /// @param context  The current [SourceCodeGeneratorContext].
    /// @param document The [FXMLDocument] whose objects need to be initialized in the constructor.
    private void addConstructorPrologue(SourceCodeGeneratorContext context, FXMLDocument document) {
        document.controller()
                .ifPresent(
                        controller -> context.sourceCode(SourcePart.CONSTRUCTOR_PROLOGUE)
                                .append(INTERNAL_CONTROLLER_FIELD)
                                .append(" = new ")
                                .append(typeHelper.typeToSourceCode(context, controller.controllerClass()))
                                .append("();\n")
                );
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

    /// Adds field creation logic to the constructor prologue section.
    /// Iterates through the ordered list of constructions and generates the corresponding  Java initialization code
    /// (e.g., `myButton = new Button();`).
    ///
    /// @param context                 The current [SourceCodeGeneratorContext].
    /// @param orderedConstructions    The list of constructions in their resolved dependency order.
    /// @param constructorDependencies The set of identifiers that act as dependencies for other constructions.
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
        boolean isInternalIdentifier = identifier instanceof FXMLInternalIdentifier;
        boolean isDependency = constructorDependencies.contains(identifier) && !isInternalIdentifier;
        if (isInternalIdentifier) {
            sourceCode.append(typeString)
                    .append(' ');
        } else if (isDependency) {
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
                .append(" = ");
        if (!(fxmlIdentifier instanceof FXMLInternalIdentifier)) {
            sourceCode.append(CONSTRUCTOR_VARIABLE_PREFIX);
        }
        sourceCode.append(fxmlIdentifier)
                .append(";\n");
    }

    /// Resolves the construction order of FXML objects based on their interdependencies.
    /// This ensures that if object A depends on object B (e.g., via a constructor property),
    /// object B is constructed before object A in the generated code.
    ///
    /// @param constructions The list of construction steps to be reordered.
    /// @return A new list of constructions ordered such that all dependencies are satisfied.
    /// @throws IllegalArgumentException If a cyclic dependency is detected in the FXML structure.
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

    /// Prepares the argument list for an object's constructor or factory method call.
    /// It identifies the most appropriate constructor or method to use based on the available FXML properties.
    ///
    /// @param builder       The [StringBuilder] to which the arguments will be appended.
    /// @param constructions The construction step containing the object and its properties.
    /// @param context       The current [SourceCodeGeneratorContext].
    /// @return The same [StringBuilder] instance with the comma-separated arguments appended.
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
                                        .map(v -> typeHelper.encodeFXMLValue(context, v, property.type()))
                                        .orElseGet(() -> typeHelper.defaultTypeValue(property.type()))
                        );
                    } else {
                        builder.append(typeHelper.encodeFXMLValue(
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

    /// Identifies dependencies for the construction of a specific FXML object.
    /// Analyzes the object's properties to find any references to other FXML elements.
    ///
    /// @param object The [AbstractFXMLObject] to analyze for dependencies.
    /// @return An [AbstractFXMLObjectAndDependencies] containing the object and its identified dependencies.
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

    /// Finds the identifier associated with an FXML value, if it has one.
    /// This is used to resolve dependencies between elements.
    ///
    /// @param value The [AbstractFXMLValue] to check for an identifier.
    /// @return An [Optional] containing the [FXMLIdentifier] if the value is an identifiable element.
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

    /// Recursively identifies all necessary construction steps and their dependencies for an FXML value.
    /// This includes traversing nested properties and children of objects and collections.
    ///
    /// @param value   The [AbstractFXMLValue] to analyze.
    /// @param context The current [SourceCodeGeneratorContext].
    /// @return A stream of [Constructions] representing the required initialization steps.
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
                    propertyRecursionHelper.walk(fxmlObject.properties(), this::findConstructions, context)
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

    /// Adds the constructor epilogue section, which handles property settings and collection population.
    /// This part of the constructor executes after all objects have been created.
    ///
    /// @param context  The current [SourceCodeGeneratorContext].
    /// @param document The [FXMLDocument] whose property settings should be added.
    private void addConstructorEpilogue(SourceCodeGeneratorContext context, FXMLDocument document) {
        addConstructorEpilogue(context, document.root());
        document.definitions()
                .forEach(d -> addConstructorEpilogue(context, d));
    }

    /// Recursively generates property-setting logic for an FXML value and its children.
    /// Handles collection population, map entries, and individual object properties.
    ///
    /// @param context The current [SourceCodeGeneratorContext].
    /// @param value   The [AbstractFXMLValue] whose properties should be initialized.
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
                        .append(typeHelper.encodeFXMLValue(context, v, FXMLType.OBJECT))
                        .append(");\n"));
                values.forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLMap(
                    FXMLIdentifier identifier,
                    _,
                    FXMLType keyType,
                    FXMLType valueType,
                    _,
                    Map<FXMLLiteral, AbstractFXMLValue> entries
            ) -> {
                entries.forEach((k, v) -> sourceCode.append(identifier.toString())
                        .append(".put(")
                        .append(typeHelper.encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(keyType)))
                        .append(", ")
                        .append(typeHelper.encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(valueType)))
                        .append(");\n"));
                entries.values().forEach(v -> addConstructorEpilogue(context, v));
            }
            case FXMLObject(
                    FXMLIdentifier identifier,
                    _,
                    _,
                    List<FXMLProperty> properties
            ) -> properties.forEach(p -> addConstructorEpilogue(context, identifier.toString(), p));
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, FXMLLazyLoadedDocument fxmlDocument) ->
                    fxmlDocument.get()
                            .controller()
                            .ifPresent(
                                    _ -> sourceCode.append(identifier)
                                            .append(CONTROLLER_SUFFIX)
                                            .append(" = ")
                                            .append(identifier)
                                            .append('.')
                                            .append(INTERNAL_CONTROLLER_FIELD)
                                            .append(";\n")
                            );
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _,
                 FXMLMethod _, FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> {
            }
        }
    }

    /// Generates the Java code for setting a specific FXML property.
    /// This includes calling setters, populating collections, or using static setters.
    ///
    /// @param context    The current [SourceCodeGeneratorContext].
    /// @param identifier The variable name of the object that owns the property.
    /// @param property   The [FXMLProperty] to be set.
    private void addConstructorEpilogue(SourceCodeGeneratorContext context, String identifier, FXMLProperty property) {
        StringBuilder sourceCode = context.sourceCode(SourcePart.CONSTRUCTOR_EPILOGUE);
        switch (property) {
            case FXMLCollectionProperties(
                    _,
                    String getter,
                    _,
                    FXMLType elementClassType,
                    List<AbstractFXMLValue> value,
                    List<FXMLProperty> properties
            ) -> {
                String collectionSelection = "%s.%s()".formatted(identifier, getter);
                value.forEach(v -> sourceCode.append(collectionSelection)
                        .append(".add(")
                        .append(typeHelper.encodeFXMLValue(context, v, elementClassType))
                        .append(");\n"));
                value.forEach(v -> addConstructorEpilogue(context, v));
                properties.forEach(p -> addConstructorEpilogue(context, collectionSelection, p));
            }
            case FXMLConstructorProperty(_, _, AbstractFXMLValue value) -> addConstructorEpilogue(context, value);
            case FXMLMapProperty(
                    _,
                    String getter,
                    _,
                    FXMLType rawKeyClass,
                    FXMLType rawValueClass,
                    Map<FXMLLiteral, AbstractFXMLValue> value
            ) -> {
                value.forEach((k, v) -> sourceCode.append(identifier)
                        .append('.')
                        .append(getter)
                        .append("().put(")
                        .append(typeHelper.encodeFXMLValue(context, k, typeHelper.getTypeForMapEntry(rawKeyClass)))
                        .append(", ")
                        .append(typeHelper.encodeFXMLValue(context, v, typeHelper.getTypeForMapEntry(rawValueClass)))
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
                        .append(typeHelper.encodeFXMLValue(context, value, type))
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
                        .append(typeHelper.encodeFXMLValue(context, value, type))
                        .append(");\n");
                addConstructorEpilogue(context, value);
            }
        }
    }

    /// Orchestrates the binding of FXML identifiers to controller fields in the generated Java code.
    /// This method identifies if a controller is present and, if so, iterates through the FXML document's
    /// root and definitions to generate field mapping assignments.
    ///
    /// @param context  The current [SourceCodeGeneratorContext]
    /// @param document The [FXMLDocument] being processed
    private void bindControllerFields(SourceCodeGeneratorContext context, FXMLDocument document) {
        Optional<FXMLController> controllerOptional = document.controller();
        if (controllerOptional.isEmpty()) {
            return;
        }
        FXMLController controller = controllerOptional.get();
        if (controller.fields().isEmpty()) {
            return;
        }
        boolean hasFieldMappings = Stream.concat(
                        Stream.of(document.root()),
                        document.definitions()
                                .stream()
                ).map(value -> bindControllerFields(context, controller, value))
                .reduce(false, Boolean::logicalOr);
        if (hasFieldMappings) {
            context.addFeature(Feature.BIND_CONTROLLER);
        }
    }

    /// Recursively traverses FXML values to find and bind identifiers to controller fields.
    ///
    /// @param context    The current [SourceCodeGeneratorContext]
    /// @param controller The [FXMLController] containing the fields to bind
    /// @param value      The [AbstractFXMLValue] to process for field bindings
    /// @return `true` if any field mappings were generated, `false` otherwise
    private boolean bindControllerFields(
            SourceCodeGeneratorContext context,
            FXMLController controller,
            AbstractFXMLValue value
    ) {
        // Using `Boolean.logicalOr` to prevent short-circuit evaluation
        return switch (value) {
            case FXMLCollection(FXMLIdentifier identifier, _, _, List<AbstractFXMLValue> values) -> Boolean.logicalOr(
                    bindField(context, identifier, controller),
                    values.stream()
                            .map(v -> bindControllerFields(context, controller, v))
                            .reduce(false, Boolean::logicalOr)
            );
            case FXMLMap(FXMLIdentifier identifier, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) ->
                    Boolean.logicalOr(
                            bindField(context, identifier, controller),
                            entries.values().stream()
                                    .map(v -> bindControllerFields(context, controller, v))
                                    .reduce(false, Boolean::logicalOr)
                    );
            case FXMLObject(FXMLIdentifier identifier, _, _, List<FXMLProperty> properties) -> Boolean.logicalOr(
                    bindField(context, identifier, controller),
                    propertyRecursionHelper.walk(
                            properties,
                            (v, c) -> Stream.of(bindControllerFields(c, controller, v)),
                            context
                    ).reduce(false, Boolean::logicalOr)
            );
            case FXMLCopy(FXMLIdentifier identifier, _) -> bindField(context, identifier, controller);
            case FXMLInclude(FXMLIdentifier identifier, _, _, _, _) -> bindField(context, identifier, controller);
            case FXMLValue(Optional<FXMLIdentifier> identifier, _, _) ->
                    identifier.map(id -> bindField(context, id, controller)).orElse(false);
            case FXMLMethod _, FXMLInlineScript _, FXMLLiteral _, FXMLConstant _, FXMLExpression _, FXMLReference _,
                 FXMLResource _, FXMLTranslation _ -> false;
        };
    }

    /// Generates the Java source code for binding an FXML identifier to a controller field.
    ///
    /// @param context    The current [SourceCodeGeneratorContext]
    /// @param identifier The [FXMLIdentifier] from the FXML document
    /// @param controller The [FXMLController] containing the fields to bind
    /// @return `true` if a successful binding was generated, `false` otherwise
    private boolean bindField(
            SourceCodeGeneratorContext context, FXMLIdentifier identifier, FXMLController controller
    ) {
        return switch (identifier) {
            case FXMLExposedIdentifier(String name) -> bindField(context, name, name, controller);
            case FXMLNamedRootIdentifier(String name) -> bindField(context, name, "this", controller);
            case FXMLRootIdentifier _, FXMLInternalIdentifier _ -> false;
        };
    }

    /// Generates the Java source code for binding an FXML identifier to a controller field.
    ///
    /// @param context          The current [SourceCodeGeneratorContext]
    /// @param identifier       The [FXMLIdentifier] from the FXML document
    /// @param assignIdentifier The identifier to use for assignment in the generated code
    /// @param controller       The [FXMLController] containing the fields to bind
    /// @return `true` if a successful binding was generated, `false` otherwise
    private boolean bindField(
            SourceCodeGeneratorContext context, String identifier, String assignIdentifier, FXMLController controller
    ) {
        return controller.fields().stream()
                .filter(f -> f.name().equals(identifier))
                .findFirst()
                .map(f -> bindField(context, controller, f, assignIdentifier))
                .orElse(false);
    }

    /// Binds a field in the given controller by generating the appropriate source code
    /// and appending it to the controller fields section of the source code context.
    ///
    /// @param context    The source code generation context used to write the generated code.
    /// @param controller The FXML controller where the field resides.
    /// @param field      The field in the FXML controller to be bound and processed.
    /// @param identifier The identifier of the field in the FXML document.
    /// @return A boolean value indicating whether the field was successfully bound, always `true`.
    private boolean bindField(
            SourceCodeGeneratorContext context,
            FXMLController controller,
            FXMLControllerField field,
            String identifier
    ) {
        context.sourceCode(SourcePart.CONTROLLER_FIELDS)
                .append(typeHelper.renderControllerFieldMapping(context, controller, field, identifier))
                .append('\n');
        return true;
    }

    /// Adds nested inner class declarations for all included FXML files.
    /// Each unique FXML includes results in a generated inner class that encapsulates its object graph.
    ///
    /// @param document The [FXMLDocument] to scan for nested includes.
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
            case FXMLObject(_, _, _, List<FXMLProperty> properties) ->
                    propertyRecursionHelper.walk(properties, this::addInnerClass, context);
            case FXMLInclude(_, String sourceFile, _, Optional<String> resources, FXMLLazyLoadedDocument document) ->
                    singleCreation(
                            context.seenNestedFXMLFiles(),
                            sourceFile,
                            () -> Stream.of(internalGenerateSourceCode(
                                    document.get(),
                                    context.with(
                                            resources.map(typeHelper::encodeString)
                                                    .map("java.util.ResourceBundle.getBundle(%s)"::formatted)
                                                    .orElse(context.resourceBundle())
                                    ),
                                    false
                            ))
                    );
            case FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInlineScript _, FXMLLiteral _, FXMLMethod _,
                 FXMLReference _, FXMLResource _, FXMLTranslation _, FXMLValue _ -> Stream.empty();
        };
    }

    /// Adds methods (such as event handlers) to the generated class.
    /// This includes processing the root node and all definitions to find required methods.
    ///
    /// @param document The [FXMLDocument] to scan for methods.
    /// @param context  The current [SourceCodeGeneratorContext].
    private void addMethods(FXMLDocument document, SourceCodeGeneratorContext context) {
        List<FXMLInterface> interfaces = document.interfaces();
        FXMLController controller = document.controller()
                .orElse(null);
        Stream.concat(
                        addMethods(document.root(), context, controller, interfaces),
                        document.definitions()
                                .stream()
                                .flatMap(value -> addMethods(value, context, controller, interfaces))
                ).reduce(context.sourceCode(SourcePart.METHODS), StringBuilder::append, StringBuilder::append)
                .append('\n');
    }

    /// Recursively identifies and generates methods for an FXML value and its children.
    /// This is primarily used to find and render event handler methods that reference controller methods
    /// or FXML interfaces.
    ///
    /// @param value      The [AbstractFXMLValue] to process for methods.
    /// @param context    The current [SourceCodeGeneratorContext].
    /// @param controller The [FXMLController] of the document, if any.
    /// @param interfaces The list of [FXMLInterface] definitions.
    /// @return A stream of rendered method strings.
    private Stream<String> addMethods(
            AbstractFXMLValue value,
            SourceCodeGeneratorContext context,
            FXMLController controller,
            List<FXMLInterface> interfaces
    ) {
        return switch (value) {
            case FXMLCollection(_, _, _, List<AbstractFXMLValue> values) -> values.stream()
                    .flatMap(nested -> addMethods(nested, context, controller, interfaces));
            case FXMLMap(_, _, _, _, _, Map<FXMLLiteral, AbstractFXMLValue> entries) -> entries.values()
                    .stream()
                    .flatMap(nested -> addMethods(nested, context, controller, interfaces));
            case FXMLObject(_, _, _, List<FXMLProperty> properties) -> propertyRecursionHelper.walk(
                    properties,
                    (v, c) -> addMethods(v, c, controller, interfaces),
                    context
            );
            case FXMLMethod fxmlMethod -> singleCreation(
                    context.seenFXMLMethods(),
                    fxmlMethod,
                    () -> typeHelper.renderMethod(context, controller, interfaces, fxmlMethod)
            );
            case FXMLValue _, FXMLConstant _, FXMLCopy _, FXMLExpression _, FXMLInclude _, FXMLInlineScript _,
                 FXMLLiteral _, FXMLReference _, FXMLResource _, FXMLTranslation _ -> Stream.empty();
        };
    }

    /// Ensures that a particular code element (e.g., a method or a nested class)
    /// is generated only once for a given key, even if it is referenced multiple times.
    ///
    /// @param <K>      The type of the key used for uniqueness tracking.
    /// @param <T>      The type of the elements in the returned stream.
    /// @param set      The set used to track which keys have already been processed.
    /// @param key      The key representing the element to be created.
    /// @param supplier A supplier that provides the stream of generated elements if the key is new.
    /// @return A stream containing the generated element(s) if the key was newly added to the set,
    ///                                                                                                                                                                                                                                                                                 or an empty stream if the key was already present.
    private <K, T> Stream<T> singleCreation(Set<K> set, K key, Supplier<Stream<T>> supplier) {
        if (set.add(key)) {
            return supplier.get();
        } else {
            return Stream.empty();
        }
    }
}
