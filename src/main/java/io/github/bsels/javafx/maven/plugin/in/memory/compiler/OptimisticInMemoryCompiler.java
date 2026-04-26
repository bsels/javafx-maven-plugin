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
///
/// This compiler performs iterative compilation. If errors are encountered during a compilation task,
/// the files responsible for those errors are identified and removed from the set of source files,
/// and the compilation is retried.
/// This "optimistic" approach allows as many files as possible to be successfully compiled into memory,
/// even if some have syntax errors or unresolved dependencies.
public final class OptimisticInMemoryCompiler {
    /// The name of the module descriptor file that should be excluded from in-memory compilation.
    private static final String MODULE_INFO_JAVA = "module-info.java";
    /// The name of the package descriptor file that should be excluded from in-memory compilation.
    private static final String PACKAGE_INFO_JAVA = "package-info.java";

    /// The list of compiler options, such as classpath configuration.
    private final List<String> options;

    /// Initializes a new [OptimisticInMemoryCompiler] instance with the specified classpath.
    ///
    /// The classpath is converted from [URL]s to OS-specific path strings and added to the compiler options using
    /// the `-cp` flag.
    ///
    /// @param classpath The list of URLs to be used as the classpath for compilation.
    ///                  If null or empty, no classpath is added to the compiler options.
    public OptimisticInMemoryCompiler(List<URL> classpath) {
        classpath = Objects.requireNonNullElseGet(classpath, List::of);
        if (!classpath.isEmpty()) {
            options = List.of(
                    "-cp",
                    String.join(File.pathSeparator, classpath.stream().map(Utils::urlPathToOsPathString).toList())
            );
        } else {
            options = List.of();
        }
    }

    /// Compiles all Java source files found in the specified source folders into memory.
    ///
    /// The method recursively scans the provided folders for `.java` files (excluding `module-info.java`
    /// and `package-info.java`).
    /// It then attempts to compile all identified files using an [InMemoryCompiledClassOutputJavaFileManager] to
    /// capture the resulting bytecode.
    ///
    /// If the compilation fails, it uses the diagnostics to identify the files with errors,
    /// removes them from the set of files to be compiled.
    /// And repeats the process until either all remaining files compile successfully or no files are left.
    ///
    /// @param logger        The Maven logger used for reporting compilation progress and debug information.
    /// @param sourceFolders The list of [Path]s representing directories to scan for Java source files.
    /// @return A [Map] where keys are fully qualified class names and values are the corresponding
    /// [InMemoryCompiledClass] instances containing the captured bytecode.
    /// @throws IOException If an I/O error occurs during file scanning or if the compiler fails to initialize.
    public Map<String, InMemoryCompiledClass> optimisticCompile(Log logger, List<Path> sourceFolders) throws IOException {
        Objects.requireNonNull(logger, "`logger` cannot be null");
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            logger.warn("No source folders provided for compilation");
            return Map.of();
        }
        List<Path> sourceFiles = getSourceFiles(sourceFolders);
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
            logger.info("Compiled %d classes".formatted(compiledClasses.size()));
            logger.debug("Compiled classes: %s".formatted(compiledClasses.keySet()));
            return compiledClasses;
        }
    }

    /// Compiles Java source files and returns a function to define them in a custom [ClassLoader].
    ///
    /// This method first performs the optimistic compilation and then returns a lambda function.
    /// When applied to an existing [ClassLoader],
    /// this function creates and returns a new [InMemoryClassLoader] that can load the compiled classes,
    /// delegating to the provided parent loader for all other classes.
    ///
    /// @param logger        The Maven logger used for reporting compilation progress.
    /// @param sourceFolders The list of [Path]s representing directories to scan for Java source files.
    /// @return A [UnaryOperator] that, when given a parent [ClassLoader], returns an
    /// [InMemoryClassLoader] populated with the successfully compiled classes.
    /// @throws IOException If an I/O error occurs during compilation.
    public UnaryOperator<ClassLoader> optimisticCompileIntoClassLoader(Log logger, List<Path> sourceFolders) throws IOException {
        Map<String, InMemoryCompiledClass> compiledClasses = optimisticCompile(logger, sourceFolders);
        return classLoader -> new InMemoryClassLoader(compiledClasses, classLoader);
    }

    /// Extracts the file paths of all source files that caused compilation errors or other failures.
    ///
    /// It filters the collected diagnostics to find those of kind [Diagnostic.Kind#ERROR] or [Diagnostic.Kind#OTHER].
    /// For each such diagnostic, it extracts the associated source file path.
    ///
    /// @param logger      The Maven logger used to log diagnostic details at the debug level.
    /// @param diagnostics The [DiagnosticCollector] containing information about the last compilation attempt.
    /// @return A [Set] of [Path]s representing the files that failed to compile.
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

    /// Recursively scans the provided directories for Java source files.
    ///
    /// It identifies all files ending in `.java`,
    /// but explicitly excludes `module-info.java` and `package-info.java` because these often contain metadata that
    /// is not needed or can cause issues during partial in-memory compilation.
    ///
    /// @param sourceFolders The list of root directories to start the recursive scan.
    /// @return An immutable [List] of [Path]s to the discovered `.java` source files.
    /// @throws IOException If an error occurs during directory traversal.
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
