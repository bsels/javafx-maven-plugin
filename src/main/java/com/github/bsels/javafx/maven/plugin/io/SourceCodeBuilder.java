package com.github.bsels.javafx.maven.plugin.io;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLController;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.Visibility;
import com.github.bsels.javafx.maven.plugin.utils.TypeEncoder;
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

public class SourceCodeBuilder {
    private static final String INTERNAL_CONTROLLER_FIELD = "$internalController$";
    private static final String INTERNAL_REFLECTION_METHOD_FIELD = "$reflectionMethod$%d";
    private static final String PARAM_NAME_FORMAT = "param%d";

    private final List<String> imports;
    private final List<String> fields;
    private final List<String> fieldInitializers;
    private final List<String> reflectionMethodInitializers;
    private final List<String> constructorBody;
    private final List<String> methods;
    private final Log log;
    private String packageLine;
    private String superLine;
    private Boolean isAbstractClass;
    private String classDefinition;
    private String className;
    private boolean isRoot;
    private FXMLController controller;
    private boolean resourceBundleSet;
    private int reflectionMethodCounter;

    public SourceCodeBuilder(Log log) {
        packageLine = null;
        imports = new ArrayList<>();
        isAbstractClass = null;
        classDefinition = null;
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
        super();
        imports.add("java.lang.annotation.Generated");
    }

    /// Sets the package declaration for the source code being built.
    /// This will define the package name used in the generated source file.
    ///
    /// @param packageName the name of the package to be set in the source code
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    public SourceCodeBuilder setPackage(String packageName) {
        packageLine = "package " + packageName + ";";
        return this;
    }

    /// Adds an import statement for the specified class to the source code being built.
    /// This method prepends "import" and appends a semicolon to the class name and ensures the import is included in
    /// the set of imports for the generated source.
    ///
    /// @param importClass the fully qualified name of the class to be imported
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    public SourceCodeBuilder addImport(String importClass) {
        imports.add("import %s;".formatted(importClass));
        return this;
    }

    /// Adds multiple import statements for the specified classes to the source code being built.
    /// Each class name in the provided collection will be processed, and corresponding import statements will be added.
    ///
    /// @param importClasses a collection of fully qualified class names to be imported
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    public SourceCodeBuilder addImports(Collection<String> importClasses) {
        importClasses.forEach(this::addImport);
        return this;
    }

    /// Sets the resource bundle to the source code being built.
    /// This method appends the necessary import and field declaration for the specified resource bundle.
    ///
    /// @param resourceBundleStaticFieldPath the static field path of the resource bundle to be added; it should define the full reference to the ResourceBundle in the code.
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    public SourceCodeBuilder setResourceBundle(String resourceBundleStaticFieldPath) {
        if (resourceBundleSet) {
            throw new IllegalStateException("Resource bundle is already set.");
        }
        resourceBundleSet = true;
        imports.add("java.util.ResourceBundle;");
        fields.add("private static final ResourceBundle RESOURCE_BUNDLE = %s;".formatted(resourceBundleStaticFieldPath));
        return this;
    }

