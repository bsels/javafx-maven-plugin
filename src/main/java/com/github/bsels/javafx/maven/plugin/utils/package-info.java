/// Provides utility classes and helper components for the JavaFX Maven Plugin.
///
/// This package contains core utility classes that support various
/// functionalities across the JavaFX Maven Plugin, offering essential
/// tools for processing, encoding, and managing plugin operations.
/// ## Key Parts
/// - [com.github.bsels.javafx.maven.plugin.utils.FXMLProcessor]: Handles processing and transformation of
///   FXML-related operations
/// - [com.github.bsels.javafx.maven.plugin.utils.InternalClassNotFoundException]: Custom exception for handling class
///   resolution issues within the plugin
/// - [com.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider]: Provides mapping and conversion utilities
///   for objects
/// - [com.github.bsels.javafx.maven.plugin.utils.TypeEncoder]: Offers encoding and type conversion mechanisms
/// - [com.github.bsels.javafx.maven.plugin.utils.Utils]: General-purpose utility methods for common operations
///
/// ## Package Responsibilities
/// - Providing cross-cutting utility functions
/// - Supporting type conversion and encoding
/// - Handling custom exceptions and error scenarios
/// - Offering helper methods for plugin operations
/// - Facilitating object mapping and transformation
///
/// ## Design Principles
/// - Modularity and reusability of utility functions
/// - Minimizing code duplication across the plugin
/// - Providing flexible and extensible utility support
/// - Ensuring consistent error handling and processing
///
/// ## Usage Context
///
/// The package serves as a central utility hub for the JavaFX Maven Plugin, providing support for:
/// - FXML processing and transformation
/// - Type conversion and encoding
/// - Object mapping and conversion
/// - Error handling and custom exception management
package com.github.bsels.javafx.maven.plugin.utils;