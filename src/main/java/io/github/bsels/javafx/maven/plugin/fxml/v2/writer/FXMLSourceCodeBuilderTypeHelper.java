package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import io.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;
import io.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider;

import java.util.List;
import java.util.Map;

final class FXMLSourceCodeBuilderTypeHelper {
    private static final FXMLClassType OBJECT_TYPE = new FXMLClassType(Object.class);
    private static final FXMLClassType STRING_TYPE = new FXMLClassType(String.class);

    FXMLSourceCodeBuilderTypeHelper() {
    }

    public FXMLClassType getTypeForMapEntry(FXMLClassType type) {
        return OBJECT_TYPE.equals(type) ? STRING_TYPE : type;
    }

    public String encodeLiteral(SourceCodeGeneratorContext context, String value, FXMLType type) {
        if (type instanceof FXMLClassType(Class<?> clazz)) {
            if (clazz.isAssignableFrom(String.class)) {
                return encodeString(value);
            }
            if (clazz.isEnum()) {
                return "%s.%s".formatted(typeToSourceCode(context, type), value);
            }
            if (char.class.equals(clazz) || Character.class.equals(clazz)) {
                String valueString = value.strip();
                if (valueString.length() == 3) {
                    return "'%s'".formatted(valueString.charAt(1));
                }
                throw new IllegalArgumentException("Character literals must be exactly 1 character long");
            }
            if (double.class.equals(clazz) || Double.class.equals(clazz)) {
                if ("-Infinity".equalsIgnoreCase(value)) {
                    return "Double.NEGATIVE_INFINITY";
                }
                if ("Infinity".equalsIgnoreCase(value)) {
                    return "Double.POSITIVE_INFINITY";
                }
                if ("NaN".equalsIgnoreCase(value)) {
                    return "Double.NaN";
                }
                return value;
            }
            if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                if ("-Infinity".equalsIgnoreCase(value)) {
                    return "Float.NEGATIVE_INFINITY";
                }
                if ("Infinity".equalsIgnoreCase(value)) {
                    return "Float.POSITIVE_INFINITY";
                }
                if ("NaN".equalsIgnoreCase(value)) {
                    return "Float.NaN";
                }
                return "%sf".formatted(value);
            }
            if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
                if ("true".equalsIgnoreCase(value)) {
                    return "true";
                }
                return "false";
            }
            if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
                return "(byte) %s".formatted(value);
            }
            if (short.class.equals(clazz) || Short.class.equals(clazz)) {
                return "(short) %s".formatted(value);
            }
            if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
                return value;
            }
            if (long.class.equals(clazz) || Long.class.equals(clazz)) {
                return "%sL".formatted(value);
            }
        }
        return "%s.valueOf(%s)".formatted(typeToSourceCode(context, type), encodeString(value));
    }

    public String encodeString(String translationKey) {
        return ObjectMapperProvider.encodeObject(translationKey);
    }

    public String typeToSourceCode(SourceCodeGeneratorContext context, FXMLType type) {
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> createBaseTypeSourceCode(context, clazz.getName());
            case FXMLGenericType(Class<?> clazz, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, clazz.getName(), generics);
            case FXMLUncompiledClassType(String className) -> createBaseTypeSourceCode(context, className);
            case FXMLUncompiledGenericType(String className, List<FXMLType> generics) ->
                    createGenericTypeSourceCode(context, className, generics);
            case FXMLWildcardType _ -> "?";
        };
    }

    private String createBaseTypeSourceCode(SourceCodeGeneratorContext context, String className) {
        return context.imports()
                .inlineClassNames()
                .getOrDefault(className, className);
    }

    private String createGenericTypeSourceCode(
            SourceCodeGeneratorContext context,
            String className,
            List<FXMLType> generics
    ) {
        Imports imports = context.imports();
        Map<String, String> inlineClassNames = imports.inlineClassNames();
        StringBuilder s = new StringBuilder()
                .append(inlineClassNames.get(className))
                .append("<");
        for (FXMLType genericType : generics) {
            s.append(typeToSourceCode(context, genericType))
                    .append(", ");
        }
        return s.deleteCharAt(s.length() - 2)
                .deleteCharAt(s.length() - 1)
                .append(">")
                .toString();
    }
}
