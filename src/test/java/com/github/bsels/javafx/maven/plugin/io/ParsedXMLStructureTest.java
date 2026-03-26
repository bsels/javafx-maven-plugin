package com.github.bsels.javafx.maven.plugin.io;

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
        assertThat(structure.name()).isEqualTo(name);
        assertThat(structure.properties()).containsAllEntriesOf(properties);
        assertThat(structure.children()).hasSameElementsAs(children);
        assertThat(structure.comments()).containsAll(comments);
        assertThat(structure.textValue()).isEqualTo(textValue);
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
        assertThat(structure.name()).isEqualTo(name);
        assertThat(structure.properties()).containsAllEntriesOf(properties);
        assertThat(structure.children()).hasSameElementsAs(children);
        assertThat(structure.comments()).containsAll(comments);
        assertThat(structure.textValue()).isEmpty();
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
        assertThat(structure.name()).isEqualTo(name);
        assertThat(structure.properties()).containsAllEntriesOf(properties);
        assertThat(structure.children()).hasSameElementsAs(children);
        assertThat(structure.comments()).isEmpty();
        assertThat(structure.textValue()).isEmpty();
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

    private static Stream<Arguments> nullCollectionsProvider() {
        return Stream.of(
                Arguments.of(null, List.of(), List.of()),
                Arguments.of(Map.of(), null, List.of()),
                Arguments.of(Map.of(), List.of(), null)
        );
    }

    /// Tests getTextValue when there are no children and a text value is present.
    @Test
    void getTextValueShouldReturnTextValueWhenNoChildren() {
        // Given
        ParsedXMLStructure structure = new ParsedXMLStructure("name", Map.of(), List.of(), List.of(), Optional.of("text"));

        // When
        String result = structure.getTextValue();

        // Then
        assertThat(result).isEqualTo("text");
    }

    /// Tests getTextValue when there are no children and no text value is present.
    @Test
    void getTextValueShouldReturnEmptyStringWhenNoTextValue() {
        // Given
        ParsedXMLStructure structure = new ParsedXMLStructure("name", Map.of(), List.of(), List.of(), Optional.empty());

        // When
        String result = structure.getTextValue();

        // Then
        assertThat(result).isEmpty();
    }

    /// Tests getTextValue when children are present.
    @Test
    void getTextValueShouldThrowIllegalStateExceptionWhenChildrenArePresent() {
        // Given
        ParsedXMLStructure child = new ParsedXMLStructure("child", Map.of(), List.of());
        ParsedXMLStructure structure = new ParsedXMLStructure("root", Map.of(), List.of(child), List.of(), Optional.of("text"));

        // When & Then
        assertThatThrownBy(structure::getTextValue)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot get text value of an element with children");
    }
}
