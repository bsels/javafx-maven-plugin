/// The core package for the JavaFX Maven Plugin, providing comprehensive support for building, running,
/// and managing JavaFX applications within the Maven build ecosystem.
///
/// ## Package Overview
/// This package and its subpackages offer a robust set of tools and utilities for seamlessly integrating
/// JavaFX application development with Maven's build lifecycle and configuration management.
///
/// ## Key Parts
/// - [com.github.bsels.javafx.maven.plugin.BaseJavaFXMojo]: Base implementation for JavaFX-related Maven goals
/// - [com.github.bsels.javafx.maven.plugin.FXMLToSourceCodeMojo]: Converts FXML files to Java source code
/// - [com.github.bsels.javafx.maven.plugin.JavaFXJlinkMojo]: Supports creating custom runtime images for
///   JavaFX applications
/// - [com.github.bsels.javafx.maven.plugin.JavaFXRunMojo]: Executes JavaFX applications during the Maven build process
/// - [com.github.bsels.javafx.maven.plugin.CheckAndCast]: Utility for type checking and casting
///
/// ## Subpackages
/// - [com.github.bsels.javafx.maven.plugin.fxml]: FXML processing and node manipulation utilities
/// - [com.github.bsels.javafx.maven.plugin.in.memory.compiler]: In-memory compilation support
/// - [com.github.bsels.javafx.maven.plugin.io]: Input/output utilities for FXML and source code processing
/// - [com.github.bsels.javafx.maven.plugin.parameters]: Parameter handling and configuration management
/// - [com.github.bsels.javafx.maven.plugin.utils]: General utility classes and helper methods
///
/// ## Key Responsibilities
/// - Providing Maven goals for JavaFX application development
/// - Supporting FXML to source code conversion
/// - Managing application runtime and execution
/// - Facilitating in-memory compilation
/// - Handling complex configuration scenarios
///
/// ## Design Principles
/// - Seamless integration with the Maven build lifecycle
/// - Modular and extensible architecture
/// - Support for dynamic code generation
/// - Flexible configuration management
///
/// ## Usage Example
/// ```xml
/// // In pom.xml
/// <plugin>
///     <groupId>com.github.bsels</groupId>
///     <artifactId>javafx-maven-plugin</artifactId>
///     <configuration>
///         <mainClass>com.example.MainApplication</mainClass>
///     </configuration>
///     <executions>
///         <execution>
///             <goals>
///                 <goal>run</goal>
///             </goals>
///         </execution>
///     </executions>
/// </plugin>
/// ```
///
/// This plugin provides a comprehensive solution for developing
/// and managing JavaFX applications within the Maven ecosystem.
package com.github.bsels.javafx.maven.plugin;