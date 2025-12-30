package com.github.bsels.javafx.maven.plugin.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

/// A utility class that provides a singleton instance of the [ObjectMapper].
/// The `ObjectMapper` is pre-configured and can be reused across the application for JSON processing.
/// This class ensures centralized management of the [ObjectMapper] instance.
///
/// The [ObjectMapper] provides functionality for:
/// - Converting Java objects to JSON.
/// - Converting JSON back to Java objects.
///
/// The use of a single shared instance ensures consistency in JSON processing and avoids the overhead
/// of creating multiple [ObjectMapper] instances.
public final class ObjectMapperProvider {
    /// A static field that holds the singleton instance of the [ObjectMapper].
    ///
    /// This instance is initialized and accessed through the `getObjectMapper()` method of
    /// the `ObjectMapperProvider` class. It is designed to be used as the shared
    /// and centralized [ObjectMapper] for all JSON processing tasks across the application.
    ///
    /// The [ObjectMapper] provides functionality for:
    /// - Serializing Java objects into JSON.
    /// - Deserializing JSON into Java objects.
    ///
    /// By maintaining a single instance, this ensures consistent configuration and improves
    /// performance by avoiding the overhead of creating multiple [ObjectMapper] instances.
    private static ObjectMapper OBJECT_MAPPER;

    /// Private constructor to prevent instantiation of the [ObjectMapperProvider] class.
    /// This ensures that the class cannot be instantiated as it is designed to provide
    /// a singleton instance of [ObjectMapper].
    private ObjectMapperProvider() {
        throw new IllegalStateException("Cannot instantiate ObjectMapperProvider class");
    }

    /// Provides access to the singleton instance of [ObjectMapper].
    ///
    /// @return the shared static instance of [ObjectMapper].
    public static ObjectMapper getObjectMapper() {
        OBJECT_MAPPER = Optional.ofNullable(OBJECT_MAPPER)
                .orElseGet(() -> new ObjectMapper()
                        .registerModule(new Jdk8Module())
                        .registerModule(
                                new SimpleModule()
                                        .addSerializer(Type.class, new JsonSerializer<>() {
                                            @Override
                                            public void serialize(
                                                    Type type,
                                                    JsonGenerator jsonGenerator,
                                                    SerializerProvider serializerProvider
                                            ) throws IOException {
                                                jsonGenerator.writeString(type.toString());
                                            }
                                        })
                        ));
        return OBJECT_MAPPER;
    }
}
