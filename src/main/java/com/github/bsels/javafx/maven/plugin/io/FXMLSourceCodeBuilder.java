package com.github.bsels.javafx.maven.plugin.io;

import com.github.bsels.javafx.maven.plugin.CheckAndCast;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLController;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLField;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLParentNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerField;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.Visibility;
import com.github.bsels.javafx.maven.plugin.utils.TypeEncoder;
import com.github.bsels.javafx.maven.plugin.utils.Utils;
import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.bsels.javafx.maven.plugin.utils.TypeEncoder.encodeTypeValue;

/// The `SourceCodeBuilder` class is a utility for constructing Java source code programmatically.
/// It allows developers to define packages, imports, classes, fields, methods,
/// and resource bundles in a structured manner.
/// This class is designed to manage the generation of Java code, ensuring correctness in structure and syntax.
/// It supports method chaining for a more streamlined API.
public final class FXMLSourceCodeBuilder {
    /// A constant string representing the literal value "this".
    /// This variable is typically used as a fixed identifier or marker and is declared as public, static,
    /// and final to ensure its value remains constant throughout the application.
    public static final String THIS = "this";

    /// A constant representing the internal controller field name used within the system.
    /// This identifier is typically used internally for system logic and should not be modified
    /// or accessed directly outside the scope of its intended usage.
    private static final String INTERNAL_CONTROLLER_FIELD = "$internalController$";
    /// A constant string pattern used internally to represent the key for reflection-method-related fields.
    /// The placeholder '%d' in the pattern is intended to be replaced with an integer value dynamically.
    /// This is primarily used to generate unique identifiers in reflection-based operations or internal mechanisms.
    private static final String INTERNAL_REFLECTION_METHOD_FIELD = "$reflectionMethod$%d";
    /// A constant string that defines the format for parameter names.
    /// Used to construct parameter names dynamically by inserting a numeric value in place of `%d`.
    /// The `%d` placeholder is replaced with a number to generate unique parameter names.
    private static final String PARAM_NAME_FORMAT = "param%d";

    /// A list that stores the fully qualified names of classes or packages imported into the current Java file.
    /// This list represents the import declarations used in the source code.
    private final List<String> imports;
    /// A list of string constants stored in an immutable structure.
    private final List<String> constants;
    /// A list of strings representing the fields associated with this class.
    private final List<String> fields;
    /// A list of field initializers represented as strings.
    /// This list contains the initial values or configurations assigned to fields,
    /// often used for setup or initialization.
    private final List<String> fieldInitializers;
    /// A list containing the names of methods intended to be used for reflection-based initialization.
    /// Each string in the list represents the name of a method.
    private final List<String> reflectionMethodInitializers;
    /// Represents the body of a constructor as a list of strings.
    /// Each string in the list corresponds to a line or statement within the constructor body.
    private final List<String> constructorBody;
    /// A list of method names represented as strings.
    private final List<String> methods;
    /// A logger instance used for logging messages and events within the application.
    /// This is a final variable ensuring that the logger reference cannot be re-assigned.
    /// It can be used to log debug information, errors, warnings, and other messages.
    private final Log log;

    /// Represents the name of the package associated with a specific component or functionality.
    /// It typically identifies the namespace or grouping of related classes and resources within an application.
    private String packageName;
    /// Represents a line of text or a string value that holds important
    /// or significant information within the given context of the application.
    /// The exact purpose and content of this string may vary depending on how it is used within the program's logic
    /// or functionality.
    private String superLine;
    /// A boolean flag indicating whether the class is abstract.
    /// If `true`, the class is an abstract class that cannot be instantiated directly.
    /// If `false`, the class is a concrete class that can be instantiated.
    private Boolean isAbstractClass;
    /// Represents the definition or description of a class in the system.
    /// This variable is intended to hold the textual representation or details regarding a specific class.
    private String classDefinition;
    /// Represents the name of a class.
    /// This variable typically stores the identifier or title used to refer to a specific class in the context
    /// of the application.
    private String className;
    /// Represents whether the current node or entity is the root element in a hierarchy or structure.
    /// Typically used in tree-like or hierarchical data structures to denote the top-most element.
    ///
    /// When set to `true`, this indicates that the current element is the root.
    /// When set to `false`, this indicates it is not the root.
    private boolean isRoot;
    /// Represents the FXML controller that is responsible for managing the user interface
    /// and handling interactions within the application.
    /// This variable is used to link and control the FXML-based user interface components.
    private FXMLController controller;
    /// A flag indicating whether the resource bundle has been set.
    /// This variable is used to determine if the resource bundle configuration is initialized and available for use.
    private boolean resourceBundleSet;
    /// Counter to track the number of methods accessed or processed via reflection during program execution.
    private int reflectionMethodCounter;

    /// Constructs a new SourceCodeBuilder instance to build source code structures.
    ///
    /// @param log the logging utility used for logging messages and tracking actions during source code generation
    public FXMLSourceCodeBuilder(Log log) {
        packageName = null;
        imports = new ArrayList<>();
        isAbstractClass = null;
        classDefinition = null;
        constants = new ArrayList<>();
        fields = new ArrayList<>();
        className = null;
        fieldInitializers = new ArrayList<>();
        reflectionMethodInitializers = new ArrayList<>();
        constructorBody = new ArrayList<>();
        methods = new ArrayList<>();
        controller = null;
        resourceBundleSet = false;
        this.log = log;
        reflectionMethodCounter = 0;
        superLine = null;
        isRoot = false;
        super();
        addImport("javax.annotation.processing.Generated");
    }

