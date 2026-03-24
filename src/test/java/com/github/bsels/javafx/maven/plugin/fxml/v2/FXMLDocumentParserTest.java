package com.github.bsels.javafx.maven.plugin.fxml.v2;

import com.github.bsels.javafx.maven.plugin.TestHelpers;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLInternalIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.identifiers.FXMLRootIdentifier;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLCollectionProperties;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLObjectProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.properties.FXMLProperty;
import com.github.bsels.javafx.maven.plugin.fxml.v2.scripts.FXMLSourceScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLClassType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.types.FXMLGenericType;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.AbstractFXMLValue;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLInlineScript;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLObject;
import com.github.bsels.javafx.maven.plugin.fxml.v2.values.FXMLValue;
import com.github.bsels.javafx.maven.plugin.io.FXMLReader;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class FXMLDocumentParserTest {
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<List, ListAssert<FXMLProperty>> PROPERTIES_ASSERT_FACTORY = InstanceOfAssertFactories.list(FXMLProperty.class);
    @SuppressWarnings("rawtypes") // The factory is generic, we can't use the type parameter here
    public static final InstanceOfAssertFactory<List, ListAssert<AbstractFXMLValue>> LIST_VALUE_ASSERT_FACTORY = InstanceOfAssertFactories.list(AbstractFXMLValue.class);

    private String originalJavaHome;
    private FXMLReader fxmlReader;
    private FXMLDocumentParser classUnderTest;

    @BeforeEach
    void setUp() {
        DefaultLog log = new DefaultLog(new ConsoleLogger());
        fxmlReader = new FXMLReader(log);
        classUnderTest = new FXMLDocumentParser(log);
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

    @Nested
    class ExamplesTest {

        @Test
        void inMemoryScript_ReturnExpected() throws MojoExecutionException {
            // Prepare
            ParsedFXML parsedFXML = readFXML("/examples/InMemoryScript.fxml");

            // Act
            FXMLDocument document = classUnderTest.parse(parsedFXML);

            // Assert
            assertThat(document)
                    // Validate document
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("controller", Optional.empty())
                    .hasFieldOrPropertyWithValue("scriptEngine", Optional.of("javascript"))
                    .hasFieldOrPropertyWithValue("definitions", List.of())
                    .satisfies(
                            doc -> assertThat(doc.scripts())
                                    .hasSize(1)
                                    .satisfiesExactly(
                                            script -> assertThat(script)
                                                    .isInstanceOf(FXMLSourceScript.class)
                                                    .hasFieldOrPropertyWithValue("source", """
                                                            function handleButtonAction(event) {
                                                                java.lang.System.out.println('You clicked me!');
                                                            }
                                                            """)
                                    ),
                            doc -> assertThat(doc.imports())
                                    .hasSize(2)
                                    .containsExactly("javafx.scene.control.*", "javafx.scene.layout.*")
                    )
                    // Validate root
                    .extracting(FXMLDocument::root)
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("identifier", FXMLRootIdentifier.INSTANCE)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(VBox.class))
                    // Validate root properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLCollectionProperties.class)
                    .extracting(FXMLCollectionProperties.class::cast)
                    .hasFieldOrPropertyWithValue("name", "children")
                    .hasFieldOrPropertyWithValue("getter", "getChildren")
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Node.class))
                    .hasFieldOrPropertyWithValue("properties", List.of())
                    // Validate root property values
                    .extracting(FXMLCollectionProperties::value, LIST_VALUE_ASSERT_FACTORY)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(FXMLObject.class)
                    .extracting(FXMLObject.class::cast)
                    .hasFieldOrPropertyWithValue("factoryMethod", Optional.empty())
                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(Button.class))
                    .satisfies(
                            object -> assertThat(object.identifier())
                                    .isInstanceOf(FXMLInternalIdentifier.class)
                    )
                    // Validate button properties
                    .extracting(FXMLObject::properties, PROPERTIES_ASSERT_FACTORY)
                    .hasSize(2)
                    .hasOnlyElementsOfType(FXMLObjectProperty.class)
                    .extracting(FXMLObjectProperty.class::cast)
                    .satisfiesExactlyInAnyOrder(
                            first -> assertThat(first)
                                    .hasFieldOrPropertyWithValue("name", "onAction")
                                    .hasFieldOrPropertyWithValue("setter", "setOnAction")
                                    .hasFieldOrPropertyWithValue("type", new FXMLGenericType(EventHandler.class, new FXMLClassType(ActionEvent.class)))
                                    .extracting(FXMLProperty::value)
                                    .isInstanceOf(FXMLInlineScript.class)
                                    .hasFieldOrPropertyWithValue("script", "handleButtonAction(event);"),
                            second -> assertThat(second)
                                    .hasFieldOrPropertyWithValue("name", "text")
                                    .hasFieldOrPropertyWithValue("setter", "setText")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .extracting(FXMLProperty::value)
                                    .isInstanceOf(FXMLValue.class)
                                    .hasFieldOrPropertyWithValue("value", "Click Me!")
                                    .hasFieldOrPropertyWithValue("type", new FXMLClassType(String.class))
                                    .hasFieldOrPropertyWithValue("identifier", Optional.empty())
                    );
        }

    }
}
