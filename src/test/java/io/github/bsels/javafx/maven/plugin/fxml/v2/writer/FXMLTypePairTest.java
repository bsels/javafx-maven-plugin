package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Unit tests for the [FXMLTypePair] record.
class FXMLTypePairTest {

    /// A reusable [FXMLType] representing [String] for use in tests.
    private static final FXMLType STRING_TYPE = FXMLType.of(String.class);

    /// A reusable [FXMLType] representing [Integer] for use in tests.
    private static final FXMLType INTEGER_TYPE = FXMLType.of(Integer.class);

    /// Verifies that a valid [FXMLTypePair] can be constructed and its accessors return the correct values.
    @Test
    void constructionWithValidArguments() {
        var pair = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);

        assertThat(pair.type()).isEqualTo(STRING_TYPE);
        assertThat(pair.interfaceType()).isEqualTo(INTEGER_TYPE);
    }

    /// Verifies that constructing an [FXMLTypePair] with a null `type` throws a [NullPointerException].
    @Test
    void constructionWithNullTypeThrowsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FXMLTypePair(null, INTEGER_TYPE))
                .withMessage("`type` cannot be null");
    }

    /// Verifies that constructing an [FXMLTypePair] with a null `interfaceType` throws a [NullPointerException].
    @Test
    void constructionWithNullInterfaceTypeThrowsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FXMLTypePair(STRING_TYPE, null))
                .withMessage("`interfaceType` cannot be null");
    }

    /// Verifies that two [FXMLTypePair] instances with the same fields are equal.
    @Test
    void equalPairsAreEqual() {
        var pair1 = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);
        var pair2 = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);

        assertThat(pair1).isEqualTo(pair2);
    }

    /// Verifies that two [FXMLTypePair] instances with different fields are not equal.
    @Test
    void differentPairsAreNotEqual() {
        var pair1 = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);
        var pair2 = new FXMLTypePair(INTEGER_TYPE, STRING_TYPE);

        assertThat(pair1).isNotEqualTo(pair2);
    }

    /// Verifies that two equal [FXMLTypePair] instances have the same hash code.
    @Test
    void equalPairsHaveSameHashCode() {
        var pair1 = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);
        var pair2 = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);

        assertThat(pair1.hashCode()).isEqualTo(pair2.hashCode());
    }

    /// Verifies that the [FXMLTypePair#toString] method returns a string containing both field values.
    @Test
    void toStringContainsBothFields() {
        var pair = new FXMLTypePair(STRING_TYPE, INTEGER_TYPE);

        assertThat(pair.toString())
                .contains(STRING_TYPE.toString())
                .contains(INTEGER_TYPE.toString());
    }
}
