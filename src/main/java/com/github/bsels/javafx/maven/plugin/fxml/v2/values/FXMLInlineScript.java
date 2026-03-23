package com.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// Represents an inline script within an FXML value.
///
/// @param script The script content.
public record FXMLInlineScript(String script) implements AbstractFXMLValue {
    /// Compact constructor to validate the inline script.
    ///
    /// @param script The script content.
    /// @throws NullPointerException     if `script` is `null`.
    /// @throws IllegalArgumentException if `script` is blank.
    public FXMLInlineScript {
        Objects.requireNonNull(script, "`script` must not be null");
        if (script.isBlank()) {
            throw new IllegalArgumentException("`script` must not be blank");
        }
    }
}