    /// Sets the package declaration for the source code. This method can only be
    /// called once per instance. If the package is already set, an exception will be thrown.
    ///
    /// @param packageName the name of the package to set. If null or blank, the method will return without making any changes.
    /// @return the current instance of `SourceCodeBuilder`.
    /// @throws IllegalStateException if the package has already been set.
    public FXMLSourceCodeBuilder setPackage(String packageName) throws IllegalStateException {
        if (this.packageName != null) {
            throw new IllegalStateException("Package is already set.");
        } else if (packageName == null || packageName.isBlank()) {
            return this;
        }
        this.packageName = packageName;
        return this;
    }

    /// Adds an import statement for the specified class to the source code being built.
    /// This method prepends "import" and appends a semicolon to the class name and ensures the import is included in
    /// the set of imports for the generated source.
    ///
    /// @param importClass the fully qualified name of the class to be imported
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws NullPointerException if the `importClass` is null or blank
    public FXMLSourceCodeBuilder addImport(String importClass) throws NullPointerException {
        if (importClass == null || importClass.isBlank()) {
            throw new NullPointerException("`importClass` cannot be null or blank.");
        }
        imports.add("import %s;".formatted(importClass));
        return this;
    }

    /// Adds multiple import statements for the specified classes to the source code being built.
    /// Each class name in the provided collection will be processed, and corresponding import statements will be added.
    ///
    /// @param importClasses a collection of fully qualified class names to be imported
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws NullPointerException if the `importClasses` collection is null or empty or has null elements
    public FXMLSourceCodeBuilder addImports(Collection<String> importClasses) throws NullPointerException {
        Objects.requireNonNull(importClasses, "`importClasses` must not be null");
        return importClasses.stream()
                .reduce(this, FXMLSourceCodeBuilder::addImport, Utils.getFirstLambda());
    }

    /// Sets the resource bundle to the source code being built.
    /// This method appends the necessary import and field declaration for the specified resource bundle.
    ///
    /// @param resourceBundleStaticFieldPath the static field path of the resource bundle to be added; it should define the full reference to the ResourceBundle in the code.
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws IllegalStateException if the resource bundle already has been set
    public FXMLSourceCodeBuilder setResourceBundle(String resourceBundleStaticFieldPath) throws IllegalStateException {
        if (resourceBundleSet) {
            throw new IllegalStateException("Resource bundle is already set.");
        }
        if (resourceBundleStaticFieldPath == null || resourceBundleStaticFieldPath.isBlank()) {
            return this;
        }
        resourceBundleSet = true;
        addImport("java.util.ResourceBundle");
        constants.add("private static final ResourceBundle RESOURCE_BUNDLE = %s;".formatted(resourceBundleStaticFieldPath));
        return this;
    }

    /// Adds a method to the source code being built.
    /// Constructs the method signature and appends it to the internal collection of methods.
    /// If the corresponding controller method is absent,
    /// marks the class as abstract and adds an abstract method signature.
    ///
    /// @param method the `FXMLMethod` representing the method details such as return type, parameters, and method name to be added
    /// @return the `SourceCodeBuilder` instance, allowing for method chaining
    /// @throws NullPointerException if the passed method is null
    public FXMLSourceCodeBuilder addMethod(FXMLMethod method) throws NullPointerException {
        Objects.requireNonNull(method, "`method` must not be null");
        Optional<ControllerMethod> matchingMethod = findMatchingMethod(method);
        StringBuilder builder = new StringBuilder().append(indent("protected ", 1));
        if (matchingMethod.isEmpty()) {
            log.debug("Controller method not found for %s, creating an abstract method".formatted(method.name()));
            isAbstractClass = true;
            builder.append("abstract ");
        }
        builder.append(TypeEncoder.typeToTypeString(method.returnType(), method.namedGenerics()))
                .append(" ")
                .append(method.name())
                .append("(");
        Function<Type, String> typeToTypeString = type -> TypeEncoder.typeToTypeString(type, method.namedGenerics());
        AtomicInteger counter = new AtomicInteger(0);
        handleSequenceOfArguments(
                builder,
                method.parameters(),
                typeToTypeString.andThen(p -> p + " " + PARAM_NAME_FORMAT.formatted(counter.getAndIncrement()))
        ).append(")");
        matchingMethod.ifPresentOrElse(
                controllerMethod -> callMethod(builder, controllerMethod, method),
                () -> builder.append(";\n")
        );
        methods.add(builder.append('\n').toString());
        return this;
    }

    /// Adds a field to the source code being constructed.
    /// The field's visibility, type, and name are determined based on the properties of the provided [FXMLField] object.
    /// The constructed field is stored internally.
    ///
    /// @param field the field information encapsulated in an [FXMLField] object; must not be null
    /// @return the current instance of [FXMLSourceCodeBuilder], allowing for method chaining
    /// @throws NullPointerException if the `field` parameter is null
    public FXMLSourceCodeBuilder addField(FXMLField field) throws NullPointerException {
        Objects.requireNonNull(field, "`field` must not be null");
        StringBuilder builder = new StringBuilder();
        if (field.internal()) {
            builder.append("private");
        } else {
            builder.append("protected");
            Optional.ofNullable(controller)
                    .stream()
                    .map(FXMLController::instanceFields)
                    .flatMap(Collection::stream)
                    .filter(f -> f.name().equals(field.name()))
                    .findFirst()
                    .ifPresent(controllerField -> bindControllerField(controllerField, field));
        }

        builder.append(" final ").append(field.clazz().getSimpleName());
        constructGenerics(field.generics(), builder).append(' ').append(field.name()).append(';');
        fields.add(builder.toString());
        return this;
    }