    /// Adds a method to the source code being built.
    /// Constructs the method signature and appends it to the internal collection of methods.
    /// If the corresponding controller method is absent,
    /// marks the class as abstract and adds an abstract method signature.
    ///
    /// @param method the `FXMLMethod` representing the method details such as return type, parameters, and method name to be added
    /// @return the `SourceCodeBuilder` instance, allowing for method chaining
    public SourceCodeBuilder addMethod(FXMLMethod method) {
        Optional<ControllerMethod> matchingMethod;
        if (controller == null) {
            matchingMethod = Optional.empty();
        } else {
            matchingMethod = findMatchingMethod(method);
        }
        StringBuilder builder = new StringBuilder()
                .append(indent("protected ", 1));
        if (matchingMethod.isEmpty()) {
            log.debug("Controller method not found for %s, creating an abstract method".formatted(method.name()));
            isAbstractClass = true;
            builder.append("abstract ");
        }
        builder.append(TypeEncoder.typeToTypeString(method.returnType(), method.namedGenerics()))
                .append(" ").append(method.name()).append("(");
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

    /// Starts the definition of a new class in the source code builder.
    /// This method initializes the class name, parent class (if specified), and the implemented interfaces (if any).
    /// It also constructs the necessary "extends" and "implements" clauses for the class definition based on the inputs.
    ///
    /// @param className   the name of the class to be opened; must not be null
    /// @param parentClass the parent class of the new class; can be null if the class does not extend any parent
    /// @param interfaces  a map where the key is the name of the interface and the value is the list of generics associated with the interface; can be null or empty if no interfaces are implemented
    /// @return the current instance of `SourceCodeBuilder` for method chaining
    /// @throws NullPointerException if the `className` is null
    public SourceCodeBuilder openClass(String className, ParentClass parentClass, Map<String, List<String>> interfaces) {
        Objects.requireNonNull(className, "`className` must not be null");
        this.className = className;
        StringBuilder builder = new StringBuilder()
                .append("class ").append(className);
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
                    entry ->
                            entry.getKey() + constructGenerics(entry.getValue(), new StringBuilder()).toString()
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
    public SourceCodeBuilder setFXMLController(FXMLController controller) {
        if (this.controller != null) {
            throw new IllegalStateException("Controller is already set.");
        }
        fields.add("private final %s %s;".formatted(controller.className(), INTERNAL_CONTROLLER_FIELD));
        fieldInitializers.add("%s = new %s();".formatted(INTERNAL_CONTROLLER_FIELD, controller.className()));
        this.controller = controller;
        return this;
    }

    // TODO: Handle FXML Node

    /// Builds a string representation of a Java source code file based on the configured package, imports,
    /// class definition, fields, and methods.
    /// The method ensures that all relevant structural elements of the class are appended to the resulting source code
    /// in the correct order.
    ///
    /// @return a string representing the complete source code of the defined class
    /// @throws IllegalStateException if the class definition is not set
    public String build() {
        if (classDefinition == null) {
            throw new IllegalStateException("Class definition is not set.");
        }

        boolean isAbstract = isAbstractClass != null ? isAbstractClass : isRoot;

        StringBuilder builder = new StringBuilder();
        if (packageLine != null) {
            builder.append(packageLine).append("\n\n");
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
                .append("\n\n")
                .append("public ");
        if (isAbstract) {
            builder.append("abstract ");
        }
        builder.append(classDefinition)
                .append(" {\n\n");
        fields.forEach(f -> builder.append(indent(f, 1)).append("\n"));
        builder.append("\n\n")
                .append(indent(" ", 1))
                .append("public")
                .append(className)
                .append(" {\n");
        fieldInitializers.stream()
                .sorted()
                .distinct()
                .forEach(f -> builder.append(indent(f, 2)).append("\n"));
        builder.append(indent("\n", 1));
        if (superLine != null) {
            builder.append(indent(superLine, 2)).append("\n");
        } else {
            builder.append(indent("super();\n", 2));
        }
        if (!reflectionMethodInitializers.isEmpty()) {
            builder.append("\n")
                    .append(indent("// Initialize reflection-based method handlers\n", 1))
                    .append(indent("try {\n", 2));
            reflectionMethodInitializers.forEach(f -> builder.append(indent(f, 3)).append("\n"));
            builder.append(indent("} catch (Throwable e) {\n", 2))
                    .append(indent("throw new RuntimeException(e);\n", 3))
                    .append(indent("}\n\n", 2))
                    .append(indent("// End reflection-based method handlers, continue with the rest of the constructor body\n", 1));
        }
        constructorBody.forEach(f -> builder.append(indent(f, 2)).append("\n"));
        builder.append(indent("}\n\n", 1));
        methods.forEach(m -> builder.append(m).append("\n"));
        builder.append("}\n");
        return builder.toString();
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
        return void.class.equals(method.returnType()) ||
                TypeEncoder.typeToClass(method.returnType())
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
        builder.append(" {");
        boolean isVoid = void.class.equals(fxmlMethod.returnType());
        if (Visibility.PUBLIC == method.visibility()) {
            log.debug("Calling public method %s on %s".formatted(method.name(), controller.className()));
            addReturnIfNeeded(builder, isVoid, 2)
                    .append(INTERNAL_CONTROLLER_FIELD).append(".").append(method.name()).append("(");
            handlerParameterSequence(builder, method)
                    .append(");");
        } else {
            log.debug("Calling non-public method %s on %s using reflection".formatted(method.name(), controller.className()));
            String reflectionMethodName = getNewReflectionMethodName();

            fields.add("private final java.lang.reflect.Method %s;".formatted(reflectionMethodName));
            reflectionMethodInitializers.add(
                    handleSequenceOfArguments(
                            new StringBuilder()
                                    .append(reflectionMethodName).append(" = ").append(controller.className())
                                    .append(".class.getDeclaredMethod(\"").append(method.name()).append("\", "),
                            method.parameterTypes(),
                            TypeEncoder::typeToReflectionClassString
                    ).append(");").toString()
            );
            constructorBody.add("%s.setAccessible(true);".formatted(reflectionMethodName));

            builder.append(indent("try {\n", 2));
            addReturnIfNeeded(builder, isVoid, 3)
                    .append("declaredMethod.invoke(%s, ".formatted(INTERNAL_CONTROLLER_FIELD));
            handlerParameterSequence(builder, method)
                    .append(");\n")
                    .append(indent("} catch (Throwable e) {\n", 2))
                    .append(indent("throw new RuntimeException(e);\n", 3))
                    .append(indent("}\n", 2));
        }
        builder.append(indent("}\n", 1));
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
        builder.append("<");
        return handleSequenceOfArguments(builder, generics, Function.identity())
                .append(">");
    }

    /// Processes a sequence of arguments, applying a specified handling function to each
    /// argument and formatting the results into a comma-separated list enclosed in parentheses.
    ///
    /// @param <T>            the type of elements in the list of arguments
    /// @param builder        the string builder to which the formatted list will be appended
    /// @param arguments      the collection of arguments to process; if the collection is empty, the method performs no action
    /// @param handleArgument a function that processes each argument and converts it into a string representation
    /// @return the provided string build
    private <T> StringBuilder handleSequenceOfArguments(
            StringBuilder builder,
            Collection<T> arguments,
            Function<T, String> handleArgument
    ) {
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
