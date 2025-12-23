package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

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
    /// @param fileManager the underlying JavaFileManager to which operations will be delegated. Must not be null.
    /// @throws NullPointerException if the provided fileManager is null.
    public InMemoryCompiledClassOutputJavaFileManager(JavaFileManager fileManager) {
        Objects.requireNonNull(fileManager, "`fileManager` cannot be null");
        this.compiledClasses = new HashMap<>();
        super(fileManager);
    }

    /// Retrieves an immutable map containing the compiled classes stored in memory.
    /// The map is keyed by the fully qualified class name, and the values are instances of [InMemoryCompiledClass],
    /// which encapsulates the compiled bytecode.
    ///
    /// @return an immutable [Map] where keys are the class names as [String], and values are the associated [InMemoryCompiledClass] objects.
    public Map<String, InMemoryCompiledClass> getCompiledClasses() {
        return Map.copyOf(compiledClasses);
    }

    /// Generates a [JavaFileObject] for a given class, allowing in-memory storage of compiled classes when needed.
    ///
    /// @param location  the location for which the file is being generated. This is typically used to indicate where the file resides in the context of a compilation process.
    /// @param className the fully qualified name of the class for which the Java file is to be generated. This is used as the identifier for the output file.
    /// @param kind      the type of the file being generated, which can be `SOURCE`, `CLASS`, `HTML`, or `OTHER`. For `CLASS` kind, an in-memory compiled class is returned.
    /// @param sibling   a file object associated with the originating source file. Can be used for context but may be null.
    /// @return a [JavaFileObject] representing the requested file. If the kind is CLASS, an instance of [InMemoryCompiledClass] is returned and stored in the internal map, allowing for in-memory storage of compiled bytecode. For other kinds, the result is delegated to the superclass.
    /// @throws IOException if an IO error occurs during file creation while handling non-CLASS kinds.
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return switch (kind) {
            case CLASS -> compiledClasses.computeIfAbsent(className, InMemoryCompiledClass::new);
            case SOURCE, HTML, OTHER -> super.getJavaFileForOutput(location, className, kind, sibling);
        };
    }
}
