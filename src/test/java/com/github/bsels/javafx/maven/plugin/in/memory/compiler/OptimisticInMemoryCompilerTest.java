package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class OptimisticInMemoryCompilerTest {
    private static final Path DIAGNOSTIC_FILE_1_PATH = Path.of("diagnostic-file-1.java").toAbsolutePath();
    private static final Path DIAGNOSTIC_FILE_3_PATH = Path.of("diagnostic-file-3.java").toAbsolutePath();

    private static final String MY_TEST_DUMMY_PARENT = "my.test.DummyParent";
    private static final String MY_TEST_DUMMY_CHILD = "my.test.DummyChild";

    @Mock
    DiagnosticCollector<JavaFileObject> diagnosticCollectorMock;

    @Mock
    Diagnostic<JavaFileObject> diagnostic0NoteMock;
    @Mock
    Diagnostic<JavaFileObject> diagnostic1ErrorMock;
    @Mock
    Diagnostic<JavaFileObject> diagnostic2WarningMock;
    @Mock
    Diagnostic<JavaFileObject> diagnostic3OtherMock;
    @Mock
    Diagnostic<JavaFileObject> diagnostic4MandatoryWarningMock;

    @Mock
    JavaFileObject javaFileObject1Mock;
    @Mock
    JavaFileObject javaFileObject3Mock;

    private Log log;
    private OptimisticInMemoryCompiler classUnderTest;
    private Path rootTestFolderPath;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        classUnderTest = new OptimisticInMemoryCompiler();
        rootTestFolderPath = Path.of(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("in-memory-compiler/data.txt")
                        .toURI()
        ).getParent();

        log = new SystemStreamLog();
    }

    @Nested
    class GetSourceFilesTest {

        @Test
        @SuppressWarnings("unchecked")
        void emptySourceFolders_ReturnsEmptyList() throws NoSuchMethodException, InvocationTargetException,
                IllegalAccessException {
            // Arrange
            Method method = OptimisticInMemoryCompiler.class.getDeclaredMethod("getSourceFiles", List.class);
            method.setAccessible(true);

            // Act
            List<Path> actual = (List<Path>) method.invoke(classUnderTest, List.of());

            // Assert
            assertThat(actual)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void nonExistingFolder_ReturnsEmptyList() throws NoSuchMethodException, InvocationTargetException,
                IllegalAccessException {
            // Arrange
            Method method = OptimisticInMemoryCompiler.class.getDeclaredMethod("getSourceFiles", List.class);
            method.setAccessible(true);

            // Act
            List<Path> actual = (List<Path>) method.invoke(classUnderTest, List.of(rootTestFolderPath.resolve("non-existing")));

            // Assert
            assertThat(actual)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        void existingFolder_ReturnsOnlyPureJavaSourceFiles() throws NoSuchMethodException, InvocationTargetException,
                IllegalAccessException {
            // Arrange
            Method method = OptimisticInMemoryCompiler.class.getDeclaredMethod("getSourceFiles", List.class);
            method.setAccessible(true);

            // Act
            List<Path> actual = (List<Path>) method.invoke(classUnderTest, List.of(rootTestFolderPath));

            // Assert
            assertThat(actual)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(8)
                    .containsExactlyInAnyOrder(
                            rootTestFolderPath.resolve("invalid/my/test/DummyChild.java"),
                            rootTestFolderPath.resolve("invalid/my/test/DummyParent.java"),
                            rootTestFolderPath.resolve("partial-valid/my/test/DummyChild.java"),
                            rootTestFolderPath.resolve("partial-valid/my/test/DummyParent.java"),
                            rootTestFolderPath.resolve("valid/my/test/DummyChild.java"),
                            rootTestFolderPath.resolve("valid/my/test/DummyParent.java"),
                            rootTestFolderPath.resolve("mixin/child/my/test/DummyChild.java"),
                            rootTestFolderPath.resolve("mixin/parent/my/test/DummyParent.java")
                    );
        }
    }

    @Nested
    class GetFailedFilesFromDiagnosticsTest {

        @BeforeEach
        void setUp() throws URISyntaxException {
            OptimisticInMemoryCompilerTest.this.setUp();

            Mockito.when(diagnostic0NoteMock.getKind())
                    .thenReturn(Diagnostic.Kind.NOTE);
            Mockito.when(diagnostic1ErrorMock.getKind())
                    .thenReturn(Diagnostic.Kind.ERROR);
            Mockito.when(diagnostic2WarningMock.getKind())
                    .thenReturn(Diagnostic.Kind.WARNING);
            Mockito.when(diagnostic3OtherMock.getKind())
                    .thenReturn(Diagnostic.Kind.OTHER);
            Mockito.when(diagnostic4MandatoryWarningMock.getKind())
                    .thenReturn(Diagnostic.Kind.MANDATORY_WARNING);

            Mockito.when(diagnostic1ErrorMock.getSource())
                    .thenReturn(javaFileObject1Mock);
            Mockito.when(diagnostic3OtherMock.getSource())
                    .thenReturn(javaFileObject3Mock);

            Mockito.when(javaFileObject1Mock.toUri())
                    .thenReturn(DIAGNOSTIC_FILE_1_PATH.toUri());
            Mockito.when(javaFileObject3Mock.toUri())
                    .thenReturn(DIAGNOSTIC_FILE_3_PATH.toUri());

            Mockito.when(diagnosticCollectorMock.getDiagnostics())
                    .thenReturn(List.of(diagnostic0NoteMock, diagnostic1ErrorMock, diagnostic2WarningMock,
                            diagnostic3OtherMock, diagnostic4MandatoryWarningMock));
        }

        @Test
        @SuppressWarnings("unchecked")
        void processDiagnosticsFromCollector_OnlyKeepOtherAndErrorPaths() throws NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
            // Arrange
            Method method = OptimisticInMemoryCompiler.class.getDeclaredMethod("getFailedFilesFromDiagnostics", DiagnosticCollector.class);
            method.setAccessible(true);

            // Act
            Set<Path> actual = (Set<Path>) method.invoke(classUnderTest, diagnosticCollectorMock);

            // Assert
            assertThat(actual)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(2)
                    .containsExactlyInAnyOrder(DIAGNOSTIC_FILE_1_PATH, DIAGNOSTIC_FILE_3_PATH);
        }
    }

    @Nested
    class OptimisticCompileTest {

        @ParameterizedTest
        @NullAndEmptySource
        void noSourceFolders_ReturnEmpty(List<Path> sourceFolders) throws IOException {
            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void nonExistingSourceFolder_ReturnEmpty() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("non-existing"));

            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void onlyNonCompilableSources_ReturnEmpty() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("invalid"));

            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void partiallyCompilableSources_OnlyReturnParent() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("partial-valid"));

            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(1)
                    .hasEntrySatisfying(MY_TEST_DUMMY_PARENT, compiledClass -> assertThat(compiledClass)
                            .hasFieldOrPropertyWithValue("className", MY_TEST_DUMMY_PARENT));
        }

        @Test
        void fullyCompilableSources_ReturnBothParentAndChild() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("valid"));

            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(2)
                    .hasEntrySatisfying(MY_TEST_DUMMY_PARENT, compiledClass -> assertThat(compiledClass)
                            .hasFieldOrPropertyWithValue("className", MY_TEST_DUMMY_PARENT))
                    .hasEntrySatisfying(MY_TEST_DUMMY_CHILD, compiledClass -> assertThat(compiledClass)
                            .hasFieldOrPropertyWithValue("className", MY_TEST_DUMMY_CHILD));
        }

        @Test
        void multipleSourceRoots_ReturnBothParentAndChild() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(
                    rootTestFolderPath.resolve("mixin").resolve("child"),
                    rootTestFolderPath.resolve("mixin").resolve("parent")
            );

            // Act & Assert
            assertThat(classUnderTest.optimisticCompile(log, sourceFolders))
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(2)
                    .hasEntrySatisfying(MY_TEST_DUMMY_PARENT, compiledClass -> assertThat(compiledClass)
                            .hasFieldOrPropertyWithValue("className", MY_TEST_DUMMY_PARENT))
                    .hasEntrySatisfying(MY_TEST_DUMMY_CHILD, compiledClass -> assertThat(compiledClass)
                            .hasFieldOrPropertyWithValue("className", MY_TEST_DUMMY_CHILD));
        }
    }

    @Nested
    class OptimisticCompileIntoClassLoaderTest {

        @Test
        void fullyNonCompilableSources_ClassesAreNotAccessible() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("invalid"));

            // Act
            UnaryOperator<ClassLoader> classLoaderCreator = classUnderTest.optimisticCompileIntoClassLoader(log, sourceFolders);
            ClassLoader classLoader = classLoaderCreator.apply(Thread.currentThread().getContextClassLoader());

            // Assert
            assertThatThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_PARENT))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessage(MY_TEST_DUMMY_PARENT);
            assertThatThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_CHILD))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessage(MY_TEST_DUMMY_CHILD);
        }

        @Test
        void partiallyCompilableSources_ParentClassIsAccessibleChildClassIsNotAccessible() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("partial-valid"));

            // Act
            UnaryOperator<ClassLoader> classLoaderCreator = classUnderTest.optimisticCompileIntoClassLoader(log, sourceFolders);
            ClassLoader classLoader = classLoaderCreator.apply(Thread.currentThread().getContextClassLoader());

            // Assert
            assertThatNoException()
                    .isThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_PARENT));
            assertThatThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_CHILD))
                    .isInstanceOf(ClassNotFoundException.class)
                    .hasMessage(MY_TEST_DUMMY_CHILD);
        }

        @Test
        void fullyCompilableSources_ClassesAreAccessible() throws IOException {
            // Arrange
            List<Path> sourceFolders = List.of(rootTestFolderPath.resolve("valid"));

            // Act
            UnaryOperator<ClassLoader> classLoaderCreator = classUnderTest.optimisticCompileIntoClassLoader(log, sourceFolders);
            ClassLoader classLoader = classLoaderCreator.apply(Thread.currentThread().getContextClassLoader());

            // Assert
            assertThatNoException()
                    .isThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_PARENT));
            assertThatNoException()
                    .isThrownBy(() -> classLoader.loadClass(MY_TEST_DUMMY_CHILD));
        }
    }
}
