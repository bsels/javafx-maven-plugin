package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Objects;

/// In-memory storage for the compiled bytecode of a Java class.
public final class InMemoryCompiledClass extends SimpleJavaFileObject {
    /// Stream used to store the compiled bytecode.
    private final ByteArrayOutputStream outputStream;
    /// Fully qualified name of the Java class.
    private final String className;

    /// Initializes a new [InMemoryCompiledClass] instance.
    ///
    /// @param className The fully qualified name of the class
    /// @throws NullPointerException If `className` is null
    public InMemoryCompiledClass(String className) {
        this.className = Objects.requireNonNull(className, "`className` must not be null");
        this.outputStream = new ByteArrayOutputStream();
        String uri = "jvm-memory:///%s%s".formatted(className.replace('.', '/'), Kind.CLASS.extension);
        super(URI.create(uri), Kind.CLASS);
    }

    /// Returns the name of the class.
    ///
    /// @return The class name
    public String getClassName() {
        return className;
    }

    /// Opens and returns the output stream for writing bytecode.
    ///
    /// @return The output stream
    @Override
    public ByteArrayOutputStream openOutputStream() {
        outputStream.reset();
        return outputStream;
    }

    /// Returns the compiled bytecode as a byte array.
    ///
    /// @return The bytecode array
    public byte[] getBytes() {
        return outputStream.toByteArray();
    }

    /// Returns a string representation of the [InMemoryCompiledClass].
    ///
    /// @return A string representation
    @Override
    public String toString() {
        return "InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(className, outputStream.size());
    }
}
