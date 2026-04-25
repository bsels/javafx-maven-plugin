package io.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.util.Objects;

/// An inline FXML script.
///
/// @param source The script source code
public record FXMLSourceScript(String source) implements FXMLScript {

    /// Initializes a new [FXMLSourceScript] record instance.
    ///
    /// @param source The script source code
    /// @throws NullPointerException If `source` is null
    public FXMLSourceScript {
        Objects.requireNonNull(source, "`source` must not be null");
    }
}
