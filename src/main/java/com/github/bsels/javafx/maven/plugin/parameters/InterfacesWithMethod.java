package com.github.bsels.javafx.maven.plugin.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// The InterfacesWithMethod class represents a data structure associating an interface name with a set of method names.
/// It provides constructors, getter and setter methods, and a string representation for managing and accessing this data.
public class InterfacesWithMethod {
    /// Represents the name of an interface.
    ///
    /// This field stores the unique identifier or name assigned to an interface,
    /// which can be used to distinguish it and associate it with method details
    /// or other related attributes in the system.
    private String interfaceName;

    /// Represents a list of generic type definitions associated with an interface.
    ///
    /// This field stores a collection of strings, each representing a generic type parameter
    /// that can be used to define type constraints on the methods or classes associated with this interface.
    private List<String> generics;

    /// Represents a collection of method names associated with an interface.
    /// This set contains unique strings, each representing the name of a method.
    private Set<String> methodNames;

    /// Default constructor for the [InterfacesWithMethod] class.
    /// Initializes an instance of the [InterfacesWithMethod] object without setting any attributes.
    public InterfacesWithMethod() {
    }

    /// Constructs a new instance of the [InterfacesWithMethod] class with the specified parameters.
    ///
    /// @param interfaceName the name of the interface
    /// @param generics      a list of generic type parameters associated with the interface
    /// @param methodNames   a set of method names associated with the interface
    public InterfacesWithMethod(String interfaceName, List<String> generics, Set<String> methodNames) {
        this();
        setInterfaceName(interfaceName);
        setGenerics(generics);
        setMethodNames(methodNames);
    }

    /// Retrieves the set of method names associated with the interface.
    ///
    /// @return a set containing the method names
    public Set<String> getMethodNames() {
        return methodNames;
    }

    /// Sets the method names associated with the interface.
    ///
    /// @param methodNames a set of method names to assign to the interface
    public void setMethodNames(Set<String> methodNames) {
        this.methodNames = methodNames;
    }

    /// Retrieves the name of the interface.
    ///
    /// @return the current value of the interfaceName field
    public String getInterfaceName() {
        return interfaceName;
    }

    /// Sets the name of the interface.
    ///
    /// @param interfaceName the new name of the interface to be set
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /// Retrieves the list of generic type parameters associated with the interface.
    ///
    /// @return a list containing the generic type parameters
    public List<String> getGenerics() {
        return generics;
    }

    /// Sets the list of generic type parameters associated with the interface.
    ///
    /// @param generics a list of generic type parameters to assign to the interface
    public void setGenerics(List<String> generics) {
        this.generics = new ArrayList<>(generics);
    }

    /// Returns a string representation of the [InterfacesWithMethod] object.
    /// The string includes the interface name and the method names.
    ///
    /// @return a string representation of the object in the format
    /// `InterfacesWithMethod[interfaceName='...', generics=..., methodNames=...]`
    @Override
    public String toString() {
        return "InterfacesWithMethod[interfaceName='%s', generics=%s, methodNames=%s]".formatted(
                interfaceName, generics, methodNames
        );
    }
}
