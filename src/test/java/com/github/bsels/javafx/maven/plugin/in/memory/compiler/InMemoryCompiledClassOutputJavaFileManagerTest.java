package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class InMemoryCompiledClassOutputJavaFileManagerTest {

    @Mock
    JavaFileManager javaFileManagerMock;

    @Mock
    JavaFileManager.Location locationMock;

    @Mock
    FileObject siblingMock;

    @Mock
    JavaFileObject javaFileObjectMock;

    @Test
    public void nullFileManager_ThrowsNullPointer() {
        // Act & Assert
        assertThatThrownBy(() -> new InMemoryCompiledClassOutputJavaFileManager(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("`javaFileManager` cannot be null");
    }

    @Test
    public void emptyInitialized_ReturnNoCompiledClasses() {
        // Arrange
        InMemoryCompiledClassOutputJavaFileManager classFileManager = new InMemoryCompiledClassOutputJavaFileManager(javaFileManagerMock);

        // Act & Assert
        assertThat(classFileManager)
                .hasFieldOrPropertyWithValue("compiledClasses", Map.of());
    }

    @ParameterizedTest
    @EnumSource(value = JavaFileObject.Kind.class, mode = EnumSource.Mode.EXCLUDE, names = "CLASS")
    public void nonClassType_DelegateToParent(JavaFileObject.Kind kind) throws IOException {
        // Arrange
        String className = "my.test.ClassName";
        InMemoryCompiledClassOutputJavaFileManager classFileManager = new InMemoryCompiledClassOutputJavaFileManager(javaFileManagerMock);
        Mockito.when(javaFileManagerMock.getJavaFileForOutput(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(javaFileObjectMock);

        // Act
        JavaFileObject actual = classFileManager.getJavaFileForOutput(locationMock, className, kind, siblingMock);

        // Assert
        assertThat(actual)
                .isSameAs(javaFileObjectMock);

        Mockito.verify(javaFileManagerMock)
                .getJavaFileForOutput(locationMock, className, kind, siblingMock);
        Mockito.verifyNoMoreInteractions(javaFileManagerMock);
    }

    @Test
    public void classType_ReturnInMemoryCompiledCode() throws IOException {
        // Arrange
        String className = "my.test.ClassName";
        InMemoryCompiledClassOutputJavaFileManager classFileManager = new InMemoryCompiledClassOutputJavaFileManager(javaFileManagerMock);


        // Act
        JavaFileObject actual = classFileManager.getJavaFileForOutput(locationMock, className, JavaFileObject.Kind.CLASS, siblingMock);

        // Assert
        assertThat(actual)
                .isInstanceOf(InMemoryCompiledClass.class)
                .hasFieldOrPropertyWithValue("className", className);

        Mockito.verifyNoInteractions(javaFileManagerMock);
    }
}
