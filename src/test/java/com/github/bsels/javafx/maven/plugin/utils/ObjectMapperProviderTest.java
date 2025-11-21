package com.github.bsels.javafx.maven.plugin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unit tests for the `ObjectMapperProvider` class.
///
/// This class focuses on testing the functionality of the `getObjectMapper` method
/// to ensure it provides a singleton instance of the `ObjectMapper`.
class ObjectMapperProviderTest {

    /// Verifies that `getObjectMapper` returns a non-null instance of `ObjectMapper`.
    @Test
    void testGetObjectMapperReturnsNonNull() {
        // Act
        ObjectMapper objectMapper = ObjectMapperProvider.getObjectMapper();

        // Assert
        assertThat(objectMapper)
                .isNotNull();
    }

    /// Verifies that `getObjectMapper` always returns the same singleton instance of `ObjectMapper`.
    @Test
    void testGetObjectMapperReturnsSingletonInstance() {
        // Act
        ObjectMapper firstInstance = ObjectMapperProvider.getObjectMapper();
        ObjectMapper secondInstance = ObjectMapperProvider.getObjectMapper();

        // Assert
        assertThat(firstInstance)
                .isSameAs(secondInstance);
    }

    @Test
    void constructionFailsWithIllegalStateException() throws NoSuchMethodException {
        Constructor<?> constructor = ObjectMapperProvider.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Cannot instantiate ObjectMapperProvider class");
    }

    @Test
    void checkBuilder() throws NoSuchFieldException, IllegalAccessException {
        Field field = ObjectMapperProvider.class.getDeclaredField("OBJECT_MAPPER");
        field.setAccessible(true);
        field.set(null, null);

        assertThat(ObjectMapperProvider.getObjectMapper())
                .isSameAs(ObjectMapperProvider.getObjectMapper());
    }

    @Test
    void testTypeSerialization() throws Exception {
        ObjectMapper objectMapper = ObjectMapperProvider.getObjectMapper();
        assertThat(objectMapper.writeValueAsString(TypeEncoderTest.class))
                .isEqualTo("\"class com.github.bsels.javafx.maven.plugin.utils.TypeEncoderTest\"");
    }
}