    /// Starts the definition of a new class in the source code builder.
    /// This method initializes the class name, parent class (if specified), and the implemented interfaces (if any).
    /// It also constructs the necessary "extends" and "implements" clauses for the class definition based on the inputs.
    ///
    /// @param className   the name of the class to be opened; must not be null
    /// @param parentClass the parent class of the new class; can be null if the class does not extend any parent
    /// @param interfaces  a map where the key is the name of the interface and the value is the list of generics associated with the interface; can be null or empty if no interfaces are implemented
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws NullPointerException  if the `className` is null
    /// @throws IllegalStateException if the class has already been opened
    public FXMLSourceCodeBuilder openClass(String className, ParentClass parentClass, Map<String, List<String>> interfaces) throws NullPointerException, IllegalStateException {
        if (classDefinition != null) {
            throw new IllegalStateException("Class definition is already set.");
        }
        Objects.requireNonNull(className, "`className` must not be null");
        this.className = className;
        StringBuilder builder = new StringBuilder().append("class ").append(className);
        if (parentClass != null) {
            builder.append('\n').append(indent("extends ", 2)).append(parentClass.parentClassName());
            constructGenerics(parentClass.generics(), builder);
        }
        if (interfaces != null && !interfaces.isEmpty()) {
            isAbstractClass = true;
            builder.append("\n").append(indent("implements ", 2));
            handleSequenceOfArguments(
                    builder,
                    interfaces.entrySet(),
                    entry -> entry.getKey() + constructGenerics(entry.getValue(), new StringBuilder()).toString()
            );
        }
        classDefinition = builder.toString();
        return this;
    }

    /// Sets the FXML controller for the source code being built.
    /// This method assigns the provided controller to the instance and ensures that a controller is not already set.
    /// It also appends the necessary field declaration and initialization for the controller instance.
    ///
    /// @param controller the FXMLController instance to be set
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws IllegalStateException if a controller is already set
    /// @throws NullPointerException  if the passed controller is null
    public FXMLSourceCodeBuilder setFXMLController(FXMLController controller) throws IllegalStateException, NullPointerException {
        if (this.controller != null) {
            throw new IllegalStateException("Controller is already set.");
        }
        Objects.requireNonNull(controller, "`controller` must not be null");
        fields.add("private final %s %s;".formatted(controller.className(), INTERNAL_CONTROLLER_FIELD));
        fieldInitializers.add("%s = new %s();".formatted(INTERNAL_CONTROLLER_FIELD, controller.className()));
        this.controller = controller;
        return this;
    }

    /// Handles the processing of a given FXMLNode by constructing objects, setting non-constructor properties,
    /// and binding wrapping objects.
    ///
    /// @param node the FXMLNode to process
    /// @return the current instance of SourceCodeBuilder for method chaining
    public FXMLSourceCodeBuilder handleFXMLNode(FXMLNode node) {
        Objects.requireNonNull(node, "`node` must not be null");
        if (node instanceof FXMLObjectNode(_, String identifier, _, _, _, _) && THIS.equals(identifier)) {
            isRoot = true;
        }
        constructObjects(node, new HashSet<>());
        setNonConstructorProperties(node, new HashSet<>());
        bindWrappingObjects(null, node, new HashSet<>());
        return this;
    }

    /// Builds a string representation of a Java source code file based on the configured package, imports,
    /// class definition, fields, and methods.
    /// The method ensures that all relevant structural elements of the class are appended to the resulting source code
    /// in the correct order.
    ///
    /// @return a string representing the complete source code of the defined class
    /// @throws IllegalStateException if the class definition is not set
    public String build() throws IllegalStateException {
        if (classDefinition == null) {
            throw new IllegalStateException("Class definition is not set.");
        }

        boolean isAbstract = isAbstractClass != null ? isAbstractClass : !isRoot;

        StringBuilder builder = new StringBuilder();
        if (packageName != null) {
            builder.append("package ")
                    .append(packageName)
                    .append(";\n\n");
        }
        imports.stream()
                .sorted()
                .distinct()
                .forEach(importLine -> builder.append(importLine).append("\n"));
        builder.append("\n\n@Generated(value=\"")
                .append(getClass().getName())
                .append("\", date=\"")
                .append(ZonedDateTime.now(ZoneOffset.UTC))
                .append("\")\n")
                .append("public ");
        if (isAbstract) {
            builder.append("abstract ");
        }
        builder.append(classDefinition).append(" {\n\n");
        constants.forEach(f -> builder.append(indent(f, 1)).append("\n"));
        builder.append("\n");
        fields.forEach(f -> builder.append(indent(f, 1)).append("\n"));
        builder.append("\n")
                .append(indent(isAbstract ? "protected" : "public", 1))
                .append(' ')
                .append(className)
                .append("() {\n");
        fieldInitializers.stream()
                .distinct()
                .forEach(f -> builder.append(indent(f, 2)).append("\n"));
        builder.append('\n');
        if (superLine != null) {
            builder.append(indent(superLine, 2)).append("\n\n");
        } else {
            builder.append(indent("super();\n\n", 2));
        }
        if (!reflectionMethodInitializers.isEmpty()) {
            builder.append(indent("// Initialize reflection-based method handlers\n", 1))
                    .append(indent("try {\n", 2));
            reflectionMethodInitializers.forEach(f -> builder.append(indent(f, 3)).append("\n"));
            builder.append(indent("} catch (Throwable e) {\n", 2))
                    .append(indent("throw new RuntimeException(e);\n", 3))
                    .append(indent("}\n\n", 2))
                    .append(indent("// End reflection-based method handlers, continue with the rest of the constructor body\n\n", 1));
        }
        constructorBody.forEach(f -> builder.append(indent(f, 2)).append("\n"));
        builder.append(indent("}\n\n", 1));
        methods.forEach(builder::append);
        builder.append("}\n");
        return builder.toString();
    }

