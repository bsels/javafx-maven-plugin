package com.github.bsels.javafx.maven.plugin.fxml.v2;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/// Utility class for common FXML-related validation tasks.
public final class Utils {
    /// Pattern to validate standard Java identifier names.
    private static final Predicate<String> VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z$][a-zA-Z0-9_$]*$")
            .asPredicate();
    /// Pattern to validate chained Java identifier names (e.g., package names).
    private static final Predicate<String> VALID_CHAINED_IDENTIFIER_PATTERN = Pattern.compile(
            "^([a-zA-Z$][a-zA-Z0-9_$]*)(\\.[a-zA-Z$][a-zA-Z0-9_$]*)*$"
    ).asPredicate();

    /// Private constructor to prevent instantiation.
    private Utils() {
        // No instances needed
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
