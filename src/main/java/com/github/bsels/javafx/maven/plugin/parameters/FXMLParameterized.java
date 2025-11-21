package com.github.bsels.javafx.maven.plugin.parameters;

import java.util.List;

/// Represents a parameterized FXML configuration or mapping entity.
/// This class stores details such as the class name, root parameters, and a list of uniquely identified parameters.
public class FXMLParameterized {
    /// Represents the name of the class associated with the [FXMLParameterized] instance.
    /// This field is used to identify or describe the class corresponding to the object.
    private String className;

    /// Represents a collection of interfaces and their associated methods to be used to bind actions.
    ///
    /// This list contains instances of [InterfacesWithMethod],
    /// serving to map interface names to their corresponding method names.
    /// It provides the ability to manage and access the relationship between interfaces and their respective methods.
    private List<InterfacesWithMethod> interfaces;

    /// Default constructor for the [FXMLParameterized] class.
    /// Initializes an instance of the [FXMLParameterized] object without setting any attributes.
    public FXMLParameterized() {
    }

    /// Constructs an instance of the [FXMLParameterized] class with the specified class name, root parameters,
    /// and identified parameters.
    ///
    /// @param className the name of the class associated with this instance
    ///
    public FXMLParameterized(String className) {
        this();
        setClassName(className);

    }

    /// Retrieves the name of the class associated with this instance.
    ///
    /// @return the name of the class as a `String`
    public String getClassName() {
        return className;
    }

    /// Sets the name of the class for this instance.
    ///
    /// @param className the name of the class to be assigned
    public void setClassName(String className) {
        this.className = className;
    }

    /// Retrieves the list of interfaces associated with this instance.
    /// Each interface is represented as an instance of the [InterfacesWithMethod] class.
    ///
    /// @return a list of [InterfacesWithMethod] objects representing the interfaces
    public List<InterfacesWithMethod> getInterfaces() {
        return interfaces;
    }

    /// Sets the list of interfaces associated with this instance.
    /// Each interface is represented as an instance of the [InterfacesWithMethod] class.
    ///
    /// @param interfaces a list of [InterfacesWithMethod] objects representing the interfaces to be assigned
    public void setInterfaces(List<InterfacesWithMethod> interfaces) {
        this.interfaces = interfaces;
    }

    /// Returns a string representation of the [FXMLParameterized] object.
    /// The string includes the className, rootParameters, and identifiedParameters fields.
    ///
    /// @return a string representation of the object in the format `FXMLParameterized[className='...', interfaces=...]`
    @Override
    public String toString() {
        return "FXMLParameterized[className='%s', interfaces=%s]".formatted(className, interfaces);
    }
}
