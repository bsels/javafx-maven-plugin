package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/// [InMemoryClassLoader] is a custom [ClassLoader] responsible for loading Java classes from in-memory bytecode.
///
/// It enables dynamic loading of classes that have been compiled on-the-fly,
/// such as those generated from FXML files during the plugin's execution.
public final class InMemoryClassLoader extends ClassLoader {
    /// The logger for this class.
    private static final Logger log = LoggerFactory.getLogger(InMemoryClassLoader.class);

    /// A [Map] where keys are fully qualified class names and values are their corresponding
    /// [InMemoryCompiledClass] instances containing the bytecode.
    private final Map<String, InMemoryCompiledClass> compiledClasses;

    /// Initializes a new [InMemoryClassLoader] instance with the provided map of compiled classes
    /// and a parent [ClassLoader].
    ///
    /// The parent class loader is used to delegate class loading for classes not found in the `compiledClasses` map,
    /// ensuring that standard library and other project dependencies are correctly resolved.
    ///
    /// @param compiledClasses A map of fully qualified class names to [InMemoryCompiledClass] instances. Must not be null.
    /// @param parent          The parent [ClassLoader] to use for delegation. Must not be null.
    /// @throws NullPointerException If `compiledClasses` or `parent` is null.
    public InMemoryClassLoader(Map<String, InMemoryCompiledClass> compiledClasses, ClassLoader parent) throws NullPointerException {
        Objects.requireNonNull(parent, "`parent` cannot be null");
        this.compiledClasses = Objects.requireNonNull(compiledClasses, "`compiledClasses` cannot be null");
        super(parent);
    }

    /// Attempts to locate and load a class by its fully qualified name from the internal map of compiled classes.
    ///
    /// This method is called by the class loading mechanism when a class cannot be found by the parent class loader.
    /// It performs the following steps:
    /// 1. Checks if the class name exists in the `compiledClasses` map.
    /// 2. If found, retrieves the associated [InMemoryCompiledClass] and its bytecode.
    /// 3. Defines the class using the inherited `defineClass` method with the retrieved bytecode.
    /// 4. If not found, throws a [ClassNotFoundException].
    ///
    /// @param name The fully qualified name of the class to find and load.
    /// @return The newly defined [Class] instance.
    /// @throws ClassNotFoundException If the class name is not present in the `compiledClasses` map.
    /// @throws ClassFormatError       If the bytecode of the class is invalid or malformed.
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException, ClassFormatError {
        log.debug("Searching class: {}", name);
        if (compiledClasses.containsKey(name)) {
            log.debug("Found class: {}", name);
            InMemoryCompiledClass inMemoryCompiledClass = compiledClasses.get(name);
            byte[] byteCode = inMemoryCompiledClass.getBytes();
            return defineClass(name, byteCode, 0, byteCode.length);
        }
        throw new ClassNotFoundException(name);
    }
}
