package io.github.bsels.javafx.maven.plugin.parameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/// Encapsulates information about an FXML-related directory, its associated package name,
/// and files to be excluded from processing.
public class FXMLDirectory {
    /// File system directory containing FXML files.
    private Path directory;
    /// Java package name corresponding to the directory structure.
    private String packageName;
    /// List of file paths to be excluded from processing.
    private List<Path> excludedFiles;

    /// Initializes a new [FXMLDirectory] instance.
    public FXMLDirectory() {
    }

    /// Initializes a new [FXMLDirectory] instance with the specified directory, package name, and excluded files.
    ///
    /// @param directory     The base directory
    /// @param packageName   The associated package name
    /// @param excludedFiles The list of files to exclude
    /// @throws NullPointerException If `directory` is null
    public FXMLDirectory(Path directory, String packageName, List<Path> excludedFiles)
            throws NullPointerException {
        this.directory = Objects.requireNonNull(directory, "`directory` must not be null");
        this.packageName = packageName;
        this.excludedFiles = List.copyOf(Objects.requireNonNullElseGet(excludedFiles, List::of));
        this();
    }

    /// Returns the file system directory.
    ///
    /// @return The base path for FXML files
    public Path getDirectory() {
        return directory;
    }

    /// Sets the file system directory.
    ///
    /// @param directory The base path for FXML files
    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    /// Returns the package name.
    ///
    /// @return The Java package name
    public String getPackageName() {
        return packageName;
    }

    /// Sets the package name.
    ///
    /// @param packageName The Java package name
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /// Returns the list of excluded file paths.
    ///
    /// @return The list of excluded paths
    public List<Path> getExcludedFiles() {
        return excludedFiles;
    }

    /// Sets the list of excluded file paths.
    ///
    /// @param excludedFiles The list of files to exclude
    public void setExcludedFiles(List<Path> excludedFiles) {
        this.excludedFiles = List.copyOf(Objects.requireNonNullElseGet(excludedFiles, List::of));
    }

    /// Validates the FXML directory configuration.
    /// Ensures that the directory is not null and resolves excluded files as absolute paths.
    ///
    /// @throws NullPointerException If the directory is null
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
