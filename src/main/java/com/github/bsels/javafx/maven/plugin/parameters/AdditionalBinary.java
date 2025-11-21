package com.github.bsels.javafx.maven.plugin.parameters;

import java.nio.file.Path;

/// The AdditionalBinary class represents information about a binary file, including its
/// file system location, name, and the Java property to which its location is mapped.
/// It provides constructors, accessors, and mutators for managing the binary details.
public class AdditionalBinary {
    /// Represents the file system location of a binary.
    /// This field stores the path or address indicating where the binary is located.
    private Path location;
    /// Represents the name of the binary.
    /// This field stores a descriptive or unique identifier for the binary entity.
    private String name;
    /// Represents the java property to which the file location is mapped.
    private String mappedJavaProperty;

    /// Default constructor for the AdditionalBinary class.
    /// Initializes an instance of the AdditionalBinary object without setting any attributes.
    public AdditionalBinary() {
    }

    /// Constructs a new instance of the AdditionalBinary class with the specified parameters.
    ///
    /// @param location           the file system location of the binary
    /// @param name               the name of the binary
    /// @param mappedJavaProperty the Java property to which the binary location is mapped
    public AdditionalBinary(Path location, String name, String mappedJavaProperty) {
        this();
        setLocation(location);
        setName(name);
        setMappedJavaProperty(mappedJavaProperty);
    }

    /// Returns a string representation of the AdditionalBinary object.
    /// The string includes the location, name, and property fields.
    ///
    /// @return a string representation of the object in the format `AdditionalBinary[location=..., name=..., property=...]`
    @Override
    public String toString() {
        return "AdditionalBinary[location=%s, name=%s, mappedJavaProperty=%s]".formatted(
                getLocation(), getName(), getMappedJavaProperty()
        );
    }

    /// Retrieves the file system location of the binary.
    ///
    /// @return the current value of the location field
    public Path getLocation() {
        return location;
    }

    /// Sets the file system location of the binary.
    ///
    /// @param location the new location to be assigned to the binary
    public void setLocation(Path location) {
        this.location = location;
    }

    /// Retrieves the name of the binary.
    ///
    /// @return the current value of the name field
    public String getName() {
        return name;
    }

    /// Sets the name of the binary.
    ///
    /// @param name the new name to be assigned to the binary
    public void setName(String name) {
        this.name = name;
    }

    /// Retrieves the Java property to which the binary location is mapped.
    ///
    /// @return the current value of the mappedJavaProperty field
    public String getMappedJavaProperty() {
        return mappedJavaProperty;
    }

    /// Sets the Java property to which the binary location is mapped.
    ///
    /// @param mappedJavaProperty the new Java property to be assigned to the binary
    public void setMappedJavaProperty(String mappedJavaProperty) {
        this.mappedJavaProperty = mappedJavaProperty;
    }
}
