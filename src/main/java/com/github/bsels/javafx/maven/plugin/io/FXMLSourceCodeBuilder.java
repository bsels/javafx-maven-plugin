package com.github.bsels.javafx.maven.plugin.io;

import com.github.bsels.javafx.maven.plugin.CheckAndCast;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstructorProperty;
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
import java.util.stream.Stream;

import static com.github.bsels.javafx.maven.plugin.utils.TypeEncoder.encodeTypeValue;

/// A utility class designed to construct Java source code for FXML-related classes.
/// [FXMLSourceCodeBuilder] supports building package declarations, imports, class definitions,
/// fields, methods, and constructors dynamically while ensuring proper syntax and structure.
///
/// This builder enforces a linear workflow where certain components must be added in a specific order
/// (e.g., a class definition must be opened before adding fields or methods).
/// It helps in automating the process of creating FXML-related source code programmatically,
/// including support for generic types, parent classes, interfaces, and resource bundles.
///
/// Instances of this class are intended to be used once for building a single class.
/// Once the source code is finalized by calling the `build` method, further modifications are disallowed.
public final class FXMLSourceCodeBuilder {
    /// A constant string representing the literal value "this".
    /// This variable is typically used as a fixed identifier or marker and is declared as public, static,
    /// and final to ensure its value remains constant throughout the application.
    public static final String THIS = "this";
    /// A mutable sequence of characters used to construct and manipulate strings efficiently.
    /// This variable is declared as a final, thread-unsafe object of the StringBuilder class.
    /// It allows for the modification of the string's content through various append, insert,
    /// replace, and delete operations.
    private final StringBuilder builder;
    /// A logger instance used for logging messages, errors, or other information within the application.
    /// It is a final variable, ensuring that the reference to the logger remains constant
    /// and cannot be reassigned after initialization.
    private final Log log;
    /// Represents whether a package is present or not.
    /// This variable is typically used as a flag to indicate the existence of a package in a given context.
    private boolean hasPackage;
    /// Indicates whether the class is currently open or not.
    /// This flag is used to determine the operational state of the class.
    /// A value of `true` means the class is open, while `false` means it is closed.
    private boolean classOpen;
    /// Indicates whether a certain process or task has been completed.
    /// The value is `true` if the process is finished, and `false` otherwise.
    private boolean finished;
    /// Represents the name of a class, which is typically used to identify a specific class or type in the application.
    /// This variable stores the class name as a string.
    private String className;
    /// Indicates whether a constructor exists for a specific entity.
    /// This variable holds a boolean value: `true` if the entity has a constructor, `false` otherwise.
    private boolean hasConstructor;

    /// Constructs a new instance of the FXMLSourceCodeBuilder class.
    /// This initializes the internal StringBuilder used to build the source code and prepares the object for use.
    ///
    /// @param log The logger instance to use for logging
    public FXMLSourceCodeBuilder(Log log) {
        super();
        builder = new StringBuilder();
        hasPackage = false;
        classOpen = false;
        finished = false;
        this.log = log;
    }

    /// Adds a package declaration to the source code being built.
    /// This method can be called only once; attempting to set the package multiple times will result in an
    /// [IllegalStateException].
    /// If the provided package name is null or blank, the method performs no action.
    ///
    /// @param packageName the name of the package to be added to the source code; must be non-null and non-blank if not it is skipped.
    /// @return the current instance of FXMLSourceCodeBuilder for method chaining.
    /// @throws IllegalStateException if the package has already been set, and the class definition has not been opened.
    public FXMLSourceCodeBuilder addPackage(String packageName) {
        if (classOpen || hasPackage) {
            throw new IllegalStateException("Package has already been set or class definition has already been opened.");
        }
        hasPackage = true;
        if (packageName == null || packageName.isBlank()) {
            return this;
        }
        builder.append("package ").append(packageName).append(";\n\n");
        return this;
    }

