package io.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.nio.charset.Charset;
import java.util.Objects;

/// An FXML script loaded from a file.
///
/// @param path    The path to the script file
/// @param charset The character set of the script file
public record FXMLFileScript(String path, Charset charset) implements FXMLScript {

    /// Initializes a new [FXMLFileScript] record instance.
    ///
    /// @param path    The path to the script file
    /// @param charset The character set of the script file
    /// @throws NullPointerException If any parameter is null
    public FXMLFileScript {
        Objects.requireNonNull(path, "`path` must not be null");
        Objects.requireNonNull(charset, "`charset` must not be null");
    }
}
