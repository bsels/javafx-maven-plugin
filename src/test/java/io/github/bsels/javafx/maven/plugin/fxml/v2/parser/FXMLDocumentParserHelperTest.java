package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLConstants;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerField;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.FXMLControllerMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.controller.Visibility;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLExposedIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLNamedRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLMethod;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import javafx.beans.NamedArg;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/// Unit tests for [FXMLDocumentParserHelper].
class FXMLDocumentParserHelperTest {

    // -------------------------------------------------------------------------
    // Helper fixtures
    // -------------------------------------------------------------------------

    /// A simple class with a setter and getter.
    static class SimpleBean {
        private String value;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    /// A class with only a getter (no setter, no constructor param).
    static class GetterOnlyBean {
        public String getName() { return "name"; }
    }

    /// A class with a constructor parameter annotated with [@NamedArg].
    static class ConstructorParamBean {
        private final String label;

        public ConstructorParamBean(@NamedArg("label") String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
    }

    /// A class with multiple overloaded setters for the same property (ambiguous).
    static class AmbiguousSetterBean {
        public void setValue(String value) {}
        public void setValue(Integer value) {}
    }

    /// A class with multiple constructors having the same [@NamedArg] parameter name (ambiguous).
    static class AmbiguousConstructorBean {
        public AmbiguousConstructorBean(@NamedArg("label") String label) {}
        public AmbiguousConstructorBean(@NamedArg("label") Integer label) {}
    }

    /// A functional interface for testing method reference resolution.
    @FunctionalInterface
    interface MyEventHandler {
        void handle(ActionEvent event);
    }

    /// A non-functional interface (two abstract methods).
    interface NotFunctional {
        void methodA();
        void methodB();
    }

    /// A class with a static setter for testing [FXMLDocumentParserHelper#findStaticSetter].
    static class StaticSetterClass {
        public static void setConstraint(Object node, String value) {}
    }

    /// A class with multiple overloaded static setters for the same property (ambiguous).
    static class AmbiguousStaticSetterClass {
        public static void setConstraint(Object node, String value) {}
        public static void setConstraint(Object node, Integer value) {}
    }

    /// A valid public controller class with various members.
    public static class ValidController {
        public String publicField;
        protected String protectedField;
        String packagePrivateField;
        private String privateField;

        public ValidController() {}

        public void publicMethod() {}
        protected void protectedMethod() {}
        void packagePrivateMethod() {}
        private void privateMethod() {}
    }

    /// A controller class with inherited members.
    public static class InheritedController extends ValidController {
        public String childField;
        public void childMethod() {}
    }

    /// A private controller class (invalid).
    private static class PrivateController {
        public PrivateController() {}
    }

    /// An abstract controller class (invalid).
    public abstract static class AbstractController {
        public AbstractController() {}
    }

    /// A controller class without a public no-arg constructor (invalid).
    public static class NoNoArgConstructorController {
        public NoNoArgConstructorController(String arg) {}
    }

    /// A controller class with only a private no-arg constructor (invalid).
    public static class PrivateConstructorController {
        private PrivateConstructorController() {}
    }

    // -------------------------------------------------------------------------
    // Test setup
    // -------------------------------------------------------------------------

    private FXMLDocumentParserHelper helper;
    private BuildContext buildContext;

    /// Creates a fresh [FXMLDocumentParserHelper] and a default [BuildContext] before each test.
    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        helper = new FXMLDocumentParserHelper(log, StandardCharsets.UTF_8);
        buildContext = new BuildContext(
                List.of(
                        SimpleBean.class.getName(),
                        GetterOnlyBean.class.getName(),
                        ConstructorParamBean.class.getName(),
                        AmbiguousSetterBean.class.getName(),
                        AmbiguousConstructorBean.class.getName(),
                        StaticSetterClass.class.getName(),
                        AmbiguousStaticSetterClass.class.getName(),
                        String.class.getName()
                ),
                "/"
        );
    }

    // -------------------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------------------

    /// Tests for the constructor of [FXMLDocumentParserHelper].
    @Nested
    class ConstructorTest {

