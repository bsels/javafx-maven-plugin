package com.github.bsels.javafx.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.util.Os;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
class BaseJavaFXMojoTest {

    @Mock
    LocationManager locationManager;

    @Mock
    ToolchainManager toolchainManager;

    private BaseJavaFXMojo classUnderTest;

    @BeforeEach
    void setUp() {
        classUnderTest = new JavaFXRunMojo(locationManager, toolchainManager);
    }


    @Nested
    class InitTest {

        @Mock
        MavenProject projectMock;

        @Mock
        LocationManager locationManagerMock;

        @BeforeEach
        void reinitWithMocks() {
            // Gebruik een verse instantie met mock LocationManager
            classUnderTest = new JavaFXRunMojo(locationManagerMock, toolchainManager);
        }

        @Test
        void whenProjectIsNull_doesNothing() throws Exception {
            classUnderTest.project = null;
            // Zou geen uitzondering mogen gooien
            classUnderTest.init(null);
            // Geen verdere assert nodig; succes is afwezigheid van uitzonderingen
        }

        @Test
        void whenOutputDirectoryIsNull_throws() {
            classUnderTest.project = projectMock;
            Build build = new Build();
            build.setOutputDirectory(null);
            Mockito.when(projectMock.getBuild()).thenReturn(build);

            assertThatExceptionOfType(MojoExecutionException.class)
                    .isThrownBy(() -> classUnderTest.init(null))
                    .withMessage("Error: Output directory does not exists");
        }

        @Test
        void whenOutputDirectoryIsEmpty_throws() {
            classUnderTest.project = projectMock;
            Build build = new Build();
            build.setOutputDirectory("");
            Mockito.when(projectMock.getBuild()).thenReturn(build);

            assertThatExceptionOfType(MojoExecutionException.class)
                    .isThrownBy(() -> classUnderTest.init(null))
                    .withMessage("Error: Output directory does not exists");
        }

        @Test
        void whenOutputDirectoryNotExists_throws() {
            classUnderTest.project = projectMock;
            Build build = new Build();
            // Wijs naar een niet-bestaand pad
            String nonExisting = Path.of("target", "no-such-dir-xyz").toString();
            build.setOutputDirectory(nonExisting);
            Mockito.when(projectMock.getBuild()).thenReturn(build);

            assertThatExceptionOfType(MojoExecutionException.class)
                    .isThrownBy(() -> classUnderTest.init(null))
                    .withMessage("Error: Output directory does not exists");
        }

