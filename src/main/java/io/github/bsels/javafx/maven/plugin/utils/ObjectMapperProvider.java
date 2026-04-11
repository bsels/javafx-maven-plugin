package io.github.bsels.javafx.maven.plugin.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Optional;

/// Utility class providing a singleton [ObjectMapper] for JSON processing.
public final class ObjectMapperProvider {
    /// Singleton [ObjectMapper] instance.
    private static ObjectMapper OBJECT_MAPPER;
    /// Singleton [ObjectWriter] instance with pretty printing.
    private static ObjectWriter OBJECT_WRITER;

    /// Private constructor to prevent instantiation.
    private ObjectMapperProvider() {
        throw new IllegalStateException("Cannot instantiate ObjectMapperProvider class");
    }

    /// Returns the singleton [ObjectMapper].
    ///
    /// @return The shared [ObjectMapper] instance
    public static ObjectMapper getObjectMapper() {
        OBJECT_MAPPER = Optional.ofNullable(OBJECT_MAPPER)
                .orElseGet(
                        () -> new ObjectMapper()
                                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                .registerModule(new Jdk8Module())
                                .registerModule(new SimpleModule().addSerializer(Type.class, new TypeJsonSerializer()))
                );
        return OBJECT_MAPPER;
    }

    /// Returns the shared [ObjectWriter].
    ///
    /// @return The shared [ObjectWriter] instance
    private static ObjectWriter getWriter() {
        OBJECT_WRITER = Optional.ofNullable(OBJECT_WRITER)
                .orElseGet(() -> getObjectMapper().writerWithDefaultPrettyPrinter());
        return OBJECT_WRITER;
    }

    /// Encodes the object as a JSON-compliant string.
    ///
    /// @param value The object to encode
    /// @return The JSON string
    /// @throws IllegalArgumentException If encoding fails
    public static String encodeObject(Object value) {
        try {
            return ObjectMapperProvider.getObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to escape the object value", e);
        }
    }

    /// Converts the object to a pretty-printed JSON string.
    ///
    /// @param value The object to convert
    /// @return The pretty-printed JSON string
    /// @throws IllegalArgumentException If printing fails
    public static String prettyPrint(Object value) {
        try {
            return getWriter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to pretty print the object value", e);
        }
    }

    /// Custom serializer for the [Type] class.
    private static class TypeJsonSerializer extends JsonSerializer<Type> {

        /// Initializes a new [TypeJsonSerializer] instance.
        private TypeJsonSerializer() {
        }

        /// Serializes a [Type] object into its string representation.
        ///
        /// @param type               The [Type] object to serialize
        /// @param jsonGenerator      The [JsonGenerator]
        /// @param serializerProvider The [SerializerProvider]
        /// @throws IOException If serialization fails
        @Override
        public void serialize(Type type, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeString(type.toString());
        }
    }
}
