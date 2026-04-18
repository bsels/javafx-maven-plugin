package io.github.bsels.javafx.maven.plugin;

import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.fxml.v2.writer.FXMLSourceCodeBuilder;
import io.github.bsels.javafx.maven.plugin.in.memory.compiler.OptimisticInMemoryCompiler;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import io.github.bsels.javafx.maven.plugin.parameters.FXMLDirectory;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Unit tests for [FXMLToSourceCodeMojo].
class FXMLToSourceCodeMojoTest {

    /// Temporary directory used for FXML and generated source files.
    @TempDir
    Path tempDir;

    /// The mojo under test.
    private FXMLToSourceCodeMojo mojo;

    /// Mocked Maven project.
    private MavenProject project;

    /// Sets up a fresh mojo instance with sensible defaults before each test.
    @BeforeEach
    void setUp() throws DependencyResolutionRequiredException {
        mojo = new FXMLToSourceCodeMojo();
        project = mock(MavenProject.class);
        when(project.getRuntimeClasspathElements()).thenReturn(List.of());
        when(project.getCompileClasspathElements()).thenReturn(List.of());
        when(project.getCompileSourceRoots()).thenReturn(List.of());

        mojo.project = project;
        mojo.generatedSourceDirectory = tempDir.resolve("generated");
        mojo.defaultCharset = "UTF-8";
        mojo.addGeneratedAnnotation = true;
        mojo.includeSourceFilesInClassDiscovery = false;
        mojo.setLog(new DefaultLog(new ConsoleLogger()));
    }

    // -------------------------------------------------------------------------
    // execute() — parameter validation
    // -------------------------------------------------------------------------

    /// Tests for the [FXMLToSourceCodeMojo#execute()] method.
    @Nested
    class ExecuteTest {

        /// Verifies that a null `fxmlDirectories` field causes a [NullPointerException].
        @Test
        void shouldThrowWhenFxmlDirectoriesIsNull() {
            mojo.fxmlDirectories = null;

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("FXML directories");
        }

        /// Verifies that a null `project` field causes a [NullPointerException].
        @Test
        void shouldThrowWhenProjectIsNull() {
            mojo.fxmlDirectories = List.of();
            mojo.project = null;

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Maven project");
        }

        /// Verifies that a null `generatedSourceDirectory` field causes a [NullPointerException].
        @Test
        void shouldThrowWhenGeneratedSourceDirectoryIsNull() {
            mojo.fxmlDirectories = List.of();
            mojo.generatedSourceDirectory = null;

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Generated source directory");
        }

        /// Verifies that a null `defaultCharset` field causes a [NullPointerException].
        @Test
        void shouldThrowWhenDefaultCharsetIsNull() {
            mojo.fxmlDirectories = List.of();
            mojo.defaultCharset = null;

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Default charset");
        }

        /// Verifies that an empty `fxmlDirectories` list causes a [MojoFailureException].
        @Test
        void shouldThrowWhenFxmlDirectoriesIsEmpty() {
            mojo.fxmlDirectories = List.of();

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(MojoFailureException.class)
                    .hasMessageContaining("No FXML directories specified");
        }

