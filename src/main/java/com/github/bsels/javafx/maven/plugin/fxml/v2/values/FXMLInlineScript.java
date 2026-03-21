package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an inline script within an FXML value.
///
/// @param script The script content.
public record FXMLInlineScript(String script) implements AbstractFXMLValue {
    /// Compact constructor to validate the inline script.
    ///
    /// @param script The script content.
    /// @throws NullPointerException     if the script is null.
    /// @throws IllegalArgumentException if the script is blank.
    public FXMLInlineScript {
        Objects.requireNonNull(script, "script cannot be null");
        if (script.isBlank()) {
            throw new IllegalArgumentException("script cannot be blank");
        }
    }
}
