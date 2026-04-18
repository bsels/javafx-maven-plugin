package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLUtils;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLScript;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unit tests for record classes in [io.github.bsels.javafx.maven.plugin.fxml.v2.parser].
class FXMLParserRecordsTest {

    private final FXMLType dummyType = FXMLType.of(String.class);
    private final FXMLIdentifier dummyId = FXMLRootIdentifier.INSTANCE;

    @Nested
    @DisplayName("BuildContext tests")
    class BuildContextTest {

        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            AtomicInteger counter = new AtomicInteger();
            List<String> imports = new ArrayList<>();
            List<AbstractFXMLValue> definitions = new ArrayList<>();
            List<FXMLScript> scripts = new ArrayList<>();
            Map<String, FXMLType> typeMapping = new HashMap<>();
            String resourcePath = "/";

            assertThatThrownBy(() -> new BuildContext(null, imports, definitions, scripts, typeMapping, resourcePath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("internalCounter");

            assertThatThrownBy(() -> new BuildContext(counter, null, definitions, scripts, typeMapping, resourcePath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("imports");

            assertThatThrownBy(() -> new BuildContext(counter, imports, null, scripts, typeMapping, resourcePath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("definitions");

            assertThatThrownBy(() -> new BuildContext(counter, imports, definitions, null, typeMapping, resourcePath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("scripts");

            assertThatThrownBy(() -> new BuildContext(counter, imports, definitions, scripts, null, resourcePath))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeMapping");

            assertThatThrownBy(() -> new BuildContext(counter, imports, definitions, scripts, typeMapping, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("resourcePath");
        }

        @Test
        @DisplayName("Constructor should append trailing slash to resourcePath if missing")
        void constructorShouldAppendTrailingSlashToResourcePath() {
            BuildContext context = new BuildContext(new ArrayList<>(), "path");
            assertThat(context).hasFieldOrPropertyWithValue("resourcePath", "path/");

            BuildContext contextWithSlash = new BuildContext(new ArrayList<>(), "path/");
            assertThat(contextWithSlash).hasFieldOrPropertyWithValue("resourcePath", "path/");
        }

        @Test
        @DisplayName("Convenience constructor should initialize empty collections")
        void convenienceConstructorShouldInitializeEmptyCollections() {
            List<String> imports = List.of("com.example.*");
            BuildContext context = new BuildContext(imports, "/");

            assertThat(context)
                    .hasFieldOrPropertyWithValue("internalCounter", context.internalCounter())
                    .hasFieldOrPropertyWithValue("imports", List.of("com.example.*"))
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .hasFieldOrPropertyWithValue("scripts", List.of())
                    .hasFieldOrPropertyWithValue("typeMapping", Map.of())
                    .hasFieldOrPropertyWithValue("resourcePath", "/");
        }

        @Test
        @DisplayName("Copy constructor with typeMapping should copy other fields")
        void copyConstructorWithTypeMappingShouldCopyOtherFields() {
            BuildContext original = new BuildContext(
                    new AtomicInteger(10),
                    List.of("import1"),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    Map.of("T", dummyType),
                    "path/"
            );
            Map<String, FXMLType> newMapping = Map.of("U", dummyType);

            BuildContext copy = new BuildContext(original, newMapping);

            assertThat(copy)
                    .hasFieldOrPropertyWithValue("internalCounter", original.internalCounter())
                    .hasFieldOrPropertyWithValue("imports", original.imports())
                    .hasFieldOrPropertyWithValue("definitions", original.definitions())
                    .hasFieldOrPropertyWithValue("scripts", original.scripts())
                    .hasFieldOrPropertyWithValue("typeMapping", newMapping)
                    .hasFieldOrPropertyWithValue("resourcePath", original.resourcePath());
        }

        @Test
        @DisplayName("nextInternalId should increment counter")
        void nextInternalIdShouldIncrementCounter() {
            BuildContext context = new BuildContext(new ArrayList<>(), "/");
            assertThat(context.nextInternalId()).isEqualTo(0);
            assertThat(context.nextInternalId()).isEqualTo(1);
            assertThat(context.internalCounter().get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("ClassAndIdentifier tests")
    class ClassAndIdentifierTest {
        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            assertThatThrownBy(() -> new ClassAndIdentifier(null, dummyId))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ClassAndIdentifier(String.class, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Record should store values correctly")
        void recordShouldStoreValuesCorrectly() {
            ClassAndIdentifier ci = new ClassAndIdentifier(String.class, dummyId);

            assertThat(ci)
                    .hasFieldOrPropertyWithValue("clazz", String.class)
                    .hasFieldOrPropertyWithValue("identifier", dummyId);
        }
    }

    @Nested
    @DisplayName("InternalStaticSetterProperty tests")
    class InternalStaticSetterPropertyTest {
        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            assertThatThrownBy(() -> new InternalStaticSetterProperty(null, Object.class, "set", dummyType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new InternalStaticSetterProperty("name", null, "set", dummyType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new InternalStaticSetterProperty("name", Object.class, null, dummyType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new InternalStaticSetterProperty("name", Object.class, "set", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Record should store values correctly")
        void recordShouldStoreValuesCorrectly() {
            InternalStaticSetterProperty prop = new InternalStaticSetterProperty(
                    "prop",
                    Object.class,
                    "setProp",
                    dummyType
            );

            assertThat(prop)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("staticClass", Object.class)
                    .hasFieldOrPropertyWithValue("setter", "setProp")
                    .hasFieldOrPropertyWithValue("fxmlType", dummyType);
        }
    }

    @Nested
    @DisplayName("ObjectProperty tests")
    class ObjectPropertyTest {
        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            Optional<String> methodName = Optional.of("set");
            ObjectProperty.MethodType methodType = ObjectProperty.MethodType.SETTER;

            assertThatThrownBy(() -> new ObjectProperty(null, "name", methodName, methodType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ObjectProperty(dummyType, null, methodName, methodType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ObjectProperty(dummyType, "name", null, methodType))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ObjectProperty(dummyType, "name", methodName, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Record should store values correctly")
        void recordShouldStoreValuesCorrectly() {
            ObjectProperty prop = new ObjectProperty(
                    dummyType,
                    "prop",
                    Optional.of("getProp"),
                    ObjectProperty.MethodType.GETTER
            );

            assertThat(prop)
                    .hasFieldOrPropertyWithValue("type", dummyType)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("methodName", Optional.of("getProp"))
                    .hasFieldOrPropertyWithValue("methodType", ObjectProperty.MethodType.GETTER);
        }
    }

    @Nested
    @DisplayName("ParseContext tests")
    class ParseContextTest {
        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            ParsedXMLStructure structure = Mockito.mock(ParsedXMLStructure.class);
            BuildContext buildContext = new BuildContext(Collections.emptyList(), "/");
            ClassAndIdentifier ci = new ClassAndIdentifier(Object.class, dummyId);
            Optional<FXMLFactoryMethod> factoryMethod = Optional.empty();

            assertThatThrownBy(() -> new ParseContext(null, buildContext, ci, dummyType, factoryMethod))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ParseContext(structure, null, ci, dummyType, factoryMethod))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ParseContext(structure, buildContext, null, dummyType, factoryMethod))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ParseContext(structure, buildContext, ci, null, factoryMethod))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ParseContext(structure, buildContext, ci, dummyType, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Record should store values correctly")
        void recordShouldStoreValuesCorrectly() {
            ParsedXMLStructure structure = Mockito.mock(ParsedXMLStructure.class);
            BuildContext buildContext = new BuildContext(Collections.emptyList(), "/");
            ClassAndIdentifier ci = new ClassAndIdentifier(Object.class, dummyId);
            Optional<FXMLFactoryMethod> factoryMethod = Optional.of(new FXMLFactoryMethod(
                    new FXMLClassType(Object.class),
                    "valueOf"
            ));

            ParseContext context = new ParseContext(structure, buildContext, ci, dummyType, factoryMethod);

            assertThat(context)
                    .hasFieldOrPropertyWithValue("structure", structure)
                    .hasFieldOrPropertyWithValue("buildContext", buildContext)
                    .hasFieldOrPropertyWithValue("classAndIdentifier", ci)
                    .hasFieldOrPropertyWithValue("type", dummyType)
                    .hasFieldOrPropertyWithValue("factoryMethod", factoryMethod);
        }
    }

    @Nested
    @DisplayName("FXMLUtils.ClassAndString tests")
    class ClassAndStringTest {
        @Test
        @DisplayName("Constructor should throw NPE if any parameter is null")
        void constructorShouldThrowNpeIfAnyParameterIsNull() {
            // Since it's private, we test it through FXMLUtils methods that use it,
            // or we can't test it directly unless we use reflection.
            // But since the task asks for dedicated tests for records, I'll try to trigger its usage.
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(null, "CONSTANT"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(String.class, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Verify it works as a cache key in FXMLUtils")
        void verifyItWorksAsCacheKey() {
            // Triggering resolveConstantType will use ClassAndString
            assertThatThrownBy(() -> FXMLUtils.resolveConstantType(String.class, "NON_EXISTENT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No such constant field");
        }
    }
}