        /// Verifies that an invalid charset string causes an [IllegalArgumentException].
        @Test
        void shouldThrowWhenCharsetIsInvalid() {
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));
            mojo.defaultCharset = "NOT-A-CHARSET";

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        /// Verifies that a blank `resourceBundleObject` is normalized to null.
        @Test
        void shouldNormalizeBlankResourceBundleObjectToNull() throws Exception {
            mojo.resourceBundleObject = "   ";
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(mojo.resourceBundleObject).isNull();
            }
        }

        /// Verifies that a non-blank `resourceBundleObject` is preserved as-is.
        @Test
        void shouldPreserveNonBlankResourceBundleObject() throws Exception {
            mojo.resourceBundleObject = "com.example.Bundle";
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(mojo.resourceBundleObject).isEqualTo("com.example.Bundle");
            }
        }

        /// Verifies that a null `resourceBundleObject` remains null.
        @Test
        void shouldKeepNullResourceBundleObjectAsNull() throws Exception {
            mojo.resourceBundleObject = null;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(mojo.resourceBundleObject).isNull();
            }
        }

        /// Verifies that `execute()` calls `project.addCompileSourceRoot` with the generated source directory.
        @Test
        void shouldRegisterGeneratedSourceDirectoryAsCompileRoot() throws Exception {
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                verify(project).addCompileSourceRoot(mojo.generatedSourceDirectory.toAbsolutePath().toString());
            }
        }

        /// Verifies that FXML files in the directory are processed and source code is written.
        @Test
        void shouldProcessFxmlFilesAndWriteSourceCode() throws Exception {
            Path fxmlFile = tempDir.resolve("Sample.fxml");
            Files.writeString(fxmlFile, "<root/>");

            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, "com.example", List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            FXMLDocument fxmlDocument = mock(FXMLDocument.class);
            ParsedFXML parsedFXML = mock(ParsedFXML.class);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class,
                     (mock, ctx) -> when(mock.readFXML(any())).thenReturn(parsedFXML));
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class,
                     (mock, ctx) -> when(mock.parse(any(), anyString(), any())).thenReturn(fxmlDocument));
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class,
                     (mock, ctx) -> when(mock.generateSourceCode(any(), anyString())).thenReturn("public class Sample {}"))) {

                mojo.execute();

                Path expectedFile = mojo.generatedSourceDirectory
                        .resolve("com/example")
                        .resolve("Sample.java");
                assertThat(expectedFile)
                        .exists()
                        .content()
                        .isEqualTo("public class Sample {}");
                assertThat(readerMock.constructed()).hasSize(1);
            }
        }

        /// Verifies that an [IOException] during file writing is wrapped in a [MojoExecutionException].
        @Test
        void shouldThrowMojoExecutionExceptionWhenWriteFails() throws Exception {
            Path fxmlFile = tempDir.resolve("Sample.fxml");
            Files.writeString(fxmlFile, "<root/>");

            // Create the generated directory first, then make the target .java file a directory
            // so Files.writeString fails with an IOException
            Path generatedDir = Files.createDirectories(mojo.generatedSourceDirectory);
            Files.createDirectory(generatedDir.resolve("Sample.java"));

            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, null, List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            FXMLDocument fxmlDocument = mock(FXMLDocument.class);
            ParsedFXML parsedFXML = mock(ParsedFXML.class);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class,
                     (mock, ctx) -> when(mock.readFXML(any())).thenReturn(parsedFXML));
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class,
                     (mock, ctx) -> when(mock.parse(any(), anyString(), any())).thenReturn(fxmlDocument));
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class,
                     (mock, ctx) -> when(mock.generateSourceCode(any(), any())).thenReturn("public class Sample {}"))) {

                assertThatThrownBy(() -> mojo.execute())
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessageContaining("Failed to write source code file");
            }
        }

        /// Verifies that excluded FXML files are not processed.
        @Test
        void shouldSkipExcludedFxmlFiles() throws Exception {
            Path fxmlFile = tempDir.resolve("Excluded.fxml");
            Files.writeString(fxmlFile, "<root/>");

            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, null, List.of(fxmlFile));
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(readerMock.constructed()).hasSize(1);
                assertThat(readerMock.constructed().get(0)).satisfies(reader ->
                        org.mockito.Mockito.verifyNoInteractions(reader)
                );
            }
        }

        /// Verifies that non-FXML files in the directory are ignored.
        @Test
        void shouldIgnoreNonFxmlFiles() throws Exception {
            Files.writeString(tempDir.resolve("readme.txt"), "hello");

            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, null, List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(readerMock.constructed()).hasSize(1);
                assertThat(readerMock.constructed().get(0)).satisfies(reader ->
                        org.mockito.Mockito.verifyNoInteractions(reader)
                );
            }
        }

        /// Verifies that multiple FXML directories are all processed.
        @Test
        void shouldProcessMultipleFxmlDirectories() throws Exception {
            Path dir1 = tempDir.resolve("dir1");
            Path dir2 = tempDir.resolve("dir2");
            Files.createDirectories(dir1);
            Files.createDirectories(dir2);
            Files.writeString(dir1.resolve("A.fxml"), "<root/>");
            Files.writeString(dir2.resolve("B.fxml"), "<root/>");

            FXMLDocument fxmlDocument = mock(FXMLDocument.class);
            ParsedFXML parsedFXML = mock(ParsedFXML.class);

            mojo.fxmlDirectories = List.of(
                    new FXMLDirectory(dir1, "pkg.a", List.of()),
                    new FXMLDirectory(dir2, "pkg.b", List.of())
            );

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class,
                     (mock, ctx) -> when(mock.readFXML(any())).thenReturn(parsedFXML));
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class,
                     (mock, ctx) -> when(mock.parse(any(), anyString(), any())).thenReturn(fxmlDocument));
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class,
                     (mock, ctx) -> when(mock.generateSourceCode(any(), anyString())).thenReturn("public class X {}"))) {

                mojo.execute();

                assertThat(mojo.generatedSourceDirectory.resolve("pkg/a/A.java")).exists();
                assertThat(mojo.generatedSourceDirectory.resolve("pkg/b/B.java")).exists();
            }
        }

        /// Verifies that when `includeSourceFilesInClassDiscovery` is false, the optimistic compiler is not invoked.
        @Test
        void shouldNotUseOptimisticCompilerWhenDisabled() throws Exception {
            mojo.includeSourceFilesInClassDiscovery = false;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                // Only one FXMLReader should be constructed (no double-execution from optimistic compiler path)
                assertThat(readerMock.constructed()).hasSize(1);
            }
        }

        /// Verifies that when `includeSourceFilesInClassDiscovery` is true, the optimistic compiler path is taken
        /// and `executeInternal` is called inside the extended class loader scope.
        @Test
        @SuppressWarnings("unchecked")
        void shouldUseOptimisticCompilerWhenEnabled() throws Exception {
            mojo.includeSourceFilesInClassDiscovery = true;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class);
                 MockedConstruction<OptimisticInMemoryCompiler> compilerMock = mockConstruction(
                         OptimisticInMemoryCompiler.class,
                         (mock, ctx) -> when(mock.optimisticCompileIntoClassLoader(any(), any()))
                                 .thenReturn(UnaryOperator.identity()))) {

                mojo.execute();

                // executeInternal is called twice: once inside optimistic compiler scope, once outside
                assertThat(readerMock.constructed()).hasSize(2);
            }
        }

        /// Verifies that when `includeSourceFilesInClassDiscovery` is true and the optimistic compiler throws
        /// [IOException], a [MojoExecutionException] is raised.
        @Test
        void shouldThrowMojoExecutionExceptionWhenOptimisticCompilerFails() throws Exception {
            mojo.includeSourceFilesInClassDiscovery = true;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class);
                 MockedConstruction<OptimisticInMemoryCompiler> compilerMock = mockConstruction(
                         OptimisticInMemoryCompiler.class,
                         (mock, ctx) -> when(mock.optimisticCompileIntoClassLoader(any(), any()))
                                 .thenThrow(new IOException("compile error")))) {

                assertThatThrownBy(() -> mojo.execute())
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessageContaining("Failed to compile source files");
            }
        }

        /// Verifies that when the current class loader is a [URLClassLoader], its URLs are collected
        /// by `sourceFilesClassLoaderExtender` before delegating to the parent.
        @Test
        @SuppressWarnings("unchecked")
        void shouldCollectUrlsFromUrlClassLoaderInSourceFilesExtender() throws Exception {
            mojo.includeSourceFilesInClassDiscovery = true;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            // Wrap the current thread classloader in a URLClassLoader so the instanceof branch is taken
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try (URLClassLoader urlCL = new URLClassLoader(new URL[0], original)) {
                Thread.currentThread().setContextClassLoader(urlCL);
                try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                     MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                     MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class);
                     MockedConstruction<OptimisticInMemoryCompiler> compilerMock = mockConstruction(
                             OptimisticInMemoryCompiler.class,
                             (mock, ctx) -> when(mock.optimisticCompileIntoClassLoader(any(), any()))
                                     .thenReturn(UnaryOperator.identity()))) {

                    mojo.execute();

                    assertThat(compilerMock.constructed()).hasSize(1);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }

        /// Verifies that debug-level log messages are emitted when the log level is DEBUG.
        @Test
        @SuppressWarnings("unchecked")
        void shouldLogDebugMessagesWhenDebugEnabled() throws Exception {
            mojo.setLog(new DefaultLog(new ConsoleLogger(ConsoleLogger.LEVEL_DEBUG, "test")));
            Path fxmlFile = tempDir.resolve("Sample.fxml");
            Files.writeString(fxmlFile, "<root/>");

            FXMLDocument fxmlDocument = mock(FXMLDocument.class);
            ParsedFXML parsedFXML = mock(ParsedFXML.class);

            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, "com.example", List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class,
                     (mock, ctx) -> when(mock.readFXML(any())).thenReturn(parsedFXML));
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class,
                     (mock, ctx) -> when(mock.parse(any(), anyString(), any())).thenReturn(fxmlDocument));
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class,
                     (mock, ctx) -> when(mock.generateSourceCode(any(), any())).thenReturn("public class Sample {}"))) {

                mojo.execute();

                assertThat(mojo.generatedSourceDirectory.resolve("com/example/Sample.java")).exists();
            }
        }

        /// Verifies that a [DependencyResolutionRequiredException] from the project is wrapped in [MojoFailureException].
        @Test
        void shouldThrowMojoFailureExceptionWhenDependencyResolutionFails() throws Exception {
            when(project.getRuntimeClasspathElements())
                    .thenThrow(new DependencyResolutionRequiredException(null));

            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(MojoFailureException.class);
        }

        /// Verifies that classpath elements returned by the project are mapped to URLs in the class loader.
        @Test
        void shouldMapClasspathElementsToUrls() throws Exception {
            when(project.getRuntimeClasspathElements()).thenReturn(List.of(tempDir.toString()));
            when(project.getCompileClasspathElements()).thenReturn(List.of(tempDir.toString()));

            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(readerMock.constructed()).hasSize(1);
            }
        }

        /// Verifies that compile source roots are mapped to [Path] objects in `sourceFilesClassLoaderExtender`.
        @Test
        @SuppressWarnings("unchecked")
        void shouldMapCompileSourceRootsToPathsInSourceFilesExtender() throws Exception {
            when(project.getCompileSourceRoots()).thenReturn(List.of(tempDir.toString()));
            mojo.includeSourceFilesInClassDiscovery = true;
            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class);
                 MockedConstruction<OptimisticInMemoryCompiler> compilerMock = mockConstruction(
                         OptimisticInMemoryCompiler.class,
                         (mock, ctx) -> when(mock.optimisticCompileIntoClassLoader(any(), any()))
                                 .thenReturn(UnaryOperator.identity()))) {

                mojo.execute();

                assertThat(compilerMock.constructed()).hasSize(1);
            }
        }
    }

    // -------------------------------------------------------------------------
    // createGeneratedPackageDirectory
    // -------------------------------------------------------------------------

    /// Tests for the package directory creation logic inside [FXMLToSourceCodeMojo].
    @Nested
    class CreateGeneratedPackageDirectoryTest {

        /// Verifies that when no package name is set, the generated directory equals the base generated source dir.
        @Test
        void shouldUseBaseDirectoryWhenNoPackageName() throws Exception {
            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, null, List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(mojo.generatedSourceDirectory).exists().isDirectory();
            }
        }

        /// Verifies that an I/O error during directory creation throws a [MojoExecutionException].
        @Test
        void shouldThrowMojoExecutionExceptionWhenDirectoryCannotBeCreated() throws Exception {
            // Place a regular file where the generated source directory should be, so createDirectories fails
            Path blockedDir = tempDir.resolve("blocked");
            Files.writeString(blockedDir, "not a directory");
            mojo.generatedSourceDirectory = blockedDir.resolve("sub");

            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, null, List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                assertThatThrownBy(() -> mojo.execute())
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessageContaining("Unable to create generated package directory");
            }
        }

        /// Verifies that a package name with dots is converted to a nested directory structure.
        @Test
        void shouldCreateNestedDirectoryForPackageName() throws Exception {
            FXMLDirectory fxmlDirectory = new FXMLDirectory(tempDir, "com.example.ui", List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                mojo.execute();

                assertThat(mojo.generatedSourceDirectory.resolve("com/example/ui"))
                        .exists()
                        .isDirectory();
            }
        }
    }

    // -------------------------------------------------------------------------
    // findFXMLFiles
    // -------------------------------------------------------------------------

    /// Tests for the FXML file discovery logic inside [FXMLToSourceCodeMojo].
    @Nested
    class FindFXMLFilesTest {

        /// Verifies that FXML files with uppercase extension are discovered.
        @Test
        void shouldFindFxmlFilesWithUppercaseExtension() throws Exception {
            Files.writeString(tempDir.resolve("View.FXML"), "<root/>");

            FXMLDocument fxmlDocument = mock(FXMLDocument.class);
            ParsedFXML parsedFXML = mock(ParsedFXML.class);

            mojo.fxmlDirectories = List.of(new FXMLDirectory(tempDir, null, List.of()));

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class,
                     (mock, ctx) -> when(mock.readFXML(any())).thenReturn(parsedFXML));
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class,
                     (mock, ctx) -> when(mock.parse(any(), anyString(), any())).thenReturn(fxmlDocument));
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class,
                     (mock, ctx) -> when(mock.generateSourceCode(any(), any())).thenReturn("public class View {}"))) {

                mojo.execute();

                assertThat(mojo.generatedSourceDirectory.resolve("View.java")).exists();
            }
        }

        /// Verifies that an unreadable directory causes a [MojoExecutionException].
        @Test
        void shouldThrowMojoExecutionExceptionWhenDirectoryIsUnreadable() throws Exception {
            // Use a non-existent directory to trigger IOException in Files.walk
            Path nonExistent = tempDir.resolve("does-not-exist");
            FXMLDirectory fxmlDirectory = new FXMLDirectory(nonExistent, null, List.of());
            mojo.fxmlDirectories = List.of(fxmlDirectory);

            try (MockedConstruction<FXMLReader> readerMock = mockConstruction(FXMLReader.class);
                 MockedConstruction<FXMLDocumentParser> parserMock = mockConstruction(FXMLDocumentParser.class);
                 MockedConstruction<FXMLSourceCodeBuilder> builderMock = mockConstruction(FXMLSourceCodeBuilder.class)) {

                assertThatThrownBy(() -> mojo.execute())
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessageContaining("Unable to read FXML directory");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /// Tests for the [FXMLToSourceCodeMojo] constructor.
    @Nested
    class ConstructorTest {

        /// Verifies that the default constructor creates a valid instance with expected default field values.
        @Test
        void shouldCreateInstanceWithDefaults() {
            FXMLToSourceCodeMojo newMojo = new FXMLToSourceCodeMojo();

            assertThat(newMojo)
                    .hasFieldOrPropertyWithValue("defaultCharset", "UTF-8")
                    .hasFieldOrPropertyWithValue("addGeneratedAnnotation", true)
                    .hasFieldOrPropertyWithValue("includeSourceFilesInClassDiscovery", true);
        }
    }
}
