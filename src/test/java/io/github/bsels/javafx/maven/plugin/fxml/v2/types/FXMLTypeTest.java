package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLTypeTest {

    @Nested
    class StaticFactoryMethodsTest {

        @Test
        void ofClassShouldReturnFXMLClassType() {
            FXMLType type = FXMLType.of(String.class);
            assertThat(type).isInstanceOf(FXMLClassType.class);
            assertThat(((FXMLClassType) type).clazz()).isEqualTo(String.class);
        }

        @Test
        void ofClassShouldThrowNpeForNull() {
            assertThatThrownBy(() -> FXMLType.of((Class<?>) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
        }

        @Test
        void ofClassAndGenericsShouldReturnFXMLClassTypeIfEmpty() {
            FXMLType type = FXMLType.of(String.class, List.of());
            assertThat(type).isInstanceOf(FXMLClassType.class);
        }

        @Test
        void ofClassAndGenericsShouldReturnFXMLGenericTypeIfNotEmpty() {
            FXMLType typeArgument = FXMLType.of(Integer.class);
            FXMLType type = FXMLType.of(List.class, List.of(typeArgument));
            assertThat(type).isInstanceOf(FXMLGenericType.class);
            assertThat(((FXMLGenericType) type).type()).isEqualTo(List.class);
            assertThat(((FXMLGenericType) type).typeArguments()).containsExactly(typeArgument);
        }

        @Test
        void ofClassAndGenericsShouldThrowNpeIfNull() {
            assertThatThrownBy(() -> FXMLType.of((Class<?>) null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
            assertThatThrownBy(() -> FXMLType.of(String.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`genericTypes` must not be null");
        }

        @Test
        void ofUncompiledAndGenericsShouldReturnFXMLUncompiledClassTypeIfEmpty() {
            FXMLType type = FXMLType.of("com.example.Foo", List.of());
            assertThat(type).isInstanceOf(FXMLUncompiledClassType.class);
            assertThat(((FXMLUncompiledClassType) type).name()).isEqualTo("com.example.Foo");
        }

        @Test
        void ofUncompiledAndGenericsShouldReturnFXMLUncompiledGenericTypeIfNotEmpty() {
            FXMLType typeArgument = FXMLType.of(Integer.class);
            FXMLType type = FXMLType.of("com.example.Foo", List.of(typeArgument));
            assertThat(type).isInstanceOf(FXMLUncompiledGenericType.class);
            assertThat(((FXMLUncompiledGenericType) type).name()).isEqualTo("com.example.Foo");
            assertThat(((FXMLUncompiledGenericType) type).typeArguments()).containsExactly(typeArgument);
        }

        @Test
        void ofUncompiledAndGenericsShouldThrowNpeIfNull() {
            assertThatThrownBy(() -> FXMLType.of((String) null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`uncompiledType` must not be null");
            assertThatThrownBy(() -> FXMLType.of("Foo", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`genericTypes` must not be null");
        }

        @Test
        void wildcardShouldReturnSingletonInstance() {
            FXMLType type1 = FXMLType.wildcard();
            FXMLType type2 = FXMLType.wildcard();
            assertThat(type1).isSameAs(FXMLWildcardType.INSTANCE);
            assertThat(type1).isSameAs(type2);
        }
    }

    @Nested
    class FXMLClassTypeTest {
        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLClassType(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`clazz` must not be null");
        }
    }

    @Nested
    class FXMLGenericTypeTest {
        @Test
        void shouldThrowNpeForNullType() {
            assertThatThrownBy(() -> new FXMLGenericType(null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`type` must not be null");
        }

        @Test
        void shouldHandleNullTypeArguments() {
            FXMLGenericType type = new FXMLGenericType(List.class, (List<FXMLType>) null);
            assertThat(type.typeArguments()).isEmpty();
        }

        @Test
        void varargsConstructorShouldWork() {
            FXMLType arg = FXMLType.of(String.class);
            FXMLGenericType type = new FXMLGenericType(List.class, arg);
            assertThat(type.typeArguments()).containsExactly(arg);
        }
    }

    @Nested
    class FXMLUncompiledClassTypeTest {
        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLUncompiledClassType(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }
    }

    @Nested
    class FXMLUncompiledGenericTypeTest {
        @Test
        void shouldThrowNpeForNullName() {
            assertThatThrownBy(() -> new FXMLUncompiledGenericType(null, List.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`name` must not be null");
        }

        @Test
        void shouldHandleNullTypeArguments() {
            FXMLUncompiledGenericType type = new FXMLUncompiledGenericType("Foo", (List<FXMLType>) null);
            assertThat(type.typeArguments()).isEmpty();
        }
    }

    @Nested
    class FXMLWildcardTypeTest {
        @Test
        void testEquals() {
            assertThat(FXMLWildcardType.INSTANCE).isEqualTo(FXMLWildcardType.INSTANCE);
            assertThat(FXMLWildcardType.INSTANCE).isNotEqualTo(null);
            assertThat(FXMLWildcardType.INSTANCE).isNotEqualTo("?");
        }

        @Test
        void testHashCode() {
            assertThat(FXMLWildcardType.INSTANCE.hashCode()).isEqualTo(0);
        }

        @Test
        void testToString() {
            assertThat(FXMLWildcardType.INSTANCE.toString()).isEqualTo("?");
        }
    }
}