    /// Adds import statements to the source code being built.
    /// This method can be called only once; calling it multiple times will result in an [IllegalStateException].
    ///
    /// @param imports a collection of fully qualified class names to be imported. Each element in the collection represents a fully qualified class or package name to be added as an import statement.
    /// @return the current instance of [FXMLSourceCodeBuilder] for method chaining
    /// @throws IllegalStateException if the imports have already been set
    public FXMLSourceCodeBuilder addImports(Collection<String> imports) {
        if (classOpen) {
            throw new IllegalStateException("Imports have already been set.");
        }
        hasPackage = true;
        imports.stream()
                .sorted()
                .forEach(i -> builder.append("import ").append(i).append(";\n"));
        return this;
    }

    /// Opens a class declaration and appends the necessary boilerplate code to the builder.
    /// This method initializes the definition of a public abstract class along with
    /// optional parent class information, generics, and implemented interfaces.
    ///
    /// @param className       the name of the class to be opened; must not be blank or null
    /// @param parentClassName the name of the parent class to extend; can be null if no parent is specified
    /// @param parentGenerics  a list of generic type arguments for the parent class; can be null or empty if no generics are required
    /// @param interfaces      a map of interfaces with their generics
    /// @return the current [FXMLSourceCodeBuilder] instance for method chaining
    /// @throws IllegalStateException    if a class is already open
    /// @throws IllegalArgumentException if the `className` is blank
    /// @throws NullPointerException     if `className` is null
    public FXMLSourceCodeBuilder openClass(
            String className,
            String parentClassName,
            List<String> parentGenerics,
            Map<String, List<String>> interfaces
    ) {
        if (classOpen) {
            throw new IllegalStateException("Class has already been opened.");
        }
        addImports(Set.of("javax.annotation.processing.Generated", "java.util.ResourceBundle"));
        classOpen = true;
        this.className = Objects.requireNonNull(className);
        if (className.isBlank()) {
            throw new IllegalArgumentException("Class name must not be blank.");
        }
        builder.append("\n\n@Generated(value=\"").append(getClass().getName()).append("\", date=\"")
                .append(ZonedDateTime.now(ZoneOffset.UTC)).append("\")\n")
                .append("public abstract class ").append(className);
        if (parentClassName != null) {
            builder.append("\n    extends ").append(parentClassName);
            if (parentGenerics != null && !parentGenerics.isEmpty()) {
                builder.append("<");
                handleSequenceOfArguments(parentGenerics, Function.identity());
                builder.append(">");
            }
        }
        if (interfaces != null && !interfaces.isEmpty()) {
            builder.append("\n    implements ");
            handleSequenceOfArguments(
                    interfaces.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByKey())
                            .collect(Collectors.toList()),
                    entry -> entry.getKey() + "<" + String.join(", ", entry.getValue()) + ">"
            );
        }
        builder.append(" {\n");
        return this;
    }

    /// Adds a resource bundle declaration to the source code being built.
    /// This method appends a public static final ResourceBundle declaration for the specified path to the source code.
    /// The resource bundle helps enable localization for the generated class.
    ///
    /// This method can only be called after a class definition has been opened and cannot be used once the builder is marked as finished.
    ///
    /// @param resourceBundleStaticFieldPath the fully qualified static field path of the resource bundle to be added, representing the resource bundle used in the FXML class.
    /// @return the current instance of FXMLSourceCodeBuilder for method chaining
    /// @throws IllegalStateException if the class has not been opened or the builder is marked as finished
    public FXMLSourceCodeBuilder addResourceBundle(String resourceBundleStaticFieldPath) {
        checkIfClassIsOpenAndBuilderIsNotFinished();
        builder.append(indent("private static final ResourceBundle RESOURCE_BUNDLE = ", 1))
                .append(resourceBundleStaticFieldPath).append(";\n\n");
        return this;
    }

    /// Adds a new field declaration to the source code being built.
    /// The field can be marked as either private or protected and supports specifying generic types.
    /// The method appends the field declaration to the source code builder and
    /// enforces that the class must be open before calling this method.
    ///
    /// @param internal  whether the field should be private (true) or protected (false)
    /// @param fieldName the name of the field to be declared
    /// @param fieldType the type of the field to be declared
    /// @param generics  a list of generic types to include in the field's declaration; can be null or empty
    /// @return the current instance of [FXMLSourceCodeBuilder] for method chaining
    /// @throws IllegalStateException if the class has not been opened, or the builder has been marked as finished
    public FXMLSourceCodeBuilder addField(boolean internal, String fieldName, String fieldType, List<String> generics) {
        checkIfClassIsOpenAndBuilderIsNotFinished();
        if (internal) {
            builder.append(indent("private ", 1));
        } else {
            builder.append(indent("protected ", 1));
        }
        builder.append("final ").append(fieldType);
        if (generics != null && !generics.isEmpty()) {
            builder.append("<");
            handleSequenceOfArguments(generics, Function.identity());
            builder.append(">");
        }
        builder.append(" ").append(fieldName).append(";\n");
        return this;
    }

    /// Adds an abstract method declaration to the source code being built.
    /// This method creates a new abstract method with the specified name, return type, and parameters,
    /// appending it to the internal source code builder.
    ///
    /// @param method the FXMLMethod object representing the method to be added
    /// @return the current instance of FXMLSourceCodeBuilder for method chaining
    /// @throws IllegalStateException if the class has not been opened, or if the builder is marked as finished
    public FXMLSourceCodeBuilder addAbstractMethod(FXMLMethod method) {
        checkIfClassIsOpenAndBuilderIsNotFinished();
        addLine();
        builder.append(indent("protected abstract ", 1))
                .append(TypeEncoder.typeToTypeString(method.returnType(), method.namedGenerics()))
                .append(" ").append(method.name()).append("(");
        Function<Type, String> typeToTypeString = type -> TypeEncoder.typeToTypeString(type, method.namedGenerics());
        AtomicInteger counter = new AtomicInteger(0);
        handleSequenceOfArguments(
                method.parameters(),
                typeToTypeString.andThen(p -> p + " param" + counter.getAndIncrement())
        );
        builder.append(");\n");
        return this;
    }

    /// Adds a constructor to the generated FXML source code.
    /// This method initializes the constructor, ensuring that objects are constructed,
    /// non-constructor properties are set, and wrapping objects are bound accordingly.
    /// Only one constructor can be added to the source code.
    ///
    /// @param root The root FXMLNode used for constructing objects and setting properties.
    /// @return An instance of [FXMLSourceCodeBuilder] to allow method chaining.
    /// @throws IllegalStateException If a constructor has already been added.
    public FXMLSourceCodeBuilder addConstructor(FXMLNode root) {
        checkIfClassIsOpenAndBuilderIsNotFinished();
        if (hasConstructor) {
            throw new IllegalStateException("Constructor has already been set.");
        }
        hasConstructor = true;
        addLine();
        builder.append(indent("protected ", 1)).append(className).append("() {\n");
        constructObjects(root, new HashSet<>());
        builder.append("\n").append(indent("super();\n\n", 2));
        setNonConstructorProperties(root, new HashSet<>());
        bindWrappingObjects(null, root, new HashSet<>());
        builder.append(indent("}\n", 1));
        return this;
    }

    /// Adds a new line to the source code being built.
    /// This method appends a newline character to the internal source code builder.
    /// The class declaration must already be opened, and the builder must not be marked as finished.
    ///
    /// @return the current instance of FXMLSourceCodeBuilder for method chaining
    /// @throws IllegalStateException if the class has not been opened or the builder is marked as finished
    public FXMLSourceCodeBuilder addLine() {
        checkIfClassIsOpenAndBuilderIsNotFinished();
        builder.append("\n");
        return this;
    }

    /// Finalizes and builds the FXML source code and returns it as a string.
    /// This method ensures that the class definition is properly closed if it has not been already.
    /// If the class has not been opened, an IllegalStateException is thrown.
    /// Once called, the builder is considered finished and cannot be modified further.
    ///
    /// @return the complete FXML source code as a string
    /// @throws IllegalStateException if the class has not been opened
    public String build() {
        if (!classOpen) {
            throw new IllegalStateException("Class has not been opened.");
        }
        if (!finished) {
            builder.append("}\n");
            finished = true;
        }
        return builder.toString();
    }

    /// Constructs and initializes objects based on the structure of the provided [FXMLNode].
    ///
    /// @param node       The root [FXMLNode] from which to start constructing objects.
    /// @param seenFields A set of field identifiers that have already been processed to ensure no duplicate processing occurs.
    private void constructObjects(FXMLNode node, Set<String> seenFields) {
        switch (node) {
            case FXMLConstantNode _, FXMLWrapperNode _, FXMLStaticMethod _ -> {
            }
            case FXMLValueNode(_, String identifier, Type type, String value) -> {
                if (!THIS.equals(identifier) && seenFields.add(identifier)) {
                    builder.append(indent(identifier, 2)).append(" = ").append(encodeTypeValue(type, value)).append(";\n");
                }
            }
            case FXMLObjectNode(
                    _, String identifier, Class<?> clazz, List<FXMLProperty> properties, _, List<String> genericTypes
            ) -> {
                if (!THIS.equals(identifier) && seenFields.add(identifier)) {
                    builder.append(indent(identifier, 2)).append(" = new ").append(clazz.getSimpleName());
                    if (!genericTypes.isEmpty()) {
                        builder.append("<>");
                    }
                    builder.append("(");
                    addConstructorParameters(clazz, properties);
                    builder.append(");\n");
                }
            }
        }
        for (FXMLNode child : getChildren(node)) {
            constructObjects(child, seenFields);
        }
    }

    /// Configures and associates constructor parameters for the specified class and its properties.
    /// Validates the presence of a matching constructor in the class, ensuring the parameter types
    /// and annotations align with the given properties.
    /// Throws an exception if either no valid constructor or multiple matching constructors are found.
    ///
    /// @param clazz      the class whose constructor parameters are to be processed
    /// @param properties a list of FXMLProperty objects to be associated with the constructor parameters
    private void addConstructorParameters(Class<?> clazz, List<FXMLProperty> properties) {
        Map<String, FXMLConstructorProperty> constructorProperties = properties.stream()
                .gather(CheckAndCast.of(FXMLConstructorProperty.class))
                .collect(Collectors.toMap(FXMLConstructorProperty::name, Function.identity()));
        findMinimalConstructor(clazz, constructorProperties);
    }

    /// Identifies and processes the minimal constructor of the given class that matches the specified set of
    /// constructor properties. A constructor is considered minimal if it has the least number of parameters
    /// while still satisfying the property name and annotation requirements.
    ///
    /// @param clazz                 the class whose constructors are being evaluated
    /// @param constructorProperties a map of property names to `FXMLConstructorProperty` that defines the required constructor parameters and their corresponding values
    private void findMinimalConstructor(Class<?> clazz, Map<String, FXMLConstructorProperty> constructorProperties) {
        Optional<List<Parameter>> minimalMatchingConstructor = Stream.of(clazz.getConstructors())
                .filter(constructor -> Stream.of(constructor.getParameters()).allMatch(parameter -> parameter.isAnnotationPresent(NamedArg.class)))
                .filter(c -> Set.copyOf(constructorParameterNames(c)).containsAll(constructorProperties.keySet()))
                .map(c -> List.of(c.getParameters()))
                .min(Comparator.comparing(List::size));

        List<Parameter> minimalConstructorParameters = minimalMatchingConstructor.orElseThrow(
                () -> new IllegalStateException("No matching constructor found for parameters: %s for type %s".formatted(
                        constructorProperties.keySet(), clazz
                ))
        );

        handleSequenceOfArguments(
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
        );
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
                            builder.append(indent(identifier, 2)).append(".").append(setter).append("(")
                                    .append(encodeTypeValue(type, value)).append(");\n");
                    case FXMLStaticProperty(_, Class<?> staticClass, String staticSetter, Type type, String value) ->
                            builder.append(indent(staticClass.getSimpleName(), 2)).append(".").append(staticSetter)
                                    .append("(").append(identifier).append(", ").append(encodeTypeValue(type, value))
                                    .append(");\n");
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
    /// @return a list of child nodes for the given FXML node,
    /// or an empty list if the node type does not contain children
    private List<FXMLNode> getChildren(FXMLNode node) {
        return switch (node) {
            case FXMLParentNode parentNode -> parentNode.children();
            case FXMLValueNode _, FXMLConstantNode _ -> List.of();
        };
    }

    /// Processes a sequence of arguments, applying a specified handling function to each
    /// argument and formatting the results into a comma-separated list enclosed in parentheses.
    ///
    /// @param <T>            the type of elements in the list of arguments
    /// @param arguments      the collection of arguments to process; if the collection is empty, the method performs no action
    /// @param handleArgument a function that processes each argument and converts it into a string representation
    private <T> void handleSequenceOfArguments(Collection<T> arguments, Function<T, String> handleArgument) {
        Iterator<T> iterator = arguments.iterator();
        if (iterator.hasNext()) {
            builder.append(handleArgument.apply(iterator.next()));
        }
        while (iterator.hasNext()) {
            builder.append(", ").append(handleArgument.apply(iterator.next()));
        }
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

    /// Verifies whether the class declaration has been opened and the builder has not yet been marked as finished.
    ///
    /// This method ensures that the builder is in a valid state before performing operations
    /// that require an open and editable class declaration. If the class has not been opened
    /// or the builder is already finished, an `IllegalStateException` is thrown.
    ///
    /// @throws IllegalStateException if the class has not been opened, or the builder has already been marked as finished.
    private void checkIfClassIsOpenAndBuilderIsNotFinished() {
        if (finished || !classOpen) {
            throw new IllegalStateException("Class has not been opened or builder is marked as finished.");
        }
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
                ) when Node.class.isAssignableFrom(clazz) -> builder.append(indent(objectIdentifier, 2))
                        .append(".getChildren().add(").append(identifier).append(");\n");
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
            builder.append(indent(clazz.getSimpleName(), 2)).append('.').append(setterName).append("(");
            handleSequenceOfArguments(
                    Stream.concat(
                            Stream.of(new FXMLValueNode(true, objectIdentifier, Object.class, "")),
                            children.stream()
                    ).toList(),
                    TypeEncoder::getIdentifier
            );
            builder.append(");\n");
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
                builder.append(indent(objectIdentifier, 2)).append(".").append(setterForName).append("(")
                        .append(TypeEncoder.getIdentifier(children.getFirst())).append(");\n");
                return;
            }
        }
        try {
            String listGetterName = Utils.getGetterName(identifier);
            Class<?> returnType = Utils.findCollectionGetterWithAllowedReturnType(clazz, identifier, listGetterName, paramType);
            if (ObservableList.class.isAssignableFrom(returnType)) {
                builder.append(indent(objectIdentifier, 2)).append(".").append(listGetterName).append("()")
                        .append(".addAll(");
                handleSequenceOfArguments(children, TypeEncoder::getIdentifier);
                builder.append(");\n");
            } else {
                for (FXMLNode child : children) {
                    builder.append(indent(objectIdentifier, 2)).append(".").append(listGetterName).append("()")
                            .append(".add(").append(TypeEncoder.getIdentifier(child)).append(");\n");
                }
            }
        } catch (IllegalStateException | NoSuchMethodException e) {
            log.warn("Unable to bind wrapping objects for %s".formatted(identifier), e);
        }
    }
}
