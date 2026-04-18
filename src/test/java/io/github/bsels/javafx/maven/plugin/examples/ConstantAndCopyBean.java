package io.github.bsels.javafx.maven.plugin.examples;

import java.util.ArrayList;
import java.util.List;

/// A custom class for testing `fx:constant` and `fx:copy`.
public class ConstantAndCopyBean {
    /// A constant value.
    public static final String MY_CONSTANT = "ConstantValue";

    private String value;
    private Object other;
    private final List<Object> items = new ArrayList<>();

    /// Default constructor.
    public ConstantAndCopyBean() {
    }

    /// Returns the value.
    /// @return The value.
    public String getValue() {
        return value;
    }

    /// Sets the value.
    /// @param value The value.
    public void setValue(String value) {
        this.value = value;
    }

    /// Returns the other object.
    /// @return The other object.
    public Object getOther() {
        return other;
    }

    /// Sets the other object.
    /// @param other The other object.
    public void setOther(Object other) {
        this.other = other;
    }

    /// Returns the items list.
    /// @return The items list.
    public List<Object> getItems() {
        return items;
    }
}
