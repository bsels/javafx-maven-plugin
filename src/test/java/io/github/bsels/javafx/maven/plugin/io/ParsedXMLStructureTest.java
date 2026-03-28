package io.github.bsels.javafx.maven.plugin.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedXMLStructureTest {

    private static Stream<Arguments> nullCollectionsProvider() {
        return Stream.of(
                Arguments.of(null, List.of(), List.of()),
                Arguments.of(Map.of(), null, List.of()),
                Arguments.of(Map.of(), List.of(), null)
        );
    }

    /// Tests the primary constructor with all parameters.
    @Test
    void shouldCreateInstanceWithAllParameters() {
        // Given
        String name = "root";
        Map<String, String> properties = Map.of("attr", "val");
        List<ParsedXMLStructure> children = List.of(new ParsedXMLStructure("child", Map.of(), List.of()));
        List<String> comments = List.of("comment");
        Optional<String> textValue = Optional.of("text");

        // When
        ParsedXMLStructure structure = new ParsedXMLStructure(name, properties, children, comments, textValue);

        // Then
        assertThat(structure)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("properties", properties)
                .hasFieldOrPropertyWithValue("children", children)
                .hasFieldOrPropertyWithValue("comments", comments)
                .hasFieldOrPropertyWithValue("textValue", textValue);
    }

    /// Tests the constructor with name, properties, children, and comments.
    @Test
    void shouldCreateInstanceWithNamePropertiesChildrenAndComments() {
        // Given
        String name = "root";
        Map<String, String> properties = Map.of("attr", "val");
        List<ParsedXMLStructure> children = List.of(new ParsedXMLStructure("child", Map.of(), List.of()));
        List<String> comments = List.of("comment");

        // When
        ParsedXMLStructure structure = new ParsedXMLStructure(name, properties, children, comments);

        // Then
        assertThat(structure)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("properties", properties)
                .hasFieldOrPropertyWithValue("children", children)
                .hasFieldOrPropertyWithValue("comments", comments)
                .hasFieldOrPropertyWithValue("textValue", Optional.empty());
    }

    /// Tests the constructor with name, properties, and children.
    @Test
    void shouldCreateInstanceWithNamePropertiesAndChildren() {
        // Given
        String name = "root";
        Map<String, String> properties = Map.of("attr", "val");
        List<ParsedXMLStructure> children = List.of(new ParsedXMLStructure("child", Map.of(), List.of()));

        // When
        ParsedXMLStructure structure = new ParsedXMLStructure(name, properties, children);

        // Then
        assertThat(structure)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("properties", properties)
                .hasFieldOrPropertyWithValue("children", children)
                .hasFieldOrPropertyWithValue("comments", List.of())
                .hasFieldOrPropertyWithValue("textValue", Optional.empty());
    }

    /// Tests that the constructor throws NullPointerException for null name.
    @Test
    void shouldThrowNullPointerExceptionForNullName() {
        assertThatThrownBy(() -> new ParsedXMLStructure(null, Map.of(), List.of(), List.of(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("`name` must not be null");
    }

    /// Tests that the constructor throws NullPointerException for null textValue.
    @Test
    void shouldThrowNullPointerExceptionForNullTextValue() {
        assertThatThrownBy(() -> new ParsedXMLStructure("name", Map.of(), List.of(), List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("`textValue` must not be null");
    }

    /// Tests that the constructor throws NullPointerException for null collections/maps.
    @ParameterizedTest
    @MethodSource("nullCollectionsProvider")
    void shouldThrowNullPointerExceptionForNullCollections(Map<String, String> properties, List<ParsedXMLStructure> children, List<String> comments) {
        assertThatThrownBy(() -> new ParsedXMLStructure("name", properties, children, comments, Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }

    /// Tests that equals and hashCode work correctly.
    @Test
    void shouldBeEqualAndHaveSameHashCode() {
        // Given
        ParsedXMLStructure structure1 = new ParsedXMLStructure("name", Map.of("key", "val"), List.of(), List.of("comment"), Optional.of("text"));
        ParsedXMLStructure structure2 = new ParsedXMLStructure("name", Map.of("key", "val"), List.of(), List.of("comment"), Optional.of("text"));
        ParsedXMLStructure structureDifferent = new ParsedXMLStructure("other", Map.of("key", "val"), List.of(), List.of("comment"), Optional.of("text"));

        // Then
        assertThat(structure1)
                .isEqualTo(structure2)
                .hasSameHashCodeAs(structure2)
                .isNotEqualTo(structureDifferent)
                .isNotEqualTo(null)
                .isNotEqualTo("not a structure");
    }

    /// Tests that toString contains all expected values.
    @Test
    void toStringShouldContainFields() {
        // Given
        ParsedXMLStructure structure = new ParsedXMLStructure("name", Map.of("key", "val"), List.of(), List.of("comment"), Optional.of("text"));

        // Then
        assertThat(structure.toString())
                .contains("name")
                .contains("key=val")
                .contains("comment")
                .contains("text");
    }
}
