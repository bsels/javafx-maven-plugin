/// Provides support for in-memory compilation and dynamic loading of Java source files.
///
/// This package contains classes that provide a flexible and efficient approach to:
/// - Compiling Java source files directly in memory
/// - Dynamically loading compiled classes without using the file system
/// - Optimistic compilation that allows partial success of source files
///
/// Key parts:
/// 1. [com.github.bsels.javafx.maven.plugin.in.memory.compiler.InMemoryCompiledClass]: Represents compiled bytecode of a Java class in memory
/// 2. [com.github.bsels.javafx.maven.plugin.in.memory.compiler.InMemoryClassLoader]: A custom ClassLoader for loading classes from in-memory bytecode
/// 3. [com.github.bsels.javafx.maven.plugin.in.memory.compiler.OptimisticInMemoryCompiler]: Facilitates iterative compilation of Java source files
///
/// Usage examples:
/// ```java
/// // Compile Java source files in memory
/// OptimisticInMemoryCompiler compiler = new OptimisticInMemoryCompiler();
/// List<Path> sourceFolders = Arrays.asList(Paths.get(“src/main/java”));
/// Map<String, InMemoryCompiledClass> compiledClasses = compiler.optimisticCompile(sourceFolders);
///
/// // Load compiled classes dynamically
/// ClassLoader dynamicClassLoader = compiler.optimisticCompileIntoClassLoader(sourceFolders)
///     .apply(getClass().getClassLoader());
/// ```
package com.github.bsels.javafx.maven.plugin.in.memory.compiler;
