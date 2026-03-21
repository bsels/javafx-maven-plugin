package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

import com.github.bsels.javafx.maven.plugin.utils.Utils;
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

/// The [OptimisticInMemoryCompiler] class facilitates the in-memory compilation of Java source files.
/// It performs iterative compilation, filtering out files with errors to allow partial success,
/// and optionally supports defining successfully compiled classes into a specified class loader.
public final class OptimisticInMemoryCompiler {
    /// Represents the file name "module-info.java" as a constant.
    /// This constant is used to identify and exclude module descriptor files during the in-memory compilation process
    /// performed by the [OptimisticInMemoryCompiler] class.
    /// Files named "module-info.java" are not considered for compilation, as they serve a specific purpose in the
    /// module system and are not standard Java class files.
    private static final String MODULE_INFO_JAVA = "module-info.java";
    /// A constant representing the special Java source file "package-info.java".
    /// This file is typically used to provide package-level annotations and documentation in Java projects.
    ///
    /// Within the context of the `OptimisticInMemoryCompiler` class, files with this name are specifically excluded
    /// from the in-memory compilation process because they do not contain class definitions that can be compiled into
    /// executable bytecode.
    private static final String PACKAGE_INFO_JAVA = "package-info.java";

    /// A collection of options that may influence the behavior of the in-memory compilation process.
    ///
    /// This list contains strings representing various configuration options
    /// or compiler arguments that can be applied during the compilation of Java source files.
    /// These options are passed to the underlying Java compiler and may include flags, optimization settings,
    /// or other compiler-specific parameters.
    ///
    /// The options do not include file-specific or error-related filters,
    /// as those are handled separately within the compilation workflow.
    /// It is intended to be immutable and is initialized during the creation of an [OptimisticInMemoryCompiler] instance.
    private final List<String> options;

    /// Constructs a new instance of [OptimisticInMemoryCompiler].
    ///
    /// This class facilitates the compilation of Java source files using an in-memory approach.
    /// It supports iterative compilation of source files, filtering out files with compilation errors until
    /// all possible files are successfully compiled or no further files can be processed.
    ///
    /// By using this compiler, users can dynamically compile Java source files
    /// and optionally load the compiled classes into a class loader at runtime.
    /// The in-memory compilation process excludes files named "module-info.java"
    /// and "package-info.java" from consideration.
    ///
    /// This constructor initializes the compiler without additional configuration.
    ///
    /// @param classpath The classpath that needs to be added to the compiler
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

    /// Attempts to compile Java source files located in the specified source folders using
    /// an in-memory compilation approach.
    /// The method iterates through the compilation process and filters out files with compilation errors,
    /// until either all files are successfully compiled or no compilable files remain.
    ///
    /// @param logger        the logger to use for logging compilation progress and errors. Must not be null.
    /// @param sourceFolders a list of paths representing the directories containing Java source files to be compiled. Each folder in the list is scanned for `.java` files, excluding files named "module-info.java" or "package-info.java". Must not be null or empty.
    /// @return a map where the keys are the fully qualified names of the successfully compiled classes, and the values are their corresponding `InMemoryCompiledClass` instances containing the compiled bytecode, or an empty map if no files were successfully compiled.
    /// @throws IOException if an I/O error occurs while accessing the source folders or their contents.
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
                InMemoryCompiledClassOutputJavaFileManager javaFileManager = new InMemoryCompiledClassOutputJavaFileManager(standardJavaFileManager)
        ) {
            boolean validCompilation = false;
            while (!sourceFiles.isEmpty() && !validCompilation) {
                Iterable<? extends JavaFileObject> compilationUnits = standardJavaFileManager.getJavaFileObjectsFromPaths(sourceFiles);
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

    /// Attempts to compile Java source files from the specified source directories using an in-memory compilation
    /// strategy and returns a function that, when invoked with a class loader, defines the compiled classes into it.
    /// This method compiles source files iteratively, removing files with errors until all compilable files
    /// are successfully processed or no further files can be compiled.
    ///
    /// @param logger        the logger to use for logging compilation progress and errors. Must not be null.
    /// @param sourceFolders a list of paths representing the source directories containing Java source files to be compiled. Each directory is scanned for `.java` files, excluding "module-info.java" and "package-info.java". Must not be null or empty.
    /// @return a unary operator that takes a class loader, defines the compiled classes into it, and returns the updated class loader.
    /// @throws IOException if an I/O error occurs while accessing the source directories or processing their contents.
    public UnaryOperator<ClassLoader> optimisticCompileIntoClassLoader(Log logger, List<Path> sourceFolders) throws IOException {
        Map<String, InMemoryCompiledClass> compiledClasses = optimisticCompile(logger, sourceFolders);
        return classLoader -> new InMemoryClassLoader(compiledClasses, classLoader);
    }

    /// Extracts and returns a set of file paths corresponding to diagnostics with an error or other severe kind.
    /// Filters diagnostics to include only those of kind ERROR or OTHER, and maps their sources to file paths.
    ///
    /// @param diagnostics a `DiagnosticCollector<JavaFileObject>` instance that contains the diagnostic information of a compilation process. Must not be null. Each diagnostic may provide details such as its severity and the associated source file.
    /// @return a `Set<Path>` containing the file paths of source files associated with error or other severe diagnostics. Returns an empty set if there are no such diagnostics or if their sources are unavailable.
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

    /// Scans the specified source folders for Java source files and returns a list of file paths.
    /// Only regular files with a `.java` extension are included,
    /// excluding files named "module-info.java" and "package-info.java".
    ///
    /// @param sourceFolders a list of paths representing the source folders to scan for source files. This list must not be null, but individual folders within the list may be empty.
    /// @return a list of `Path` objects representing the discovered source files that meet the criteria (e.g., `.java` files excluding "module-info.java" and "package-info.java").
    /// @throws IOException if an I/O error occurs while accessing any of the source folders or traversing their contents.
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
