package io.github.bsels.javafx.maven.plugin.parameters;

import java.nio.file.Path;

/// Information about a binary file, including its location, name, and mapped Java property.
public class AdditionalBinary {
    /// File system location of the binary.
    private Path location;
    /// Name of the binary.
    private String name;
    /// Java property to which the file location is mapped.
    private String mappedJavaProperty;

    /// Initializes a new [AdditionalBinary] instance.
    public AdditionalBinary() {
    }

    /// Initializes a new [AdditionalBinary] instance with the specified parameters.
    ///
    /// @param location           The file system location
    /// @param name               The name of the binary
    /// @param mappedJavaProperty The Java property to which the location is mapped
    public AdditionalBinary(Path location, String name, String mappedJavaProperty) {
        this();
        setLocation(location);
        setName(name);
        setMappedJavaProperty(mappedJavaProperty);
    }

    /// Returns a string representation of the [AdditionalBinary] object.
    ///
    /// @return A string representation
    @Override
    public String toString() {
        return "AdditionalBinary[location=%s, name=%s, mappedJavaProperty=%s]".formatted(
                getLocation(), getName(), getMappedJavaProperty()
        );
    }

    /// Returns the file system location.
    ///
    /// @return The location path
    public Path getLocation() {
        return location;
    }

    /// Sets the file system location.
    ///
    /// @param location The new location
    public void setLocation(Path location) {
        this.location = location;
    }

    /// Returns the name of the binary.
    ///
    /// @return The name
    public String getName() {
        return name;
    }

    /// Sets the name of the binary.
    ///
    /// @param name The new name
    public void setName(String name) {
        this.name = name;
    }

    /// Returns the Java property to which the binary location is mapped.
    ///
    /// @return The mapped Java property
    public String getMappedJavaProperty() {
        return mappedJavaProperty;
    }

    /// Sets the Java property to which the binary location is mapped.
    ///
    /// @param mappedJavaProperty The new Java property
    public void setMappedJavaProperty(String mappedJavaProperty) {
        this.mappedJavaProperty = mappedJavaProperty;
    }
}
