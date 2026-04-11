package io.github.bsels.javafx.maven.plugin.parameters;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/// Tests for [FXMLDirectory] class.
@DisplayName("FXMLDirectory Tests")
class FXMLDirectoryTest {

    @TempDir
    Path tempDir;

    /// Tests for constructors.
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {
        /// Should create an instance with the default constructor.
        @Test
        @DisplayName("Should create instance with default constructor")
        void defaultConstructor() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            assertThat(fxmlDirectory.getDirectory()).isNull();
            assertThat(fxmlDirectory.getPackageName()).isNull();
            assertThat(fxmlDirectory.getExcludedFiles()).isNull();
        }

        /// Should create an instance with the parameterized constructor and valid arguments.
        @Test
        @DisplayName("Should create instance with parameterized constructor")
        void parameterizedConstructor() {
            Path dir = tempDir.resolve("fxml");
            List<Path> excluded = List.of(Path.of("Exclude.fxml"));
            FXMLDirectory fxmlDirectory = new FXMLDirectory(dir, "com.example", excluded);

            assertThat(fxmlDirectory.getDirectory()).isEqualTo(dir);
            assertThat(fxmlDirectory.getPackageName()).isEqualTo("com.example");
            assertThat(fxmlDirectory.getExcludedFiles()).isEqualTo(excluded);
        }

        /// Should throw [NullPointerException] if the directory is null in the parameterized constructor.
        @Test
        @DisplayName("Should throw NPE if directory is null in parameterized constructor")
        void parameterizedConstructorNullDir() {
            assertThatThrownBy(() -> new FXMLDirectory(null, "pkg", List.of()))
                .isInstanceOf(NullPointerException.class);
        }

        /// Should initialize with an empty list if excludedFiles is null in the parameterized constructor.
        @Test
        @DisplayName("Should handle null excludedFiles in parameterized constructor")
        void parameterizedConstructorNullExcluded() {
            Path dir = tempDir.resolve("fxml");
            FXMLDirectory fxmlDirectory = new FXMLDirectory(dir, "pkg", null);
            assertThat(fxmlDirectory.getExcludedFiles()).isNotNull().isEmpty();
        }
    }

    /// Tests for getters and setters.
    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTest {
        /// Should correctly set and get the directory.
        @Test
        @DisplayName("Should set and get directory")
        void setGetDirectory() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            Path dir = tempDir.resolve("fxml");
            fxmlDirectory.setDirectory(dir);
            assertThat(fxmlDirectory.getDirectory()).isEqualTo(dir);
        }

        /// Should correctly set and get the package name.
        @Test
        @DisplayName("Should set and get package name")
        void setGetPackageName() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            fxmlDirectory.setPackageName("com.test");
            assertThat(fxmlDirectory.getPackageName()).isEqualTo("com.test");
        }

        /// Should correctly set and get excluded files.
        @Test
        @DisplayName("Should set and get excluded files")
        void setGetExcludedFiles() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            List<Path> excluded = List.of(Path.of("File.fxml"));
            fxmlDirectory.setExcludedFiles(excluded);
            assertThat(fxmlDirectory.getExcludedFiles()).isEqualTo(excluded);
        }

        /// Should handle null when setting excluded files by using an empty list.
        @Test
        @DisplayName("Should handle null in setExcludedFiles")
        void setExcludedFilesNull() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            fxmlDirectory.setExcludedFiles(null);
            assertThat(fxmlDirectory.getExcludedFiles()).isNotNull().isEmpty();
        }
    }

    /// Tests for the validate method.
    @Nested
    @DisplayName("Validate Method Tests")
    class ValidateTest {
        /// Should throw [NullPointerException] if the directory is null when validating.
        @Test
        @DisplayName("Should throw NPE if directory is null during validation")
        void validateNullDir() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            assertThatThrownBy(fxmlDirectory::validate)
                .isInstanceOf(NullPointerException.class);
        }

        /// Should resolve directory and excluded files to absolute paths during validation.
        @Test
        @DisplayName("Should resolve paths to absolute during validation")
        void validateResolution() {
            Path relativeDir = Path.of("src/main/resources/fxml");
            Path excludedFile = Path.of("Excluded.fxml");
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            fxmlDirectory.setDirectory(relativeDir);
            fxmlDirectory.setExcludedFiles(List.of(excludedFile));

            fxmlDirectory.validate();

            assertThat(fxmlDirectory.getDirectory().isAbsolute()).isTrue();
            assertThat(fxmlDirectory.getDirectory()).isEqualTo(relativeDir.toAbsolutePath());

            List<Path> resolvedExcluded = fxmlDirectory.getExcludedFiles();
            assertThat(resolvedExcluded).hasSize(1);
            Path expectedPath = relativeDir.toAbsolutePath().resolve(excludedFile).toAbsolutePath();
            assertThat(resolvedExcluded.getFirst())
                    .isEqualTo(expectedPath)
                    .extracting(Path::isAbsolute, InstanceOfAssertFactories.BOOLEAN)
                    .isTrue();
        }

        /// Should handle null or empty excluded files list during validation.
        @Test
        @DisplayName("Should handle null or empty excludedFiles during validation")
        void validateNullExcluded() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            fxmlDirectory.setDirectory(tempDir);
            // Case 1: null excludedFiles (from default constructor)
            fxmlDirectory.validate();
            assertThat(fxmlDirectory.getExcludedFiles()).isNotNull().isEmpty();

            // Case 2: empty list
            fxmlDirectory.setExcludedFiles(List.of());
            fxmlDirectory.validate();
            assertThat(fxmlDirectory.getExcludedFiles()).isEmpty();
        }

        /// Should filter out null elements from the excluded files list during validation.
        @Test
        @DisplayName("Should filter nulls from excludedFiles during validation")
        void validateFiltersNulls() {
            FXMLDirectory fxmlDirectory = new FXMLDirectory();
            fxmlDirectory.setDirectory(tempDir);
            // We need a list that allows nulls
            java.util.List<Path> excludedWithNull = new java.util.ArrayList<>();
            excludedWithNull.add(Path.of("Valid.fxml"));
            excludedWithNull.add(null);
            
            // FXMLDirectory.setExcludedFiles uses List.copyOf which doesn't allow nulls
            // but the validate method uses Stream.ofNullable(excludedFiles).flatMap(List::stream).filter(Objects::nonNull)
            // So if someone managed to get a null in there (e.g. through reflection or if setExcludedFiles didn't use List.copyOf), 
            // validate would handle it.
            // Let's use reflection to set a list with a null to test this branch.
            try {
                java.lang.reflect.Field field = FXMLDirectory.class.getDeclaredField("excludedFiles");
                field.setAccessible(true);
                field.set(fxmlDirectory, excludedWithNull);
            } catch (Exception e) {
                fail("Failed to set excludedFiles via reflection: " + e.getMessage());
            }

            fxmlDirectory.validate();
            assertThat(fxmlDirectory.getExcludedFiles())
                    .hasSize(1)
                    .first()
                    .isEqualTo(tempDir.toAbsolutePath().resolve("Valid.fxml").toAbsolutePath());
        }
    }
}
