package io.github.bsels.javafx.maven.plugin.parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/// The FXMLDirectory class encapsulates information about an FXML-related directory, its associated package name,
/// and files to be excluded from processing within the context of the JavaFX Maven Plugin.
///
/// This class is designed to provide utility in managing the directory structure and organization of FXML resources,
/// offering a structured representation of key attributes needed for Maven build configuration tasks.
///
/// Responsibilities:
/// - Encapsulate and manage the base directory for FXML files.
/// - Handle the mapping of directory structures to Java package names.
/// - Support the exclusion of specific files from processing operations.
///
/// Design Considerations:
/// - Paths must be valid and resolvable within the file system.
/// - Package names must adhere to Java naming conventions.
/// - Excluded files are specified as a list of paths for fine-grained control.
///
/// Usage Context:
/// Used primarily in the JavaFX Maven Plugin parameter configuration to define resource directories,
/// logical package groupings, and exclusion rules.
public class FXMLDirectory {
    /// Represents the file system directory associated with the current FXML parameter configuration.
    /// This field is used to store the base path for FXML files or related resources within
    /// the JavaFX Maven Plugin project structure.
    ///
    /// Responsibilities:
    /// - To hold the path information for the directory containing FXML files.
    /// - Serve as a key point for managing resource lookup and processing in the Maven build lifecycle.
    ///
    /// Design Considerations:
    /// - Must be a valid file system path referencing the required directory.
    /// - Can support both relative and absolute paths depending on the configuration needs.
    ///
    /// Usage Context:
    /// Primarily used within the `FXMLDirectory` class to encapsulate and manage FXML resource paths
    /// to ensure effective parameter handling in Maven Plugin tasks.
    private Path directory;
    /// Represents the package name associated with the FXML resources in the JavaFX Maven Plugin parameter
    /// configuration.
    ///
    /// This field is used to store the Java package name that corresponds to the directory structure containing
    /// FXML files or related resources.
    /// It serves as a logical grouping identifier for the FXML files, aiding in resource organization and lookup.
    ///
    /// Responsibilities:
    /// - Defines the package path for FXML files within the project.
    /// - Facilitates mapping between directory-based file storage and package-based resource referencing.
    ///
    /// Design Considerations:
    /// - Must adhere to Java package naming conventions.
    /// - Supports hierarchical structuring for better modularity.
    ///
    /// Usage Context:
    /// Primarily used within the `FXMLDirectory` class for enabling effective handling of FXML file resources
    /// in context with their associated package names.
    private String packageName;
    /// Represents a list of file paths that are excluded from certain operations
    /// or processing within the context of FXML parameter configuration in the JavaFX Maven Plugin.
    ///
    /// Responsibilities:
    /// - Stores file paths that are intentionally set aside or ignored during resource processing or validation.
    /// - Helps in managing exceptions or exclusions for specific files within the configured directory structure.
    ///
    /// Design Considerations:
    /// - Each file path in the list must be valid and resolvable within the file system.
    /// - The excluded files can include absolute or relative paths, depending on the specific setup and use case.
    ///
    /// Usage Context:
    /// This field is used to streamline resource handling by marking certain files as excluded,
    /// preventing them from being part of the source code generation in the Maven build lifecycle.
    private List<Path> excludedFiles;

    /// Default constructor for the `FXMLDirectory` class.
    ///
    /// Initializes a new instance of the FXMLDirectory class with no arguments.
    /// This constructor allows the class to be instantiated without setting any initial properties.
    /// It is primarily intended for scenarios where the fields or properties of the instance will be set after
    /// construction.
    public FXMLDirectory() {
    }

    /// Constructs a new instance of the `FXMLDirectory` class with the specified directory, package name,
    /// and a list of files to exclude.
    ///
    /// @param directory     the base directory to be managed; must not be `null`
    /// @param packageName   the name of the package associated with the directory; may be `null`
    /// @param excludedFiles a list of paths representing the files to exclude; may be `null`
    /// @throws NullPointerException if the `directory` parameter is `null`
    public FXMLDirectory(Path directory, String packageName, List<Path> excludedFiles)
            throws NullPointerException {
        this.directory = Objects.requireNonNull(directory, "`directory` must not be null");
        this.packageName = packageName;
        this.excludedFiles = List.copyOf(Objects.requireNonNullElseGet(excludedFiles, List::of));
        this();
    }

    /// Returns the file system directory associated with the current FXML parameter configuration.
    ///
    /// @return the base path for FXML files
    public Path getDirectory() {
        return directory;
    }

    /// Sets the file system directory associated with the current FXML parameter configuration.
    ///
    /// @param directory the base path for FXML files
    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    /// Returns the package name associated with the FXML resources.
    ///
    /// @return the Java package name for the FXML files
    public String getPackageName() {
        return packageName;
    }

    /// Sets the package name associated with the FXML resources.
    ///
    /// @param packageName the Java package name for the FXML files
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /// Returns the list of file paths that are excluded from processing.
    ///
    /// @return a list of excluded file paths
    public List<Path> getExcludedFiles() {
        return excludedFiles;
    }

    /// Sets the list of file paths that are excluded from processing.
    ///
    /// @param excludedFiles a list of paths representing the files to exclude
    public void setExcludedFiles(List<Path> excludedFiles) {
        this.excludedFiles = List.copyOf(Objects.requireNonNullElseGet(excludedFiles, List::of));
    }

    /// Validates the FXML directory configuration.
    /// Ensures that the directory is not null and resolves excluded files as absolute paths.
    ///
    /// @throws NullPointerException if the directory is null
    public void validate() throws NullPointerException {
        Objects.requireNonNull(directory, "`directory` must not be null");
        directory = directory.toAbsolutePath();
        excludedFiles = Stream.ofNullable(excludedFiles)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(directory::resolve)
                .map(Path::toAbsolutePath)
                .toList();
    }
}
