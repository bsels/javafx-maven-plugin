package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLFactoryMethod;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLValueTest {

    private final FXMLIdentifier id = new FXMLInternalIdentifier(1);
    private final FXMLType stringType = FXMLType.of(String.class);
    private final FXMLType listType = FXMLType.of(ArrayList.class);
    private final FXMLType mapType = FXMLType.of(HashMap.class);
    private final Optional<FXMLFactoryMethod> noFactory = Optional.empty();

    @Nested
    class FXMLObjectTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLObject obj = new FXMLObject(id, stringType, noFactory, List.of());
            assertThat(obj.identifier()).isEqualTo(id);
            assertThat(obj.type()).isEqualTo(stringType);
            assertThat(obj.factoryMethod()).isEqualTo(noFactory);
            assertThat(obj.properties()).isEmpty();
        }

        @Test
        void shouldHandleNullProperties() {
            FXMLObject obj = new FXMLObject(id, stringType, noFactory, null);
            assertThat(obj.properties()).isEmpty();
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLObject(null, stringType, noFactory, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLObject(id, null, noFactory, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLObject(id, stringType, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`factoryMethod` must not be null");
        }
    }

    @Nested
    class FXMLCollectionTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLCollection coll = new FXMLCollection(id, listType, noFactory, List.of());
            assertThat(coll.identifier()).isEqualTo(id);
            assertThat(coll.type()).isEqualTo(listType);
            assertThat(coll.factoryMethod()).isEqualTo(noFactory);
            assertThat(coll.values()).isEmpty();
        }

        @Test
        void shouldHandleNullValues() {
            FXMLCollection coll = new FXMLCollection(id, listType, noFactory, null);
            assertThat(coll.values()).isEmpty();
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLCollection(null, listType, noFactory, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLCollection(id, null, noFactory, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLCollection(id, listType, null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`factoryMethod` must not be null");
        }

        @Test
        void shouldThrowIaeForNonCollectionType() {
            assertThatThrownBy(() -> new FXMLCollection(id, stringType, noFactory, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a Collection");
        }

        @Test
        void shouldCreateWithValidGenericCollectionType() {
            FXMLType genericListType = FXMLType.of(ArrayList.class, List.of(FXMLType.of(String.class)));
            FXMLCollection coll = new FXMLCollection(id, genericListType, noFactory, List.of());
            assertThat(coll.type()).isEqualTo(genericListType);
        }

        @Test
        void shouldThrowIaeForNonCollectionGenericType() {
            FXMLType genericStringType = FXMLType.of(String.class, List.of(FXMLType.of(Integer.class)));
            assertThatThrownBy(() -> new FXMLCollection(id, genericStringType, noFactory, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a Collection");
        }

        @Test
        void shouldAcceptUncompiledAndWildcardTypes() {
            FXMLType uncompiledClass = FXMLType.of("com.example.MyList", List.of());
            FXMLType uncompiledGeneric = FXMLType.of("com.example.MyList", List.of(FXMLType.of(String.class)));
            FXMLType wildcard = FXMLType.wildcard();
            assertThat(new FXMLCollection(id, uncompiledClass, noFactory, List.of()).type()).isEqualTo(uncompiledClass);
            assertThat(new FXMLCollection(id, uncompiledGeneric, noFactory, List.of()).type()).isEqualTo(
                    uncompiledGeneric);
            assertThat(new FXMLCollection(id, wildcard, noFactory, List.of()).type()).isEqualTo(wildcard);
        }
    }

    @Nested
    class FXMLMapTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLMap map = new FXMLMap(id, mapType, String.class, String.class, noFactory, Map.of());
            assertThat(map)
                    .hasFieldOrPropertyWithValue("identifier", id)
                    .hasFieldOrPropertyWithValue("rawKeyClass", String.class)
                    .hasFieldOrPropertyWithValue("rawValueClass", String.class)
                    .hasFieldOrPropertyWithValue("factoryMethod", noFactory)
                    .hasFieldOrPropertyWithValue("entries", Map.of());
        }

        @Test
        void shouldHandleNullEntries() {
            FXMLMap map = new FXMLMap(id, mapType, String.class, String.class, noFactory, null);
            assertThat(map.entries()).isEmpty();
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLMap(null, mapType, String.class, String.class, noFactory, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLMap(id, null, String.class, String.class, noFactory, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLMap(id, mapType, null, String.class, noFactory, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`rawKeyClass` must not be null");
            assertThatThrownBy(() -> new FXMLMap(id, mapType, String.class, null, noFactory, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`rawValueClass` must not be null");
            assertThatThrownBy(() -> new FXMLMap(id, mapType, String.class, String.class, null, Map.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`factoryMethod` must not be null");
        }

        @Test
        void shouldThrowIaeForNonMapType() {
            assertThatThrownBy(() -> new FXMLMap(id, stringType, String.class, String.class, noFactory, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a Map");
        }

        @Test
        void shouldCreateWithValidGenericMapType() {
            FXMLType genericMapType = FXMLType.of(
                    HashMap.class,
                    List.of(FXMLType.of(String.class), FXMLType.of(Integer.class))
            );
            FXMLMap map = new FXMLMap(id, genericMapType, String.class, Integer.class, noFactory, Map.of());
            assertThat(map.type()).isEqualTo(genericMapType);
        }

        @Test
        void shouldThrowIaeForNonMapGenericType() {
            FXMLType genericStringType = FXMLType.of(String.class, List.of(FXMLType.of(Integer.class)));
            assertThatThrownBy(() -> new FXMLMap(id, genericStringType, String.class, String.class, noFactory, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a Map");
        }

        @Test
        void shouldAcceptUncompiledAndWildcardTypes() {
            FXMLType uncompiledClass = FXMLType.of("com.example.MyMap", List.of());
            FXMLType uncompiledGeneric = FXMLType.of("com.example.MyMap", List.of(FXMLType.of(String.class)));
            FXMLType wildcard = FXMLType.wildcard();
            assertThat(new FXMLMap(id, uncompiledClass, Object.class, String.class, noFactory, Map.of()).type()).isEqualTo(
                    uncompiledClass);
            assertThat(new FXMLMap(id, uncompiledGeneric, Object.class, String.class, noFactory, Map.of()).type()).isEqualTo(
                    uncompiledGeneric);
            assertThat(new FXMLMap(id, wildcard, Object.class, String.class, noFactory, Map.of()).type()).isEqualTo(wildcard);
        }
    }

    @Nested
    class FXMLValueTestInner {
        @Test
        void shouldCreateWithValidParams() {
            FXMLValue val = new FXMLValue(Optional.of(id), stringType, "raw");
            assertThat(val.identifier()).contains(id);
            assertThat(val.type()).isEqualTo(stringType);
            assertThat(val.value()).isEqualTo("raw");
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLValue(null, stringType, "raw"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLValue(Optional.of(id), null, "raw"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLValue(Optional.of(id), stringType, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`value` must not be null");
        }
    }

    @Nested
    class FXMLLiteralTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLLiteral lit = new FXMLLiteral("val");
            assertThat(lit.value()).isEqualTo("val");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLLiteral(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`value` must not be null");
        }
    }

    @Nested
    class FXMLConstantTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLConstant con = new FXMLConstant(String.class, "id", stringType);
            assertThat(con.clazz()).isEqualTo(String.class);
            assertThat(con.identifier()).isEqualTo("id");
            assertThat(con.constantType()).isEqualTo(stringType);
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLConstant(null, "id", stringType))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
            assertThatThrownBy(() -> new FXMLConstant(String.class, null, stringType))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLConstant(String.class, "id", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`constantType` must not be null");
        }

        @Test
        void shouldThrowIaeForInvalidIdentifier() {
            assertThatThrownBy(() -> new FXMLConstant(String.class, "1invalid", stringType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("`identifier` must be a valid Java identifier");
        }
    }

    @Nested
    class FXMLReferenceTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLReference ref = new FXMLReference("name");
            assertThat(ref.name()).isEqualTo("name");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLReference(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @Test
        void shouldThrowIaeForInvalidName() {
            assertThatThrownBy(() -> new FXMLReference("1invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("`name` must be a valid Java identifier");
        }
    }

    @Nested
    class FXMLResourceTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLResource res = new FXMLResource("path");
            assertThat(res.path()).isEqualTo("path");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLResource(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`path` must not be null");
        }

        @Test
        void shouldThrowIaeForBlankPath() {
            assertThatThrownBy(() -> new FXMLResource("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`path` must not be blank");
        }
    }

    @Nested
    class FXMLTranslationTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLTranslation trans = new FXMLTranslation("key");
            assertThat(trans.translationKey()).isEqualTo("key");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLTranslation(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`translationKey` must not be null");
        }

        @Test
        void shouldThrowIaeForBlankKey() {
            assertThatThrownBy(() -> new FXMLTranslation("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`translationKey` must not be blank");
        }
    }

    @Nested
    class FXMLExpressionTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLExpression exp = new FXMLExpression("expr");
            assertThat(exp.expression()).isEqualTo("expr");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLExpression(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`expression` must not be null");
        }
    }

    @Nested
    class FXMLCopyTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLCopy copy = new FXMLCopy(id, "name");
            assertThat(copy.identifier()).isEqualTo(id);
            assertThat(copy.name()).isEqualTo("name");
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLCopy(null, "name"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLCopy(id, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @Test
        void shouldThrowIaeForInvalidName() {
            assertThatThrownBy(() -> new FXMLCopy(id, "1invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("`name` must be a valid Java identifier");
        }
    }

    @Nested
    class FXMLIncludeTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLInclude inc = new FXMLInclude(id, "src", StandardCharsets.UTF_8, Optional.of("res"));
            assertThat(inc.identifier()).isEqualTo(id);
            assertThat(inc.sourceFile()).isEqualTo("src");
            assertThat(inc.charset()).isEqualTo(StandardCharsets.UTF_8);
            assertThat(inc.resources()).contains("res");
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLInclude(null, "s", StandardCharsets.UTF_8, Optional.empty()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`identifier` must not be null");
            assertThatThrownBy(() -> new FXMLInclude(id, null, StandardCharsets.UTF_8, Optional.empty()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`sourceFile` must not be null");
            assertThatThrownBy(() -> new FXMLInclude(id, "s", null, Optional.empty()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`charset` must not be null");
            assertThatThrownBy(() -> new FXMLInclude(id, "s", StandardCharsets.UTF_8, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`resources` must not be null");
        }
    }

    @Nested
    class FXMLInlineScriptTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLInlineScript script = new FXMLInlineScript("source");
            assertThat(script.script()).isEqualTo("source");
        }

        @Test
        void shouldThrowNpeWithMessage() {
            assertThatThrownBy(() -> new FXMLInlineScript(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`script` must not be null");
        }

        @Test
        void shouldThrowIaeForBlankScript() {
            assertThatThrownBy(() -> new FXMLInlineScript("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`script` must not be blank");
        }
    }

    @Nested
    class FXMLMethodTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLMethod meth = new FXMLMethod("method", List.of(stringType), stringType);
            assertThat(meth.name()).isEqualTo("method");
            assertThat(meth.parameters()).containsExactly(stringType);
            assertThat(meth.returnType()).isEqualTo(stringType);
        }

        @Test
        void shouldHandleNullParameters() {
            FXMLMethod meth = new FXMLMethod("method", null, stringType);
            assertThat(meth.parameters()).isEmpty();
        }

        @Test
        void shouldThrowNpeWithMessages() {
            assertThatThrownBy(() -> new FXMLMethod(null, List.of(), stringType))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLMethod("m", List.of(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`returnType` must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1invalid", "invalid-name"})
        void shouldThrowIaeForInvalidName(String name) {
            assertThatThrownBy(() -> new FXMLMethod(name, List.of(), stringType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a valid Java identifier");
        }
    }
}