        @Test
        void whenModuleInfoMissing_throws() throws Exception {
            classUnderTest.project = projectMock;

            Path tempOut = Files.createTempDirectory("init-test-out");
            try {
                Build build = new Build();
                build.setOutputDirectory(tempOut.toString());
                Mockito.when(projectMock.getBuild()).thenReturn(build);

                assertThatExceptionOfType(MojoExecutionException.class)
                        .isThrownBy(() -> classUnderTest.init(null))
                        .withMessage("Error: module-info.class file is required");
            } finally {
                Files.deleteIfExists(tempOut);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void success_populatesFields_andMergesModuleAndClassPaths_andRespectsJdkHome() throws Exception {
            classUnderTest.project = projectMock;

            // Maak output dir met module-info.class
            Path tempOut = Files.createTempDirectory("init-test-out");
            Path moduleInfo = tempOut.resolve("module-info.class");
            Files.createFile(moduleInfo);

            try {
                Build build = new Build();
                build.setOutputDirectory(tempOut.toString());
                Mockito.when(projectMock.getBuild()).thenReturn(build);
                Mockito.when(projectMock.getDependencies()).thenReturn(List.of());
                Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of());

                // Prepareer ResolvePathsResult mock
                var result = Mockito.mock(ResolvePathsResult.class);
                JavaModuleDescriptor mainDesc = Mockito.mock(JavaModuleDescriptor.class);
                JavaModuleDescriptor peDesc = Mockito.mock(JavaModuleDescriptor.class);

                Map<Path, JavaModuleDescriptor> pathElements = Map.of(Path.of("lib", "m1.jar"), peDesc);
                List<Path> classpath = List.of(Path.of("cp", "a"), Path.of("cp", "b"));
                Map<Path, ModuleNameSource> modulepath =
                        Map.of(Path.of("mp", "x"), ModuleNameSource.MODULEDESCRIPTOR);

                Mockito.when(result.getPathElements()).thenReturn(pathElements);
                Mockito.when(result.getPathExceptions()).thenReturn(Map.of());
                Mockito.when(result.getMainModuleDescriptor()).thenReturn(mainDesc);
                Mockito.when(result.getClasspathElements()).thenReturn(classpath);
                Mockito.when(result.getModulepathElements()).thenReturn(modulepath);

                Mockito.when(locationManagerMock.resolvePaths(Mockito.any())).thenReturn(result);

                Path jdkHome = Path.of("fake", "jdk", "home");
                classUnderTest.init(jdkHome);

                // Velden gecontroleerd
                assertThat(classUnderTest.moduleDescriptor).isSameAs(mainDesc);

                // classPathElements leeg na merge
                assertThat(classUnderTest.classPathElements).isEmpty();

                // modulePathElements bevat module- en classpath-items
                assertThat(classUnderTest.modulePathElements)
                        .containsExactlyInAnyOrder(
                                Path.of("mp", "x").toString(),
                                Path.of("cp", "a").toString(),
                                Path.of("cp", "b").toString()
                        );

                // pathElements key is String (pad.toString) met dezelfde descriptor
                assertThat(classUnderTest.pathElements)
                        .containsEntry(Path.of("lib", "m1.jar").toString(), peDesc);

                // Verify dat resolvePaths is aangeroepen
                Mockito.verify(locationManagerMock, Mockito.times(1))
                        .resolvePaths(Mockito.any());
            } finally {
                // Cleanup
                Files.deleteIfExists(moduleInfo);
                Files.deleteIfExists(tempOut);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void pathExceptions_areLogged_butDoNotFail() throws Exception {
            classUnderTest.project = projectMock;

            Path tempOut = Files.createTempDirectory("init-test-out");
            Path moduleInfo = tempOut.resolve("module-info.class");
            Files.createFile(moduleInfo);

            try {
                Build build = new Build();
                build.setOutputDirectory(tempOut.toString());
                Mockito.when(projectMock.getBuild()).thenReturn(build);
                Mockito.when(projectMock.getDependencies()).thenReturn(List.of());
                Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of());

                var result = Mockito.mock(ResolvePathsResult.class);
                Mockito.when(result.getPathElements()).thenReturn(Map.of());
                Mockito.when(result.getPathExceptions()).thenReturn(
                        Map.of(Path.of("bad", "dep.jar"), new RuntimeException("boom", new IOException("boom-io")))
                );
                Mockito.when(result.getMainModuleDescriptor()).thenReturn(null);
                Mockito.when(result.getClasspathElements()).thenReturn(List.of());
                Mockito.when(result.getModulepathElements()).thenReturn(Map.of());

                Mockito.when(locationManagerMock.resolvePaths(Mockito.any()))
                        .thenReturn(result);

                // Zou niet falen; alleen loggen
                classUnderTest.init(null);

                assertThat(classUnderTest.modulePathElements).isEmpty();
                assertThat(classUnderTest.classPathElements).isEmpty();
                assertThat(classUnderTest.pathElements).isEmpty();
            } finally {
                Files.deleteIfExists(moduleInfo);
                Files.deleteIfExists(tempOut);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void filenameModule_isResolved_andAddedToPathElements() throws Exception {
            classUnderTest.project = projectMock;

            Path tempOut = Files.createTempDirectory("init-test-out");
            Path moduleInfo = tempOut.resolve("module-info.class");
            Files.createFile(moduleInfo);

            try {
                Build build = new Build();
                build.setOutputDirectory(tempOut.toString());
                Mockito.when(projectMock.getBuild()).thenReturn(build);
                Mockito.when(projectMock.getDependencies()).thenReturn(List.of());
                Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of());

                var result = Mockito.mock(ResolvePathsResult.class);
                JavaModuleDescriptor mainDesc = Mockito.mock(JavaModuleDescriptor.class);
                JavaModuleDescriptor filenameDesc = Mockito.mock(JavaModuleDescriptor.class);

                Path lib = Path.of("lib", "m1.jar");
                Map<Path, JavaModuleDescriptor> pathElements = Map.of(
                        lib, filenameDesc
                );
                List<Path> classpath = List.of();
                Map<Path, ModuleNameSource> modulepath = Map.of(
                        lib, ModuleNameSource.FILENAME
                );

                Mockito.when(result.getPathElements()).thenReturn(pathElements);
                Mockito.when(result.getPathExceptions()).thenReturn(Map.of());
                Mockito.when(result.getMainModuleDescriptor()).thenReturn(mainDesc);
                Mockito.when(result.getClasspathElements()).thenReturn(classpath);
                Mockito.when(result.getModulepathElements()).thenReturn(modulepath);

                Mockito.when(locationManagerMock.resolvePaths(Mockito.any())).thenReturn(result);

                classUnderTest.init(null);

                assertThat(classUnderTest.moduleDescriptor).isSameAs(mainDesc);
                assertThat(classUnderTest.modulePathElements)
                        .containsExactly(lib.toString());
                assertThat(classUnderTest.classPathElements).isEmpty();
                assertThat(classUnderTest.pathElements)
                        .containsEntry(lib.toString(), filenameDesc);

            } finally {
                Files.deleteIfExists(moduleInfo);
                Files.deleteIfExists(tempOut);
            }
        }

        @Test
        void whenLocationManagerThrows_ExceptionIsPropagated() throws Exception {
            classUnderTest.project = projectMock;

            Path tempOut = Files.createTempDirectory("init-test-out");
            Path moduleInfo = tempOut.resolve("module-info.class");
            Files.createFile(moduleInfo);

            try {
                Build build = new Build();
                build.setOutputDirectory(tempOut.toString());
                Mockito.when(projectMock.getBuild()).thenReturn(build);
                Mockito.when(projectMock.getDependencies()).thenReturn(List.of());
                Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of());

                IOException exception = new IOException("Failed to resolve paths");
                Mockito.when(locationManagerMock.resolvePaths(Mockito.any())).thenThrow(exception);

                assertThatExceptionOfType(MojoExecutionException.class)
                        .isThrownBy(() -> classUnderTest.init(null))
                        .withMessage("Error: Could not resolve paths")
                        .withCause(exception);

            } finally {
                Files.deleteIfExists(moduleInfo);
                Files.deleteIfExists(tempOut);
            }
        }
    }

    @Nested
    class IsWindowsOsTest {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void isWindowsOs_ReturnsExpectedResult(boolean expectedResult) {
            try (MockedStatic<Os> systemMock = Mockito.mockStatic(Os.class)) {
                systemMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(expectedResult);
                assertThat(classUnderTest.isWindowsOs())
                        .isEqualTo(expectedResult);
            }
        }
    }

    @Nested
    class HasWindowsNativeExtensionTest {

        @ParameterizedTest
        @CsvSource({
                "java,false",
                "java.cmd,false",
                "java.CMD,false",
                "java.exe,true",
                "java.EXE,true",
                "java.com,true",
                "java.COM,true",
        })
        void hasWindowsNativeExtension_ReturnsExpectedResult(String executable, boolean expectedResult)
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            // Arrange
            Method hasWindowsNativeExtensionMethod = BaseJavaFXMojo.class.getDeclaredMethod(
                    "hasWindowsNativeExtension", String.class
            );
            hasWindowsNativeExtensionMethod.setAccessible(true);

            // Act & Verify
            assertThat(hasWindowsNativeExtensionMethod.invoke(classUnderTest, executable))
                    .isEqualTo(expectedResult);
        }
    }

