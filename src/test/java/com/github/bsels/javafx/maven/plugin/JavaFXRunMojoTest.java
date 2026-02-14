package com.github.bsels.javafx.maven.plugin;

import com.github.bsels.javafx.maven.plugin.parameters.AdditionalBinary;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.github.bsels.javafx.maven.plugin.BaseJavaFXMojo.ENABLE_NATIVE_ACCESS_JAVAFX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JavaFXRunMojoTest {

    @Mock
    LocationManager locationManager;

    @Mock
    ToolchainManager toolchainManager;

    private JavaFXRunMojo classUnderTest;
    private Method getJavaRunCommand;
    private String originalJavaHome;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        originalJavaHome = System.getProperty("java.home");
        classUnderTest = new JavaFXRunMojo(locationManager, toolchainManager);
        getJavaRunCommand = JavaFXRunMojo.class.getDeclaredMethod("getJavaRunCommand");
        getJavaRunCommand.setAccessible(true);
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

    @Nested
    class ExecuteMethodTest {

        @Test
        void testExecuteWhenSkipIsTrue() throws Exception {
            // Setup
            classUnderTest.skip = true;

            // Act
            classUnderTest.execute();
        }

        @Test
        void testExecuteThrowsExceptionWhenBaseDirectoryIsNull() {
            // Setup
            classUnderTest.executable = "java";
            classUnderTest.baseDirectory = null;

            // Act & Assert
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("JavaFX base directory is not specified");
        }

        @Test
        void testExecuteThrowsExceptionWhenExecutableIsEmpty() {
            // Setup
            classUnderTest.executable = "";

            // Act & Assert
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("JavaFX executable is not specified");
        }

        @Test
        void testExecuteWithValidConfiguration() throws Exception {
            // Setup
            classUnderTest.executable = "java";
            classUnderTest.baseDirectory = Path.of("some/base/directory");
            classUnderTest.workingDirectory = Path.of("some/working/directory");
            classUnderTest.options = List.of("-Xmx512m");
            classUnderTest.commandlineArgs = "-arg1 value1 -arg2 value2";
            classUnderTest.mainClass = "com.example.Main";

            // Mock ProcessBuilder and Process
            Process mockProcess = Mockito.mock(Process.class);
            try (
                    MockedConstruction<ProcessBuilder> _ = Mockito.mockConstruction(
                            ProcessBuilder.class, (mock, context) -> {
                                Mockito.when(mock.start()).thenReturn(mockProcess);
                                Mockito.when(mock.inheritIO()).thenReturn(mock);
                                Mockito.when(mock.directory(Mockito.any())).thenReturn(mock);
                                assertThat(context.arguments())
                                        .hasSize(1)
                                        .first()
                                        .isEqualTo(List.of(
                                                "java", ENABLE_NATIVE_ACCESS_JAVAFX, "-Xmx512m", "com.example.Main",
                                                "-arg1", "value1", "-arg2", "value2"
                                        ));
                            }
                    );
                    MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                    MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)
            ) {
                mockStatic(files, osMock);
                Mockito.when(mockProcess.waitFor()).thenReturn(0);

                // Act
                classUnderTest.execute();
            }
        }

        @Test
        void testExecuteWithValidConfigurationButFailingProcess() throws Exception {
            // Setup
            classUnderTest.executable = "java";
            classUnderTest.baseDirectory = Path.of("some/base/directory");
            classUnderTest.workingDirectory = Path.of("some/working/directory");
            classUnderTest.options = List.of("-Xmx512m");
            classUnderTest.commandlineArgs = "-arg1 value1 -arg2 value2";
            classUnderTest.mainClass = "com.example.Main";

            // Mock ProcessBuilder and Process
            Process mockProcess = Mockito.mock(Process.class);
            try (
                    MockedConstruction<ProcessBuilder> _ = Mockito.mockConstruction(
                            ProcessBuilder.class, (mock, context) -> {
                                Mockito.when(mock.start()).thenReturn(mockProcess);
                                Mockito.when(mock.inheritIO()).thenReturn(mock);
                                Mockito.when(mock.directory(Mockito.any())).thenReturn(mock);
                                assertThat(context.arguments())
                                        .hasSize(1)
                                        .first()
                                        .isEqualTo(List.of(
                                                "java", ENABLE_NATIVE_ACCESS_JAVAFX, "-Xmx512m", "com.example.Main",
                                                "-arg1", "value1", "-arg2", "value2"
                                        ));
                            }
                    );
                    MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                    MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)
            ) {
                mockStatic(files, osMock);
                Mockito.when(mockProcess.waitFor())
                        .thenThrow(InterruptedException.class);

                // Act
                assertThatThrownBy(classUnderTest::execute)
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessage("Error: Could not start JavaFX application")
                        .hasCauseInstanceOf(InterruptedException.class);
            }
        }

        @Test
        void testExecuteWithValidConfigurationButUnableToStartProcess() {
            // Setup
            classUnderTest.executable = "java";
            classUnderTest.baseDirectory = Path.of("some/base/directory");
            classUnderTest.workingDirectory = Path.of("some/working/directory");
            classUnderTest.options = List.of("-Xmx512m");
            classUnderTest.commandlineArgs = "-arg1 value1 -arg2 value2";
            classUnderTest.mainClass = "com.example.Main";

            // Mock ProcessBuilder and Process
            try (
                    MockedConstruction<ProcessBuilder> _ = Mockito.mockConstruction(
                            ProcessBuilder.class, (mock, context) -> {
                                Mockito.when(mock.start()).thenThrow(IOException.class);
                                Mockito.when(mock.inheritIO()).thenReturn(mock);
                                Mockito.when(mock.directory(Mockito.any())).thenReturn(mock);
                                assertThat(context.arguments())
                                        .hasSize(1)
                                        .first()
                                        .isEqualTo(List.of(
                                                "java", ENABLE_NATIVE_ACCESS_JAVAFX, "-Xmx512m", "com.example.Main",
                                                "-arg1", "value1", "-arg2", "value2"
                                        ));
                            }
                    );
                    MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                    MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)
            ) {
                mockStatic(files, osMock);

                // Act
                assertThatThrownBy(classUnderTest::execute)
                        .isInstanceOf(MojoExecutionException.class)
                        .hasMessage("Error: Could not start JavaFX application")
                        .hasCauseInstanceOf(IOException.class);
            }
        }
    }

    @Nested
    class GetJavaRunCommandTest {

        /**
         * Tests the `getJavaRunCommand` method in JavaFXRunMojo.
         * <p>
         * This method constructs the command to run the JavaFX application
         * based on various configuration parameters such as executable, debugger options,
         * classpath, module path, and main class.
         */

        @Test
        void testGetJavaRunCommandWithDebuggerEnabled() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);

                // Setup
                classUnderTest.executable = "java";
                classUnderTest.attachDebugger = true;
                classUnderTest.debuggerPort = 5005;
                classUnderTest.mainClass = "com.example.Main";

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "com.example.Main"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandWithoutDebugger() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.attachDebugger = false;
                classUnderTest.mainClass = "com.example.Main";

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "com.example.Main"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandWithModuleAndClasspath() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.modulePathElements = List.of("module-path1", "module-path2");
                classUnderTest.classPathElements = List.of("classpath1", "classpath2");
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.moduleDescriptor = null;
                classUnderTest.pathElements = Map.of(
                        "key-ignored", JavaModuleDescriptor.newModule("javafx.testing").build()
                );

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "--module-path",
                                "module-path1" + File.pathSeparator + "module-path2",
                                "--add-modules",
                                "javafx.testing",
                                "--classpath",
                                "classpath1" + File.pathSeparator + "classpath2",
                                "com.example.Main"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandWithOptionsAndArgsAndLogFormat() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.options = List.of("-Xmx512m", "-Dproperty=value");
                classUnderTest.commandlineArgs = "-arg1 value1 -arg2 value2";
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.loggingFormat = "%6$s%n";

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                "-Djava.util.logging.SimpleFormatter.format=%6$s%n",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "-Xmx512m",
                                "-Dproperty=value",
                                "com.example.Main",
                                "-arg1", "value1", "-arg2", "value2"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandFallsBackToDefaults() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.attachDebugger = false;

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly("java", ENABLE_NATIVE_ACCESS_JAVAFX);
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandWithModuleDescriptor() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.moduleDescriptor = JavaModuleDescriptor.newModule("test.module").build();
                classUnderTest.mainClass = "com.example.Main";

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "--module",
                                "com.example.Main"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void testGetJavaRunCommandWithAdditionalBinaries() throws InvocationTargetException, IllegalAccessException {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                mockStatic(files, osMock);
                // Setup
                classUnderTest.executable = "java";
                classUnderTest.mainClass = "com.example.Main";
                classUnderTest.additionalBinaries = List.of(
                        new AdditionalBinary(Path.of("/path/to/binary1"), "binary1", "java.library.path"),
                        new AdditionalBinary(Path.of("/path/to/binary2"), "binary2", "jna.library.path")
                );

                // Act
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) getJavaRunCommand.invoke(classUnderTest);

                // Assert
                assertThat(result)
                        .containsExactly(
                                "java",
                                "-Djava.library.path=/path/to/binary1",
                                "-Djna.library.path=/path/to/binary2",
                                ENABLE_NATIVE_ACCESS_JAVAFX,
                                "com.example.Main"
                        );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }
    }
}