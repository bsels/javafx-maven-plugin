package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLUncompiledGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLWildcardType;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/// Utility class for common FXML-related validation tasks.
public final class FXMLUtils {
    /// Pattern to validate standard Java identifier names.
    private static final Predicate<String> VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z$][a-zA-Z0-9_$]*$")
            .asPredicate();
    /// Pattern to validate chained Java identifier names (e.g., package names).
    private static final Predicate<String> VALID_CHAINED_IDENTIFIER_PATTERN = Pattern.compile(
            "^([a-zA-Z$][a-zA-Z0-9_$]*)(\\.[a-zA-Z$][a-zA-Z0-9_$]*)*$"
    ).asPredicate();

    /// Private constructor to prevent instantiation.
    private FXMLUtils() {
        // No instances needed
    }

    /// Determines the raw type of the given [FXMLType] instance.
    ///
    /// @param type the [FXMLType] instance whose raw type is to be identified
    /// @return the raw [Class] type corresponding to the FXMLType instance; returns `Object.class` for unsupported or wildcard types
    public static Class<?> findRawType(FXMLType type) {
        return switch (type) {
            case FXMLClassType(Class<?> clazz) -> clazz;
            case FXMLGenericType(Class<?> clazz, List<FXMLType> _) -> clazz;
            case FXMLWildcardType _, FXMLUncompiledClassType _, FXMLUncompiledGenericType _ -> Object.class;
        };
    }

    /// Checks if the provided name is an invalid Java identifier.
    ///
    /// @param name The name to check.
    /// @return `true` if the name is invalid, `false` otherwise.
    /// @throws NullPointerException if `name` is `null`.
    public static boolean isInvalidIdentifierName(String name) {
        Objects.requireNonNull(name, "`name` must not be null");
        return !VALID_NAME_PATTERN.test(name);
    }

    /// Checks if the provided name is an invalid chained Java identifier (e.g., "com.example.Class").
    ///
    /// @param name The name to check.
    /// @return `true` if the name is invalid, `false` otherwise.
    /// @throws NullPointerException if `name` is `null`.
    public static boolean isInvalidChainedIdentifierName(String name) {
        Objects.requireNonNull(name, "`name` must not be null");
        return !VALID_CHAINED_IDENTIFIER_PATTERN.test(name);
    }
}
