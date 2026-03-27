package io.github.bsels.javafx.maven.plugin.fxml.v2.types;

import java.util.List;
import java.util.Objects;

/// Represents an uncompiled generic type in FXML.
///
/// This record is used to define a generic type where the base type is specified as a string,
/// which might not yet be resolved or compiled.
/// It can include a list of type arguments, which are other FXML types.
///
/// @param name          The name of the uncompiled generic type. Must not be `null`.
/// @param typeArguments The list of type arguments. If `null`, an empty list is used.
public record FXMLUncompiledGenericType(String name, List<FXMLType> typeArguments)
        implements FXMLType {

    /// Compact constructor for the `FXMLUncompiledGenericType` record,
    /// which validates that the name is provided and ensures a non-null, immutable list of type arguments.
    ///
    /// @param name          The name of the uncompiled generic type. Must not be `null`.
    /// @param typeArguments The list of type arguments. If `null`, an empty list is used. The resulting list will be immutable.
    /// @throws NullPointerException if `name` is `null`.
    public FXMLUncompiledGenericType {
        Objects.requireNonNull(name, "`name` must not be null");
        typeArguments = List.copyOf(Objects.requireNonNullElseGet(typeArguments, List::of));
    }
}