    /// Binds a specified controller field to the corresponding FXML field.
    /// Depending on the visibility of the controller field,
    /// it uses direct assignment or reflection to establish the binding.
    ///
    /// @param controllerField the [ControllerField] instance representing the controller field to bind
    /// @param field           the [FXMLField] instance representing the FXML field to bind to the controller field
    private void bindControllerField(ControllerField controllerField, FXMLField field) {
        if (canCallControllerWithoutReflection(controllerField.visibility())) {
            constructorBody.add("%s.%s = %s;".formatted(INTERNAL_CONTROLLER_FIELD, controllerField.name(), field.name()));
        } else {
            String body = """
                    try {
                        java.lang.reflect.Field field = %1$s.getClass().getDeclaredField("%2$s");
                        field.setAccessible(true);
                        field.set(%1$s, %3$s);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    """.formatted(INTERNAL_CONTROLLER_FIELD, controllerField.name(), field.name());
            constructorBody.add(
                    body.lines()
                            .collect(Collectors.joining("\n        "))
            );
        }
    }

    /// Constructs and initializes objects based on the provided [FXMLNode] structure.
    /// The method traverses through the hierarchy of nodes to initialize all necessary objects,
    /// ensuring no duplicate objects are processed using the given set of seen objects.
    ///
    /// @param node        The root [FXMLNode] to process. This node and its children will be traversed and used to construct objects.
    /// @param seenObjects A set of object identifiers that have already been processed. This is used to ensure that objects are not initialized more than once.
    private void constructObjects(FXMLNode node, Set<String> seenObjects) {
        for (FXMLNode child : getChildren(node)) {
            constructObjects(child, seenObjects);
        }
        switch (node) {
            case FXMLValueNode(
                    _, String identifier, Type type, String value
            ) when isNewNonThisNode(seenObjects, identifier) ->
                    fieldInitializers.add("%s = %s;".formatted(identifier, encodeTypeValue(type, value)));
            case FXMLObjectNode(
                    _, String identifier, Class<?> clazz, List<FXMLProperty> properties, _, List<String> generics
            ) when isNewNonThisNode(seenObjects, identifier) -> {
                StringBuilder builder = new StringBuilder()
                        .append(identifier)
                        .append(" = new ")
                        .append(clazz.getSimpleName());
                if (!generics.isEmpty()) {
                    builder.append("<>");
                }
                fieldInitializers.add(addConstructorParameters(builder, clazz, properties).toString());
            }
            case FXMLObjectNode(_, String identifier, Class<?> clazz, List<FXMLProperty> properties, _, _)
                    when THIS.equals(identifier) ->
                    superLine = addConstructorParameters(new StringBuilder("super"), clazz, properties).toString();
            default -> {
            }
        }
    }

    /// Configures and associates constructor parameters for the specified class and its properties.
    /// Validates the presence of a matching constructor in the class, ensuring the parameter types
    /// and annotations align with the given properties.
    /// Throws an exception if either no valid constructor or multiple matching constructors are found.
    ///
    /// @param clazz      the class whose constructor parameters are to be processed
    /// @param properties a list of FXMLProperty objects to be associated with the constructor parameters
    private StringBuilder addConstructorParameters(
            StringBuilder builder,
            Class<?> clazz,
            List<FXMLProperty> properties
    ) {
        Map<String, FXMLConstructorProperty> constructorProperties = properties.stream()
                .gather(CheckAndCast.of(FXMLConstructorProperty.class))
                .collect(Collectors.toMap(FXMLConstructorProperty::name, Function.identity()));
        return findMinimalConstructor(builder, clazz, constructorProperties);
    }

