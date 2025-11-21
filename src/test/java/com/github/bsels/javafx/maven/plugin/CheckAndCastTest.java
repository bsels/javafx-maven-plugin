package com.github.bsels.javafx.maven.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Gatherer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckAndCastTest {

    @Mock
    Gatherer.Downstream<String> stringDownstream;

    @Mock
    Gatherer.Downstream<Integer> integerDownstream;

    /**
     * Description: This class tests the `integrator` method in the `CheckAndCast` class.
     * The `integrator` method defines a lambda function that casts objects to a specified
     * type if possible. It interacts with a downstream to forward filtered and casted elements.
     */

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testIntegratorWithMatchingType(boolean expectedResult) {
        // Arrange
        Class<String> targetClass = String.class;
        CheckAndCast<Object, String> checkAndCast = CheckAndCast.of(targetClass);
        Object element = "TestString";
        when(stringDownstream.push(any()))
                .thenReturn(expectedResult);

        // Act
        boolean result = checkAndCast.integrator().integrate(null, element, stringDownstream);

        // Assert
        verify(stringDownstream).push("TestString");
        assertThat(result)
                .isEqualTo(expectedResult);
    }

    @Test
    void testIntegratorWithNonMatchingType() {
        // Arrange
        Class<Integer> targetClass = Integer.class;
        CheckAndCast<Object, Integer> checkAndCast = CheckAndCast.of(targetClass);
        Object element = "NonMatchingString";

        // Act
        boolean result = checkAndCast.integrator().integrate(null, element, integerDownstream);

        // Assert
        verify(integerDownstream, never()).push(any());
        assertThat(result)
                .isTrue();
    }

    @Test
    void testIntegratorWithNullElement() {
        // Arrange
        Class<String> targetClass = String.class;
        CheckAndCast<Object, String> checkAndCast = CheckAndCast.of(targetClass);
        Object element = null;

        // Act
        boolean result = checkAndCast.integrator().integrate(null, element, stringDownstream);

        // Assert
        verify(stringDownstream, never()).push(any());
        assertThat(result)
                .isTrue();
    }

    @Test
    void testIntegratorWithNullClassInConstructor() {
        // Arrange & Assert
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CheckAndCast<>(null))
                .withMessage("`clazz` must not be null");
    }

    @Test
    void testInStream() {
        // Arrange
        List<Object> input = List.of("alpha", 1, "beta", 2, "gamma", 3, 8.0);
        CheckAndCast<Object, String> checkAndCast = CheckAndCast.of(String.class);

        // Test
        List<String> filtered = input.stream()
                .gather(checkAndCast)
                .toList();
        assertThat(filtered)
                .hasSize(3)
                .containsExactly("alpha", "beta", "gamma");
    }

    @Test
    void testRecordGetter() {
        // Arrange
        Class<String> targetClass = String.class;
        CheckAndCast<Object, String> checkAndCast = CheckAndCast.of(targetClass);

        // Test
        assertThat(checkAndCast.clazz())
                .isEqualTo(targetClass);
    }
}