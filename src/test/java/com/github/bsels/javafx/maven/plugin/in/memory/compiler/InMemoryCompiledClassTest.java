package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InMemoryCompiledClassTest {

    @Nested
    class ConstructorTests {

        @Test
        public void nullClassName_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> new InMemoryCompiledClass(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`className` must not be null");
        }

        @Test
        public void validClassName_Valid() {
            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> new InMemoryCompiledClass("com.example.MyClass"));
        }
    }

    @Nested
    class EasyGetterTests {

        @Test
        public void fullyQualifiedName_ReturnsCorrectClassName() {
            // Arrange
            String expectedClassName = "com.example.MyClass";
            InMemoryCompiledClass compiledClass = new InMemoryCompiledClass(expectedClassName);

            // Act & Assert
            assertThat(compiledClass)
                    .returns(expectedClassName, InMemoryCompiledClass::getClassName)
                    .returns("InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(expectedClassName, 0), InMemoryCompiledClass::toString)
                    .returns(URI.create("jvm-memory:///com/example/MyClass%s".formatted(JavaFileObject.Kind.CLASS.extension)), InMemoryCompiledClass::toUri);
        }

        @Test
        public void simpleClassName_SpecificEdgeCaseClassName() {
            // Arrange
            String expectedClassName = "MyClass";
            InMemoryCompiledClass compiledClass = new InMemoryCompiledClass(expectedClassName);

            // Act & Assert
            assertThat(compiledClass)
                    .returns(expectedClassName, InMemoryCompiledClass::getClassName)
                    .returns("InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(expectedClassName, 0), InMemoryCompiledClass::toString)
                    .returns(URI.create("jvm-memory:///MyClass%s".formatted(JavaFileObject.Kind.CLASS.extension)), InMemoryCompiledClass::toUri);
        }
    }

    @Nested
    class MemoryTests {

        @Test
        public void inputDate_ReturnSameData() {
            // Arrange
            String expectedClassName = "com.example.MyClass";
            InMemoryCompiledClass compiledClass = new InMemoryCompiledClass(expectedClassName);

            byte[] inputData = new byte[]{1, 2, 3};

            // Act
            compiledClass.openOutputStream()
                    .writeBytes(inputData);

            //
            assertThat(compiledClass)
                    .returns("InMemoryCompiledClass{className='%s', compiledSize=%d}".formatted(expectedClassName, inputData.length), InMemoryCompiledClass::toString)
                    .satisfies(
                            c -> assertThat(c.getBytes())
                                    .isNotEmpty()
                                    .containsExactly(inputData)
                    );
        }
    }
}