    /// Identifies and processes the minimal constructor of the given class that matches the specified set of
    /// constructor properties. A constructor is considered minimal if it has the least number of parameters
    /// while still satisfying the property name and annotation requirements.
    ///
    /// @param clazz                 the class whose constructors are being evaluated
    /// @param constructorProperties a map of property names to `FXMLConstructorProperty` that defines the required constructor parameters and their corresponding values
    /// @throws IllegalStateException if no minimal matching constructor can be found
    private StringBuilder findMinimalConstructor(
            StringBuilder builder,
            Class<?> clazz,
            Map<String, FXMLConstructorProperty> constructorProperties
    ) throws IllegalStateException {
        List<Parameter> minimalConstructorParameters = Stream.of(clazz.getConstructors())
                .filter(constructor -> Stream.of(constructor.getParameters()).allMatch(parameter -> parameter.isAnnotationPresent(NamedArg.class)))
                .filter(c -> Set.copyOf(constructorParameterNames(c)).containsAll(constructorProperties.keySet()))
                .map(c -> List.of(c.getParameters()))
                .min(Comparator.comparing(List::size))
                .orElseThrow(() -> new IllegalStateException(
                        "No matching constructor found for parameters: %s for type %s".formatted(
                                constructorProperties.keySet(), clazz
                        )
                ));

        return handleSequenceOfArguments(
                builder.append('('),
                minimalConstructorParameters,
                parameter -> {
                    String parameterName = parameter.getAnnotation(NamedArg.class).value();
                    if (constructorProperties.containsKey(parameterName)) {
                        FXMLConstructorProperty constructorProperty = constructorProperties.get(parameterName);
                        return encodeTypeValue(constructorProperty.type(), constructorProperty.value());
                    } else {
                        return TypeEncoder.defaultValueAsString(parameter.getType());
                    }
                }
        ).append(");");
    }

    /// Retrieves a list of parameter names annotated with `@NamedArg` from the given constructor.
    ///
    /// @param constructor the constructor to examine for parameters annotated with `@NamedArg`
    /// @return a list of parameter names annotated with `@NamedArg`, or an empty list if none are found
    private List<String> constructorParameterNames(Constructor<?> constructor) {
        return Stream.of(constructor.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(NamedArg.class))
                .map(parameter -> parameter.getAnnotation(NamedArg.class).value())
                .toList();
    }

    /// Processes the non-constructor properties of the specified FXMLNode and its children,
    /// appending the necessary code to configure those properties.
    ///
    /// @param node      the FXMLNode object whose non-constructor properties will be processed and configured
    /// @param seenNodes a set of [FXMLNode] objects that have already been processed to ensure no duplicate processing occurs
    private void setNonConstructorProperties(FXMLNode node, Set<FXMLNode> seenNodes) {
        if (!seenNodes.add(node)) {
            return;
        }
        if (node instanceof FXMLObjectNode(_, String identifier, _, List<FXMLProperty> properties, _, _)) {
            List<FXMLProperty> sortedProperties = properties.stream()
                    .sorted(Comparator.comparing(FXMLProperty::name))
                    .toList();
            for (FXMLProperty property : sortedProperties) {
                switch (property) {
                    case FXMLConstructorProperty _ -> {
                    }
                    case FXMLObjectProperty(_, String setter, Type type, String value) ->
                            constructorBody.add("%s.%s(%s);".formatted(identifier, setter, encodeTypeValue(type, value)));
                    case FXMLStaticProperty(_, Class<?> staticClass, String staticSetter, Type type, String value) ->
                            constructorBody.add("%s.%s(%s, %s);".formatted(staticClass.getSimpleName(), staticSetter, identifier, encodeTypeValue(type, value)));
                }
            }
        }
        getChildren(node).forEach(child -> setNonConstructorProperties(child, seenNodes));
    }

    /// Retrieves the list of child nodes associated with the provided FXML node.
    /// Depending on the type of the given FXMLNode, this method extracts and returns the child nodes if applicable,
    /// or an empty list if the node type does not contain child nodes.
    ///
    /// @param node the FXML node whose child nodes are to be retrieved
    /// @return a list of child nodes for the given FXML node, or an empty list if the node type does not contain children
    private List<FXMLNode> getChildren(FXMLNode node) {
        return switch (node) {
            case FXMLParentNode parentNode -> parentNode.children();
            case FXMLValueNode _, FXMLConstantNode _ -> List.of();
        };
    }

    /// Finds a matching method in the controller instance for the given [FXMLMethod].
    /// The matching is based on method name, return type compatibility, and parameter type compatibility.
    /// If multiple matching methods are found, the method with the highest visibility
    /// and the largest number of parameter types (in descending order of size) is chosen.
    ///
    /// @param method the [FXMLMethod] instance representing the method to match against
    /// @return an [Optional] containing the matching [ControllerMethod] if found, or an empty [Optional] if no matching method exists
    private Optional<ControllerMethod> findMatchingMethod(FXMLMethod method) {
        if (controller == null) {
            return Optional.empty();
        }
        String methodName = method.name();
        Comparator<List<?>> listSizeComparator = Comparator.comparingInt(List::size);
        return controller.instanceMethods()
                .stream()
                .filter(controllerMethod -> methodName.equals(controllerMethod.name()))
                .filter(controllerMethod -> validateReturnType(method, controllerMethod))
                .filter(controllerMethod -> validateParameterTypes(method, controllerMethod))
                .min(
                        // Sort by visibility, then by parameter types in descending order of size
                        Comparator.comparing(ControllerMethod::visibility)
                                .thenComparing(ControllerMethod::parameterTypes, listSizeComparator.reversed())
                );
    }

    /// Validates whether the parameter types of the given [FXMLMethod] match the parameter types of the corresponding
    /// [ControllerMethod].
    /// Ensures that the parameters are compatible and can be assigned appropriately.
    ///
    /// @param method           the FXMLMethod whose parameter types are being validated
    /// @param controllerMethod the ControllerMethod whose parameter types are compared
    /// @return true if the parameter types are compatible, false otherwise
    private boolean validateParameterTypes(FXMLMethod method, ControllerMethod controllerMethod) {
        if (controllerMethod.parameterTypes().isEmpty()) {
            return true;
        }
        if (method.parameters().size() != controllerMethod.parameterTypes().size()) {
            return false;
        }
        int size = method.parameters().size();
        for (int i = 0; i < size; i++) {
            Type methodParam = method.parameters().get(i);
            Type controllerParam = controllerMethod.parameterTypes().get(i);
            if (!TypeEncoder.typeToClass(controllerParam).isAssignableFrom(TypeEncoder.typeToClass(methodParam))) {
                return false;
            }
        }
        return true;
    }

