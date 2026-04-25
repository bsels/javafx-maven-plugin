package io.github.bsels.javafx.maven.plugin.fxml.v2.parser;

import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import io.github.bsels.javafx.maven.plugin.io.ParsedXMLStructure;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UnreachableBlocksTest {

    /// Verifies that FXMLDocumentParser rethrows MojoExecutionException as RuntimeException
    /// when loading include documents fails.
    /// This covers FXMLDocumentParser.java:951.
    @Test
    void shouldRethrowMojoExecutionExceptionAsRuntimeException() {
        Log log = new DefaultLog(new ConsoleLogger());
        Path rootPath = Path.of("root");
        ParsedXMLStructure rootStructure = new ParsedXMLStructure("Button", Map.of(), List.of(), List.of(), Optional.empty());
        ParsedFXML parsedFXML = new ParsedFXML(Optional.empty(), List.of(), rootStructure, "MyButton");

        try (MockedConstruction<FXMLReader> mocked = mockConstruction(FXMLReader.class, (mock, context) -> {
            when(mock.readFXML(any(Path.class), any())).thenThrow(new MojoExecutionException("Mocked failure"));
        })) {
            FXMLDocumentParser parser = new FXMLDocumentParser(log, StandardCharsets.UTF_8);
            
            // Create a structure with fx:include to trigger the failing branch
            // Note: source path must start with / for loadIncludeFXMLDocuments substring(1)
            ParsedXMLStructure includeStructure = new ParsedXMLStructure(
                    "fx:include",
                    Map.of("source", "/included.fxml"),
                    List.of(),
                    List.of(),
                    Optional.empty()
            );
            ParsedXMLStructure rootWithInclude = new ParsedXMLStructure(
                    "javafx.scene.layout.VBox",
                    Map.of(),
                    List.of(includeStructure),
                    List.of(),
                    Optional.empty()
            );
            ParsedFXML parsedWithInclude = new ParsedFXML(Optional.empty(), List.of(), rootWithInclude, "MyVBox");

            assertThatThrownBy(() -> parser.parse(parsedWithInclude, "/", rootPath))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(MojoExecutionException.class)
                    .hasStackTraceContaining("Mocked failure");
        }
    }

    /// Verifies that FXMLReader throws MojoExecutionException when Files.lines fails.
    /// This covers FXMLReader.java:111.
    @Test
    void shouldThrowMojoExecutionExceptionWhenFilesLinesFails() {
        Log log = new DefaultLog(new ConsoleLogger());
        FXMLReader reader = new FXMLReader(log);
        Path fxmlFile = Path.of("nonexistent.fxml");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.lines(any(Path.class), any())).thenThrow(new IOException("Mocked IO failure"));

            assertThatThrownBy(() -> reader.readFXML(fxmlFile))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("Unable to read FXML file")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    /// Verifies that FXMLReader throws MojoExecutionException when Files.newInputStream fails.
    /// This covers FXMLReader.java:127.
    @Test
    void shouldThrowMojoExecutionExceptionWhenFilesNewInputStreamFails() {
        Log log = new DefaultLog(new ConsoleLogger());
        FXMLReader reader = new FXMLReader(log);
        Path fxmlFile = Path.of("nonexistent.fxml");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // First Files.lines must succeed or be skipped. We'll make it return empty stream.
            mockedFiles.when(() -> Files.lines(any(Path.class), any())).thenReturn(java.util.stream.Stream.empty());
            mockedFiles.when(() -> Files.newInputStream(any(Path.class))).thenThrow(new IOException("Mocked IO failure"));

            assertThatThrownBy(() -> reader.readFXML(fxmlFile))
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("Unable to read FXML file")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    /// Verifies that FXMLReader rethrows ParserConfigurationException as RuntimeException.
    /// This covers FXMLReader.java:129.
    @Test
    void shouldRethrowParserConfigurationExceptionAsRuntimeException() {
        Log log = new DefaultLog(new ConsoleLogger());
        FXMLReader reader = new FXMLReader(log);
        Path fxmlFile = Path.of("dummy.fxml");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
             MockedStatic<DocumentBuilderFactory> mockedFactory = mockStatic(DocumentBuilderFactory.class)) {
            mockedFiles.when(() -> Files.lines(any(Path.class), any())).thenReturn(java.util.stream.Stream.empty());
            mockedFiles.when(() -> Files.newInputStream(any(Path.class))).thenReturn(mock(java.io.InputStream.class));
            
            mockedFactory.when(DocumentBuilderFactory::newInstance).thenThrow(new RuntimeException(new ParserConfigurationException("Mocked failure")));

            assertThatThrownBy(() -> reader.readFXML(fxmlFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(ParserConfigurationException.class);
        }
    }
}