        /// Verifies that a null `log` throws [NullPointerException].
        @Test
        void nullLogThrowsNullPointerException() {
            assertThatThrownBy(() -> new FXMLDocumentParserHelper(null, StandardCharsets.UTF_8))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`log` must not be null");
        }

        /// Verifies that a null `defaultCharset` throws [NullPointerException].
        @Test
        void nullDefaultCharsetThrowsNullPointerException() {
            DefaultLog log = new DefaultLog(new ConsoleLogger());
            assertThatThrownBy(() -> new FXMLDocumentParserHelper(log, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`defaultCharset` must not be null");
        }

        /// Verifies that valid arguments construct the helper successfully.
        @Test
        void validArgumentsConstructSuccessfully() {
            DefaultLog log = new DefaultLog(new ConsoleLogger());
            assertThat(new FXMLDocumentParserHelper(log, StandardCharsets.UTF_8)).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // buildFXMLType tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#buildFXMLType].
    @Nested
    class BuildFXMLTypeTest {

        /// Verifies that a null `type` throws [NullPointerException].
        @Test
        void nullTypeThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.buildFXMLType(null, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`type` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.buildFXMLType(String.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a plain [Class] without type parameters returns [FXMLClassType].
        @Test
        void plainClassReturnsClassType() {
            assertThat(helper.buildFXMLType(String.class, buildContext))
                    .isInstanceOf(FXMLClassType.class)
                    .hasFieldOrPropertyWithValue("clazz", String.class);
        }

        /// Verifies that a [Class] with type parameters returns [FXMLGenericType].
        @Test
        void classWithTypeParametersReturnsGenericType() {
            assertThat(helper.buildFXMLType(List.class, buildContext))
                    .isInstanceOf(FXMLGenericType.class)
                    .hasFieldOrPropertyWithValue("type", List.class);
        }

        /// Verifies that a [ParameterizedType] (e.g. List&lt;String&gt;) returns [FXMLGenericType] with resolved args.
        @Test
        void parameterizedTypeReturnsGenericTypeWithArgs() throws Exception {
            class Holder { List<String> field; }
            var paramType = Holder.class.getDeclaredField("field").getGenericType();
            assertThat(helper.buildFXMLType(paramType, buildContext))
                    .isInstanceOf(FXMLGenericType.class)
                    .hasFieldOrPropertyWithValue("type", List.class);
        }

        /// Verifies that a [TypeVariable] not in the mapping returns a wildcard type.
        @Test
        void typeVariableNotInMappingReturnsWildcard() throws Exception {
            class Holder<T> { List<T> field; }
            var paramType = Holder.class.getDeclaredField("field").getGenericType();
            var typeVar = ((ParameterizedType) paramType).getActualTypeArguments()[0];
            assertThat(helper.buildFXMLType(typeVar, buildContext))
                    .isEqualTo(FXMLType.wildcard());
        }

        /// Verifies that a [TypeVariable] present in the mapping returns the mapped type.
        @Test
        void typeVariableInMappingReturnsMappedType() throws Exception {
            class Holder<T> { List<T> field; }
            var paramType = Holder.class.getDeclaredField("field").getGenericType();
            var typeVar = ((ParameterizedType) paramType).getActualTypeArguments()[0];
            FXMLType mappedType = FXMLType.of(String.class);
            BuildContext ctxWithMapping = new BuildContext(buildContext, Map.of("T", mappedType));
            assertThat(helper.buildFXMLType(typeVar, ctxWithMapping))
                    .isEqualTo(mappedType);
        }

        /// Verifies that a [WildcardType] with an upper bound (not Object) resolves to the upper bound.
        @Test
        void wildcardTypeWithUpperBoundResolvesToUpperBound() throws Exception {
            class Holder { List<? extends String> field; }
            var wildcardType = ((ParameterizedType) Holder.class.getDeclaredField("field").getGenericType())
                    .getActualTypeArguments()[0];
            assertThat(helper.buildFXMLType(wildcardType, buildContext))
                    .isInstanceOf(FXMLClassType.class)
                    .hasFieldOrPropertyWithValue("clazz", String.class);
        }

        /// Verifies that a [WildcardType] with a lower bound resolves to the lower bound.
        @Test
        void wildcardTypeWithLowerBoundResolvesToLowerBound() throws Exception {
            class Holder { List<? super String> field; }
            var wildcardType = ((ParameterizedType) Holder.class.getDeclaredField("field").getGenericType())
                    .getActualTypeArguments()[0];
            assertThat(helper.buildFXMLType(wildcardType, buildContext))
                    .isInstanceOf(FXMLClassType.class)
                    .hasFieldOrPropertyWithValue("clazz", String.class);
        }

        /// Verifies that an unbounded [WildcardType] (&lt;?&gt;) returns a wildcard type.
        @Test
        void unboundedWildcardTypeReturnsWildcard() throws Exception {
            class Holder { List<?> field; }
            var wildcardType = ((ParameterizedType) Holder.class.getDeclaredField("field").getGenericType())
                    .getActualTypeArguments()[0];
            assertThat(helper.buildFXMLType(wildcardType, buildContext))
                    .isEqualTo(FXMLType.wildcard());
        }

        /// Verifies that a [WildcardType] with an upper bound of [Object] (i.e. &lt;? extends Object&gt;) returns a wildcard type.
        @Test
        void wildcardTypeWithObjectUpperBoundReturnsWildcard() throws Exception {
            // ? extends Object is equivalent to ?, upper bound is Object so it falls through to wildcard
            class Holder { List<? extends Object> field; }
            var wildcardType = ((ParameterizedType) Holder.class.getDeclaredField("field").getGenericType())
                    .getActualTypeArguments()[0];
            assertThat(helper.buildFXMLType(wildcardType, buildContext))
                    .isEqualTo(FXMLType.wildcard());
        }

        /// Verifies that a [GenericArrayType] (default branch) throws [IllegalStateException] since it is unsupported.
        @Test
        void genericArrayTypeThrowsIllegalStateException() throws Exception {
            class Holder { List<String>[] field; }
            var genericArrayType = Holder.class.getDeclaredField("field").getGenericType();
            assertThatThrownBy(() -> helper.buildFXMLType(genericArrayType, buildContext))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // resolveTypeMapping tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#resolveTypeMapping].
    @Nested
    class ResolveTypeMappingTest {

        /// Verifies that a null `clazz` throws [NullPointerException].
        @Test
        void nullClazzThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveTypeMapping(null, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`clazz` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveTypeMapping(String.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a plain class returns a non-null mapping.
        @Test
        void plainClassReturnsNonNullMapping() {
            assertThat(helper.resolveTypeMapping(String.class, buildContext)).isNotNull();
        }

        /// Verifies that a generic class (e.g. List) returns a non-null mapping.
        @Test
        void genericClassReturnsNonNullMapping() {
            assertThat(helper.resolveTypeMapping(List.class, buildContext)).isNotNull();
        }

        /// Verifies that the returned mapping includes entries from the original context mapping.
        @Test
        void returnedMappingIncludesContextMappingEntries() {
            FXMLType mappedType = FXMLType.of(String.class);
            BuildContext ctxWithMapping = new BuildContext(buildContext, Map.of("T", mappedType));
            assertThat(helper.resolveTypeMapping(String.class, ctxWithMapping))
                    .containsEntry("T", mappedType);
        }
    }

    // -------------------------------------------------------------------------
    // findStaticSetter tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#findStaticSetter].
    @Nested
    class FindStaticSetterTest {

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findStaticSetter(null, "StaticSetterClass.constraint"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a null `name` throws [NullPointerException].
        @Test
        void nullNameThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findStaticSetter(buildContext, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`name` must not be null");
        }

        /// Verifies that an unresolvable class name returns empty.
        @Test
        void unresolvableClassReturnsEmpty() {
            assertThat(helper.findStaticSetter(buildContext, "com.example.UnknownClass.constraint"))
                    .isEmpty();
        }

        /// Verifies that a valid static setter is found and returned with correct fields.
        @Test
        void validStaticSetterReturnsPresent() {
            // Use binary name (with $) so ClassLoader.loadClass can resolve the inner class
            String qualifiedName = StaticSetterClass.class.getName() + ".constraint";
            assertThat(helper.findStaticSetter(buildContext, qualifiedName))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "constraint")
                    .hasFieldOrPropertyWithValue("staticClass", StaticSetterClass.class)
                    .hasFieldOrPropertyWithValue("setter", "setConstraint");
        }

        /// Verifies that a property with no matching static setter returns empty.
        @Test
        void noMatchingStaticSetterReturnsEmpty() {
            String qualifiedName = StaticSetterClass.class.getName() + ".nonExistentProperty";
            assertThat(helper.findStaticSetter(buildContext, qualifiedName))
                    .isEmpty();
        }

        /// Verifies that multiple matching static setters (ambiguous) returns empty.
        @Test
        void multipleStaticSettersReturnsEmpty() {
            String qualifiedName = AmbiguousStaticSetterClass.class.getName() + ".constraint";
            assertThat(helper.findStaticSetter(buildContext, qualifiedName))
                    .isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findObjectProperty tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#findObjectProperty].
    @Nested
    class FindObjectPropertyTest {

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findObjectProperty(null, SimpleBean.class, "value"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a null `clazz` throws [NullPointerException].
        @Test
        void nullClazzThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findObjectProperty(buildContext, null, "value"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`clazz` must not be null");
        }

        /// Verifies that a null `name` throws [NullPointerException].
        @Test
        void nullNameThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findObjectProperty(buildContext, SimpleBean.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`name` must not be null");
        }

        /// Verifies that a property with a setter is found via the setter path.
        @Test
        void propertyWithSetterFoundViaSetter() {
            assertThat(helper.findObjectProperty(buildContext, SimpleBean.class, "value"))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("methodType", ObjectProperty.MethodType.SETTER)
                    .hasFieldOrPropertyWithValue("name", "value")
                    .hasFieldOrPropertyWithValue("methodName", Optional.of("setValue"));
        }

        /// Verifies that multiple setters for the same property returns empty.
        @Test
        void multipleSettersReturnsEmpty() {
            assertThat(helper.findObjectProperty(buildContext, AmbiguousSetterBean.class, "value"))
                    .isEmpty();
        }

        /// Verifies that a property with a constructor parameter is found via the constructor path.
        @Test
        void propertyWithConstructorParamFoundViaConstructor() {
            assertThat(helper.findObjectProperty(buildContext, ConstructorParamBean.class, "label"))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("methodType", ObjectProperty.MethodType.CONSTRUCTOR)
                    .hasFieldOrPropertyWithValue("name", "label")
                    .hasFieldOrPropertyWithValue("methodName", Optional.empty());
        }

        /// Verifies that multiple constructor parameters with the same name returns empty.
        @Test
        void multipleConstructorParamsReturnsEmpty() {
            assertThat(helper.findObjectProperty(buildContext, AmbiguousConstructorBean.class, "label"))
                    .isEmpty();
        }

        /// Verifies that a property with only a getter is found via the getter path.
        @Test
        void propertyWithGetterOnlyFoundViaGetter() {
            assertThat(helper.findObjectProperty(buildContext, GetterOnlyBean.class, "name"))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("methodType", ObjectProperty.MethodType.GETTER)
                    .hasFieldOrPropertyWithValue("name", "name")
                    .hasFieldOrPropertyWithValue("methodName", Optional.of("getName"));
        }

        /// Verifies that a property with no setter, constructor param, or getter returns empty.
        @Test
        void propertyWithNoMatchReturnsEmpty() {
            assertThat(helper.findObjectProperty(buildContext, SimpleBean.class, "nonExistent"))
                    .isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findMethodReferenceType tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#findMethodReferenceType].
    @Nested
    class FindMethodReferenceTypeTest {

        /// Verifies that a null `methodName` throws [NullPointerException].
        @Test
        void nullMethodNameThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findMethodReferenceType(null, MyEventHandler.class, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`methodName` must not be null");
        }

        /// Verifies that a null `paramType` throws [NullPointerException].
        @Test
        void nullParamTypeThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findMethodReferenceType("onAction", null, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`paramType` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.findMethodReferenceType("onAction", MyEventHandler.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a functional interface resolves to an [FXMLMethod] with correct name and parameters.
        @Test
        void functionalInterfaceReturnsCorrectFXMLMethod() {
            assertThat(helper.findMethodReferenceType("onAction", MyEventHandler.class, buildContext))
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", "onAction")
                    .satisfies(method -> {
                        assertThat(method.parameters())
                                .hasSize(1)
                                .first()
                                .isInstanceOf(FXMLClassType.class)
                                .hasFieldOrPropertyWithValue("clazz", ActionEvent.class);
                    });
        }

        /// Verifies that a standard functional interface ([EventHandler]) resolves correctly.
        @Test
        void standardFunctionalInterfaceEventHandlerResolves() {
            assertThat(helper.findMethodReferenceType("onAction", EventHandler.class, buildContext))
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", "onAction");
        }

        /// Verifies that a non-functional interface throws [IllegalArgumentException].
        @Test
        void nonFunctionalInterfaceThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.findMethodReferenceType("onAction", NotFunctional.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a functional interface");
        }

        /// Verifies that a plain class (not a functional interface) throws [IllegalArgumentException].
        @Test
        void plainClassThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.findMethodReferenceType("onAction", String.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a functional interface");
        }
    }

    // -------------------------------------------------------------------------
    // resolveResourcePath tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#resolveResourcePath].
    @Nested
    class ResolveResourcePathTest {

        /// Verifies that a null `value` throws [NullPointerException].
        @Test
        void nullValueThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveResourcePath(null, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`value` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveResourcePath("foo.fxml", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that an absolute path (starting with `/`) is returned unchanged.
        @Test
        void absolutePathReturnedUnchanged() {
            assertThat(helper.resolveResourcePath("/foo.fxml", buildContext))
                    .isEqualTo("/foo.fxml");
        }

        /// Verifies that a relative path with root context (`/`) is prepended correctly.
        @Test
        void relativePathWithRootContextPrependedCorrectly() {
            BuildContext rootCtx = new BuildContext(List.of(), "/");
            assertThat(helper.resolveResourcePath("foo.fxml", rootCtx))
                    .isEqualTo("/foo.fxml");
        }

        /// Verifies that a relative path with a non-root context is prepended correctly.
        @Test
        void relativePathWithNonRootContextPrependedCorrectly() {
            BuildContext examplesCtx = new BuildContext(List.of(), "/examples");
            assertThat(helper.resolveResourcePath("foo.fxml", examplesCtx))
                    .isEqualTo("/examples/foo.fxml");
        }

        /// Verifies that an absolute path is returned unchanged even with a non-root context.
        @Test
        void absolutePathWithNonRootContextReturnedUnchanged() {
            BuildContext examplesCtx = new BuildContext(List.of(), "/examples");
            assertThat(helper.resolveResourcePath("/foo.fxml", examplesCtx))
                    .isEqualTo("/foo.fxml");
        }
    }

    // -------------------------------------------------------------------------
    // resolveOptionalIdentifier tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#resolveOptionalIdentifier].
    @Nested
    class ResolveOptionalIdentifierTest {

        /// Verifies that a null `attributes` throws [NullPointerException].
        @Test
        void nullAttributesThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveOptionalIdentifier(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`attributes` must not be null");
        }

        /// Verifies that attributes without `fx:id` returns empty.
        @Test
        void attributesWithoutFxIdReturnsEmpty() {
            assertThat(helper.resolveOptionalIdentifier(Map.of("someAttr", "someValue")))
                    .isEmpty();
        }

        /// Verifies that attributes with `fx:id` returns an [FXMLExposedIdentifier].
        @Test
        void attributesWithFxIdReturnsExposedIdentifier() {
            assertThat(helper.resolveOptionalIdentifier(Map.of(FXMLConstants.FX_ID_ATTRIBUTE, "myId")))
                    .isPresent()
                    .get()
                    .isInstanceOf(FXMLExposedIdentifier.class);
        }

        /// Verifies that an empty attributes map returns empty.
        @Test
        void emptyAttributesReturnsEmpty() {
            assertThat(helper.resolveOptionalIdentifier(Map.of()))
                    .isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // resolveClassAndIdentifier tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#resolveClassAndIdentifier].
    @Nested
    class ResolveClassAndIdentifierTest {

        /// Verifies that a null `structure` throws [NullPointerException].
        @Test
        void nullStructureThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.resolveClassAndIdentifier(null, buildContext, true))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`structure` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            ParsedXMLStructure structure = new ParsedXMLStructure("String", Map.of(), List.of());
            assertThatThrownBy(() -> helper.resolveClassAndIdentifier(structure, null, true))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a root node without `fx:id` gets [FXMLRootIdentifier].
        @Test
        void rootNodeWithoutFxIdGetsRootIdentifier() {
            ParsedXMLStructure structure = new ParsedXMLStructure("String", Map.of(), List.of());
            assertThat(helper.resolveClassAndIdentifier(structure, buildContext, true))
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("clazz", String.class);
        }

        /// Verifies that a root node with `fx:id` gets [FXMLNamedRootIdentifier].
        @Test
        void rootNodeWithFxIdGetsNamedRootIdentifier() {
            Map<String, String> attrs = new HashMap<>();
            attrs.put(FXMLConstants.FX_ID_ATTRIBUTE, "rootId");
            ParsedXMLStructure structure = new ParsedXMLStructure("String", attrs, List.of());
            assertThat(helper.resolveClassAndIdentifier(structure, buildContext, true))
                    .extracting(ClassAndIdentifier::identifier)
                    .isInstanceOf(FXMLNamedRootIdentifier.class);
        }

        /// Verifies that a non-root node with `fx:id` gets [FXMLExposedIdentifier].
        @Test
        void nonRootNodeWithFxIdGetsExposedIdentifier() {
            Map<String, String> attrs = new HashMap<>();
            attrs.put(FXMLConstants.FX_ID_ATTRIBUTE, "myId");
            ParsedXMLStructure structure = new ParsedXMLStructure("String", attrs, List.of());
            assertThat(helper.resolveClassAndIdentifier(structure, buildContext, false))
                    .extracting(ClassAndIdentifier::identifier)
                    .isInstanceOf(FXMLExposedIdentifier.class);
        }

        /// Verifies that a non-root node without `fx:id` gets [FXMLInternalIdentifier].
        @Test
        void nonRootNodeWithoutFxIdGetsInternalIdentifier() {
            ParsedXMLStructure structure = new ParsedXMLStructure("String", Map.of(), List.of());
            assertThat(helper.resolveClassAndIdentifier(structure, buildContext, false))
                    .extracting(ClassAndIdentifier::identifier)
                    .isInstanceOf(FXMLInternalIdentifier.class);
        }

        /// Verifies that an unresolvable class name throws an exception.
        @Test
        void unresolvableClassNameThrowsException() {
            ParsedXMLStructure structure = new ParsedXMLStructure("UnknownClass", Map.of(), List.of());
            assertThatThrownBy(() -> helper.resolveClassAndIdentifier(structure, buildContext, false))
                    .isInstanceOf(Exception.class);
        }
    }

    // -------------------------------------------------------------------------
    // getCharsetOfElement tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#getCharsetOfElement].
    @Nested
    class GetCharsetOfElementTest {

        /// Verifies that a null `structure` throws [NullPointerException].
        @Test
        void nullStructureThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.getCharsetOfElement(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`structure` must not be null");
        }

        /// Verifies that a structure without a charset attribute returns the default charset.
        @Test
        void structureWithoutCharsetReturnsDefaultCharset() {
            assertThat(helper.getCharsetOfElement(new ParsedXMLStructure("element", Map.of(), List.of())))
                    .isEqualTo(StandardCharsets.UTF_8);
        }

        /// Verifies that a structure with a charset attribute returns the specified charset.
        @Test
        void structureWithCharsetAttributeReturnsSpecifiedCharset() {
            Map<String, String> props = new HashMap<>();
            props.put(FXMLConstants.CHARSET_ATTRIBUTE, "ISO-8859-1");
            assertThat(helper.getCharsetOfElement(new ParsedXMLStructure("element", props, List.of())))
                    .isEqualTo(StandardCharsets.ISO_8859_1);
        }

        /// Verifies that the default charset from the constructor is used when no charset attribute is present.
        @Test
        void defaultCharsetFromConstructorIsUsedWhenNoCharsetAttribute() {
            DefaultLog log = new DefaultLog(new ConsoleLogger());
            FXMLDocumentParserHelper helperWithLatin1 = new FXMLDocumentParserHelper(log, StandardCharsets.ISO_8859_1);
            assertThat(helperWithLatin1.getCharsetOfElement(new ParsedXMLStructure("element", Map.of(), List.of())))
                    .isEqualTo(StandardCharsets.ISO_8859_1);
        }
    }

    // -------------------------------------------------------------------------
    // introspectControllerClass tests
    // -------------------------------------------------------------------------

    /// Tests for [FXMLDocumentParserHelper#introspectControllerClass].
    @Nested
    class IntrospectControllerClassTest {

        /// Verifies that a null `clazz` throws [NullPointerException].
        @Test
        void nullClazzThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(null, buildContext))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`clazz` must not be null");
        }

        /// Verifies that a null `buildContext` throws [NullPointerException].
        @Test
        void nullBuildContextThrowsNullPointerException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(ValidController.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("`buildContext` must not be null");
        }

        /// Verifies that a non-public class throws [IllegalArgumentException].
        @Test
        void nonPublicClassThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(PrivateController.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be public and not abstract");
        }

        /// Verifies that an abstract class throws [IllegalArgumentException].
        @Test
        void abstractClassThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(AbstractController.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be public and not abstract");
        }

        /// Verifies that a class without a public no-arg constructor throws [IllegalArgumentException].
        @Test
        void noPublicNoArgConstructorThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(NoNoArgConstructorController.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Controller class must have a public no-arg constructor");
        }

        /// Verifies that a class with only a private no-arg constructor throws [IllegalArgumentException].
        @Test
        void privateNoArgConstructorThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> helper.introspectControllerClass(PrivateConstructorController.class, buildContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Controller class must have a public no-arg constructor");
        }

        /// Verifies that a valid controller class is introspected correctly, including all member visibilities.
        @Test
        void validControllerIntrospectedCorrectly() {
            assertThat(helper.introspectControllerClass(ValidController.class, buildContext))
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("controllerClass", ValidController.class)
                    .satisfies(controller -> {
                        assertThat(controller.fields())
                                .extracting(FXMLControllerField::name, FXMLControllerField::visibility)
                                .containsExactlyInAnyOrder(
                                        tuple("publicField", Visibility.PUBLIC),
                                        tuple("protectedField", Visibility.PROTECTED),
                                        tuple("packagePrivateField", Visibility.PACKAGE_PRIVATE),
                                        tuple("privateField", Visibility.PRIVATE)
                                );
                        assertThat(controller.methods())
                                .extracting(FXMLControllerMethod::name, FXMLControllerMethod::visibility)
                                .contains(
                                        tuple("publicMethod", Visibility.PUBLIC),
                                        tuple("protectedMethod", Visibility.PROTECTED),
                                        tuple("packagePrivateMethod", Visibility.PACKAGE_PRIVATE),
                                        tuple("privateMethod", Visibility.PRIVATE)
                                );
                    });
        }

        /// Verifies that inherited members are also introspected.
        @Test
        void inheritedMembersAreIntrospected() {
            assertThat(helper.introspectControllerClass(InheritedController.class, buildContext))
                    .isNotNull()
                    .satisfies(controller -> {
                        assertThat(controller.fields())
                                .extracting(FXMLControllerField::name)
                                .contains("childField", "publicField");
                        assertThat(controller.methods())
                                .extracting(FXMLControllerMethod::name)
                                .contains("childMethod", "publicMethod");
                    });
        }
    }
}
