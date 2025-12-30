package com.github.bsels.javafx.maven.plugin.fxml;

import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerField;
import com.github.bsels.javafx.maven.plugin.fxml.introspect.ControllerMethod;

import java.util.List;
import java.util.Objects;

/// Represents metadata about an FXML controller, encapsulating its class name, type, instance fields, and instance methods.
///
/// This record is designed to hold the structural and behavioral details of an FXML controller in a JavaFX application.
/// It provides access to the controller's fully qualified class name, its corresponding Class object,
/// the fields it defines, and its methods. The provided data is immutable to ensure integrity.
///
/// All fields are validated for non-null values. Collections are copied to ensure they are immutable,
/// providing safe access without the risk of unintended modifications.
///
/// @param className       the fully qualified name of the controller's class; must not be null
/// @param type            the Class object representing the type of the controller; must not be null
/// @param instanceFields  a list of ControllerField objects representing the instance fields of the controller; must not be null
/// @param instanceMethods a list of ControllerMethod objects representing the instance methods of the controller; must not be null
public record FXMLController(
        String className,
        Class<?> type,
        List<ControllerField> instanceFields,
        List<ControllerMethod> instanceMethods
) {

    /// Constructs an instance of FXMLController, representing metadata about an FXML controller.
    /// This includes its class name, type, instance fields, and instance methods.
    /// All parameters are validated to ensure they are non-null,
    /// and immutable copies are created for collections to guarantee unmodifiable data integrity.
    ///
    /// @param className       the fully qualified name of the controller's class; must not be null
    /// @param type            the Class object representing the type of the controller; must not be null
    /// @param instanceFields  a list of ControllerField objects representing instance fields of the controller; must not be null
    /// @param instanceMethods a list of ControllerMethod objects representing instance methods of the controller; must not be null
    /// @throws NullPointerException if any parameter is null
    public FXMLController {
        Objects.requireNonNull(className, "`className` must not be null");
        Objects.requireNonNull(type, "`type` must not be null");
        instanceFields = List.copyOf(Objects.requireNonNull(instanceFields, "`instanceFields` must not be null"));
        instanceMethods = List.copyOf(Objects.requireNonNull(instanceMethods, "`instanceMethods` must not be null"));
    }
}
