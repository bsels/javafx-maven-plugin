package io.github.bsels.javafx.maven.plugin.in.memory.compiler;

import io.github.bsels.javafx.maven.plugin.utils.Utils;
import org.apache.maven.plugin.logging.Log;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Facilitates in-memory compilation of Java source files.
/// Performs iterative compilation, filtering out files with errors to allow partial success.
public final class OptimisticInMemoryCompiler {
    /// Constant for "module-info.java".
    private static final String MODULE_INFO_JAVA = "module-info.java";
    /// Constant for "package-info.java".
    private static final String PACKAGE_INFO_JAVA = "package-info.java";

    /// Compiler options.
    private final List<String> options;

    /// Initializes a new [OptimisticInMemoryCompiler] instance.
    ///
    /// @param classpath The classpath to use for compilation
    public OptimisticInMemoryCompiler(List<URL> classpath) {
        classpath = Objects.requireNonNullElseGet(classpath, List::of);
        super();
        if (!classpath.isEmpty()) {
            options = List.of(
                    "-cp",
                    String.join(File.pathSeparator, classpath.stream().map(Utils::urlPathToOsPathString).toList())
            );
        } else {
            options = List.of();
        }
    }

    /// Compiles Java source files in the specified folders in memory.
    /// Filters out files with errors until either all files compile or no compilable files remain.
    ///
    /// @param logger        The logger instance
    /// @param sourceFolders The directories containing Java source files
    /// @return A map of fully qualified class names to [InMemoryCompiledClass] instances
    /// @throws IOException If an I/O error occurs
    public Map<String, InMemoryCompiledClass> optimisticCompile(Log logger, List<Path> sourceFolders) throws IOException {
        Objects.requireNonNull(logger, "`logger` cannot be null");
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            logger.warn("No source folders provided for compilation");
            return Map.of();
        }
        List<Path> sourceFiles = getSourceFiles(sourceFolders);
        int originalSize = sourceFiles.size();
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        try (
                StandardJavaFileManager standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null);
                InMemoryCompiledClassOutputJavaFileManager javaFileManager = new InMemoryCompiledClassOutputJavaFileManager(
                        standardJavaFileManager)
        ) {
            boolean validCompilation = false;
            while (!sourceFiles.isEmpty() && !validCompilation) {
                Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjectsFromPaths(
                        sourceFiles);
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
                JavaCompiler.CompilationTask javaCompilerTask = javaCompiler.getTask(
                        null, javaFileManager, diagnostics, options, null, compilationUnits
                );
                validCompilation = javaCompilerTask.call();
                if (!validCompilation) {
                    Set<Path> failedFiles = getFailedFilesFromDiagnostics(logger, diagnostics);
                    sourceFiles = sourceFiles.stream()
                            .filter(Predicate.not(failedFiles::contains))
                            .toList();
                }
            }
            Map<String, InMemoryCompiledClass> compiledClasses = javaFileManager.getCompiledClasses();
            logger.info("Compiled %d of %d source files".formatted(compiledClasses.size(), originalSize));
            return compiledClasses;
        }
    }

    /// Compiles Java source files and returns a function to define them in a [ClassLoader].
    ///
    /// @param logger        The logger instance
    /// @param sourceFolders The directories containing Java source files
    /// @return A function that defines the compiled classes into a [ClassLoader]
    /// @throws IOException If an I/O error occurs
    public UnaryOperator<ClassLoader> optimisticCompileIntoClassLoader(Log logger, List<Path> sourceFolders) throws IOException {
        Map<String, InMemoryCompiledClass> compiledClasses = optimisticCompile(logger, sourceFolders);
        return classLoader -> new InMemoryClassLoader(compiledClasses, classLoader);
    }

    /// Extracts file paths from diagnostics with an error or other severe kind.
    ///
    /// @param logger      The logger instance
    /// @param diagnostics The diagnostic information
    /// @return A set of source file paths with errors
    private Set<Path> getFailedFilesFromDiagnostics(Log logger, DiagnosticCollector<JavaFileObject> diagnostics) {
        return diagnostics.getDiagnostics()
                .stream()
                .filter(diagnostic -> switch (diagnostic.getKind()) {
                    case NOTE, MANDATORY_WARNING, WARNING -> false;
                    case OTHER, ERROR -> true;
                })
                .peek(diagnostic -> logger.debug(diagnostic.toString()))
                .map(Diagnostic::getSource)
                .map(JavaFileObject::toUri)
                .map(Path::of)
                .collect(Collectors.toSet());
    }

    /// Scans the specified folders for Java source files.
    ///
    /// @param sourceFolders The folders to scan
    /// @return A list of paths to `.java` files
    /// @throws IOException If an I/O error occurs
    private List<Path> getSourceFiles(List<Path> sourceFolders) throws IOException {
        List<Path> sourceFilesGrouped = new ArrayList<>();
        for (Path sourceFolder : sourceFolders) {
            if (Files.exists(sourceFolder)) {
                try (Stream<Path> sourceFiles = Files.walk(sourceFolder)) {
                    sourceFiles.filter(Files::isRegularFile)
                            .filter(Predicate.not(file -> MODULE_INFO_JAVA.equalsIgnoreCase(file.getFileName().toString())))
                            .filter(Predicate.not(file -> PACKAGE_INFO_JAVA.equalsIgnoreCase(file.getFileName().toString())))
                            .filter(file -> file.toString().toLowerCase().endsWith(JavaFileObject.Kind.SOURCE.extension))
                            .forEach(sourceFilesGrouped::add);
                }
            }
        }
        return List.copyOf(sourceFilesGrouped);
    }
}
