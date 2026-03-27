package io.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.util.Objects;

/// Represents an inline FXML script.
///
/// @param source The script source code.
public record FXMLSourceScript(String source) implements FXMLScript {
    /// Compact constructor to validate the script source.
    ///
    /// @param source The script source code.
    /// @throws NullPointerException if `source` is `null`.
    public FXMLSourceScript {
        Objects.requireNonNull(source, "`source` must not be null");
    }
}