    /// Validates if the return type of the FXML method matches or is compatible with the return type of the
    /// corresponding controller method.
    ///
    /// @param method           the FXML method whose return type is being validated
    /// @param controllerMethod the controller method against which the FXML method's return type is validated
    /// @return true if the return type of the FXML method is void or is assignable from the return type of the controller method; false otherwise
    private boolean validateReturnType(FXMLMethod method, ControllerMethod controllerMethod) {
        return void.class.equals(method.returnType()) || TypeEncoder.typeToClass(method.returnType())
                .isAssignableFrom(TypeEncoder.typeToClass(controllerMethod.returnType()));
    }

    /// Constructs and appends the method call logic to the given StringBuilder.
    /// Handles both public and non-public methods, including reflection for the latter.
    /// Ensures proper method invocation and exception handling.
    ///
    /// @param builder    The `StringBuilder` used to construct the method call representation.
    /// @param method     The `ControllerMethod` representing the method to be called.
    /// @param fxmlMethod The `FXMLMethod` providing additional metadata about the method such as return type.
    private void callMethod(StringBuilder builder, ControllerMethod method, FXMLMethod fxmlMethod) {
        log.debug("Constructing method call: %s".formatted(method.name()));
        builder.append(" {\n");
        boolean isVoid = void.class.equals(fxmlMethod.returnType());
        if (canCallControllerWithoutReflection(method.visibility())) {
            log.debug("Calling public method %s on %s".formatted(method.name(), controller.className()));
            addReturnIfNeeded(builder, isVoid, 2)
                    .append(INTERNAL_CONTROLLER_FIELD)
                    .append(".")
                    .append(method.name())
                    .append("(");
            handlerParameterSequence(builder, method).append(");\n");
        } else {
            log.debug("Calling non-public method %s on %s using reflection".formatted(method.name(), controller.className()));
            String reflectionMethodName = getNewReflectionMethodName();

            fields.add("private final java.lang.reflect.Method %s;".formatted(reflectionMethodName));
            StringBuilder declaredMethodBuilder = new StringBuilder()
                    .append(reflectionMethodName)
                    .append(" = ")
                    .append(controller.className())
                    .append(".class.getDeclaredMethod(\"")
                    .append(method.name())
                    .append("\"");
            if (!method.parameterTypes().isEmpty()) {
                declaredMethodBuilder.append(", ");
            }
            reflectionMethodInitializers.add(handleSequenceOfArguments(
                    declaredMethodBuilder,
                    method.parameterTypes(),
                    TypeEncoder::typeToReflectionClassString
            ).append(");").toString());
            constructorBody.add("%s.setAccessible(true);".formatted(reflectionMethodName));

            builder.append(indent("try {\n", 2));
            addReturnIfNeeded(builder, isVoid, 3)
                    .append(reflectionMethodName)
                    .append(".invoke(%s, ".formatted(INTERNAL_CONTROLLER_FIELD));
            handlerParameterSequence(builder, method)
                    .append(");\n")
                    .append(indent("} catch (Throwable e) {\n", 2))
                    .append(indent("throw new RuntimeException(e);\n", 3))
                    .append(indent("}\n", 2));
        }
        builder.append(indent("}\n", 1));
    }

    /// Determines if the controller can be called without using reflection based on its visibility and package
    /// structure.
    ///
    /// Returns `true` if:
    /// - The visibility is `PUBLIC`
    /// - The visibility is `PROTECTED` or `PACKAGE_PRIVATE` and the current package matches the controller's package
    /// - Returns `false` if the visibility is `PRIVATE`
    ///
    /// @param visibility the visibility of the controller method or type (e.g., `PUBLIC`, `PROTECTED`, etc.)
    /// @return `true` if the controller can be accessed without reflection; `false` otherwise
    private boolean canCallControllerWithoutReflection(Visibility visibility) {
        String packageName = controller.type().getPackageName();
        String definedPackageName = this.packageName == null ? "" : this.packageName;
        return switch (visibility) {
            case PUBLIC -> true;
            case PROTECTED, PACKAGE_PRIVATE -> definedPackageName.equals(packageName);
            case PRIVATE -> false;
        };
    }

    /// Generates and returns a new reflection method name by formatting the internal reflection method field
    /// with an incrementing counter.
    ///
    /// @return a string representing the newly generated reflection method name
    private String getNewReflectionMethodName() {
        String methodName = INTERNAL_REFLECTION_METHOD_FIELD.formatted(reflectionMethodCounter);
        reflectionMethodCounter++;
        return methodName;
    }

    /// Adds a `return ` statement to the provided StringBuilder if the method is not void,
    /// otherwise appends an empty string with the specified indentation.
    ///
    /// @param builder the StringBuilder to append the resulting string
    /// @param isVoid  a boolean indicating if the method return type is void
    /// @param indent  the level of indentation to apply before the appended string
    private StringBuilder addReturnIfNeeded(StringBuilder builder, boolean isVoid, int indent) {
        if (!isVoid) {
            builder.append(indent("return ", indent));
        } else {
            builder.append(indent("", indent));
        }
        return builder;
    }