    @Nested
    class CreateAddModulesStringTest {

        @Test
        void testCreateAddModulesString_WithModuleDescriptor() {
            // Arrange
            JavaModuleDescriptor moduleDescriptor = Mockito.mock(JavaModuleDescriptor.class);
            Mockito.when(moduleDescriptor.name()).thenReturn("my.javafx.module");

            // Set moduleDescriptor
            classUnderTest.moduleDescriptor = moduleDescriptor;

            // Act
            String result = classUnderTest.createAddModulesString();

            // Assert
            assertThat(result)
                    .isEqualTo("my.javafx.module");
        }

        @Test
        void testCreateAddModulesString_WithPathElements() {
            // Arrange
            JavaModuleDescriptor javafxModule1 = Mockito.mock(JavaModuleDescriptor.class);
            JavaModuleDescriptor javafxModule2 = Mockito.mock(JavaModuleDescriptor.class);
            JavaModuleDescriptor nonJavafxModule = Mockito.mock(JavaModuleDescriptor.class);
            JavaModuleDescriptor javafxEmptyModule = Mockito.mock(JavaModuleDescriptor.class);

            Mockito.when(javafxModule1.name()).thenReturn("javafx.graphics");
            Mockito.when(javafxModule2.name()).thenReturn("javafx.controls");
            Mockito.when(nonJavafxModule.name()).thenReturn("some.other.module");
            Mockito.when(javafxEmptyModule.name()).thenReturn("javafx.Empty");

            Map<String, JavaModuleDescriptor> pathElements = new HashMap<>();
            pathElements.put("path1", javafxModule1);
            pathElements.put("path2", javafxModule2);
            pathElements.put("path3", nonJavafxModule);
            pathElements.put("path4", javafxEmptyModule);

            // Set pathElements
            classUnderTest.pathElements = pathElements;

            // Act
            String result = classUnderTest.createAddModulesString();

            // Assert
            assertThat(result)
                    .isEqualTo("javafx.graphics,javafx.controls");
        }

