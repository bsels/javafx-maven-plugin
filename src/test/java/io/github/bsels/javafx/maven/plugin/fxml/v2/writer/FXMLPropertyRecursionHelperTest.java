package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLConstructorProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLMapProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLStaticObjectProperty;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import io.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLLiteral;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/// Unit tests for [FXMLPropertyRecursionHelper].
class FXMLPropertyRecursionHelperTest {

    /// The instance under test.
    private FXMLPropertyRecursionHelper classUnderTest;

    /// A simple literal value used as a leaf in property trees.
    private static final FXMLLiteral LITERAL = new FXMLLiteral("hello");

    /// A simple string type used in property definitions.
    private static final FXMLType STRING_TYPE = FXMLType.of(String.class);

    /// A simple class type used in static property definitions.
    private static final FXMLClassType STRING_CLASS_TYPE = new FXMLClassType(String.class);

    @BeforeEach
    void setUp() {
        classUnderTest = new FXMLPropertyRecursionHelper();
    }

    /// Tests for the single-property [FXMLPropertyRecursionHelper#walk] overload.
    @Nested
    class WalkSinglePropertyTest {

        /// Verifies that a null property throws [NullPointerException].
        @Test
        void nullPropertyThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> classUnderTest.walk((FXMLProperty) null, (v, c) -> Stream.of(v), null))
                    .withMessage("`property` must not be null");
        }

        /// Verifies that a null valueWalk function throws [NullPointerException].
        @Test
        void nullValueWalkThrowsNullPointerException() {
            FXMLConstructorProperty property = new FXMLConstructorProperty("p", STRING_TYPE, LITERAL);
            assertThatNullPointerException()
                    .isThrownBy(() -> classUnderTest.walk(property, null, null))
                    .withMessage("`valueWalk` must not be null");
        }

        /// Verifies that walking a [FXMLCollectionProperties] streams all its values.
        @Test
        void collectionPropertyStreamsAllValues() {
            FXMLLiteral second = new FXMLLiteral("world");
            FXMLCollectionProperties property = new FXMLCollectionProperties(
                    "items", "getItems", STRING_TYPE, STRING_TYPE,
                    List.of(LITERAL, second), Optional.empty()
            );

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(LITERAL, second);
        }

        /// Verifies that walking a [FXMLConstructorProperty] streams its single value.
        @Test
        void constructorPropertyStreamsSingleValue() {
            FXMLConstructorProperty property = new FXMLConstructorProperty("p", STRING_TYPE, LITERAL);

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(LITERAL);
        }

        /// Verifies that walking a [FXMLMapProperty] streams all map values.
        @Test
        void mapPropertyStreamsAllValues() {
            FXMLLiteral key1 = new FXMLLiteral("k1");
            FXMLLiteral key2 = new FXMLLiteral("k2");
            FXMLLiteral val1 = new FXMLLiteral("v1");
            FXMLLiteral val2 = new FXMLLiteral("v2");
            FXMLMapProperty property = new FXMLMapProperty(
                    "props", "getProperties", STRING_TYPE, STRING_TYPE, STRING_TYPE,
                    Map.of(key1, val1, key2, val2), Optional.empty()
            );

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .containsExactlyInAnyOrder(val1, val2);
        }

        /// Verifies that walking a [FXMLObjectProperty] streams its single value.
        @Test
        void objectPropertyStreamsSingleValue() {
            FXMLObjectProperty property = new FXMLObjectProperty("text", "setText", STRING_TYPE, LITERAL);

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(LITERAL);
        }

        /// Verifies that walking a [FXMLStaticObjectProperty] streams its single value.
        @Test
        void staticObjectPropertyStreamsSingleValue() {
            FXMLStaticObjectProperty property = new FXMLStaticObjectProperty(
                    "row", STRING_CLASS_TYPE, "setRow", STRING_TYPE, LITERAL
            );

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(LITERAL);
        }

        /// Verifies that the context object is passed through to the valueWalk function.
        @Test
        void contextIsPassedToValueWalk() {
            FXMLConstructorProperty property = new FXMLConstructorProperty("p", STRING_TYPE, LITERAL);
            String context = "myContext";

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(c), context).toList())
                    .containsExactly(context);
        }

        /// Verifies that an empty collection property produces an empty stream.
        @Test
        void emptyCollectionPropertyProducesEmptyStream() {
            FXMLCollectionProperties property = new FXMLCollectionProperties(
                    "items", "getItems", STRING_TYPE, STRING_TYPE,
                    List.of(), Optional.empty()
            );

            assertThat(classUnderTest.walk(property, (v, c) -> Stream.of(v), null).toList())
                    .isEmpty();
        }
    }

    /// Tests for the collection-of-properties [FXMLPropertyRecursionHelper#walk] overload.
    @Nested
    class WalkCollectionOfPropertiesTest {

        /// Verifies that a null properties collection throws [NullPointerException].
        @Test
        void nullPropertiesThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> classUnderTest.walk((List<FXMLProperty>) null, (v, c) -> Stream.of(v), null))
                    .withMessage("`properties` must not be null");
        }

        /// Verifies that a null valueWalk function throws [NullPointerException].
        @Test
        void nullValueWalkThrowsNullPointerException() {
            assertThatNullPointerException()
                    .isThrownBy(() -> classUnderTest.walk(List.of(), null, null))
                    .withMessage("`valueWalk` must not be null");
        }

        /// Verifies that an empty collection produces an empty stream.
        @Test
        void emptyCollectionProducesEmptyStream() {
            assertThat(classUnderTest.walk(List.<FXMLProperty>of(), (v, c) -> Stream.of(v), null).toList())
                    .isEmpty();
        }

        /// Verifies that multiple properties are all traversed and their values collected.
        @Test
        void multiplePropertiesAreAllTraversed() {
            FXMLLiteral lit1 = new FXMLLiteral("a");
            FXMLLiteral lit2 = new FXMLLiteral("b");
            FXMLConstructorProperty p1 = new FXMLConstructorProperty("p1", STRING_TYPE, lit1);
            FXMLObjectProperty p2 = new FXMLObjectProperty("p2", "setP2", STRING_TYPE, lit2);

            assertThat(classUnderTest.walk(List.of(p1, p2), (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(lit1, lit2);
        }

        /// Verifies that the context is forwarded to each value walk call.
        @Test
        void contextIsForwardedToEachValueWalk() {
            FXMLConstructorProperty p1 = new FXMLConstructorProperty("p1", STRING_TYPE, LITERAL);
            FXMLObjectProperty p2 = new FXMLObjectProperty("p2", "setP2", STRING_TYPE, LITERAL);
            String context = "ctx";

            assertThat(classUnderTest.walk(List.of(p1, p2), (v, c) -> Stream.of(c), context).toList())
                    .containsExactly(context, context);
        }

        /// Verifies that values from a collection property within the list are all included.
        @Test
        void collectionPropertyWithinListIncludesAllValues() {
            FXMLLiteral lit1 = new FXMLLiteral("x");
            FXMLLiteral lit2 = new FXMLLiteral("y");
            FXMLCollectionProperties collProp = new FXMLCollectionProperties(
                    "items", "getItems", STRING_TYPE, STRING_TYPE,
                    List.of(lit1, lit2), Optional.empty()
            );

            assertThat(classUnderTest.walk(List.of(collProp), (v, c) -> Stream.of(v), null).toList())
                    .containsExactly(lit1, lit2);
        }
    }
}
