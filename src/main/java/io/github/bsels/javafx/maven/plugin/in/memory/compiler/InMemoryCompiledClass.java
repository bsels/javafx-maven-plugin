package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Objects;

/// In-memory storage for the compiled bytecode of a Java class.
///
/// This class extends [SimpleJavaFileObject] to provide a custom implementation of a class file that resides in memory.
/// It uses a [ByteArrayOutputStream] to capture the compiler's output
/// and a custom `jvm-memory:///` URI to identify the class.
/// This is particularly useful for capturing the results of runtime compilation and making them available to
/// an [InMemoryClassLoader] without the need for physical storage.
public final class InMemoryCompiledClass extends SimpleJavaFileObject {
    /// Stream used to store the compiled bytecode written by the compiler.
    /// This stream is lazily initialized by the constructor and remains open until the object is garbage collected.
    private final ByteArrayOutputStream outputStream;
    /// Fully qualified name of the Java class (e.g., `com.example.MyClass`).
    private final String className;

    /// Initializes a new [InMemoryCompiledClass] instance for the specified class name.
    ///
    /// This constructor creates a unique `jvm-memory:///` URI based on the provided class name
    /// and sets the kind to `CLASS`.
    /// It also prepares an internal [ByteArrayOutputStream] to store the compiled bytecode.
    ///
    /// @param className the fully qualified name of the class being compiled. Must not be null.
    /// @throws NullPointerException if the provided `className` is `null`.
    public InMemoryCompiledClass(String className) {
        this.className = Objects.requireNonNull(className, "`className` must not be null");
        this.outputStream = new ByteArrayOutputStream();
        String uri = "jvm-memory:///%s%s".formatted(className.replace('.', '/'), Kind.CLASS.extension);
        super(URI.create(uri), Kind.CLASS);
    }

    /// Returns the fully qualified name of the compiled class.
    ///
    /// @return the class name as a [String].
    public String getClassName() {
        return className;
    }

    /// Opens and returns the internal [ByteArrayOutputStream] for the compiler to write into.
    ///
    /// This method overrides the standard behavior by returning a reference to the internal buffer.
    /// It calls `reset()` on the stream before returning it,
    /// ensuring that any previous contents are cleared and the compiler starts writing from the beginning.
    ///
    /// @return the [ByteArrayOutputStream] used for writing the class bytecode.
    @Override
    public ByteArrayOutputStream openOutputStream() {
        outputStream.reset();
        return outputStream;
    }

    /// Returns the compiled bytecode stored in the internal output stream.
    ///
    /// This method converts the current contents of the [ByteArrayOutputStream] into a byte array,
    /// which can then be used for class loading or other processing.
    ///
    /// @return a `byte[]` containing the compiled bytecode.
    public byte[] getBytes() {
        return outputStream.toByteArray();
    }

    /// Returns a human-readable string representation of this [InMemoryCompiledClass].
    ///
    /// The string includes the fully qualified class name and the current size of the compiled bytecode (in bytes).
    ///
    /// @return a [String] detailing the class name and its compiled size.
    @Override
    public String toString() {
        return "InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(className, outputStream.size());
    }
}
