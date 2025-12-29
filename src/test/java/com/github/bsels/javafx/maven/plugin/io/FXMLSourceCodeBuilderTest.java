package com.github.bsels.javafx.maven.plugin.io;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstructorProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLController;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLField;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticProperty;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.Visibility;
import com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder.ParentClass;
import javafx.beans.NamedArg;
import javafx.scene.control.Button;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FXMLSourceCodeBuilderTest {

    @Mock
    private Log mockLog;

    private FXMLSourceCodeBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new FXMLSourceCodeBuilder(mockLog);
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructorInitializesAllFieldsCorrectly() {
            FXMLSourceCodeBuilder newBuilder = new FXMLSourceCodeBuilder(mockLog);

            String result = newBuilder
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("import javax.annotation.processing.Generated;")
                    .contains("public class TestClass");
        }
    }

    @Nested
    class SetPackageTests {

        @Test
        void setPackageSuccessfully() {
            String result = builder
                    .setPackage("com.example.test")
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result).contains("package com.example.test;");
        }

        @Test
        void setNullPackageReturnsThis() {
            FXMLSourceCodeBuilder result = builder.setPackage(null);

            assertThat(result).isSameAs(builder);
        }

        @Test
        void setBlankPackageReturnsThis() {
            FXMLSourceCodeBuilder result = builder.setPackage("   ");

            assertThat(result).isSameAs(builder);
        }

        @Test
        void setPackageTwiceThrowsIllegalStateException() {
            builder.setPackage("com.example.first");

            assertThatThrownBy(() -> builder.setPackage("com.example.second"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Package is already set.");
        }
    }

    @Nested
    class AddImportTests {

        @Test
        void addImportSuccessfully() {
            String result = builder
                    .addImport("java.util.List")
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result).contains("import java.util.List;");
        }

        @Test
        void addNullImportThrowsNullPointerException() {
            assertThatThrownBy(() -> builder.addImport(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`importClass` cannot be null or blank.");
        }

        @Test
        void addBlankImportThrowsNullPointerException() {
            assertThatThrownBy(() -> builder.addImport("  "))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`importClass` cannot be null or blank.");
        }
    }

    @Nested
    class AddImportsTests {

        @Test
        void addMultipleImportsSuccessfully() {
            String result = builder
                    .addImports(List.of("java.util.List", "java.util.Map"))
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("import java.util.List;")
                    .contains("import java.util.Map;");
        }

        @Test
        void addNullImportsCollectionThrowsNullPointerException() {
            assertThatThrownBy(() -> builder.addImports(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`importClasses` must not be null");
        }

        @Test
        void addEmptyImportsReturnsThis() {
            FXMLSourceCodeBuilder result = builder.addImports(List.of());

            assertThat(result).isSameAs(builder);
        }
    }

    @Nested
    class SetResourceBundleTests {

        @Test
        void setResourceBundleSuccessfully() {
            String result = builder
                    .setResourceBundle("MyClass.BUNDLE")
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("import java.util.ResourceBundle;")
                    .contains("private static final ResourceBundle RESOURCE_BUNDLE = MyClass.BUNDLE;");
        }

        @Test
        void setNullResourceBundleReturnsThis() {
            FXMLSourceCodeBuilder result = builder.setResourceBundle(null);

            assertThat(result).isSameAs(builder);
        }

        @Test
        void setBlankResourceBundleReturnsThis() {
            FXMLSourceCodeBuilder result = builder.setResourceBundle("  ");

            assertThat(result).isSameAs(builder);
        }

        @Test
        void setResourceBundleTwiceThrowsIllegalStateException() {
            builder.setResourceBundle("MyClass.BUNDLE");

            assertThatThrownBy(() -> builder.setResourceBundle("AnotherClass.BUNDLE"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Resource bundle is already set.");
        }
    }

    @Nested
    class AddMethodTests {

        @Test
        void addMethodWithoutControllerCreatesAbstractMethod() {
            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("protected abstract void handleClick();");
            verify(mockLog).debug("Controller method not found for handleClick, creating an abstract method");
        }

        @Test
        void addMethodWithNullThrowsNullPointerException() {
            builder.openClass("TestClass", null, null);

            assertThatThrownBy(() -> builder.addMethod(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`method` must not be null");
        }

        @Test
        void addMethodWithControllerPublicMethodCallsDirectly() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PUBLIC,
                            "handleClick",
                            void.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("protected void handleClick() {")
                    .contains("$internalController$.handleClick();")
                    .doesNotContain("abstract");
        }

        @Test
        void addMethodWithControllerPrivateMethodUsesReflection() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PRIVATE,
                            "handleClick",
                            void.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("private final java.lang.reflect.Method $reflectionMethod$0;")
                    .contains("// Initialize reflection-based method handlers")
                    .contains("$reflectionMethod$0 = TestController.class.getDeclaredMethod(\"handleClick\");")
                    .contains("$reflectionMethod$0.setAccessible(true);")
                    .contains("try {")
                    .contains("} catch (Throwable e) {");
        }

        @Test
        void addMethodWithControllerProtectedMethodSamePackageCallsDirectly() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PROTECTED,
                            "handleClick",
                            void.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.github.bsels.javafx.maven.plugin.io")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("$internalController$.handleClick();")
                    .doesNotContain("getDeclaredMethod");
        }

        @Test
        void addMethodWithControllerProtectedMethodDifferentPackageUsesReflection() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PROTECTED,
                            "handleClick",
                            void.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example.different")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("getDeclaredMethod");
        }

        @Test
        void addMethodWithNonVoidReturnTypeIncludesReturn() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PUBLIC,
                            "getValue",
                            String.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "getValue",
                    List.of(),
                    String.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("protected String getValue() {")
                    .contains("return $internalController$.getValue();");
        }

        @Test
        void addMethodWithParametersIncludesParameters() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PUBLIC,
                            "process",
                            void.class,
                            List.of(String.class, Integer.class)
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "process",
                    List.of(String.class, Integer.class),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("protected void process(String param0, Integer param1) {")
                    .contains("$internalController$.process(param0, param1);");
        }

        @Test
        void addMethodWithIncompatibleReturnTypeCreatesAbstractMethod() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PUBLIC,
                            "getValue",
                            Integer.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "getValue",
                    List.of(),
                    String.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("protected abstract String getValue();");
        }

        @Test
        void addMethodWithIncompatibleParametersCreatesAbstractMethod() {
            FXMLController fxmlController = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PUBLIC,
                            "process",
                            void.class,
                            List.of(Integer.class)
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "process",
                    List.of(String.class),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .setPackage("com.example")
                    .openClass("TestClass", null, null)
                    .setFXMLController(fxmlController)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("protected abstract void process(String param0);");
        }
    }

    @Nested
    class AddFieldTests {

        @Test
        void addInternalFieldCreatesPrivateField() {
            FXMLField field = new FXMLField(
                    String.class,
                    "myField",
                    true,
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(field)
                    .build();

            assertThat(result).contains("private final String myField;");
        }

        @Test
        void addNonInternalFieldCreatesProtectedField() {
            FXMLField field = new FXMLField(
                    String.class,
                    "myField",
                    false,
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(field)
                    .build();

            assertThat(result).contains("protected final String myField;");
        }

        @Test
        void addFieldWithGenerics() {
            FXMLField field = new FXMLField(
                    List.class,
                    "myList",
                    false,
                    List.of("String")
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(field)
                    .build();

            assertThat(result).contains("protected final List<String> myList;");
        }

        @Test
        void addNullFieldThrowsNullPointerException() {
            builder.openClass("TestClass", null, null);

            assertThatThrownBy(() -> builder.addField(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`field` must not be null");
        }
    }

    @Nested
    class OpenClassTests {

        @Test
        void openClassWithNameOnly() {
            String result = builder
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("public class TestClass {")
                    .doesNotContain("extends")
                    .doesNotContain("implements");
        }

        @Test
        void openClassWithParentClassWithoutGenerics() {
            ParentClass parent = new ParentClass("BaseClass", null);

            String result = builder
                    .openClass("TestClass", parent, null)
                    .build();

            assertThat(result).contains("extends BaseClass");
        }

        @Test
        void openClassWithParentClassWithGenerics() {
            ParentClass parent = new ParentClass("BaseClass", List.of("String"));

            String result = builder
                    .openClass("TestClass", parent, null)
                    .build();

            assertThat(result).contains("extends BaseClass<String>");
        }

        @Test
        void openClassWithInterfacesCreatesAbstractClass() {
            Map<String, List<String>> interfaces = Map.of(
                    "Runnable", List.of(),
                    "Comparable", List.of("String")
            );

            String result = builder
                    .openClass("TestClass", null, interfaces)
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("implements");
        }

        @Test
        void openClassWithEmptyInterfacesMapDoesNotImplement() {
            String result = builder
                    .openClass("TestClass", null, Map.of())
                    .build();

            assertThat(result).doesNotContain("implements");
        }

        @Test
        void openClassWithNullClassNameThrowsNullPointerException() {
            assertThatThrownBy(() -> builder.openClass(null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`className` must not be null");
        }

        @Test
        void openClassTwiceThrowsIllegalStateException() {
            builder.openClass("TestClass", null, null);

            assertThatThrownBy(() -> builder.openClass("AnotherClass", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class definition is already set.");
        }
    }

    @Nested
    class SetFXMLControllerTests {

        @Test
        void setFXMLControllerSuccessfully() {
            FXMLController controller = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .setFXMLController(controller)
                    .build();

            assertThat(result)
                    .contains("private final TestController $internalController$;")
                    .contains("$internalController$ = new TestController();");
        }

        @Test
        void setFXMLControllerTwiceThrowsIllegalStateException() {
            FXMLController controller = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of()
            );

            builder.setFXMLController(controller);

            assertThatThrownBy(() -> builder.setFXMLController(controller))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Controller is already set.");
        }

        @Test
        void setNullFXMLControllerThrowsNullPointerException() {
            assertThatThrownBy(() -> builder.setFXMLController(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`controller` must not be null");
        }
    }

    @Nested
    class HandleFXMLNodeTests {

        @Test
        void handleFXMLObjectNodeAsRootSetsIsRoot() {
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    false,
                    "this",
                    Button.class,
                    List.of(),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .handleFXMLNode(rootNode)
                    .build();

            assertThat(result).contains("public abstract class TestClass");
        }

        @Test
        void handleFXMLValueNodeCreatesFieldInitializer() {
            FXMLValueNode valueNode = new FXMLValueNode(
                    false,
                    "myString",
                    String.class,
                    "Hello World"
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(String.class, "myString", false, List.of()))
                    .handleFXMLNode(valueNode)
                    .build();

            assertThat(result).contains("myString = \"Hello World\";");
        }

        @Test
        void handleFXMLObjectNodeCreatesObjectConstruction() {
            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myButton",
                    Button.class,
                    List.of(),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(Button.class, "myButton", false, List.of()))
                    .handleFXMLNode(objectNode)
                    .build();

            assertThat(result).contains("myButton = new Button();");
        }

        @Test
        void handleFXMLObjectNodeWithGenericsUsesEmptyDiamondOperator() {
            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myList",
                    ArrayList.class,
                    List.of(),
                    List.of(),
                    List.of("String")
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(List.class, "myList", false, List.of("String")))
                    .handleFXMLNode(objectNode)
                    .build();

            assertThat(result).contains("myList = new ArrayList<>();");
        }

        @Test
        void handleFXMLObjectNodeAsThisCreatesSuperCall() {
            FXMLObjectNode thisNode = new FXMLObjectNode(
                    false,
                    "this",
                    Button.class,
                    List.of(),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", new ParentClass("Button", null), null)
                    .handleFXMLNode(thisNode)
                    .build();

            assertThat(result).contains("super();");
        }

        @Test
        void handleFXMLNodeWithObjectPropertySetsSetter() {
            FXMLObjectProperty property = new FXMLObjectProperty(
                    "text",
                    "setText",
                    String.class,
                    "Click Me"
            );

            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myButton",
                    Button.class,
                    List.of(property),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(Button.class, "myButton", false, List.of()))
                    .handleFXMLNode(objectNode)
                    .build();

            assertThat(result).contains("myButton.setText(\"Click Me\");");
        }

        @Test
        void handleFXMLNodeWithStaticPropertySetsStaticSetter() {
            FXMLStaticProperty property = new FXMLStaticProperty(
                    "margin",
                    Button.class,
                    "setMargin",
                    Double.class,
                    "5.0"
            );

            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myButton",
                    Button.class,
                    List.of(property),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(Button.class, "myButton", false, List.of()))
                    .handleFXMLNode(objectNode)
                    .build();

            assertThat(result).contains("Button.setMargin(myButton, 5.0);");
        }

        @Test
        void handleFXMLNodeWithConstructorPropertyUsesConstructor() {
            FXMLConstructorProperty property = new FXMLConstructorProperty(
                    "text",
                    "Hello",
                    String.class
            );

            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myButton",
                    TestButtonWithNamedArg.class,
                    List.of(property),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(TestButtonWithNamedArg.class, "myButton", false, List.of()))
                    .handleFXMLNode(objectNode)
                    .build();

            assertThat(result).contains("myButton = new TestButtonWithNamedArg(\"Hello\");");
        }

        @Test
        void handleFXMLNodeWithMissingConstructorPropertyThrowsIllegalStateException() {
            FXMLConstructorProperty property = new FXMLConstructorProperty(
                    "nonExistentProperty",
                    "value",
                    String.class
            );

            FXMLObjectNode objectNode = new FXMLObjectNode(
                    false,
                    "myButton",
                    TestButtonWithNamedArg.class,
                    List.of(property),
                    List.of(),
                    List.of()
            );

            builder.openClass("TestClass", null, null)
                    .addField(new FXMLField(TestButtonWithNamedArg.class, "myButton", false, List.of()));

            assertThatThrownBy(() -> builder.handleFXMLNode(objectNode))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No matching constructor found");
        }
    }

    @Nested
    class BuildTests {

        @Test
        void buildWithPackageIncludesPackageDeclaration() {
            String result = builder
                    .setPackage("com.example.test")
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result).startsWith("package com.example.test;");
        }

        @Test
        void buildWithoutPackageDoesNotIncludePackageDeclaration() {
            String result = builder
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result).doesNotContain("package");
        }

        @Test
        void buildWithoutOpenClassThrowsIllegalStateException() {
            assertThatThrownBy(() -> builder.build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Class definition is not set.");
        }

        @Test
        void buildCreatesConcreteClassByDefault() {
            String result = builder
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("public class TestClass")
                    .doesNotContain("abstract")
                    .contains("public TestClass() {");
        }

        @Test
        void buildCreatesAbstractClassWhenIsRootIsSet() {
            FXMLObjectNode rootNode = new FXMLObjectNode(
                    false,
                    "this",
                    Button.class,
                    List.of(),
                    List.of(),
                    List.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .handleFXMLNode(rootNode)
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("protected TestClass() {");
        }

        @Test
        void buildCreatesAbstractClassWhenInterfacesAreSet() {
            String result = builder
                    .openClass("TestClass", null, Map.of("Runnable", List.of()))
                    .build();

            assertThat(result)
                    .contains("public abstract class TestClass")
                    .contains("protected TestClass() {");
        }

        @Test
        void buildIncludesGeneratedAnnotation() {
            String result = builder
                    .openClass("TestClass", null, null)
                    .build();

            assertThat(result)
                    .contains("@Generated(value=\"com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder\"")
                    .contains("date=\"");
        }

        @Test
        void buildIncludesFieldsAndConstructor() {
            String result = builder
                    .openClass("TestClass", null, null)
                    .addField(new FXMLField(String.class,"myField", false,  List.of()))
                    .build();

            assertThat(result)
                    .contains("protected final String myField;")
                    .contains("public TestClass() {");
        }

        @Test
        void buildWithReflectionMethodsIncludesTryCatchBlock() {
            FXMLController controller = new FXMLController(
                    TestController.class.getSimpleName(),
                    TestController.class,
                    List.of(),
                    List.of(new ControllerMethod(
                            Visibility.PRIVATE,
                            "handleClick",
                            void.class,
                            List.of()
                    ))
            );

            FXMLMethod method = new FXMLMethod(
                    "handleClick",
                    List.of(),
                    void.class,
                    Map.of()
            );

            String result = builder
                    .openClass("TestClass", null, null)
                    .setFXMLController(controller)
                    .addMethod(method)
                    .build();

            assertThat(result)
                    .contains("// Initialize reflection-based method handlers")
                    .contains("try {")
                    .contains("} catch (Throwable e) {")
                    .contains("throw new RuntimeException(e);")
                    .contains("// End reflection-based method handlers");
        }

        @Test
        void buildSortsAndDeduplicatesImports() {
            String result = builder
                    .addImport("java.util.Map")
                    .addImport("java.util.List")
                    .addImport("java.util.Map")
                    .openClass("TestClass", null, null)
                    .build();

            String importsSection = result.substring(0, result.indexOf("@Generated"));
            int firstMapIndex = importsSection.indexOf("import java.util.Map;");
            int lastMapIndex = importsSection.lastIndexOf("import java.util.Map;");

            assertThat(firstMapIndex).isEqualTo(lastMapIndex);
        }
    }

    @Nested
    class ParentClassTests {

        @Test
        void parentClassWithGenerics() {
            ParentClass parent = new ParentClass("BaseClass", List.of("String", "Integer"));

            assertThat(parent.parentClassName()).isEqualTo("BaseClass");
            assertThat(parent.generics()).containsExactly("String", "Integer");
        }

        @Test
        void parentClassWithoutGenerics() {
            ParentClass parent = new ParentClass("BaseClass", null);

            assertThat(parent.parentClassName()).isEqualTo("BaseClass");
            assertThat(parent.generics()).isEmpty();
        }

        @Test
        void parentClassWithEmptyGenerics() {
            ParentClass parent = new ParentClass("BaseClass", List.of());

            assertThat(parent.parentClassName()).isEqualTo("BaseClass");
            assertThat(parent.generics()).isEmpty();
        }

        @Test
        void parentClassWithNullNameThrowsNullPointerException() {
            assertThatThrownBy(() -> new ParentClass(null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`parentClassName` must not be null");
        }

        @Test
        void parentClassGenericsAreImmutable() {
            ParentClass parent = new ParentClass("BaseClass", List.of("String"));

            assertThatThrownBy(() -> parent.generics().add("Integer"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ThisConstantTests {

        @Test
        void thisConstantHasCorrectValue() {
            assertThat(FXMLSourceCodeBuilder.THIS).isEqualTo("this");
        }
    }

    // Test helper classes
    public static class TestController {
        public void handleClick() {
        }

        protected void handleProtected() {
        }

        private void handlePrivate() {
        }

        public String getValue() {
            return "value";
        }

        public void process(String s, Integer i) {
        }
    }

    public static class TestButtonWithNamedArg {
        public TestButtonWithNamedArg(@NamedArg("text") String text) {
        }
    }
}
