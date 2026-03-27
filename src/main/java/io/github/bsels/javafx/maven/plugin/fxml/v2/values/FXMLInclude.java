package io.github.bsels.javafx.maven.plugin.fxml.v2.values;

import io.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLIdentifier;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;

/// Represents an FXML include (e.g., using fx:include).
///
/// @param identifier The identifier of the included FXML file.
/// @param sourceFile The source file to include.
/// @param charset    The charset to use for the included file.
/// @param resources  Optional resources to include with the FXML file.
public record FXMLInclude(
        FXMLIdentifier identifier,
        String sourceFile,
        Charset charset,
        Optional<String> resources
) implements AbstractFXMLValue {

    /// Compact constructor to validate the identifier and source file.
    ///
    /// @param identifier The identifier of the included FXML file.
    /// @param sourceFile The source file to include.
    /// @param charset    The charset to use for the included file.
    /// @param resources  Optional resources to include with the FXML file.
    /// @throws NullPointerException if `identifier` or `sourceFile` is `null`.
    public FXMLInclude {
        Objects.requireNonNull(identifier, "`identifier` must not be null");
        Objects.requireNonNull(sourceFile, "`sourceFile` must not be null");
        Objects.requireNonNull(charset, "`charset` must not be null");
        Objects.requireNonNull(resources, "`resources` must not be null");
    }
}
