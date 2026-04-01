package io.github.bsels.javafx.maven.plugin.fxml.v2.writer;

import io.github.bsels.javafx.maven.plugin.TestHelpers;
import io.github.bsels.javafx.maven.plugin.fxml.v2.FXMLDocument;
import io.github.bsels.javafx.maven.plugin.fxml.v2.parser.FXMLDocumentParser;
import io.github.bsels.javafx.maven.plugin.io.FXMLReader;
import io.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FXMLSourceCodeBuilderTest {


    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser documentParser;
    private FXMLSourceCodeBuilder classUnderTest;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        documentParser = new FXMLDocumentParser(log, StandardCharsets.UTF_8);
        classUnderTest = new FXMLSourceCodeBuilder(log, true);
        originalJavaHome = System.getProperty("java.home");
        System.setProperty("java.home", "/java/home");
        assertThat(fxmlReader.toString())
                .isNotNull()
                .startsWith("FXMLReader[log=");
    }

    @AfterEach
    void tearDown() {
        if (originalJavaHome != null) {
            System.setProperty("java.home", originalJavaHome);
        } else {
            System.clearProperty("java.home");
        }
    }

    private ParsedFXML readFXML(String fxml) throws MojoExecutionException {
        Path fxmlFile = TestHelpers.getTestResourcePath(fxml);
        return fxmlReader.readFXML(fxmlFile);
    }

    private FXMLDocument parse(String fxml) throws MojoExecutionException {
        ParsedFXML parsedFXML = readFXML(fxml);
        return documentParser.parse(parsedFXML, "/examples");
    }

    @Nested
    class GenerateSourceCodeTest {

        @Test
        void setListGenerateSourceCode() throws MojoExecutionException {
            FXMLDocument document = parse("/examples/SetList.fxml");
            String sourceCode = classUnderTest.generateSourceCode(document, "com.example");
            assertThat(sourceCode)
                    .isNotNull()
                    .isEqualTo("");
        }
    }
}
