package com.github.bsels.javafx.maven.plugin.fxml.v2;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Utils {
    private static final Predicate<String> VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z$][a-zA-Z0-9_$]*$")
            .asPredicate();
    private static final Predicate<String> VALID_CHAINED_IDENTIFIER_PATTERN = Pattern.compile(
            "^([a-zA-Z$][a-zA-Z0-9_$]*)(\\.[a-zA-Z$][a-zA-Z0-9_$]*)*$"
    ).asPredicate();

    private Utils() {
        // No instances needed
    }

    public static boolean isInvalidIdentifierName(String name) {
        Objects.requireNonNull(name);
        return !VALID_NAME_PATTERN.test(name);
    }

    public static boolean isInvalidChainedIdentifierName(String name) {
        Objects.requireNonNull(name);
        return !VALID_CHAINED_IDENTIFIER_PATTERN.test(name);
    }
}
