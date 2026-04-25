package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import java.util.Objects;

/// A wrapper for a reference to another FXML identifier.
///
/// @param reference The name of the FXML identifier being referenced.
public record ReferenceWrapper(String reference) implements TypeWrapper {

    /// Constructs a `ReferenceWrapper` and ensures the reference name is not null.
    ///
    /// @param reference The name of the FXML identifier being referenced.
    public ReferenceWrapper {
        Objects.requireNonNull(reference, "`reference` must not be null");
    }
}
