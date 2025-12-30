package com.github.bsels.javafx.maven.plugin.in.memory.compiler;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InMemoryClassLoaderTest {
    private static final String DUMMY_CLASS_NAME = "my.test.DummyTestClass";
    private static final String DUMMY_METHOD_NAME = "dummyMethod";
    private static final int EXPECTED_RETURN_VALUE = 42;

    private static final String BASE64_ENCODED_CLASS_BYTES = """
            yv66vgAAAEUAEgoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAWbXkvdGVzdC9EdW1teVRlc3R\
            DbGFzcwEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQAYTG15L3Rlc3QvRHVtbXlUZXN0Q2\
            xhc3M7AQALZHVtbXlNZXRob2QBAAMoKUkBAApTb3VyY2VGaWxlAQATRHVtbXlUZXN0Q2xhc3MuamF2YQAhAAcAAgAAAAAAAgABAAUABgABA\
            AkAAAAvAAEAAQAAAAUqtwABsQAAAAIACgAAAAYAAQAAAAMACwAAAAwAAQAAAAUADAANAAAAAQAOAA8AAQAJAAAALQABAAEAAAADECqsAAAA\
            AgAKAAAABgABAAAABgALAAAADAABAAAAAwAMAA0AAAABABAAAAACABE=\
            """;
    private static final byte[] CLASS_BYTES = Base64.getDecoder().decode(BASE64_ENCODED_CLASS_BYTES);

    @Test
    public void parentIsNull_ThrowsNullPointerException() {
        // Act & Assert
        assertThatThrownBy(() -> new InMemoryClassLoader(Map.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("`parent` cannot be null");
    }

    @Test
    public void compiledClassesIsNull_ThrownNullPointerException() {
        // Act & Assert
        assertThatThrownBy(() -> new InMemoryClassLoader(null, Thread.currentThread().getContextClassLoader()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("`compiledClasses` cannot be null");
    }

    @Test
    public void noInMemoryCompiledClasses_ThrowClassNotFoundException() {
        // Arrange
        InMemoryClassLoader classLoader = new InMemoryClassLoader(Map.of(), Thread.currentThread().getContextClassLoader());

        // Act & Assert
        assertThatThrownBy(() -> classLoader.loadClass(DUMMY_CLASS_NAME))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessage(DUMMY_CLASS_NAME);
    }

    @Test
    public void withInMemoryCompiledClass_FindClassAndIsUsable() throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        // Arrange
        InMemoryCompiledClass inMemoryCompiledClass = new InMemoryCompiledClass(DUMMY_CLASS_NAME);
        inMemoryCompiledClass.openOutputStream()
                .writeBytes(CLASS_BYTES);
        Map<String, InMemoryCompiledClass> compiledClasses = Map.of(DUMMY_CLASS_NAME, inMemoryCompiledClass);

        InMemoryClassLoader classLoader = new InMemoryClassLoader(compiledClasses, Thread.currentThread().getContextClassLoader());

        // Act & Assert
        assertThatNoException()
                .isThrownBy(() -> classLoader.loadClass(DUMMY_CLASS_NAME));

        Class<?> clazz = classLoader.loadClass(DUMMY_CLASS_NAME);
        Method method = clazz.getMethod(DUMMY_METHOD_NAME);
        Constructor<?> defaultConstructor = clazz.getDeclaredConstructor();

        Object instance = defaultConstructor.newInstance();
        assertThat(method.invoke(instance))
                .isEqualTo(EXPECTED_RETURN_VALUE);
    }
}
