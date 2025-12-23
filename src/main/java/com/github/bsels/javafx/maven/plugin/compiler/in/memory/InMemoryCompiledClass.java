package com.github.bsels.javafx.maven.plugin.compiler.in.memory;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Objects;

/// Represents an in-memory storage for the compiled bytecode of a Java class.
/// This class is a custom extension of [SimpleJavaFileObject]
/// that enables storing compiled class data directly in memory.
///
/// The bytecode is written to an internal [ByteArrayOutputStream],
/// which can be accessed to retrieve the compiled data as a byte array.
///
/// This is useful in scenarios where compiled classes need to be stored
/// and loaded dynamically without involving a filesystem.
public final class InMemoryCompiledClass extends SimpleJavaFileObject {
    /// A [ByteArrayOutputStream] used to store the compiled bytecode of a Java class in memory.
    ///
    /// This output stream serves as an internal buffer for writing
    /// and storing the class data during the compilation process.
    /// The contents of this stream can later be accessed as a byte array, representing the compiled class.
    ///
    /// Once initialized, the output stream is associated with a specific instance of [InMemoryCompiledClass]
    /// and cannot be modified externally.
    /// It provides the mechanism to save compiled data of the related class in a byte array format.
    private final ByteArrayOutputStream outputStream;
    /// The fully qualified name of the Java class associated with this in-memory representation of compiled bytecode.
    ///
    /// This variable holds the name of the class and is used to identify the compiled data stored in this object.
    /// It is initialized during construction and cannot be null.
    private final String className;

    /// Constructs an instance of InMemoryCompiledClass with the specified class name.
    /// This class represents an in-memory storage for the compiled bytecode of a Java class.
    ///
    /// @param className the fully qualified name of the class to be associated with this object. Must not be null.
    /// @throws NullPointerException if the className parameter is null.
    public InMemoryCompiledClass(String className) {
        this.className = Objects.requireNonNull(className, "`className` must not be null");
        this.outputStream = new ByteArrayOutputStream();
        String uri = "memory:///%s%s".formatted(className.replace('.', '/'), Kind.CLASS.extension);
        super(URI.create(uri), Kind.CLASS);
    }

    /// Retrieves the name of the class associated with this object.
    ///
    /// @return the name of the class.
    public String getClassName() {
        return className;
    }

    /// Opens and returns an output stream associated with this in-memory compiled class.
    /// The output stream can be used to write the compiled bytecode of the class.
    ///
    /// @return the output stream used for writing the compiled class data
    @Override
    public ByteArrayOutputStream openOutputStream() {
        return outputStream;
    }

    /// Returns the byte array representation of the compiled class.
    ///
    /// @return a byte array containing the compiled class data.
    public byte[] getBytes() {
        return outputStream.toByteArray();
    }

    /// Returns a string representation of the `InMemoryCompiledClass`.
    /// The string includes the class name and the compiled class size in bytes.
    ///
    /// @return a string representation of the object, including the class name and the size of the compiled bytecode.
    @Override
    public String toString() {
        return "InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(className, outputStream.size());
    }
}
