package io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLIdentifierTest {

    @Nested
    class FXMLExposedIdentifierTest {
        @Test
        void shouldCreateWithValidName() {
            FXMLExposedIdentifier id = new FXMLExposedIdentifier("myId");
            assertThat(id.name()).isEqualTo("myId");
            assertThat(id.toString()).isEqualTo("myId");
        }

        @Test
        void shouldThrowNpeForNullName() {
            assertThatThrownBy(() -> new FXMLExposedIdentifier(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1invalid", "invalid-name"})
        void shouldThrowIaeForInvalidName(String name) {
            assertThatThrownBy(() -> new FXMLExposedIdentifier(name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a valid Java identifier");
        }
    }

    @Nested
    class FXMLInternalIdentifierTest {
        @Test
        void shouldCreateWithValidIndex() {
            FXMLInternalIdentifier id = new FXMLInternalIdentifier(1);
            assertThat(id.internalId()).isEqualTo(1);
            assertThat(id.toString()).isEqualTo("$internalVariable$001");
        }

        @Test
        void shouldFormatWithThreeDigits() {
            assertThat(new FXMLInternalIdentifier(0).toString()).isEqualTo("$internalVariable$000");
            assertThat(new FXMLInternalIdentifier(999).toString()).isEqualTo("$internalVariable$999");
            assertThat(new FXMLInternalIdentifier(1000).toString()).isEqualTo("$internalVariable$1000");
        }

        @Test
        void shouldThrowIaeForNegativeIndex() {
            assertThatThrownBy(() -> new FXMLInternalIdentifier(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("`internalId` must be non-negative");
        }
    }

    @Nested
    class FXMLRootIdentifierTest {
        @Test
        void shouldBeSingleton() {
            assertThat(FXMLRootIdentifier.INSTANCE).isSameAs(FXMLRootIdentifier.INSTANCE);
            assertThat(FXMLRootIdentifier.INSTANCE.toString()).isEqualTo("this");
        }

        @Test
        void testEquals() {
            assertThat(FXMLRootIdentifier.INSTANCE).isEqualTo(FXMLRootIdentifier.INSTANCE);
            assertThat(FXMLRootIdentifier.INSTANCE).isNotEqualTo(null);
            assertThat(FXMLRootIdentifier.INSTANCE).isNotEqualTo(new Object());
        }

        @Test
        void testHashCode() {
            assertThat(FXMLRootIdentifier.INSTANCE.hashCode()).isEqualTo(0);
        }
    }

    @Nested
    class FXMLNamedRootIdentifierTest {
        @Test
        void shouldCreateWithValidName() {
            FXMLNamedRootIdentifier id = new FXMLNamedRootIdentifier("myRoot");
            assertThat(id.name()).isEqualTo("myRoot");
            assertThat(id.toString()).isEqualTo("this");
        }

        @Test
        void shouldThrowNpeForNullName() {
            assertThatThrownBy(() -> new FXMLNamedRootIdentifier(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1invalid", "invalid-name"})
        void shouldThrowIaeForInvalidName(String name) {
            assertThatThrownBy(() -> new FXMLNamedRootIdentifier(name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a valid Java identifier");
        }
    }

    @Nested
    class FXMLFactoryMethodTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLFactoryMethod fm = new FXMLFactoryMethod(new FXMLClassType(String.class), "valueOf");
            assertThat(fm)
                    .hasFieldOrPropertyWithValue("clazz", new FXMLClassType(String.class))
                    .hasFieldOrPropertyWithValue("method", "valueOf");
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLFactoryMethod(null, "method"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` cannot be null");
            assertThatThrownBy(() -> new FXMLFactoryMethod(new FXMLClassType(String.class), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`method` cannot be null");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "1invalid", "invalid-name"})
        void shouldThrowIaeForInvalidMethodName(String method) {
            assertThatThrownBy(() -> new FXMLFactoryMethod(new FXMLClassType(String.class), method))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a valid Java identifier");
        }
    }
}
