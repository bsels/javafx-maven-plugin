package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Unit tests for all record types in the [io.github.bsels.javafx.maven.plugin.fxml.v2.writer] package:
/// [ConstructorProperty], [FXMLTypeWrapper], [GroupedClassCount], [ReferenceWrapper], and [ClassCount].
class WriterRecordsTest {

    /// A reusable [FXMLType] for use in tests.
    private static final FXMLType STRING_TYPE = FXMLType.of(String.class);

    /// A reusable [FXMLLiteral] for use in tests.
    private static final FXMLLiteral LITERAL = new FXMLLiteral("hello");

    /// Tests for the [ConstructorProperty] record.
    @Nested
    class ConstructorPropertyTest {

        /// Verifies that a null name throws [NullPointerException].
        @Test
        void nullNameThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty(null, STRING_TYPE, Optional.empty()));
        }

        /// Verifies that a null type throws [NullPointerException].
        @Test
        void nullTypeThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty("prop", null, Optional.empty()));
        }

        /// Verifies that a null defaultValue throws [NullPointerException].
        @Test
        void nullDefaultValueThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ConstructorProperty("prop", STRING_TYPE, null));
        }

        /// Verifies that a valid instance without a default value stores all fields correctly.
        @Test
        void validInstanceWithoutDefaultValueStoresFields() {
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(property)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("defaultValue", Optional.empty());
        }

        /// Verifies that a valid instance with a default value stores all fields correctly.
        @Test
        void validInstanceWithDefaultValueStoresFields() {
            Optional<FXMLLiteral> defaultValue = Optional.of(LITERAL);
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, defaultValue);

            assertThat(property)
                    .hasFieldOrPropertyWithValue("name", "prop")
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE)
                    .hasFieldOrPropertyWithValue("defaultValue", defaultValue);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ConstructorProperty a = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());
            ConstructorProperty b = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ConstructorProperty#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            ConstructorProperty property = new ConstructorProperty("prop", STRING_TYPE, Optional.empty());

            assertThat(property.toString())
                    .contains("prop")
                    .contains(STRING_TYPE.toString());
        }
    }

    /// Tests for the [FXMLTypeWrapper] record.
    @Nested
    class FXMLTypeWrapperTest {

        /// Verifies that a null type throws [NullPointerException].
        @Test
        void nullTypeThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new FXMLTypeWrapper(null));
        }

        /// Verifies that a valid instance stores the type correctly.
        @Test
        void validInstanceStoresType() {
            FXMLTypeWrapper wrapper = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(wrapper)
                    .hasFieldOrPropertyWithValue("type", STRING_TYPE);
        }

        /// Verifies that two instances with the same type are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            FXMLTypeWrapper a = new FXMLTypeWrapper(STRING_TYPE);
            FXMLTypeWrapper b = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [FXMLTypeWrapper#toString] contains the type value.
        @Test
        void toStringContainsType() {
            FXMLTypeWrapper wrapper = new FXMLTypeWrapper(STRING_TYPE);

            assertThat(wrapper.toString())
                    .contains(STRING_TYPE.toString());
        }
    }

    /// Tests for the [GroupedClassCount] record.
    @Nested
    class GroupedClassCountTest {

        /// Verifies that a null group throws [NullPointerException].
        @Test
        void nullGroupThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new GroupedClassCount(null, 1, List.of()));
        }

        /// Verifies that a null classes list throws [NullPointerException].
        @Test
        void nullClassesThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new GroupedClassCount("group", 1, null));
        }

        /// Verifies that a negative count throws [IllegalArgumentException].
        @Test
        void negativeCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new GroupedClassCount("group", -1, List.of()))
                    .withMessage("`count` must be non-negative");
        }

        /// Verifies that a zero count is valid.
        @Test
        void zeroCountIsValid() {
            GroupedClassCount gcc = new GroupedClassCount("group", 0, List.of());

            assertThat(gcc)
                    .hasFieldOrPropertyWithValue("group", "group")
                    .hasFieldOrPropertyWithValue("count", 0);
        }

        /// Verifies that a valid instance stores all fields correctly and copies the list defensively.
        @Test
        void validInstanceStoresFieldsAndCopiesList() {
            ClassCount classCount = new ClassCount("com.example.Foo", 3);
            List<ClassCount> mutableList = new java.util.ArrayList<>(List.of(classCount));
            GroupedClassCount gcc = new GroupedClassCount("com.example", 3, mutableList);

            mutableList.add(new ClassCount("com.example.Bar", 1));

            assertThat(gcc)
                    .hasFieldOrPropertyWithValue("group", "com.example")
                    .hasFieldOrPropertyWithValue("count", 3);
            assertThat(gcc.classes())
                    .containsExactly(classCount);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ClassCount classCount = new ClassCount("com.example.Foo", 2);
            GroupedClassCount a = new GroupedClassCount("com.example", 2, List.of(classCount));
            GroupedClassCount b = new GroupedClassCount("com.example", 2, List.of(classCount));

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [GroupedClassCount#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            GroupedClassCount gcc = new GroupedClassCount("com.example", 5, List.of());

            assertThat(gcc.toString())
                    .contains("com.example")
                    .contains("5");
        }
    }

    /// Tests for the [ReferenceWrapper] record.
    @Nested
    class ReferenceWrapperTest {

        /// Verifies that a null reference throws [NullPointerException].
        @Test
        void nullReferenceThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ReferenceWrapper(null));
        }

        /// Verifies that a valid instance stores the reference correctly.
        @Test
        void validInstanceStoresReference() {
            ReferenceWrapper wrapper = new ReferenceWrapper("myId");

            assertThat(wrapper)
                    .hasFieldOrPropertyWithValue("reference", "myId");
        }

        /// Verifies that two instances with the same reference are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ReferenceWrapper a = new ReferenceWrapper("myId");
            ReferenceWrapper b = new ReferenceWrapper("myId");

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ReferenceWrapper#toString] contains the reference value.
        @Test
        void toStringContainsReference() {
            ReferenceWrapper wrapper = new ReferenceWrapper("myId");

            assertThat(wrapper.toString())
                    .contains("myId");
        }
    }

    /// Tests for the package-private [ClassCount] record.
    @Nested
    class ClassCountTest {

        /// Verifies that a null fullClassName throws [NullPointerException].
        @Test
        void nullFullClassNameThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ClassCount(null, 1));
        }

        /// Verifies that a zero or negative count throws [IllegalArgumentException].
        @Test
        void zeroCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ClassCount("com.example.Foo", 0));
        }

        /// Verifies that a negative count throws [IllegalArgumentException].
        @Test
        void negativeCountThrowsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ClassCount("com.example.Foo", -1));
        }

        /// Verifies that a valid instance stores all fields correctly.
        @Test
        void validInstanceStoresFields() {
            ClassCount classCount = new ClassCount("com.example.Foo", 5);

            assertThat(classCount)
                    .hasFieldOrPropertyWithValue("fullClassName", "com.example.Foo")
                    .hasFieldOrPropertyWithValue("count", 5);
        }

        /// Verifies that two instances with the same fields are equal and have the same hash code.
        @Test
        void equalInstancesHaveSameHashCode() {
            ClassCount a = new ClassCount("com.example.Foo", 3);
            ClassCount b = new ClassCount("com.example.Foo", 3);

            assertThat(a).isEqualTo(b)
                    .hasSameHashCodeAs(b);
        }

        /// Verifies that [ClassCount#toString] contains all field values.
        @Test
        void toStringContainsFieldValues() {
            ClassCount classCount = new ClassCount("com.example.Foo", 7);

            assertThat(classCount.toString())
                    .contains("com.example.Foo")
                    .contains("7");
        }
    }
}
