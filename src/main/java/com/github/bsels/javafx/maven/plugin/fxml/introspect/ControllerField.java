package com.github.bsels.javafx.maven.plugin.fxml.introspect;

import java.lang.reflect.Type;
import java.util.Objects;

/// Represents a field in a controller with its visibility, name, and type.
///
/// This class is designed to encapsulate the characteristics of a field in a controller by storing
/// its visibility level, name, and type.
///
/// It also enforces non-null constraints on all parameters to ensure the integrity of the data being represented.
///
/// @param visibility the visibility level of the field; must not be null
/// @param name       the name of the field; must not be null
/// @param type       the type of the field; must not be null
public record ControllerField(Visibility visibility, String name, Type type) {

    /// Constructs a new instance of `ControllerField`.
    ///
    /// @param visibility the visibility level of the field; must not be null
    /// @param name       the name of the field; must not be null
    /// @param type       the type of the field; must not be null
    /// @throws NullPointerException if any of the parameters are null
    public ControllerField {
        Objects.requireNonNull(visibility, "`visibility` must not be null");
        Objects.requireNonNull(name, "`name` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
    }
}
