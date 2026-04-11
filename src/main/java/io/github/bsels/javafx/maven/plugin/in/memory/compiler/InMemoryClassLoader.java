package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import java.util.Map;
import java.util.Objects;

/// Custom [ClassLoader] for loading Java classes from in-memory bytecode.
public final class InMemoryClassLoader extends ClassLoader {
    /// Map of fully qualified class names to their [InMemoryCompiledClass] instances.
    private final Map<String, InMemoryCompiledClass> compiledClasses;

    /// Initializes a new [InMemoryClassLoader] instance.
    ///
    /// @param compiledClasses The map of compiled classes
    /// @param parent          The parent [ClassLoader]
    /// @throws NullPointerException If `compiledClasses` or `parent` is null
    public InMemoryClassLoader(Map<String, InMemoryCompiledClass> compiledClasses, ClassLoader parent) throws NullPointerException {
        Objects.requireNonNull(parent, "`parent` cannot be null");
        this.compiledClasses = Objects.requireNonNull(compiledClasses, "`compiledClasses` cannot be null");
        super(parent);
    }

    /// Attempts to locate and load a class by its fully qualified name.
    ///
    /// @param name The fully qualified name of the class
    /// @return The loaded [Class]
    /// @throws ClassNotFoundException If the class cannot be found
    /// @throws ClassFormatError       If the bytecode is invalid
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException, ClassFormatError {
        if (compiledClasses.containsKey(name)) {
            InMemoryCompiledClass inMemoryCompiledClass = compiledClasses.get(name);
            byte[] byteCode = inMemoryCompiledClass.getBytes();
            return defineClass(name, byteCode, 0, byteCode.length);
        }
        throw new ClassNotFoundException(name);
    }
}
