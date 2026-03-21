package com.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/// Represents an FXML script loaded from a file.
///
/// @param path    The path to the script file.
/// @param charset The character set of the script file.
public record FXMLFileScript(Path path, Charset charset) implements FXMLScript {
    /// Compact constructor to validate the script file path and character set.
    ///
    /// @param path    The path to the script file.
    /// @param charset The character set of the script file.
    /// @throws NullPointerException if any parameter is null.
    public FXMLFileScript {
        Objects.requireNonNull(path, "`path` must not be null");
        Objects.requireNonNull(charset, "`charset` must not be null");
    }
}
