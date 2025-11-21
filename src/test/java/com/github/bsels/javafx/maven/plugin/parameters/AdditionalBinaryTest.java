package com.github.bsels.javafx.maven.plugin.parameters;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdditionalBinaryTest {

    /**
     * Test case to verify the correct string representation of an
     * AdditionalBinary object with all fields initialized.
     */
    @Test
    void testToStringWithAllFields() {
        Path location = Path.of("/path/to/file");
        String name = "TestBinary";
        String mappedJavaProperty = "java.home";

        AdditionalBinary additionalBinary = new AdditionalBinary(location, name, mappedJavaProperty);

        String expected = "AdditionalBinary[location=/path/to/file, name=TestBinary, mappedJavaProperty=java.home]";
        assertEquals(expected, additionalBinary.toString());
    }

    /**
     * Test case to verify the correct string representation of an
     * AdditionalBinary object with null values for all fields.
     */
    @Test
    void testToStringWithNullFields() {
        AdditionalBinary additionalBinary = new AdditionalBinary(null, null, null);

        String expected = "AdditionalBinary[location=null, name=null, mappedJavaProperty=null]";
        assertEquals(expected, additionalBinary.toString());
    }

    /**
     * Test case to verify the correct string representation of an
     * AdditionalBinary object with some null values and some set fields.
     */
    @Test
    void testToStringWithPartialNullFields() {
        Path location = Path.of("/path/to/file");
        String name = "TestBinary";

        AdditionalBinary additionalBinary = new AdditionalBinary(location, name, null);

        String expected = "AdditionalBinary[location=/path/to/file, name=TestBinary, mappedJavaProperty=null]";
        assertEquals(expected, additionalBinary.toString());
    }
}