    /// Constructs the parameter sequence for a controller method by appending formatted parameter
    /// names to the provided StringBuilder.
    ///
    /// @param builder the StringBuilder to which the parameter sequence will be appended
    /// @param method  the ControllerMethod whose parameters are to be processed
    /// @return the modified StringBuilder containing the constructed parameter sequence
    private StringBuilder handlerParameterSequence(StringBuilder builder, ControllerMethod method) {
        return handleSequenceOfArguments(
                builder,
                IntStream.range(0, method.parameterTypes().size()).mapToObj(PARAM_NAME_FORMAT::formatted).toList(),
                Function.identity()
        );
    }

    /// Constructs a generic type representation by appending the provided generics to the given StringBuilder.
    /// This method appends the generics in a comma-separated format enclosed by angle brackets ("<" and ">").
    /// If the provided generics list is null or empty, no modifications are made to the builder.
    ///
    /// @param generics a list of generic type names to be appended to the builder; if null or empty, the builder remains unchanged
    /// @param builder  the StringBuilder to which the generic type representation will be appended
    /// @return the modified StringBuilder with the appended generic type representation
    private StringBuilder constructGenerics(List<String> generics, StringBuilder builder) {
        if (generics == null || generics.isEmpty()) {
            return builder;
        }
        return handleSequenceOfArguments(builder.append("<"), generics, Function.identity()).append(">");
    }

    /// Processes a sequence of arguments, applying a specified handling function to each
    /// argument and formatting the results into a comma-separated list enclosed in parentheses.
    ///
    /// @param <T>            the type of elements in the list of arguments
    /// @param builder        the string builder to which the formatted list will be appended
    /// @param arguments      the collection of arguments to process; if the collection is empty, the method performs no action
    /// @param handleArgument a function that processes each argument and converts it into a string representation
    /// @return the provided string build
    private <T> StringBuilder handleSequenceOfArguments(StringBuilder builder, Collection<T> arguments, Function<T, String> handleArgument) {
        Iterator<T> iterator = arguments.iterator();
        if (iterator.hasNext()) {
            builder.append(handleArgument.apply(iterator.next()));
        }
        while (iterator.hasNext()) {
            builder.append(", ").append(handleArgument.apply(iterator.next()));
        }
        return builder;
    }

    /// Indents a given line of text by a specified number of indentation levels.
    /// Each indentation level corresponds to a fixed sequence of four spaces.
    ///
    /// @param line   the line of text to be indented
    /// @param indent the number of indentation levels to apply; if less than 0, no indentation will be applied
    /// @return the resulting line of text with the applied indentation
    private String indent(String line, int indent) {
        return " ".repeat(Math.max(0, indent) * 4) + line;
    }

    /// Determines if the given identifier represents a new node that has not been encountered before.
    ///
    /// @param seenObjects a set of identifiers that have already been encountered
    /// @param identifier  the identifier to be checked
    /// @return true if the identifier is new and has been added to the set, false otherwise
    private boolean isNewNonThisNode(Set<String> seenObjects, String identifier) {
        return !THIS.equals(identifier) && seenObjects.add(identifier);
    }

    /// Binds wrapping objects based on the provided type and FXML node.
    /// Processes different types of nodes and generates the appropriate bindings
    /// or logs debug messages for unexpected configurations.
    ///
    /// @param type      The class type associated with the parent FXML object.
    /// @param node      The FXML node to process and bind, which may include object nodes, wrapper nodes, or other types of nodes.
    /// @param seenNodes a set of [FXMLNode] objects that have already been processed to ensure no duplicate processing occurs
    private void bindWrappingObjects(Class<?> type, FXMLNode node, Set<FXMLNode> seenNodes) {
        if (!seenNodes.add(node)) {
            return;
        }
        if (node instanceof FXMLValueNode _ || node instanceof FXMLWrapperNode _) {
            log.debug("Unexpected root node type: %s, for wrapping objects".formatted(node.getClass()));
            return;
        }
        final Class<?> finalType;
        if (node instanceof FXMLObjectNode(_, _, Class<?> clazz, _, _, _)) {
            finalType = clazz;
        } else {
            finalType = type;
        }
        String objectIdentifier = TypeEncoder.getIdentifier(node);
        for (FXMLNode child : getChildren(node)) {
            switch (child) {
                case FXMLValueNode _, FXMLConstantNode _ -> {
                }
                case FXMLObjectNode(
                        _, String identifier, Class<?> clazz, _, _, _
                ) when Node.class.isAssignableFrom(clazz) ->
                        constructorBody.add("%s.getChildren().add(%s);".formatted(objectIdentifier, identifier));
                case FXMLObjectNode objectNode ->
                        log.debug("Unexpected child node type: %s without wrapper node".formatted(objectNode.getClass()));
                case FXMLStaticMethod staticMethod when Node.class.isAssignableFrom(finalType) ->
                        bindStaticMethod(objectIdentifier, staticMethod);
                case FXMLStaticMethod _ -> log.debug("Unexpected child node type: %s".formatted(finalType));
                case FXMLWrapperNode(String identifier, List<FXMLNode> children) ->
                        bindWrappingObjects(finalType, objectIdentifier, identifier, children);
            }
            switch (child) {
                case FXMLValueNode _, FXMLConstantNode _ -> {
                }
                case FXMLObjectNode objectNode -> bindWrappingObjects(finalType, objectNode, seenNodes);
                case FXMLParentNode parentNode ->
                        parentNode.children().forEach(subChild -> bindWrappingObjects(finalType, subChild, seenNodes));
            }
        }
    }

