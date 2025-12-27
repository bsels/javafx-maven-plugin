package com.github.bsels.javafx.maven.plugin.utils;

import com.github.bsels.javafx.maven.plugin.fxml.FXMLConstantNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLObjectNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLStaticMethod;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLValueNode;
import com.github.bsels.javafx.maven.plugin.fxml.FXMLWrapperNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

/// Utility class for encoding values based on their types into specific string formats.
///
/// This class provides static methods intended to perform type-specific encoding.
/// The class is not designed for instantiation and should be used in a static context.
public final class TypeEncoder {
    /// Private constructor for the TypeEncoder class.
    ///
    /// This constructor is intentionally private to prevent instantiation of the [TypeEncoder] class,
    /// as it is designed to provide only static utility methods for type encoding functionalities.
    private TypeEncoder() {
    }

    /// Encodes a given value based on its type into a specific format.
    ///
    /// This method takes a type and its corresponding value, performs checks on the type,
    /// and returns the encoded representation of the value suitable for the type.
    ///
    /// @param type  the type of the value to be encoded; may be a class, parameterized type, or other type information
    /// @param value the value to encode, provided as a string
    /// @return the encoded string representation of the value, formatted to match the expected type
    /// @throws IllegalArgumentException if the type cannot be encoded or is unrecognized
    public static String encodeTypeValue(Type type, String value) {
        if (value.startsWith("$")) {
            return value.substring(1);
        }
        boolean isParameterizedType = false;
        if (type instanceof ParameterizedType parameterizedType) {
            isParameterizedType = true;
            type = parameterizedType.getRawType();
        }
        if (type instanceof Class<?> clazz) {
            if (clazz.isInterface()) {
                if (value.startsWith("#")) {
                    return "this::%s".formatted(value.substring(1));
                } else {
                    throw new IllegalArgumentException("Unexpected value for interface type: %s".formatted(type));
                }
            }
            if (CharSequence.class.isAssignableFrom(clazz)) {
                if (value.startsWith("%")) {
                    return "RESOURCE_BUNDLE.getString(%s)".formatted(escapeString(value.substring(1)));
                }
                return escapeString(value);
            }
            if (char.class.equals(clazz) || Character.class.equals(clazz)) {
                String escaped = escapeString(value);
                return "'" + escaped.substring(1, escaped.length() - 1) + "'";
            }
            if (clazz.isEnum()) {
                return clazz.getSimpleName() + "." + value;
            }
            if (double.class.equals(clazz) || Double.class.equals(clazz)) {
                if ("-Infinity".equals(value)) {
                    return "Double.NEGATIVE_INFINITY";
                } else if ("Infinity".equals(value)) {
                    return "Double.POSITIVE_INFINITY";
                }
                return value;
            }
            if (int.class.equals(clazz) || Integer.class.equals(clazz)
                    || boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
                return value;
            }
            if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                if ("-Infinity".equals(value)) {
                    return "Float.NEGATIVE_INFINITY";
                } else if ("Infinity".equals(value)) {
                    return "Float.POSITIVE_INFINITY";
                }
                return value + "f";
            }
            if (long.class.equals(clazz) || Long.class.equals(clazz)) {
                return value + "L";
            }
            if (short.class.equals(clazz) || Short.class.equals(clazz)) {
                return "(short) " + value;
            }
            if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
                return "(byte) " + value;
            }
            try {
                clazz.getConstructor(String.class);
                if (isParameterizedType) {
                    return "new %s<>(%s)".formatted(clazz.getSimpleName(), value);
                }
                return "new %s(%s)".formatted(clazz.getSimpleName(), escapeString(value));
            } catch (NoSuchMethodException _) {
            }
        }
        throw new IllegalArgumentException("Unable to encode type value: %s".formatted(type));
    }

    /// Converts a given [Type] object to its corresponding string representation.
    ///
    /// @param type the [Type] object to be converted; must be an instance of [Class] or [TypeVariable]
    /// @return a string representation of the input type; the simple name if the type is a [Class],
    /// or the name if the type is a [TypeVariable]
    /// @throws IllegalArgumentException if the provided type is unsupported
    public static String typeToTypeString(Type type) {
        return typeToTypeString(type, Map.of());
    }

    /// Converts a given Type object to its corresponding string representation.
    /// The method processes different types of Type objects, including Class, TypeVariable,
    /// and ParameterizedType, and generates a string representation based on the type's structure.
    ///
    /// @param type          the Type object to be converted; it may represent a class, a type variable, or a parameterized type
    /// @param namedGenerics a map containing named generics for type variables, where the key is the name of the type variable, and the value is its replacement string
    /// @return a string representation of the type, where classes are represented by their simple names,
    /// type variables by their names or mapped strings, and parameterized types by combining the raw type and their
    /// type arguments
    /// @throws IllegalArgumentException if the provided type is unsupported
    public static String typeToTypeString(Type type, Map<String, String> namedGenerics) {
        return switch (type) {
            case Class<?> c -> c.getSimpleName();
            case TypeVariable<?> typeVariable ->
                    namedGenerics.getOrDefault(typeVariable.getName(), typeVariable.getName());
            case ParameterizedType parameterizedType -> {
                StringBuilder builder = new StringBuilder()
                        .append(typeToTypeString(parameterizedType.getRawType(), namedGenerics))
                        .append("<");
                boolean first = true;
                for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(typeToTypeString(typeArgument, namedGenerics));
                    first = false;
                }
                yield builder.append(">")
                        .toString();
            }
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    /// Converts a given Type object to its corresponding [Class] object.
    /// The method processes instances of [Class] and [ParameterizedType],
    /// and returns the raw class type. Unsupported types will throw an exception.
    ///
    /// @param type the Type object to be converted; must be an instance of [Class] or [ParameterizedType]
    /// @return the equivalent [Class] object of the provided type
    /// @throws IllegalArgumentException if the provided type is unsupported
    public static Class<?> typeToClass(Type type) {
        return switch (type) {
            case Class<?> c -> c;
            case ParameterizedType parameterizedType -> (Class<?>) parameterizedType.getRawType();
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    /// Converts a given [Type] object to the fully qualified class name of its corresponding raw type.
    /// If the type is a [Class], it directly returns the class's fully qualified name.
    /// If the type is a [ParameterizedType], it recursively processes the raw type of the parameterized type.
    /// Unsupported types will result in an exception.
    ///
    /// @param type the [Type] object to be converted; it must either be an instance of [Class] or [ParameterizedType]
    /// @return the fully qualified class name of the type if it is supported with the `.class` suffix, such as `java.lang.String.class`
    /// @throws IllegalArgumentException if the provided type is unsupported
    public static String typeToReflectionClassString(Type type) {
        return switch (type) {
            case Class<?> c -> c.getName();
            case ParameterizedType parameterizedType -> typeToReflectionClassString(parameterizedType.getRawType());
            default -> throw new IllegalArgumentException("Unsupported type: " + type);
        } + ".class";
    }

    /// Returns a default value as a string for the given primitive type or null for non-primitive types.
    ///
    /// @param type the class object representing the type for which the default value is to be determined
    /// @return a string representation of the default value for the specified type, such as "false" for boolean, "0" for int, or "null" for non-primitive types
    public static String defaultValueAsString(Class<?> type) {
        if (boolean.class.equals(type)) {
            return "false";
        } else if (char.class.equals(type)) {
            return "'\\0'";
        } else if (double.class.equals(type)) {
            return "0.0";
        } else if (float.class.equals(type)) {
            return "0.0f";
        } else if (byte.class.equals(type)) {
            return "(byte) 0";
        } else if (int.class.equals(type)) {
            return "0";
        } else if (long.class.equals(type)) {
            return "0L";
        } else if (short.class.equals(type)) {
            return "(short) 0";
        } else {
            return "null";
        }
    }

    /// Retrieves the identifier string associated with the given FXML node.
    ///
    /// @param fxmlNode the FXML node whose identifier is to be fetched
    /// @return the identifier string if the node type has an identifier,
    /// or a qualified identifier if the node is an [FXMLConstantNode]
    /// @throws IllegalStateException if the provided FXML node is an [FXMLWrapperNode]
    public static String getIdentifier(FXMLNode fxmlNode) {
        return switch (fxmlNode) {
            case FXMLObjectNode(_, String identifier, _, _, _, _) -> identifier;
            case FXMLValueNode(_, String identifier, _, _) -> identifier;
            case FXMLConstantNode(Class<?> clazz, String identifier, _) -> clazz.getSimpleName() + "." + identifier;
            case FXMLWrapperNode _, FXMLStaticMethod _ -> throw new IllegalStateException("Unexpected child node");
        };
    }

    /// Escapes the provided string by converting it into a JSON-compliant format.
    ///
    /// @param value the string to be escaped
    /// @return the escaped string in JSON-compliant format
    /// @throws IllegalArgumentException if the string cannot be escaped
    private static String escapeString(String value) {
        try {
            return ObjectMapperProvider.getObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to escape the string value", e);
        }
    }
}
