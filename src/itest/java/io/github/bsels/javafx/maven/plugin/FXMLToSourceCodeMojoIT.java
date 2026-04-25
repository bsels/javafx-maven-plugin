package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.parameters.FXMLDirectory;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/// Integration tests for [FXMLToSourceCodeMojo].
///
/// Each nested class covers a distinct scenario of the mojo's `execute()` flow,
/// directly instantiating the mojo and setting its fields to simulate Maven plugin execution.
@ExtendWith(MockitoExtension.class)
class FXMLToSourceCodeMojoIT {

    @TempDir
    Path outputDir;

    @Mock
    MavenProject project;

    FXMLToSourceCodeMojo mojo;

    /// Returns the path to a named FXML resource in the itest resources directory.
    ///
    /// @param name the FXML file name (without path prefix)
    /// @return the resolved [Path] to the resource
    private Path fxmlResource(String name) throws URISyntaxException {
        return Path.of(getClass().getClassLoader().getResource("fxml/" + name).toURI());
    }

    /// Copies a single FXML resource file into a fresh temporary directory and returns that directory.
    ///
    /// This ensures each test processes only the intended FXML file.
    ///
    /// @param tempDir the temporary directory to copy the file into
    /// @param name    the FXML file name (without path prefix)
    /// @return the [Path] to the temporary directory containing the copied file
    private Path singleFxmlDir(Path tempDir, String name) throws Exception {
        Files.createDirectories(tempDir);
        Path source = fxmlResource(name);
        Files.copy(source, tempDir.resolve(name));
        return tempDir;
    }

    /// Creates a configured [FXMLDirectory] pointing to the given directory with the given package name.
    ///
    /// @param directory   the directory containing FXML files
    /// @param packageName the Java package name for generated classes
    /// @return a configured [FXMLDirectory]
    private FXMLDirectory fxmlDirectory(Path directory, String packageName) {
        FXMLDirectory dir = new FXMLDirectory();
        dir.setDirectory(directory);
        dir.setPackageName(packageName);
        return dir;
    }

    /// Sets up a fresh [FXMLToSourceCodeMojo] instance before each test with common defaults.
    ///
    /// Classpath element stubs are registered as lenient to avoid UnnecessaryStubbingException
    /// in validation tests that fail before the classpath is needed.
    @BeforeEach
    void setUp() throws Exception {
        mojo = new FXMLToSourceCodeMojo();
        mojo.setLog(new DefaultLog(new ConsoleLogger()));
        mojo.project = project;
        mojo.generatedSourceDirectory = outputDir;
        mojo.defaultCharset = "UTF-8";
        mojo.resourceBundleObject = "bundles.Resources";
        mojo.addGeneratedAnnotation = false;
        mojo.includeSourceFilesInClassDiscovery = false;

        List<String> classpathElements = List.of(
                Path.of(System.getProperty("java.class.path").split(System.getProperty("path.separator"))[0]).toString()
        );
        lenient().when(project.getRuntimeClasspathElements()).thenReturn(classpathElements);
        lenient().when(project.getCompileClasspathElements()).thenReturn(classpathElements);
    }

    /// Tests for generating source code from a simple Button FXML with no controller or action.
    @Nested
    class SimpleButtonTest {

        /// Verifies that a simple Button FXML generates a Java class extending Button
        /// with the correct package, class name, and setText call.
        @Test
        void generatesJavaFileWithCorrectContent() throws Exception {
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            Path generated = outputDir.resolve("io/github/bsels/itest/fxml/SimpleButton.java");
            assertThat(generated).exists();

            String content = Files.readString(generated);
            assertThat(content)
                    .contains("package io.github.bsels.itest.fxml;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("class SimpleButton extends Button")
                    .contains("setText(\"Click me\")");
        }

        /// Verifies that the generated source directory is registered as a compile source root.
        @Test
        void registersGeneratedSourceDirectoryAsCompileRoot() throws Exception {
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            verify(project).addCompileSourceRoot(outputDir.toAbsolutePath().toString());
        }
    }

    /// Tests for generating source code from a Button FXML with an onAction handler but no controller.
    @Nested
    class ActionButtonTest {

        /// Verifies that a Button FXML with onAction but no controller generates an abstract class
        /// with an abstract handler method and a setOnAction call using a method reference.
        @Test
        void generatesAbstractClassWithActionHandler() throws Exception {
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "ActionButton.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            Path generated = outputDir.resolve("io/github/bsels/itest/fxml/ActionButton.java");
            assertThat(generated).exists();

            String content = Files.readString(generated);
            assertThat(content)
                    .contains("package io.github.bsels.itest.fxml;")
                    .contains("import javafx.scene.control.Button;")
                    .contains("public abstract class ActionButton extends Button")
                    .contains("handleAction")
                    .contains("setOnAction")
                    .contains("setText(\"Do Action\")");
        }
    }

    /// Tests for generating source code from an FXML with fx:define color definitions.
    @Nested
    class ColorDefinitionsTest {

