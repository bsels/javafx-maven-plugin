package com.github.bsels.javafx.maven.plugin.compiler.in.memory;

import java.util.Map;
import java.util.Objects;

/// A custom [ClassLoader] implementation that allows for loading Java classes from
/// in-memory bytecode representations instead of external sources such as a file system.
///
/// This class loader is designed to work with instances of [InMemoryCompiledClass],
/// which encapsulate the bytecode of compiled Java classes.
/// The [InMemoryClassLoader] maintains a map of compiled class data and uses it to define and load classes dynamically.
///
/// This class is useful in scenarios such as runtime class generation, on-the-fly compilation,
/// or environments where the use of traditional file-based class loading is restricted.
public final class InMemoryClassLoader extends ClassLoader {
    /// A map for storing in-memory compiled classes, where the keys represent the fully qualified class names
    /// and the values are instances of [InMemoryCompiledClass].
    ///
    /// This map is used by the [InMemoryClassLoader] to load classes dynamically from in-memory bytecode rather than
    /// from a file system or external source.
    /// It serves as an internal storage for compiled bytecode, allowing for efficient and isolated class loading.
    ///
    /// The keys in this map are unique class names,
    /// and the associated values contain the compiled bytecode and metadata for each class.
    /// The map must not be null during the initialization of the [InMemoryClassLoader].
    ///
    /// This design enables custom class loading and is particularly useful in scenarios such as dynamic compilation,
    /// testing environments, or frameworks requiring runtime class creation.
    private final Map<String, InMemoryCompiledClass> compiledClasses;

    /// Constructs an instance of [InMemoryClassLoader] with the specified map of compiled classes
    /// and a parent class loader.
    /// This class loader enables loading of classes directly from an in-memory map of compiled classes.
    ///
    /// @param compiledClasses a map containing the fully qualified class names as keys and the corresponding [InMemoryCompiledClass] instances as values. This map must not be null.
    /// @param parent          the parent [ClassLoader] to delegate class loading to if a class is not found in the in-memory compiled classes. This must not be null.
    /// @throws NullPointerException if `compiledClasses` or `parent` is null.
    public InMemoryClassLoader(Map<String, InMemoryCompiledClass> compiledClasses, ClassLoader parent) throws NullPointerException {
        Objects.requireNonNull(parent, "`parent` must not be null");
        this.compiledClasses = Objects.requireNonNull(compiledClasses, "`compiledClasses` must not be null");
        super(parent);
    }

    /// Attempts to locate and load a class with the specified name.
    /// If the class bytecode exists in the in-memory compiled classes map, it defines the class using the bytecode.
    /// Otherwise, it delegates the class loading to the superclass.
    ///
    /// @param name the fully qualified name of the class to find.
    /// @return the resulting Class object representing the loaded class.
    /// @throws ClassNotFoundException if the class cannot be found by this class loader or its parent class loader.
    /// @throws ClassFormatError       if the byte code of the internal compiled classes is invalid
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException, ClassFormatError {
        if (compiledClasses.containsKey(name)) {
            InMemoryCompiledClass inMemoryCompiledClass = compiledClasses.get(name);
            byte[] byteCode = inMemoryCompiledClass.getBytes();
            return defineClass(name, byteCode, 0, byteCode.length);
        }
        return super.findClass(name);
    }
}
