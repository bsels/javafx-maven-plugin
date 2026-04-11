package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// A custom implementation of [JavaFileManager] that stores compiled class files in memory.
/// Extends [ForwardingJavaFileManager] to delegate standard file manager operations
/// while providing an in-memory mechanism for storing the compiled bytecode of Java classes.
///
/// This class is primarily designed for use cases involving runtime Java compilation,
/// where compiled classes need to be accessed or loaded dynamically without relying on the filesystem.
public final class InMemoryCompiledClassOutputJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    /// A map that stores compiled classes in memory, where the key is the fully qualified class name
    /// and the value is an [InMemoryCompiledClass] instance representing the compiled class data.
    ///
    /// This map provides efficient in-memory storage and retrieval of Java classes that are compiled during runtime.
    /// It is particularly useful in scenarios involving dynamic or on-the-fly Java compilation,
    /// as it eliminates the need for filesystem-based storage.
    ///
    /// The keys in the map are [String] values representing fully qualified class names,
    /// ensuring that each compiled class can be uniquely identified.
    /// The values are instances of [InMemoryCompiledClass],
    /// which encapsulate the compiled bytecode of the corresponding class.
    ///
    /// This field is initialized during the construction of the containing class
    /// and remains immutable throughout its lifecycle.
    private final Map<String, InMemoryCompiledClass> compiledClasses;

    /// Constructs an instance of InMemoryCompiledClassOutputJavaFileManager.
    /// This file manager allows in-memory storage of compiled classes,
    /// delegating to the given file manager for other operations.
    ///
    /// @param javaFileManager the underlying JavaFileManager to which operations will be delegated. Must not be null.
    /// @throws NullPointerException if the provided javaFileManager is null.
    public InMemoryCompiledClassOutputJavaFileManager(JavaFileManager javaFileManager) {
        Objects.requireNonNull(javaFileManager, "`javaFileManager` cannot be null");
        this.compiledClasses = new HashMap<>();
        super(javaFileManager);
    }

    /// Returns the map of compiled classes.
    ///
    /// @return A map of fully qualified class names to [InMemoryCompiledClass] instances
    public Map<String, InMemoryCompiledClass> getCompiledClasses() {
        return Map.copyOf(compiledClasses);
    }

    /// Returns a [JavaFileObject] for the specified file name and kind.
    ///
    /// @param location  The location of the file
    /// @param className The fully qualified class name
    /// @param kind      The type of the file
    /// @param sibling   The originating source file
    /// @return The requested [JavaFileObject]
    /// @throws IOException If an I/O error occurs
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return switch (kind) {
            case CLASS -> compiledClasses.computeIfAbsent(className, InMemoryCompiledClass::new);
            case SOURCE, HTML, OTHER -> super.getJavaFileForOutput(location, className, kind, sibling);
        };
    }
}