        @Test
        void testCreateAddModulesString_EmptyPathElementsAndModuleDescriptor() {
            // Arrange

            // Set empty values
            classUnderTest.moduleDescriptor = null;
            classUnderTest.pathElements = new HashMap<>();

            // Act
            String result = classUnderTest.createAddModulesString();

            // Assert
            assertThat(result)
                    .isEmpty();
        }
    }

    @Nested
    class HandleWorkingDirectoryTest {

        @Test
        void setsDefaultBaseDirectoryWhenWorkingDirectoryIsNull() throws Exception {
            // Arrange
            classUnderTest.baseDirectory = Files.createTempDirectory("base-directory");
            classUnderTest.workingDirectory = null;

            // Act
            classUnderTest.handleWorkingDirectory();

            // Assert
            assertThat(classUnderTest.workingDirectory)
                    .isEqualTo(classUnderTest.baseDirectory);
        }

        @Test
        void createsWorkingDirectoryWhenItDoesNotExist() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("test");
            Files.delete(tempDir); // Remove the temp dir to simulate non-existence
            classUnderTest.workingDirectory = tempDir;

            // Act
            classUnderTest.handleWorkingDirectory();

            // Assert
            assertThat(Files.exists(tempDir)).isTrue();

            // Cleanup
            Files.deleteIfExists(tempDir);
        }

        @Test
        void doesNotThrowExceptionIfWorkingDirectoryExists() throws Exception {
            // Arrange
            Path tempDir = Files.createTempDirectory("test");
            classUnderTest.workingDirectory = tempDir;

            // Act
            classUnderTest.handleWorkingDirectory();

            // Assert
            assertThat(Files.exists(tempDir)).isTrue();

            // Cleanup
            Files.deleteIfExists(tempDir);
        }