    /// Binds a static method to a generated behavior based on the specified object identifier and method details.
    /// It processes the child nodes of the static method and determines the appropriate parameters to call the static
    /// method.
    ///
    /// @param objectIdentifier the identifier for the object instance on which the static method should be bound
    /// @param staticMethod     the definition of the static method including its characteristics and child nodes
    private void bindStaticMethod(String objectIdentifier, FXMLStaticMethod staticMethod) {
        List<FXMLNode> children = staticMethod.children();
        List<Class<?>> parameterClasses = Stream.concat(
                Stream.of(Node.class),
                children.stream()
                        .map(node -> switch (node) {
                            case FXMLConstantNode(_, _, Type type) -> Utils.getClassType(type);
                            case FXMLObjectNode(_, _, Class<?> clazz, _, _, _) -> clazz;
                            case FXMLValueNode(_, _, Class<?> clazz, _) -> clazz;
                            case FXMLStaticMethod _, FXMLWrapperNode _ ->
                                    throw new IllegalStateException("Unexpected static method node: %s".formatted(node));
                        })
        ).toList();
        String setterName = Utils.getSetterName(staticMethod.method());
        Class<?> clazz = staticMethod.clazz();
        if (Utils.checkIfStaticMethodExists(clazz, setterName, parameterClasses)) {
            constructorBody.add(
                    handleSequenceOfArguments(
                            new StringBuilder()
                                    .append(clazz.getSimpleName())
                                    .append('.')
                                    .append(setterName)
                                    .append("("),
                            Stream.concat(
                                    Stream.of(new FXMLValueNode(true, objectIdentifier, Object.class, "")),
                                    children.stream()
                            ).toList(),
                            TypeEncoder::getIdentifier
                    ).append(");")
                            .toString()
            );
        }
    }

    /// Binds wrapping objects by checking the provided child nodes and generating code to set or add those child
    /// objects to a specified parent object.
    /// This method processes different types of FXMLNode and attempts to create the appropriate binding based on
    /// the types and structures of the children.
    ///
    /// @param clazz            The class of the parent object where the children will be bound.
    /// @param objectIdentifier The identifier for the parent object in the generated code.
    /// @param identifier       The name of the property or field in the parent object to which the children will be bound.
    /// @param children         A list of child nodes representing the objects to be bound.
    private void bindWrappingObjects(
            Class<?> clazz,
            String objectIdentifier,
            String identifier,
            List<FXMLNode> children
    ) {
        if (children.isEmpty()) {
            return;
        }
        Class<?> paramType = switch (children.getFirst()) {
            case FXMLConstantNode(_, _, Type type) -> Utils.getClassType(type);
            case FXMLValueNode(_, _, Type type, _) -> Utils.getClassType(type);
            case FXMLObjectNode(_, _, Class<?> subClass, _, _, _) -> subClass;
            case FXMLWrapperNode _, FXMLStaticMethod _ -> throw new IllegalStateException("Unexpected child node");
        };
        if (children.size() == 1) {
            String setterForName = Utils.getSetterName(identifier);
            Optional<Method> method = Utils.findMethod(clazz, setterForName, paramType);
            if (method.isPresent()) {
                constructorBody.add("%s.%s(%s);".formatted(objectIdentifier, setterForName, TypeEncoder.getIdentifier(children.getFirst())));
                return;
            }
        }
        try {
            String listGetterName = Utils.getGetterName(identifier);
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(clazz, identifier, listGetterName, paramType);
            if (ObservableList.class.isAssignableFrom(returnType)) {
                constructorBody.add(
                        handleSequenceOfArguments(
                                new StringBuilder()
                                        .append(objectIdentifier)
                                        .append(".")
                                        .append(listGetterName)
                                        .append("()")
                                        .append(".addAll("),
                                children,
                                TypeEncoder::getIdentifier
                        ).append(");")
                                .toString()
                );
            } else {
                for (FXMLNode child : children) {
                    constructorBody.add("%s.%s().add(%s);".formatted(objectIdentifier, listGetterName, TypeEncoder.getIdentifier(child)));
                }
            }
        } catch (IllegalStateException | NoSuchMethodException e) {
            log.warn("Unable to bind wrapping objects for %s".formatted(identifier), e);
        }
    }


    /// Represents a parent class in a source code structure, including its name and any associated generics.
    ///
    /// This record is immutable. It requires a non-null name for the parent class
    /// and ensures the generics list is defensively copied to maintain integrity.
    ///
    /// @param parentClassName the name of the parent class; must not be null
    /// @param generics        the list of generics; if null, an empty list is assigned
    public record ParentClass(String parentClassName, List<String> generics) {

        /// Constructs an instance of ParentClass.
        ///
        /// @param parentClassName the name of the parent class; must not be null
        /// @param generics        the list of generics; if null, an empty list is assigned
        public ParentClass {
            Objects.requireNonNull(parentClassName, "`parentClassName` must not be null");
            generics = List.copyOf(Objects.requireNonNullElseGet(generics, List::of));
        }
    }
}
