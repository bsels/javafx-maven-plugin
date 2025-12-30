/// Provides input/output utilities for processing and transforming FXML files within the JavaFX Maven Plugin.
///
/// This package offers core functionality for reading, parsing, and generating source code from FXML files,
/// serving as a critical part in the plugin's file processing pipeline.
///
/// ## Key Parts
/// - [com.github.bsels.javafx.maven.plugin.io.FXMLReader]: Handles parsing of FXML files, extracting import statements
///   and XML structure
/// - [com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder]: Generates Java source code from parsed FXML
///   structures
/// - [com.github.bsels.javafx.maven.plugin.io.ParsedFXML]: Represents the result of parsing an FXML file,
///   including imports and XML structure
/// - [com.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure]: Encapsulates the hierarchical structure of
///   an XML node with its attributes and children</dd>
///
/// ## Package Responsibilities
/// - Reading FXML files from various sources
/// - Parsing XML structure of FXML documents
/// - Extracting import statements and node details
/// - Generating corresponding Java source code
///
/// The package is designed to support dynamic source code generation for JavaFX FXML files during
/// the Maven build process, providing a flexible and extensible approach to FXML file processing.
/// ## Usage Example
/// ```java
/// // Reading and processing an FXML file
/// FXMLReader reader = new FXMLReader(log);
/// ParsedFXML parsedFXML = reader.readFXML(pathToFxmlFile);
/// // Generate source code from parsed FXML
/// FXMLSourceCodeBuilder codeBuilder = new FXMLSourceCodeBuilder();
/// String generatedSourceCode = codeBuilder.build(parsedFXML);
/// ```
package com.github.bsels.javafx.maven.plugin.io;