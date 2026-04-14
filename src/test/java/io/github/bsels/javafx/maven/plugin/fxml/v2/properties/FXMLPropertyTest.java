package io.github.bsels.javafx.maven.plugin.fxml.v2.properties;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLPropertyTest {

    private final FXMLType stringType = FXMLType.of(String.class);
    private final AbstractFXMLValue literalValue = new FXMLLiteral("val");

    @Nested
    class FXMLObjectPropertyTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLObjectProperty prop = new FXMLObjectProperty("name", "setProp", stringType, literalValue);
            assertThat(prop.name()).isEqualTo("name");
            assertThat(prop.setter()).isEqualTo("setProp");
            assertThat(prop.type()).isEqualTo(stringType);
            assertThat(prop.value()).isEqualTo(literalValue);
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLObjectProperty(null, "s", stringType, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLObjectProperty("n", null, stringType, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`getter` must not be null");
            assertThatThrownBy(() -> new FXMLObjectProperty("n", "s", null, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLObjectProperty("n", "s", stringType, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`value` must not be null");
        }
    }

    @Nested
    class FXMLStaticObjectPropertyTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLStaticObjectProperty prop = new FXMLStaticObjectProperty(
                    "name",
                    new FXMLClassType(String.class),
                    "setProp",
                    stringType,
                    literalValue
            );
            assertThat(prop)
                    .hasFieldOrPropertyWithValue("name", "name")
                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(String.class))
                    .hasFieldOrPropertyWithValue("setter", "setProp")
                    .hasFieldOrPropertyWithValue("type", stringType)
                    .hasFieldOrPropertyWithValue("value", literalValue);
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLStaticObjectProperty(
                    null,
                    new FXMLClassType(String.class),
                    "s",
                    stringType,
                    literalValue
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLStaticObjectProperty("n", null, "s", stringType, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
            assertThatThrownBy(() -> new FXMLStaticObjectProperty(
                    "n",
                    new FXMLClassType(String.class),
                    null,
                    stringType,
                    literalValue
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`setter` must not be null");
            assertThatThrownBy(() -> new FXMLStaticObjectProperty(
                    "n",
                    new FXMLClassType(String.class),
                    "s",
                    null,
                    literalValue
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLStaticObjectProperty(
                    "n",
                    new FXMLClassType(String.class),
                    "s",
                    stringType,
                    null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`value` must not be null");
        }
    }

    @Nested
    class FXMLCollectionPropertiesTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLCollectionProperties prop = new FXMLCollectionProperties(
                    "name",
                    "getProp",
                    FXMLType.of(List.class, List.of(stringType)),
                    stringType,
                    List.of(literalValue),
                    List.of()
            );
            assertThat(prop)
                    .hasFieldOrPropertyWithValue("name", "name")
                    .hasFieldOrPropertyWithValue("getter", "getProp")
                    .hasFieldOrPropertyWithValue("type", FXMLType.of(List.class, List.of(stringType)))
                    .hasFieldOrPropertyWithValue("elementType", stringType)
                    .hasFieldOrPropertyWithValue("value", List.of(literalValue))
                    .hasFieldOrPropertyWithValue("properties", List.of());
        }

        @Test
        void shouldHandleNullLists() {
            FXMLCollectionProperties prop = new FXMLCollectionProperties(
                    "name",
                    "getProp",
                    FXMLType.of(List.class, List.of(stringType)),
                    stringType,
                    null,
                    null
            );
            assertThat(prop.value()).isEmpty();
            assertThat(prop.properties()).isEmpty();
        }

        @Test
        void shouldThrowNpeForNullRequired() {
            assertThatThrownBy(() -> new FXMLCollectionProperties(
                    null,
                    "g",
                    FXMLType.of(List.class, List.of(stringType)),
                    stringType,
                    List.of(),
                    List.of()
            )).isInstanceOf(NullPointerException.class).hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLCollectionProperties(
                    "n",
                    null,
                    FXMLType.of(List.class, List.of(stringType)),
                    stringType,
                    List.of(),
                    List.of()
            )).isInstanceOf(NullPointerException.class).hasMessage("`getter` must not be null");
            assertThatThrownBy(() -> new FXMLCollectionProperties(
                    "n",
                    "g",
                    null,
                    stringType,
                    List.of(),
                    List.of()
            )).isInstanceOf(NullPointerException.class).hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLCollectionProperties(
                    "n",
                    "g",
                    FXMLType.of(List.class, List.of(stringType)),
                    null,
                    List.of(),
                    List.of()
            )).isInstanceOf(NullPointerException.class).hasMessage("`elementType` must not be null");
        }
    }

    @Nested
    class FXMLMapPropertyTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLMapProperty prop = new FXMLMapProperty(
                    "name",
                    "getProp",
                    stringType,
                    stringType,
                    stringType,
                    Map.of(new FXMLLiteral("k"), literalValue)
            );
            assertThat(prop)
                    .hasFieldOrPropertyWithValue("name", "name")
                    .hasFieldOrPropertyWithValue("getter", "getProp")
                    .hasFieldOrPropertyWithValue("type", stringType)
                    .hasFieldOrPropertyWithValue("keyType", stringType)
                    .hasFieldOrPropertyWithValue("valueType", stringType)
                    .hasFieldOrPropertyWithValue("value", Map.of(new FXMLLiteral("k"), literalValue));
        }

        @Test
        void shouldHandleNullMap() {
            FXMLMapProperty prop = new FXMLMapProperty(
                    "name",
                    "getProp",
                    stringType,
                    stringType,
                    stringType,
                    null
            );
            assertThat(prop.value()).isEmpty();
        }

        @Test
        void shouldThrowNpeForNullRequired() {
            assertThatThrownBy(() -> new FXMLMapProperty(
                    null,
                    "g",
                    stringType,
                    stringType,
                    stringType,
                    Map.of()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLMapProperty(
                    "n",
                    null,
                    stringType,
                    stringType,
                    stringType,
                    Map.of()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`getter` must not be null");
            assertThatThrownBy(() -> new FXMLMapProperty(
                    "n",
                    "g",
                    null,
                    stringType,
                    stringType,
                    Map.of()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLMapProperty(
                    "n",
                    "g",
                    stringType,
                    null,
                    stringType,
                    Map.of()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`keyType` must not be null");
            assertThatThrownBy(() -> new FXMLMapProperty(
                    "n",
                    "g",
                    stringType,
                    stringType,
                    null,
                    Map.of()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`valueType` must not be null");
        }
    }

    @Nested
    class FXMLConstructorPropertyTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLConstructorProperty prop = new FXMLConstructorProperty("name", stringType, literalValue);
            assertThat(prop.name()).isEqualTo("name");
            assertThat(prop.type()).isEqualTo(stringType);
            assertThat(prop.value()).isEqualTo(literalValue);
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLConstructorProperty(null, stringType, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
            assertThatThrownBy(() -> new FXMLConstructorProperty("n", null, literalValue))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
            assertThatThrownBy(() -> new FXMLConstructorProperty("n", stringType, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`value` must not be null");
        }
    }
}