        /// Verifies that an FXML with fx:define Color entries generates a class with Color fields
        /// and constructor assignments for each defined color.
        @Test
        void generatesClassWithColorFields() throws Exception {
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "ColorDefinitions.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            Path generated = outputDir.resolve("io/github/bsels/itest/fxml/ColorDefinitions.java");
            assertThat(generated).exists();

            String content = Files.readString(generated);
            assertThat(content)
                    .contains("package io.github.bsels.itest.fxml;")
                    .contains("import javafx.scene.paint.Color;")
                    .contains("class ColorDefinitions")
                    .contains("Color primary")
                    .contains("Color secondary");
        }
    }

    /// Tests for mojo validation failures due to missing or invalid configuration.
    @Nested
    class ValidationTest {

        /// Verifies that a null fxmlDirectories parameter causes a NullPointerException.
        @Test
        void nullFxmlDirectoriesThrowsNullPointerException() {
            mojo.fxmlDirectories = null;

            assertThatNullPointerException()
                    .isThrownBy(() -> mojo.execute())
                    .withMessage("FXML directories must be specified");
        }

        /// Verifies that an empty fxmlDirectories list causes a MojoFailureException.
        @Test
        void emptyFxmlDirectoriesThrowsMojoFailureException() {
            mojo.fxmlDirectories = List.of();

            assertThatExceptionOfType(MojoFailureException.class)
                    .isThrownBy(() -> mojo.execute())
                    .withMessage("No FXML directories specified");
        }

        /// Verifies that a null project parameter causes a NullPointerException.
        @Test
        void nullProjectThrowsNullPointerException() throws Exception {
            mojo.project = null;
            mojo.fxmlDirectories = List.of(fxmlDirectory(
                    singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml"),
                    "io.github.bsels.itest.fxml"
            ));

            assertThatNullPointerException()
                    .isThrownBy(() -> mojo.execute())
                    .withMessage("Maven project must be specified");
        }

        /// Verifies that a null generatedSourceDirectory causes a NullPointerException.
        @Test
        void nullGeneratedSourceDirectoryThrowsNullPointerException() throws Exception {
            mojo.generatedSourceDirectory = null;
            mojo.fxmlDirectories = List.of(fxmlDirectory(
                    singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml"),
                    "io.github.bsels.itest.fxml"
            ));

            assertThatNullPointerException()
                    .isThrownBy(() -> mojo.execute())
                    .withMessage("Generated source directory must be specified");
        }

        /// Verifies that a null defaultCharset causes a NullPointerException.
        @Test
        void nullDefaultCharsetThrowsNullPointerException() throws Exception {
            mojo.defaultCharset = null;
            mojo.fxmlDirectories = List.of(fxmlDirectory(
                    singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml"),
                    "io.github.bsels.itest.fxml"
            ));

            assertThatNullPointerException()
                    .isThrownBy(() -> mojo.execute())
                    .withMessage("Default charset must be specified");
        }
    }

    /// Tests for the addGeneratedAnnotation configuration option.
    @Nested
    class GeneratedAnnotationTest {

        /// Verifies that when addGeneratedAnnotation is true, the generated class contains the @Generated annotation.
        @Test
        void withGeneratedAnnotationEnabled() throws Exception {
            mojo.addGeneratedAnnotation = true;
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            Path generated = outputDir.resolve("io/github/bsels/itest/fxml/SimpleButton.java");
            String content = Files.readString(generated);
            assertThat(content).contains("@Generated");
        }

        /// Verifies that when addGeneratedAnnotation is false, the generated class does not contain the @Generated annotation.
        @Test
        void withGeneratedAnnotationDisabled() throws Exception {
            mojo.addGeneratedAnnotation = false;
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml");
            mojo.fxmlDirectories = List.of(fxmlDirectory(fxmlDir, "io.github.bsels.itest.fxml"));

            mojo.execute();

            Path generated = outputDir.resolve("io/github/bsels/itest/fxml/SimpleButton.java");
            String content = Files.readString(generated);
            assertThat(content).doesNotContain("@Generated");
        }
    }

    /// Tests for the default package (no packageName) configuration.
    @Nested
    class DefaultPackageTest {

        /// Verifies that when no packageName is set, the generated file is placed directly
        /// in the generatedSourceDirectory with no package declaration.
        @Test
        void generatesFileInDefaultPackageWhenNoPackageNameSet() throws Exception {
            Path fxmlDir = singleFxmlDir(outputDir.resolve("fxml"), "SimpleButton.fxml");
            FXMLDirectory dir = new FXMLDirectory();
            dir.setDirectory(fxmlDir);
            mojo.fxmlDirectories = List.of(dir);

            mojo.execute();

            Path generated = outputDir.resolve("SimpleButton.java");
            assertThat(generated).exists();

            String content = Files.readString(generated);
            assertThat(content).doesNotContain("package ");
        }
    }
}
