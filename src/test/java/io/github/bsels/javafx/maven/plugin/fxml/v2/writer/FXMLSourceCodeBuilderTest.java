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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FXMLSourceCodeBuilderTest {
    private static final ZonedDateTime ZONED_DATE_TIME_NOW_MOCK = ZonedDateTime.of(
            2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC
    );

    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser documentParser;
    private FXMLSourceCodeBuilder classUnderTest;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        documentParser = new FXMLDocumentParser(log, StandardCharsets.UTF_8);
        originalJavaHome = System.getProperty("java.home");
        System.setProperty("java.home", "/java/home");
        assertThat(fxmlReader.toString())
                .isNotNull()
                .startsWith("FXMLReader[log=");

        MockedStatic<ZonedDateTime> zonedDateTimeMock = Mockito.mockStatic(ZonedDateTime.class);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(ZoneOffset.UTC)).thenReturn(ZONED_DATE_TIME_NOW_MOCK);
        classUnderTest = new FXMLSourceCodeBuilder(log, "org.example.Translations.RESOURCE_BUNDLE", true);
        zonedDateTimeMock.close();
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
        return documentParser.parse(parsedFXML, "/examples", getRootPath());
    }

    private Path getRootPath() {
        return TestHelpers.getTestResourcePath("/examples").getParent();
    }

    @Nested
    class GenerateSourceCodeTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "/examples/ColorDefinitions.fxml",
                "/examples/SetList.fxml",
                "/examples/MapWithReferences.fxml",
                "/examples/GridPaneStaticPropertyElement.fxml",
                "/examples/GridPaneStaticPropertyAttribute.fxml",
                "/examples/GridPaneStaticPropertyElementExplicitValue.fxml",
                "/examples/MyButtonWithConstants.fxml",
                "/examples/MyHashMap.fxml",
                "/examples/NodeProperties.fxml",
                "/examples/FXInclude.fxml"
        })
        @Disabled("working on it")
        void dummy(String file) throws MojoExecutionException {
            FXMLDocument document = parse(file);
            String sourceCode = classUnderTest.generateSourceCode(document, "com.example");
            assertThat(sourceCode)
                    .isNotNull()
                    .isEqualTo("");
        }
    }
}
