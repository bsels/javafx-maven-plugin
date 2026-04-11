package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import java.util.Objects;

/// An inline script within an FXML value.
///
/// @param script The script content
public record FXMLInlineScript(String script) implements AbstractFXMLValue {

    /// Initializes a new [FXMLInlineScript] record instance.
    ///
    /// @param script The script content
    /// @throws NullPointerException     If `script` is null
    /// @throws IllegalArgumentException If `script` is blank
    public FXMLInlineScript {
        Objects.requireNonNull(script, "`script` must not be null");
        if (script.isBlank()) {
            throw new IllegalArgumentException("`script` must not be blank");
        }
    }
}
