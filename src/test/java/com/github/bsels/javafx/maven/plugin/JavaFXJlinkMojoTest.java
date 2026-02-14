package com.github.bsels.javafx.maven.plugin;

import com.github.bsels.javafx.maven.plugin.parameters.AdditionalBinary;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static com.github.bsels.javafx.maven.plugin.BaseJavaFXMojo.ENABLE_NATIVE_ACCESS_JAVAFX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class JavaFXJlinkMojoTest {

    @Mock
    LocationManager locationManager;

    @Mock
    ToolchainManager toolchainManager;

    @Mock
    Archiver mockZipArchiver;

    @Mock
    MavenProject mockProject;

    @Mock
    Artifact artifactMock;

    private JavaFXJlinkMojo classUnderTest;
    private Method copyAdditionalBinariesToBinaryFolderMethod;
    private Method createApplicationZipArchiveMethod;
    private Method zipApplicationMethod;
    private Method handleJvmOptionMethod;
    private Method handleCommandLineArgsMethod;
    private Method patchLauncherScriptMethod;
    private Method patchLauncherScriptsMethod;
    private Method cleanupOutputDirectoryMethod;
    private Method getJLinkCommandMethod;
    private Method patchLoggingFormatMethod;

    @Mock
    private Process mockProcess;

    private String originalJavaHome;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        originalJavaHome = System.getProperty("java.home");
        classUnderTest = new JavaFXJlinkMojo(locationManager, toolchainManager, mockZipArchiver);
        classUnderTest.project = mockProject;
        copyAdditionalBinariesToBinaryFolderMethod = JavaFXJlinkMojo.class.getDeclaredMethod("copyAdditionalBinariesToBinaryFolder");
        copyAdditionalBinariesToBinaryFolderMethod.setAccessible(true);
        createApplicationZipArchiveMethod = JavaFXJlinkMojo.class.getDeclaredMethod("createApplicationZipArchive");
        createApplicationZipArchiveMethod.setAccessible(true);
        zipApplicationMethod = JavaFXJlinkMojo.class.getDeclaredMethod("zipApplication");
        zipApplicationMethod.setAccessible(true);
        handleJvmOptionMethod = JavaFXJlinkMojo.class.getDeclaredMethod("handleJvmOption", String.class, String.class);
        handleJvmOptionMethod.setAccessible(true);
        handleCommandLineArgsMethod = JavaFXJlinkMojo.class.getDeclaredMethod("handleCommandLineArgs", String.class);
        handleCommandLineArgsMethod.setAccessible(true);
        patchLauncherScriptMethod = JavaFXJlinkMojo.class.getDeclaredMethod("patchLauncherScript", String.class);
        patchLauncherScriptMethod.setAccessible(true);
        patchLauncherScriptsMethod = JavaFXJlinkMojo.class.getDeclaredMethod("patchLauncherScripts");
        patchLauncherScriptsMethod.setAccessible(true);
        cleanupOutputDirectoryMethod = JavaFXJlinkMojo.class.getDeclaredMethod("cleanupOutputDirectory", Path.class);
        cleanupOutputDirectoryMethod.setAccessible(true);
        getJLinkCommandMethod = JavaFXJlinkMojo.class.getDeclaredMethod("getJLinkCommand");
        getJLinkCommandMethod.setAccessible(true);
        patchLoggingFormatMethod = JavaFXJlinkMojo.class.getDeclaredMethod("patchLoggingFormat");
        patchLoggingFormatMethod.setAccessible(true);
    }

    @AfterEach
    void restoreJavaHome() {
        if (originalJavaHome == null) {
            System.clearProperty("java.home");
        } else {
            System.setProperty("java.home", originalJavaHome);
        }
    }

    private void mockStatic(MockedStatic<Files> files, MockedStatic<Os> osMock) {
        files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
        Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                .thenReturn(null);

        // Niet-Windows zodat PATH pad niet gebruikt wordt
        System.clearProperty("java.home");
        osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(false);
    }

    private JavaModuleDescriptor createMockModuleDescriptor(String moduleName) {
        // This would need to be implemented based on the actual ModuleDescriptor type
        // For now, using a mock or stub that provides the name() method
        return JavaModuleDescriptor.newModule(moduleName).build();
    }

    private static void deleteFolder(Path temporaryDirectory) {
        try (Stream<Path> pathStream = Files.walk(temporaryDirectory)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException _) {
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class CreateApplicationZipArchiveTest {

        /**
         * Test for the createApplicationZipArchive method in the JavaFXJlinkMojo class.
         * Verifies that the method properly interacts with ZipArchiver to create a zip archive of the application.
         */
        @Test
        void testCreateApplicationZipArchive_Success() throws Exception {
            // Arrange
            Path buildDirectory = Paths.get("target");
            String jlinkImageName = "image";
            String jlinkZipName = "app-archive";

            classUnderTest.jlinkImageName = jlinkImageName;
            classUnderTest.jlinkZipName = jlinkZipName;
            classUnderTest.buildDirectory = buildDirectory;

            Path imageDirectory = buildDirectory.resolve(jlinkImageName);
            Path expectedArchive = buildDirectory.resolve(jlinkZipName + ".zip");

            // Act
            Path result = (Path) createApplicationZipArchiveMethod.invoke(classUnderTest);

            // Assert
            assertThat(result)
                    .isEqualTo(expectedArchive);

            verify(mockZipArchiver).addFileSet(Mockito.argThat(fileSet ->
                    fileSet instanceof DefaultFileSet &&
                            ((DefaultFileSet) fileSet).getDirectory().equals(imageDirectory.toFile())/* &&
                            ((DefaultFileSet) fileSet).isIncludeEmptyDirs()*/
            ));
            verify(mockZipArchiver)
                    .setDestFile(expectedArchive.toFile());
            verify(mockZipArchiver)
                    .createArchive();
            verifyNoMoreInteractions(mockZipArchiver);
        }

        /**
         * Test for the createApplicationZipArchive method in the JavaFXJlinkMojo class.
         * Verifies that the method throws a MojoExecutionException if the zip creation fails.
         */
        @Test
        void testCreateApplicationZipArchive_ZipCreationFailure() throws Exception {
            // Arrange
            Path buildDirectory = Paths.get("target");
            String jlinkImageName = "image";
            String jlinkZipName = "app-archive";

            classUnderTest.jlinkImageName = jlinkImageName;
            classUnderTest.jlinkZipName = jlinkZipName;
            classUnderTest.buildDirectory = buildDirectory;

            doThrow(new IOException("Mocked zip failure")).when(mockZipArchiver).createArchive();

            // Act & Assert
            assertThatThrownBy(() -> createApplicationZipArchiveMethod.invoke(classUnderTest))
                    .isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(MojoExecutionException.class)
                    .cause()
                    .hasMessage("Unable to create ZIP archive");

            verify(mockZipArchiver).addFileSet(Mockito.any(DefaultFileSet.class));
            verify(mockZipArchiver).setDestFile(any(File.class));
            verify(mockZipArchiver).createArchive();
            verifyNoMoreInteractions(mockZipArchiver);
        }
    }

    @Nested
    class ZipApplicationTest {

        @Test
        void testZipApplication_Success() throws Exception {
            // Arrange
            Path buildDirectory = Paths.get("target");
            String jlinkZipName = "app-archive.zip";

            classUnderTest.jlinkImageName = "image";
            classUnderTest.buildDirectory = buildDirectory;
            classUnderTest.jlinkZipName = "app-archive";
            classUnderTest.jlinkZipName = jlinkZipName;

            Path expectedArchive = buildDirectory.resolve(jlinkZipName + ".zip");
            Mockito.when(mockProject.getArtifact())
                    .thenReturn(artifactMock);

            // Act
            zipApplicationMethod.invoke(classUnderTest);

            // Assert
            ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
            verify(artifactMock)
                    .setFile(fileCaptor.capture());
            assertThat(fileCaptor.getValue())
                    .isEqualTo(expectedArchive.toFile());
        }
    }

    @Nested
    class HandleCommandLineArgsTest {

        @Test
        void testCommandLineArgs_AppendsArguments() throws Exception {
            // Arrange
            String input = "java $@";
            classUnderTest.commandlineArgs = "-Dkey1=val1 -Dkey2=val2";

            // Act
            String result = (String) handleCommandLineArgsMethod.invoke(classUnderTest, input);

            // Assert
            assertThat(result).isEqualTo("java -Dkey1=val1 -Dkey2=val2 $@");
        }

        @Test
        void testCommandLineArgs_NoAppendIfNoPlaceholder() throws Exception {
            // Arrange
            String input = "java";
            classUnderTest.commandlineArgs = "-Dkey1=val1 -Dkey2=val2";

            // Act
            String result = (String) handleCommandLineArgsMethod.invoke(classUnderTest, input);

            // Assert
            assertThat(result).isEqualTo("java");
        }

        @Test
        void testCommandLineArgs_MultipleSpacesHandledCorrectly() throws Exception {
            // Arrange
            String input = "java   $@ ";
            classUnderTest.commandlineArgs = "-Xmx1024m -XX:+UseG1GC";

            // Act
            String result = (String) handleCommandLineArgsMethod.invoke(classUnderTest, input);

            // Assert
            assertThat(result).isEqualTo("java   -Xmx1024m -XX:+UseG1GC $@ ");
        }
    }

    @Nested
    class CleanupOutputDirectoryTest {

        @Test
        void testCleanupOutputDirectory_Success() throws Exception {
            // Arrange
            Path temporaryDirectory = Files.createTempDirectory("cleanup-success-test");
            Path file1 = temporaryDirectory.resolve("file1.txt");
            Path file2 = temporaryDirectory.resolve("file2.txt");

            Files.createFile(file1);
            Files.createFile(file2);

            // Act
            cleanupOutputDirectoryMethod.invoke(classUnderTest, temporaryDirectory);

            // Assert
            assertThat(Files.exists(file1)).isFalse();
            assertThat(Files.exists(file2)).isFalse();
            assertThat(Files.exists(temporaryDirectory)).isFalse();
        }

        @Test
        void testCleanupOutputDirectory_IOException() throws Exception {
            // Arrange
            Path temporaryDirectory = Files.createTempDirectory("cleanup-ioexception-test");

            try {
                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.walk(Mockito.any()))
                            .thenThrow(new IOException("Mocked IOException"));

                    // Act & Assert
                    assertThatThrownBy(() -> cleanupOutputDirectoryMethod.invoke(classUnderTest, temporaryDirectory))
                            .isInstanceOf(InvocationTargetException.class)
                            .hasCauseInstanceOf(MojoExecutionException.class)
                            .cause()
                            .hasMessage("Error: Could not remove existing output directory");

                    filesMock.verify(() -> Files.walk(Mockito.any()), Mockito.times(1));
                }
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testCleanupOutputDirectoryFileDeletionFailed_IOException() throws Exception {
            // Arrange
            Path temporaryDirectory = Files.createTempDirectory("cleanup-ioexception-test");
            Path file1 = temporaryDirectory.resolve("file1.txt");
            Path file2 = temporaryDirectory.resolve("file2.txt");

            try {
                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.walk(Mockito.any()))
                            .thenReturn(Stream.of(file1, file2));
                    filesMock.when(() -> Files.deleteIfExists(Mockito.any()))
                            .thenThrow(IOException.class);

                    // Act & Assert
                    assertThatThrownBy(() -> cleanupOutputDirectoryMethod.invoke(classUnderTest, temporaryDirectory))
                            .isInstanceOf(InvocationTargetException.class)
                            .hasCauseInstanceOf(MojoExecutionException.class)
                            .cause()
                            .hasMessage("Error: Could not remove existing output directory");

                    filesMock.verify(() -> Files.walk(Mockito.any()), Mockito.times(1));
                    filesMock.verify(() -> Files.deleteIfExists(Mockito.any()), Mockito.times(1));
                    filesMock.verifyNoMoreInteractions();
                }
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }
    }

    @Nested
    class HandleJvmOptionTest {

        @Test
        void testHandleJvmOption_UnixStyle_Success() throws Exception {
            // Arrange
            String input = "JLINK_VM_OPTIONS=";
            String options = "-Xmx512m -Xms256m";

            // Act
            String result = (String) handleJvmOptionMethod.invoke(classUnderTest, input, options);

            // Assert
            assertThat(result).isEqualTo("JLINK_VM_OPTIONS='-Xmx512m -Xms256m'");
        }

        @Test
        void testHandleJvmOption_WindowsStyle_Success() throws Exception {
            // Arrange
            String input = "set JLINK_VM_OPTIONS=";
            String options = "-Xmx1g -Xms512m";

            // Act
            String result = (String) handleJvmOptionMethod.invoke(classUnderTest, input, options);

            // Assert
            assertThat(result).isEqualTo("set JLINK_VM_OPTIONS=-Xmx1g -Xms512m");
        }

        @Test
        void testHandleJvmOption_InvalidLine_Unchanged() throws Exception {
            // Arrange
            String input = "UNRELATED_OPTION=";
            String options = "-XX:+UseG1GC";

            // Act
            String result = (String) handleJvmOptionMethod.invoke(classUnderTest, input, options);

            // Assert
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    class PatchLauncherScriptTest {

        @Test
        void testPatchLauncherScript_Success() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path scriptPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher");
                Files.createDirectories(scriptPath.getParent());
                Files.writeString(scriptPath, "line1\nJLINK_VM_OPTIONS=\nline3\n");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.options = List.of("-Xmx1024m", "-XX:+UseG1GC");
                classUnderTest.commandlineArgs = "-Dkey=value";

                // Act
                patchLauncherScriptMethod.invoke(classUnderTest, "launcher");

                // Assert
                List<String> modifiedLines = Files.readAllLines(scriptPath);
                assertThat(modifiedLines).containsExactly(
                        "line1",
                        "JLINK_VM_OPTIONS='%s -Xmx1024m -XX:+UseG1GC'".formatted(ENABLE_NATIVE_ACCESS_JAVAFX),
                        "line3"
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testPatchLauncherScript_NoLauncherScript() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";

            // Act
            patchLauncherScriptMethod.invoke(classUnderTest, "nonexistent-launcher");
        }

        @Test
        void testPatchLauncherScript_IOError() throws IOException {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path scriptPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher");
                Files.createDirectories(scriptPath.getParent());
                Files.writeString(scriptPath, "line1\nJLINK_VM_OPTIONS=\nline3\n");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.options = List.of();
                classUnderTest.commandlineArgs = "";

                // Simulate script file being non-writable (e.g., permission error)
                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.exists(Mockito.any()))
                            .thenReturn(true);
                    filesMock.when(() -> Files.lines(Mockito.any()))
                            .thenThrow(IOException.class);

                    // Act & Assert
                    assertThatThrownBy(
                            () -> patchLauncherScriptMethod.invoke(classUnderTest, "launcher")
                    ).isInstanceOf(InvocationTargetException.class)
                            .hasCauseInstanceOf(MojoExecutionException.class)
                            .cause()
                            .hasMessage("Unable to patch launcher script")
                            .hasCauseInstanceOf(IOException.class);
                }
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testPatchLauncherScriptWithAdditionalBinaries_Success() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path scriptPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher");
                Files.createDirectories(scriptPath.getParent());
                Files.writeString(scriptPath, "line1\nJLINK_VM_OPTIONS=\nline3\n");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.additionalBinaries = List.of(
                        new AdditionalBinary(Path.of("/path/to/binary1"), "binary1", "java.library.path"),
                        new AdditionalBinary(Path.of("/path/to/binary2"), "binary2", "jna.library.path")
                );

                // Act
                patchLauncherScriptMethod.invoke(classUnderTest, "launcher");

                // Assert
                List<String> modifiedLines = Files.readAllLines(scriptPath);
                assertThat(modifiedLines).containsExactly(
                        "line1",
                        "JLINK_VM_OPTIONS='%s -Djava.library.path=./binary1 -Djna.library.path=./binary2'".formatted(ENABLE_NATIVE_ACCESS_JAVAFX),
                        "line3"
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }
    }

    @Nested
    class PatchLauncherScriptsTest {

        @Test
        void testPatchLauncherScripts_HandlesUnixAndWindowsScripts() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test-scripts");
            try (MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS))
                        .thenReturn(true);
                // Arrange
                Path unixLauncher = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher");
                Path windowsLauncher = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher.bat");

                Files.createDirectories(unixLauncher.getParent());
                Files.writeString(unixLauncher, "line1\nJLINK_VM_OPTIONS=\nline3\n");
                Files.writeString(windowsLauncher, "@echo off\nset JLINK_VM_OPTIONS=\n");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.options = List.of("-Xmx512m");
                classUnderTest.commandlineArgs = "-Dkey=value";
                classUnderTest.launcher = "launcher";

                // Act
                patchLauncherScriptsMethod.invoke(classUnderTest);

                // Assert
                List<String> unixLines = Files.readAllLines(unixLauncher);
                assertThat(unixLines).containsExactly(
                        "line1",
                        "JLINK_VM_OPTIONS='%s -Xmx512m'".formatted(ENABLE_NATIVE_ACCESS_JAVAFX),
                        "line3"
                );

                List<String> windowsLines = Files.readAllLines(windowsLauncher);
                assertThat(windowsLines).containsExactly(
                        "@echo off",
                        "set JLINK_VM_OPTIONS=%s -Xmx512m".formatted(ENABLE_NATIVE_ACCESS_JAVAFX)
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testPatchLauncherScripts_MissingWindowsLauncher() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test-missing-windows");
            try {
                // Arrange
                Path unixLauncher = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("bin")
                        .resolve("launcher");

                Files.createDirectories(unixLauncher.getParent());
                Files.writeString(unixLauncher, "line1\nJLINK_VM_OPTIONS=\nline3\n");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.options = List.of("-Xmx256m");
                classUnderTest.commandlineArgs = "-Dproperty=value";
                classUnderTest.launcher = "launcher";

                // Act
                patchLauncherScriptsMethod.invoke(classUnderTest);

                // Assert
                List<String> unixLines = Files.readAllLines(unixLauncher);
                assertThat(unixLines).containsExactly(
                        "line1",
                        "JLINK_VM_OPTIONS='%s -Xmx256m'".formatted(ENABLE_NATIVE_ACCESS_JAVAFX),
                        "line3"
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }
    }

    @Nested
    class GetJLinkCommandTest {

        @Test
        void testGetJLinkCommand_BasicConfiguration() throws Exception {
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Arrange
                classUnderTest.buildDirectory = Paths.get("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.modulePathElements = List.of("module1.jar", "module2.jar");
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
                classUnderTest.mainClass = "com.example.Main";

                // Act
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

                // Assert
                assertThat(command).containsSequence(
                        "jlink",
                        "--module-path",
                        "module1.jar" + File.pathSeparator + "module2.jar",
                        "--add-modules",
                        "com.example.app",
                        "--output",
                        Paths.get("target/image").toAbsolutePath().toString()
                );
            }
        }

        @Test
        void testGetJLinkCommand_WithCustomJmodsPath() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.modulePathElements = List.of("module1.jar");
            classUnderTest.jmodsPath = "/custom/jmods/path";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).contains(
                    "/custom/jmods/path" + File.pathSeparator + "module1.jar"
            );
        }

        @Test
        void testGetJLinkCommand_MissingModuleDescriptor_ThrowsException() {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.modulePathElements = List.of("module1.jar");
            classUnderTest.moduleDescriptor = null;

            // Act & Assert
            assertThatThrownBy(() -> getJLinkCommandMethod.invoke(classUnderTest))
                    .isInstanceOf(InvocationTargetException.class)
                    .cause()
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("jlink requires a module descriptor");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0", "1", "2",
                "zip-0", "zip-1", "zip-2", "zip-3", "zip-4", "zip-5", "zip-6", "zip-7", "zip-8", "zip-9"
        })
        void testGetJLinkCommand_WithAllCompressionOptions(String compression) throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");

            // Test valid compression options
            classUnderTest.compress = compression;

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).contains("--compress=" + compression);
        }

        @Test
        void testGetJLinkCommand_InvalidCompressionOption_ThrowsException() {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.compress = "invalid-compression";

            // Act & Assert
            assertThatThrownBy(() -> getJLinkCommandMethod.invoke(classUnderTest))
                    .isInstanceOf(InvocationTargetException.class)
                    .cause()
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("Invalid compression option: invalid-compression");
        }

        @Test
        void testGetJLinkCommand_WithAllBooleanFlags() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");

            // Set all boolean flags to true
            classUnderTest.stripDebug = true;
            classUnderTest.stripJavaDebugAttributes = true;
            classUnderTest.noHeaderFiles = true;
            classUnderTest.noManPages = true;
            classUnderTest.bindServices = true;
            classUnderTest.ignoreSigningInformation = true;
            classUnderTest.jlinkVerbose = true;

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).contains(
                    "--strip-debug",
                    "--strip-java-debug-attributes",
                    "--no-header-files",
                    "--no-man-pages",
                    "--bind-services",
                    "--ignore-signing-information",
                    "--verbose"
            );
        }

        @Test
        void testGetJLinkCommand_WithLauncher_MainClassContainsSlash() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
            classUnderTest.launcher = "myLauncher";
            classUnderTest.mainClass = "com.example.module/com.example.Main";

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).containsSequence(
                    "--launcher",
                    "myLauncher=com.example.module/com.example.Main"
            );
        }

        @Test
        void testGetJLinkCommand_WithLauncher_MainClassWithoutSlash() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
            classUnderTest.launcher = "myLauncher";
            classUnderTest.mainClass = "com.example.Main";

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).containsSequence(
                    "--launcher",
                    "myLauncher=com.example.app/com.example.Main"
            );
        }

        @Test
        void testGetJLinkCommand_WithoutLauncher() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
            classUnderTest.launcher = null;

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).doesNotContain("--launcher");
        }

        @Test
        void testGetJLinkCommand_EmptyModulePathElements() throws Exception {
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Arrange
                classUnderTest.buildDirectory = Paths.get("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.modulePathElements = List.of();

                // Act
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

                // Assert
                assertThat(command).doesNotContain("--module-path", "--add-modules");
                assertThat(command).containsSequence(
                        "jlink",
                        "--output",
                        Paths.get("target/image").toAbsolutePath().toString()
                );
            }
        }

        @Test
        void testGetJLinkCommand_ExistingOutputDirectoryCleansUp() throws Exception {
            // Arrange
            Path temporaryDirectory = Files.createTempDirectory("jlink-output-test");
            Path buildDir = temporaryDirectory.resolve("target");
            Path outputDir = buildDir.resolve("image");
            Files.createDirectories(outputDir);
            Files.createFile(outputDir.resolve("existing-file.txt"));

            classUnderTest.buildDirectory = buildDir;
            classUnderTest.jlinkImageName = "image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");

            try {
                // Act
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

                // Assert
                assertThat(Files.exists(outputDir.resolve("existing-file.txt"))).isFalse();
                assertThat(command).contains(outputDir.toAbsolutePath().toString());
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testGetJLinkCommand_WithCustomImageName() throws Exception {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "custom-runtime-image";
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");

            // Act
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

            // Assert
            assertThat(command).contains(
                    Paths.get("target/custom-runtime-image").toAbsolutePath().toString()
            );
        }

        @Test
        void testGetJLinkCommand_CompleteConfigurationScenario() throws Exception {
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Arrange
                classUnderTest.buildDirectory = Paths.get("target");
                classUnderTest.jlinkImageName = "complete-image";
                classUnderTest.jlinkExecutable = "/custom/path/jlink";
                classUnderTest.modulePathElements = List.of("mod1.jar", "mod2.jar", "mod3.jar");
                classUnderTest.jmodsPath = "/custom/jmods";
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.complete");
                classUnderTest.compress = "zip-6";
                classUnderTest.stripDebug = true;
                classUnderTest.stripJavaDebugAttributes = true;
                classUnderTest.noHeaderFiles = true;
                classUnderTest.noManPages = true;
                classUnderTest.bindServices = true;
                classUnderTest.ignoreSigningInformation = true;
                classUnderTest.jlinkVerbose = true;
                classUnderTest.launcher = "complete-launcher";
                classUnderTest.mainClass = "com.example.CompleteMain";

                // Act
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) getJLinkCommandMethod.invoke(classUnderTest);

                // Assert
                assertThat(command).containsExactly(
                        "/custom/path/jlink",
                        "--module-path",
                        "/custom/jmods" + File.pathSeparator + "mod1.jar" + File.pathSeparator + "mod2.jar" + File.pathSeparator + "mod3.jar",
                        "--add-modules",
                        "com.example.complete",
                        "--output",
                        Paths.get("target/complete-image").toAbsolutePath().toString(),
                        "--strip-debug",
                        "--strip-java-debug-attributes",
                        "--compress=zip-6",
                        "--no-header-files",
                        "--no-man-pages",
                        "--bind-services",
                        "--ignore-signing-information",
                        "--verbose",
                        "--launcher",
                        "complete-launcher=com.example.complete/com.example.CompleteMain"
                );
            }
        }
    }

    @Nested
    class ExecuteTest {

        @Test
        void testExecute_SkipExecution() throws Exception {
            // Arrange
            classUnderTest.skip = true;

            // Act
            classUnderTest.execute();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void testExecute_MissingJlinkExecutable_ThrowsException(String jlink) {
            // Arrange
            classUnderTest.skip = false;
            classUnderTest.jlinkExecutable = jlink;

            // Act & Assert
            assertThatThrownBy(() -> classUnderTest.execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("JavaFX jlink executable is not specified");
        }

        @Test
        void testExecute_MissingBaseDirectory_ThrowsException() {
            // Arrange
            classUnderTest.skip = false;
            classUnderTest.jlinkExecutable = "jlink";
            classUnderTest.baseDirectory = null;

            // Act & Assert
            assertThatThrownBy(() -> classUnderTest.execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("JavaFX base directory is not specified");
        }

        @Test
        void testExecute_SuccessfulExecution_BasicConfiguration() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("jlink-execute-test");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class);
                 AutoCloseable _ = setupMockProcess()) {
                Mockito.when(mockProcess.waitFor()).thenReturn(0);
                mockStatic(files, osMock);
                classUnderTest.skip = false;
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.baseDirectory = tempDir.resolve("base");
                classUnderTest.workingDirectory = tempDir.resolve("working");
                classUnderTest.buildDirectory = tempDir.resolve("working").resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.project = null;

                // Act
                classUnderTest.execute();

                // Assert
                Mockito.verify(mockProcess).waitFor();
                Mockito.verifyNoMoreInteractions(mockProcess);
            } finally {
                deleteFolder(tempDir);
            }
        }

        @Test
        void testExecute_ProcessFailedExitCode1_ThrowMojoFailureException() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("jlink-execute-test");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class);
                 AutoCloseable _ = setupMockProcess()) {
                Mockito.when(mockProcess.waitFor()).thenReturn(1);
                mockStatic(files, osMock);
                classUnderTest.skip = false;
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.baseDirectory = tempDir.resolve("base");
                classUnderTest.workingDirectory = tempDir.resolve("working");
                classUnderTest.buildDirectory = tempDir.resolve("working").resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.project = null;

                // Act
                assertThatThrownBy(() -> classUnderTest.execute())
                        .isInstanceOf(MojoFailureException.class)
                        .hasMessage("Error: Could not link JavaFX application");

                // Assert
                Mockito.verify(mockProcess).waitFor();
                Mockito.verifyNoMoreInteractions(mockProcess);
            } finally {
                deleteFolder(tempDir);
            }
        }

        @Test
        void testExecute_WaitForInterrupted_ThrowMojoExecutionException() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("jlink-execute-test");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class);
                 AutoCloseable _ = setupMockProcess()) {
                Mockito.when(mockProcess.waitFor()).thenThrow(InterruptedException.class);
                mockStatic(files, osMock);
                classUnderTest.skip = false;
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.baseDirectory = tempDir.resolve("base");
                classUnderTest.workingDirectory = tempDir.resolve("working");
                classUnderTest.buildDirectory = tempDir.resolve("working").resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.project = null;

                // Act
                assertThatThrownBy(() -> classUnderTest.execute())
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessage("Error: Could not link JavaFX application")
                        .hasCauseInstanceOf(InterruptedException.class);

                // Assert
                Mockito.verify(mockProcess).waitFor();
                Mockito.verifyNoMoreInteractions(mockProcess);
            } finally {
                deleteFolder(tempDir);
            }
        }

        @Test
        void testExecute_SuccessfulExecutionWithLauncherAndZip_BasicConfiguration() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("jlink-execute-test");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class);
                 AutoCloseable _ = setupMockProcess()) {
                Mockito.when(mockProcess.waitFor()).thenReturn(0);
                mockStatic(files, osMock);
                classUnderTest.skip = false;
                classUnderTest.jlinkExecutable = "jlink";
                classUnderTest.baseDirectory = tempDir.resolve("base");
                classUnderTest.workingDirectory = tempDir.resolve("working");
                classUnderTest.buildDirectory = tempDir.resolve("working").resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.moduleDescriptor = createMockModuleDescriptor("com.example.app");
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.project = null;
                classUnderTest.launcher = "launcher";
                classUnderTest.jlinkZipName = "zip";
                classUnderTest.loggingFormat = "%2s";

                // Act
                classUnderTest.execute();

                // Assert
                Mockito.verify(mockProcess).waitFor();
                Mockito.verifyNoMoreInteractions(mockProcess);
            } finally {
                deleteFolder(tempDir);
            }
        }

        private AutoCloseable setupMockProcess() {
            return Mockito.mockConstruction(ProcessBuilder.class, (mock, _) -> {
                Mockito.when(mock.start()).thenReturn(mockProcess);
                Mockito.when(mock.directory(Mockito.any())).thenReturn(mock);
                Mockito.when(mock.inheritIO()).thenReturn(mock);
                Mockito.when(mock.environment()).thenReturn(new HashMap<>());
            });
        }
    }

    @Nested
    class PatchLoggingFormatTest {

        @Test
        void testLoggingPropertiesFileDoesNotExists_NothingTodo() {
            // Arrange
            classUnderTest.buildDirectory = Paths.get("target");
            classUnderTest.jlinkImageName = "image";
            classUnderTest.loggingFormat = "%1s";

            // Act
            assertThatNoException()
                    .isThrownBy(() -> patchLoggingFormatMethod.invoke(classUnderTest));
        }

        @Test
        void testLoggingPropertiesDoesNotContainFormatButFileNotReadable_ThrowExecutionException() throws IOException {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path configPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("conf")
                        .resolve("logging.properties");
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, "java.util.logging.ConsoleHandler.formatter=com.example.Formatter");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.loggingFormat = "%1s";

                // Simulate script file being non-writable (e.g., permission error)
                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.exists(Mockito.any()))
                            .thenReturn(true);
                    filesMock.when(() -> Files.lines(Mockito.any()))
                            .thenThrow(IOException.class);

                    // Act & Assert
                    assertThatThrownBy(
                            () -> patchLoggingFormatMethod.invoke(classUnderTest)
                    ).isInstanceOf(InvocationTargetException.class)
                            .hasCauseInstanceOf(MojoExecutionException.class)
                            .cause()
                            .hasMessage("Unable to patch configuration logging properties")
                            .hasCauseInstanceOf(IOException.class);
                }
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testLoggingPropertiesDoesNotContainFormat_AddedFormat() throws IOException {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path configPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("conf")
                        .resolve("logging.properties");
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, "java.util.logging.ConsoleHandler.formatter=com.example.Formatter");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.loggingFormat = "%1s";

                // Act
                assertThatNoException()
                        .isThrownBy(() -> patchLoggingFormatMethod.invoke(classUnderTest));

                List<String> modifiedLines = Files.readAllLines(configPath);
                assertThat(modifiedLines).containsExactly(
                        "java.util.logging.ConsoleHandler.formatter=com.example.Formatter",
                        "java.util.logging.SimpleFormatter.format=%s".formatted(classUnderTest.loggingFormat)
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "java.util.logging.SimpleFormatter.format=format",
                "#java.util.logging.SimpleFormatter.format=commented",
                "# java.util.logging.SimpleFormatter.format=commented",
                "#\tjava.util.logging.SimpleFormatter.format=commented"
        })
        void testLoggingPropertiesHasFormat_AddedFormat(String line) throws IOException {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path configPath = temporaryDirectory.resolve("target")
                        .resolve("image")
                        .resolve("conf")
                        .resolve("logging.properties");
                Files.createDirectories(configPath.getParent());
                Files.writeString(configPath, line);

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.loggingFormat = "%1s";

                // Act
                assertThatNoException()
                        .isThrownBy(() -> patchLoggingFormatMethod.invoke(classUnderTest));

                List<String> modifiedLines = Files.readAllLines(configPath);
                assertThat(modifiedLines).containsExactly(
                        "java.util.logging.SimpleFormatter.format=%s".formatted(classUnderTest.loggingFormat)
                );
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }
    }

    @Nested
    class CopyAdditionalBinariesToBinaryFolderTest {

        @Test
        void testCopyAdditionalBinaries_Success() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path binary1 = temporaryDirectory.resolve("binary1");
                Path binary2 = temporaryDirectory.resolve("binary2");
                Files.writeString(binary1, "content1");
                Files.writeString(binary2, "content2");

                Path buildDirectory = temporaryDirectory.resolve("target");
                Path imagePath = buildDirectory.resolve("image");
                Path binaryPath = imagePath.resolve("bin");
                Files.createDirectories(binaryPath);

                classUnderTest.buildDirectory = buildDirectory;
                classUnderTest.jlinkImageName = "image";
                classUnderTest.additionalBinaries = List.of(
                        new AdditionalBinary(binary1, "dest1", null),
                        new AdditionalBinary(binary2, "dest2", null)
                );

                // Act
                copyAdditionalBinariesToBinaryFolderMethod.invoke(classUnderTest);

                // Assert
                assertThat(binaryPath.resolve("binary1")).exists().hasContent("content1");
                assertThat(binaryPath.resolve("binary2")).exists().hasContent("content2");
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testCopyAdditionalBinaries_IOException() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path binary = temporaryDirectory.resolve("binary");
                Files.writeString(binary, "content");

                Path buildDirectory = temporaryDirectory.resolve("target");
                Path imagePath = buildDirectory.resolve("image");
                Path binaryPath = imagePath.resolve("bin");
                Files.createDirectories(binaryPath);

                classUnderTest.buildDirectory = buildDirectory;
                classUnderTest.jlinkImageName = "image";
                classUnderTest.additionalBinaries = List.of(
                        new AdditionalBinary(binary, "dest", null)
                );

                try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                    filesMock.when(() -> Files.copy(any(Path.class), any(), any(CopyOption[].class)))
                            .thenThrow(new IOException("Mocked copy error"));

                    // Act & Assert
                    assertThatThrownBy(() -> copyAdditionalBinariesToBinaryFolderMethod.invoke(classUnderTest))
                            .isInstanceOf(InvocationTargetException.class)
                            .hasCauseInstanceOf(MojoExecutionException.class)
                            .cause()
                            .hasMessage("Unable to copy `%s` to `%s`".formatted(
                                    binary,
                                    imagePath.resolve("bin").resolve("binary")
                            ));
                }
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testCopyAdditionalBinaries_NoAdditionalBinaries() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path buildDirectory = temporaryDirectory.resolve("target");
                Path imagePath = buildDirectory.resolve("image");
                Path binaryPath = imagePath.resolve("bin");
                Files.createDirectories(binaryPath);

                classUnderTest.buildDirectory = buildDirectory;
                classUnderTest.jlinkImageName = "image";
                classUnderTest.additionalBinaries = null;

                // Act
                copyAdditionalBinariesToBinaryFolderMethod.invoke(classUnderTest);

                // Assert
                assertThat(binaryPath).exists();
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }

        @Test
        void testCopyAdditionalBinaries_DestinationDirectoryDoesNotExist() throws Exception {
            Path temporaryDirectory = Files.createTempDirectory("javafx-jlink-mojo-test");
            try {
                // Arrange
                Path binary = temporaryDirectory.resolve("binary");
                Files.writeString(binary, "content");

                classUnderTest.buildDirectory = temporaryDirectory.resolve("target");
                classUnderTest.jlinkImageName = "image";
                classUnderTest.additionalBinaries = List.of(
                        new AdditionalBinary(binary, "dest", null)
                );

                // Act & Assert
                assertThatThrownBy(() -> copyAdditionalBinariesToBinaryFolderMethod.invoke(classUnderTest))
                        .isInstanceOf(InvocationTargetException.class)
                        .hasCauseInstanceOf(MojoExecutionException.class)
                        .cause()
                        .hasMessage("Unable to copy `%s` to `%s`".formatted(
                                binary,
                                temporaryDirectory.resolve("target").resolve("image").resolve("bin").resolve("binary")
                        ));
            } finally {
                deleteFolder(temporaryDirectory);
            }
        }
    }
// TODO: Update tests to support Windows
}