        @Test
        void directoryCreationFailed() {
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
                // Arrange
                files.when(() -> Files.exists(Mockito.any())).thenReturn(false);
                files.when(() -> Files.createDirectories(Mockito.any())).thenThrow(new IOException("Test exception"));
                classUnderTest.baseDirectory = Path.of("base-directory");
                classUnderTest.workingDirectory = classUnderTest.baseDirectory;

                // Act
                assertThatExceptionOfType(MojoExecutionException.class)
                        .isThrownBy(classUnderTest::handleWorkingDirectory)
                        .withMessage("Could not make working directory: '" + classUnderTest.baseDirectory.toAbsolutePath() + "'")
                        .havingCause()
                        .isExactlyInstanceOf(IOException.class)
                        .withMessage("Test exception");
            }
        }
    }

    @Nested
    class IsEmptyTest {

        @ParameterizedTest
        @NullAndEmptySource
        void testEmptyList(List<?> list) {
            assertThat(classUnderTest.isEmpty(list))
                    .isTrue();
        }

        @Test
        void testNonEmptyList() {
            assertThat(classUnderTest.isEmpty(List.of("alpha", "beta", "gamma")))
                    .isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void testEmptyMap(Map<?, ?> map) {
            assertThat(classUnderTest.isEmpty(map))
                    .isTrue();
        }

        @Test
        void testNonEmptyMap() {
            assertThat(classUnderTest.isEmpty(Map.of("alpha", "beta", "gamma", "delta")))
                    .isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void testEmptyString(String string) {
            assertThat(classUnderTest.isEmpty(string))
                    .isTrue();
        }

        @Test
        void testNonEmptyString() {
            assertThat(classUnderTest.isEmpty("TestString"))
                    .isFalse();
        }
    }

    @Nested
    class SplitComplexArgumentStringTest {

        @Test
        void splitsSimpleSpaceSeparatedArguments() {
            List<String> result = classUnderTest.splitComplexArgumentString("alpha beta gamma");
            assertThat(result).containsExactly("alpha", "beta", "gamma");
        }

        @Test
        void trimsLeadingAndTrailingWhitespace() {
            List<String> result = classUnderTest.splitComplexArgumentString("   alpha   beta   ");
            assertThat(result).containsExactly("alpha", "beta");
        }

        @Test
        void handlesTabsAndNewlinesAsWhitespace() {
            String input = "\talpha\tbeta\n gamma\r\ndelta";
            List<String> result = classUnderTest.splitComplexArgumentString(input);
            assertThat(result).containsExactly("alpha", "beta", "gamma", "delta");
        }

        @Test
        void preservesDoubleQuotedSubstringAsSingleToken_includingQuotes() {
            // Let op: implementatie behoudt aanhalingstekens in het resultaat
            List<String> result = classUnderTest.splitComplexArgumentString("run \"My App\"");
            assertThat(result).containsExactly("run", "\"My App\"");
        }

        @Test
        void preservesSingleQuotedSubstringAsSingleToken_includingQuotes() {
            List<String> result = classUnderTest.splitComplexArgumentString("run 'My App'");
            assertThat(result).containsExactly("run", "'My App'");
        }

        @Test
        void unbalancedQuotesAreReturnedAsIs() {
            // Ongebalanceerde quotes blijven onderdeel van het token
            List<String> result = classUnderTest.splitComplexArgumentString("run \"My App");
            assertThat(result).containsExactly("run", "\"My App");
        }

        @Test
        void mixedQuotedAndUnquotedArguments() {
            List<String> result = classUnderTest.splitComplexArgumentString("--flag \"value one\" 'value two' plain");
            assertThat(result).containsExactly("--flag", "\"value one\"", "'value two'", "plain");
        }

        @Test
        void emptyInputReturnsEmptyList() {
            List<String> result = classUnderTest.splitComplexArgumentString("");
            assertThat(result).isEmpty();
        }

        @Test
        void onlyWhitespaceReturnsEmptyList() {
            List<String> result = classUnderTest.splitComplexArgumentString("   \t  \n  ");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class CompareArtifactTest {

        @Mock
        Artifact artifact1;

        @Mock
        Artifact artifact2;

        @ParameterizedTest
        @CsvSource({
                "0,false,false,0",
                "0,false,true,-1",
                "0,true,false,1",
                "0,true,true,0",
                "-1,false,false,-1",
                "-1,false,true,-1",
                "-1,true,false,-1",
                "-1,true,true,-1",
                "1,false,false,1",
                "1,false,true,1",
                "1,true,false,1",
                "1,true,true,1",
        })
        void basedOnComparableStatus(int compareTo, boolean hasClassifier1, boolean hasClassifier2, int expectedResult)
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            // Arrange
            Mockito.when(artifact1.compareTo(artifact2))
                    .thenReturn(compareTo);
            if (compareTo == 0) {
                Mockito.when(artifact1.hasClassifier())
                        .thenReturn(hasClassifier1);
                Mockito.when(artifact2.hasClassifier())
                        .thenReturn(hasClassifier2);
            }

            Method compareArtifactMethod = BaseJavaFXMojo.class.getDeclaredMethod(
                    "compareArtifact", Artifact.class, Artifact.class
            );
            compareArtifactMethod.setAccessible(true);

            // Act
            assertThat(compareArtifactMethod.invoke(classUnderTest, artifact1, artifact2))
                    .isEqualTo(expectedResult);

            // Verify
            Mockito.verify(artifact1).compareTo(artifact2);
            VerificationMode verificationMode = compareTo == 0 ? Mockito.times(1) : Mockito.never();
            Mockito.verify(artifact1, verificationMode).hasClassifier();
            Mockito.verify(artifact2, verificationMode).hasClassifier();
        }
    }

    @Nested
    class GetDependenciesTest {

        @Mock
        MavenProject projectMock;

        @Mock
        Artifact artifactMock;

        @Test
        void returnsCorrectDependencies() {
            // Arrange
            classUnderTest.project = projectMock;

            Path outputPath = Path.of("target", "classes");
            Build build = new Build();
            build.setOutputDirectory(outputPath.toString());
            Mockito.when(projectMock.getBuild()).thenReturn(build);

            Dependency dependency = new Dependency();
            dependency.setSystemPath("libs/dependency.jar");
            Mockito.when(projectMock.getDependencies()).thenReturn(List.of(dependency));

            Mockito.when(artifactMock.getFile()).thenReturn(new File("libs/artifact.jar"));
            Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of(artifactMock));

            // Act
            List<Path> dependencies = classUnderTest.getDependencies();

            // Assert
            assertThat(dependencies)
                    .containsExactlyInAnyOrder(
                            outputPath,
                            Path.of("libs/dependency.jar"),
                            Path.of("libs/artifact.jar")
                    );
        }

        @Test
        void duplicatesReturnsCorrectSingleDependencies() {
            // Arrange
            classUnderTest.project = projectMock;

            Path outputPath = Path.of("target", "classes");
            Build build = new Build();
            build.setOutputDirectory(outputPath.toString());
            Mockito.when(projectMock.getBuild()).thenReturn(build);

            Dependency dependency = new Dependency();
            dependency.setSystemPath("libs/dependency.jar");
            Mockito.when(projectMock.getDependencies()).thenReturn(List.of(dependency));

            Mockito.when(artifactMock.getFile()).thenReturn(new File("libs/dependency.jar"));
            Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of(artifactMock));

            // Act
            List<Path> dependencies = classUnderTest.getDependencies();

            // Assert
            assertThat(dependencies)
                    .containsExactlyInAnyOrder(
                            outputPath,
                            Path.of("libs/dependency.jar")
                    );
        }

        @Test
        void returnsEmptyListWhenNoDependenciesAndOutputDirectory() {
            // Arrange
            classUnderTest.project = projectMock;

            Mockito.when(projectMock.getBuild()).thenReturn(null);
            Mockito.when(projectMock.getDependencies()).thenReturn(List.of());
            Mockito.when(projectMock.getArtifacts()).thenReturn(Set.of());

            // Act
            List<Path> dependencies = classUnderTest.getDependencies();

            // Assert
            assertThat(dependencies).isEmpty();
        }
    }

    @Nested
    class GetExecutableExtensionsTest {

        @Test
        void pathExtensionEmptyEnvironment_Default()
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            String pathExt = System.getProperty("PATHEXT");
            try {
                System.clearProperty("PATHEXT");
                Method getExecutableExtensionsMethod = BaseJavaFXMojo.class.getDeclaredMethod(
                        "getExecutableExtensions"
                );
                getExecutableExtensionsMethod.setAccessible(true);
                assertThat(getExecutableExtensionsMethod.invoke(classUnderTest))
                        .isEqualTo(List.of(".bat", ".cmd"));
            } finally {
                if (pathExt == null) {
                    System.clearProperty("PATHEXT");
                } else {
                    System.setProperty("PATHEXT", pathExt);
                }
            }
        }

        @Test
        void pathExtensionNotEmptyEnvironment()
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            String pathExt = ".exe%s.sh".formatted(File.pathSeparator);
            String storedPathExt = System.getProperty("PATHEXT");
            try {
                System.setProperty("PATHEXT", pathExt);
                Method getExecutableExtensionsMethod = BaseJavaFXMojo.class.getDeclaredMethod(
                        "getExecutableExtensions"
                );
                getExecutableExtensionsMethod.setAccessible(true);
                assertThat(getExecutableExtensionsMethod.invoke(classUnderTest))
                        .isEqualTo(List.of(".exe", ".sh"));
            } finally {
                if (storedPathExt == null) {
                    System.clearProperty("PATHEXT");
                } else {
                    System.setProperty("PATHEXT", storedPathExt);
                }
            }
        }
    }

    @Nested
    class HasExecutableExtensionTest {


        @ParameterizedTest
        @CsvSource({
                "java,false",
                "jave.exe,false",
                "java.bat,true",
                "java.cmd,true",
        })
        void withDefaultExtensions_ReturnExpected(String executable, boolean expectedResult)
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            String pathExt = System.getProperty("PATHEXT");
            try {
                System.clearProperty("PATHEXT");
                Method getExecutableExtensionsMethod = BaseJavaFXMojo.class.getDeclaredMethod(
                        "hasExecutableExtension", String.class
                );
                getExecutableExtensionsMethod.setAccessible(true);
                assertThat(getExecutableExtensionsMethod.invoke(classUnderTest, executable))
                        .isEqualTo(expectedResult);
            } finally {
                if (pathExt == null) {
                    System.clearProperty("PATHEXT");
                } else {
                    System.setProperty("PATHEXT", pathExt);
                }
            }
        }
    }

    @Nested
    class GetExecutableTest {

        @Test
        void returnsAbsolutePathWhenExecutableIsRegularFile() throws Exception {
            Path tempExe = Files.createTempFile("java-test-exe", ".bin");
            try {
                List<String> cmd = classUnderTest.getExecutable(tempExe.toString());
                assertThat(cmd)
                        .hasSize(1)
                        .containsExactly(tempExe.toAbsolutePath().toString());
            } finally {
                Files.deleteIfExists(tempExe);
            }
        }

        @Test
        void usesToolchainWhenAvailableAndExecutableIsNotAFile() {
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
                // Files.isRegularFile -> false, zodat pad-resolutie via toolchain gaat
                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);

                var toolchain = new TestToolchain("type", "/opt/jdk/bin/java");
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(toolchain);

                List<String> cmd = classUnderTest.getExecutable("java");

                assertThat(cmd).containsExactly("/opt/jdk/bin/java");
                Mockito.verify(toolchainManager).getToolchainFromBuildContext("jdk", classUnderTest.session);
            }
        }

        @Test
        void usesJavaHomeWhenPresentAndNoToolchain() {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);

                // toolchainManager levert niets op
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                Path fakeHome = Path.of("fake", "jdk", "home");
                System.setProperty("java.home", fakeHome.toString());

                List<String> cmd = classUnderTest.getExecutable("java");

                assertThat(cmd).containsExactly(fakeHome.resolve("bin").resolve("java").toString());
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void onWindows_usesPathLookupWhenJavaHomeEmpty() {
            String originalJavaHome = System.getProperty("java.home");
            String originalPath = System.getenv("PATH");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {

                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                // java.home leeg
                System.clearProperty("java.home");

                // Windows
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(true);

                // PATH-zoekpad
                String p1 = "tools";
                String p2 = "bin";
                String pathEnv = p1 + File.pathSeparator + p2;
                System.setProperty("PATH", pathEnv);

                // getExecutableExtensions() gebruikt System.getProperty("PATHEXT"), maar zonder die
                // property vallen we terug op .bat/.cmd. We laten default gedrag staan.

                // Simuleer dat D:\bin\java.cmd bestaat
                Path expected = Path.of(p2, "java.cmd");
                files.when(() -> Files.exists(Mockito.eq(expected))).thenReturn(true);
                // alle andere Files.exists false default
                files.when(() -> Files.exists(Mockito.any(Path.class))).thenAnswer(inv -> {
                    Path arg = inv.getArgument(0);
                    return expected.equals(arg);
                });

                List<String> cmd = classUnderTest.getExecutable("java");

                assertThat(cmd).containsExactly(expected.toString());
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
                if (originalPath == null) {
                    System.clearProperty("PATH");
                } else {
                    System.setProperty("PATH", originalPath);
                }
            }
        }

        @Test
        void onWindows_usesPathLookupWhenJavaHomeEmptyNotInPath() {
            String originalJavaHome = System.getProperty("java.home");
            String originalPath = System.getenv("PATH");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {

                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                // java.home leeg
                System.clearProperty("java.home");

                // Windows
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(true);

                // PATH-zoekpad
                String p1 = "tools";
                String p2 = "bin";
                String pathEnv = p1 + File.pathSeparator + p2;
                System.setProperty("PATH", pathEnv);

                files.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(false);

                List<String> cmd = classUnderTest.getExecutable("java.exe");

                assertThat(cmd).containsExactly("java.exe");
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
                if (originalPath == null) {
                    System.clearProperty("PATH");
                } else {
                    System.setProperty("PATH", originalPath);
                }
            }
        }

        @Test
        void onWindows_noLocationManagerUsesPathLookupWhenJavaHomeEmptyAndEmptyPath() {
            classUnderTest = new JavaFXRunMojo(locationManager, null);
            String originalJavaHome = System.getProperty("java.home");
            String originalPath = System.getenv("PATH");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {

                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);

                // java.home leeg
                System.clearProperty("java.home");

                // Windows
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(true);

                // PATH-zoekpad
                System.clearProperty("PATH");

                files.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(false);

                List<String> cmd = classUnderTest.getExecutable("java");

                assertThat(cmd).containsExactly("java");
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
                if (originalPath == null) {
                    System.clearProperty("PATH");
                } else {
                    System.setProperty("PATH", originalPath);
                }
            }
        }

        @Test
        void fallsBackToGivenExecutableWhenNothingFound() {
            String originalJavaHome = System.getProperty("java.home");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {
                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                // Niet-Windows zodat PATH pad niet gebruikt wordt
                System.clearProperty("java.home");
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(false);

                // java.home niet nodig; methode valt terug op het doorgegeven pad
                List<String> cmd = classUnderTest.getExecutable("java");
                assertThat(cmd).containsExactly("java");
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
            }
        }

        @Test
        void onWindows_wrapsWithCmdWhenScriptExtensionAndComSpecSet() {
            String originalJavaHome = System.getProperty("java.home");
            String originalComSpec = System.getenv("ComSpec");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {

                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                // Windows
                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(true);

                // java.home aanwezig zodat pad geconstrueerd wordt
                Path fakeHome = Path.of("C:\\Java\\Home");
                System.setProperty("java.home", fakeHome.toString());

                // ComSpec aanwezig
                String comSpec = "C:\\Windows\\System32\\cmd.exe";
                System.setProperty("ComSpec", comSpec);

                // Executable met script-extensie (.cmd) en geen native (.exe/.com) -> wrapping
                List<String> cmd = classUnderTest.getExecutable("java.cmd");

                assertThat(cmd).containsExactly(
                        comSpec, "/c",
                        fakeHome.resolve("bin").resolve("java.cmd").toString()
                );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
                if (originalComSpec == null) {
                    System.clearProperty("ComSpec");
                } else {
                    System.setProperty("ComSpec", originalComSpec);
                }
            }
        }

        @Test
        void onWindows_wrapsWithDefaultCmdWhenComSpecMissing() {
            String originalJavaHome = System.getProperty("java.home");
            String originalComSpec = System.getenv("ComSpec");
            try (MockedStatic<Files> files = Mockito.mockStatic(Files.class);
                 MockedStatic<Os> osMock = Mockito.mockStatic(Os.class)) {

                files.when(() -> Files.isRegularFile(Mockito.any(Path.class))).thenReturn(false);
                Mockito.when(toolchainManager.getToolchainFromBuildContext(Mockito.eq("jdk"), Mockito.isNull()))
                        .thenReturn(null);

                osMock.when(() -> Os.isFamily(Os.FAMILY_WINDOWS)).thenReturn(true);

                Path fakeHome = Path.of("C:\\Java\\Home");
                System.setProperty("java.home", fakeHome.toString());

                // ComSpec ontbreekt -> verwacht "cmd"
                System.clearProperty("ComSpec");

                List<String> cmd = classUnderTest.getExecutable("java.cmd");

                assertThat(cmd).containsExactly(
                        "cmd", "/c",
                        fakeHome.resolve("bin").resolve("java.cmd").toString()
                );
            } finally {
                if (originalJavaHome == null) {
                    System.clearProperty("java.home");
                } else {
                    System.setProperty("java.home", originalJavaHome);
                }
                if (originalComSpec == null) {
                    System.clearProperty("ComSpec");
                } else {
                    System.setProperty("ComSpec", originalComSpec);
                }
            }
        }
    }

    private record TestToolchain(String type, String findTool) implements Toolchain {
        @Override
        public String getType() {
            return type();
        }

        @Override
        public String findTool(String ignored) {
            return findTool();
        }
    }
}