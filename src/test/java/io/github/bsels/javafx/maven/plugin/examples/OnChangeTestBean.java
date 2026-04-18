package io.github.bsels.javafx.maven.plugin.examples;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A custom test fixture bean for testing `onChange` listener parsing on collection and map properties.
public class OnChangeTestBean {

    private final ObservableList<String> observableList = FXCollections.observableArrayList();
    private final ObservableSet<String> observableSet = FXCollections.observableSet(new HashSet<>());
    private final ObservableMap<String, String> observableMap = FXCollections.observableHashMap();
    private final List<String> plainList = new ArrayList<>();
    private final Map<String, String> plainMap = new HashMap<>();

    /// Default constructor.
    public OnChangeTestBean() {
    }

    /// Returns the observable list property.
    ///
    /// @return The observable list.
    public ObservableList<String> getObservableList() {
        return observableList;
    }

    /// Returns the observable set property.
    ///
    /// @return The observable set.
    public ObservableSet<String> getObservableSet() {
        return observableSet;
    }

    /// Returns the observable map property.
    ///
    /// @return The observable map.
    public ObservableMap<String, String> getObservableMap() {
        return observableMap;
    }

    /// Returns the plain (non-observable) list property.
    ///
    /// @return The plain list.
    public List<String> getPlainList() {
        return plainList;
    }

    /// Returns the plain (non-observable) map property.
    ///
    /// @return The plain map.
    public Map<String, String> getPlainMap() {
        return plainMap;
    }
}
