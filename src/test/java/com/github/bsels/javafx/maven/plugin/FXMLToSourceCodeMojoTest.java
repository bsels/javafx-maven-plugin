package com.github.bsels.javafx.maven.plugin;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.bsels.javafx.maven.plugin.fxml.ProcessedFXML;
import com.github.bsels.javafx.maven.plugin.io.ParsedFXML;
import com.github.bsels.javafx.maven.plugin.parameters.FXMLParameterized;
import com.github.bsels.javafx.maven.plugin.parameters.InterfacesWithMethod;
import com.github.bsels.javafx.maven.plugin.utils.ObjectMapperProvider;
import javafx.scene.control.TableColumn;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class FXMLToSourceCodeMojoTest {
    private static final Path FXML_RESOURCE_DIRECTORY = Path.of("src", "main", "resources", "fxml");
    private static final Path GENERATED_SOURCE_DIRECTORY = Path.of("target", "generated-sources", "fxml");
    private static final String TEST_PACKAGE_GENERATED = "test.package.generated";
    private static final String TEST_PROJECT_MY_CLASS_RESOURCE_BUNDLE = "test.project.MyClass.RESOURCE_BUNDLE";

    @Mock
    MavenProject mockProject;

    private MockedStatic<Files> filesMockedStatic;
    private FXMLToSourceCodeMojo classUnderTest;
    private Path rootTestFolderPath;
    private Path myButtonJavaPath;

    @BeforeEach
    void setUp() throws URISyntaxException {
        filesMockedStatic = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS);

        classUnderTest = new FXMLToSourceCodeMojo();
        classUnderTest.resourceBundleObject = TEST_PROJECT_MY_CLASS_RESOURCE_BUNDLE;
        classUnderTest.fxmlDirectory = FXML_RESOURCE_DIRECTORY;
        classUnderTest.packageName = TEST_PACKAGE_GENERATED;
        classUnderTest.generatedSourceDirectory = GENERATED_SOURCE_DIRECTORY;
        classUnderTest.debugInternalModel = false;
        classUnderTest.fxmlParameterizations = null;
        classUnderTest.project = mockProject;
        myButtonJavaPath = Path.of(
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResource("test-source/javafx/test/MyButton.java")
                        .toURI()
        );
        rootTestFolderPath = myButtonJavaPath.getParent().getParent().getParent()
                .toAbsolutePath();
    }

    @AfterEach
    void tearDown() {
        filesMockedStatic.close();
    }

    @Nested
    class ExecuteTest {

        @Test
        void nullFxmlDirectoryThrowsNullPointerException() {
            // Given
            classUnderTest.fxmlDirectory = null;
            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("FXML directory must be specified");
        }

        @Test
        void nullProjectThrowsNullPointerException() {
            // Given
            classUnderTest.project = null;
            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Maven project must be specified");
        }

        @Test
        void nullGeneratedSourceDirectoryThrowsNullPointerException() {
            // Given
            classUnderTest.generatedSourceDirectory = null;
            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Generated source directory must be specified");
        }

        @ParameterizedTest
        @ValueSource(classes = {DependencyResolutionRequiredException.class, RuntimeException.class})
        void classLoaderFailedThrowsMojoExecutionException(Class<? extends Throwable> exceptionClass)
                throws DependencyResolutionRequiredException {
            Mockito.when(mockProject.getRuntimeClasspathElements())
                    .thenThrow(exceptionClass);

            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasRootCauseInstanceOf(exceptionClass);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void failingToReadFxmlFolderThrowsMojoExecutionException(String data) {
            // Given
            classUnderTest.packageName = data;
            classUnderTest.resourceBundleObject = data;

            filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                    .thenThrow(IOException.class);

            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessage("Unable to read FXML directory")
                    .hasRootCauseInstanceOf(IOException.class);

            filesMockedStatic.verify(() -> Files.walk(classUnderTest.fxmlDirectory), Mockito.times(1));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = TEST_PACKAGE_GENERATED)
        void creatingOutputDirectoryFailedThrowMojoExecutionException(String packageName) {
            // Custom settings
            filesMockedStatic.close();
            filesMockedStatic = Mockito.mockStatic(Files.class);

            // Given
            classUnderTest.packageName = packageName;
            filesMockedStatic.when(() -> Files.createDirectories(Mockito.any()))
                    .thenThrow(IOException.class);

            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasRootCauseInstanceOf(IOException.class);

            final Path generatedSourceDirectory;
            if (packageName != null && !packageName.isEmpty()) {
                generatedSourceDirectory = classUnderTest.generatedSourceDirectory.resolve(packageName.replace('.', '/'));
            } else {
                generatedSourceDirectory = classUnderTest.generatedSourceDirectory;
            }
            filesMockedStatic.verify(
                    () -> Files.createDirectories(generatedSourceDirectory),
                    Mockito.times(1)
            );
        }

        @Test
        void handleTableViewValidSourceCode() {
            // Given
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);

                String tableViewFxml = """
                         <?xml version="1.0" encoding="UTF-8"?>
                        
                         <?import javafx.scene.control.TableColumn?>
                         <?import javafx.scene.control.TableView?>
                        
                         <fx:root hgap="8.0" maxHeight="1.7976931348623157E308" maxWidth="1.7976931348623157E308"
                                  minHeight="-Infinity" minWidth="-Infinity" prefHeight="440.0" prefWidth="800.0"
                                  type="TableView"
                                  xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">
                            <!-- generic 0: test.module.Student -->
                            <columns>
                                <TableColumn fx:id="nameColumn" fx:prefWidth="100.0" text="Name" onEditCommit="#edit">
                                    <!-- generic 0: test.module.Student -->
                                    <!-- generic 1: java.lang.String -->
                                </TableColumn>
                                <TableColumn fx:id="ageColumn" fx:prefWidth="100.0" text="Age">
                                    <!-- generic 1: test.data.String -->
                                    <!-- generic 0: test.module.Student -->
                                </TableColumn>
                            </columns>
                        </fx:root>
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("MyTableView.fxml");
                filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(tableViewFxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenReturn(new ByteArrayInputStream(tableViewFxml.getBytes(StandardCharsets.UTF_8)));

                FXMLParameterized fxmlParameterized = new FXMLParameterized("MyTableView");
                fxmlParameterized.setInterfaces(List.of(
                        new InterfacesWithMethod(),
                        new InterfacesWithMethod(
                                "java.util.functional.Consumer",
                                List.of("java.lang.Integer"),
                                Set.of("accept", "andThen")
                        )
                ));
                assertThat(fxmlParameterized.toString())
                        .satisfiesAnyOf(
                                toString -> assertThat(toString)
                                        .isEqualTo("""
                                                FXMLParameterized[className='MyTableView', \
                                                interfaces=[InterfacesWithMethod[interfaceName='null', generics=null, \
                                                methodNames=null], \
                                                InterfacesWithMethod[interfaceName='java.util.functional.Consumer', \
                                                generics=[java.lang.Integer], methodNames=[accept, andThen]]]]\
                                                """),
                                toString -> assertThat(toString)
                                        .isEqualTo("""
                                                FXMLParameterized[className='MyTableView', \
                                                interfaces=[InterfacesWithMethod[interfaceName='null', generics=null, \
                                                methodNames=null], \
                                                InterfacesWithMethod[interfaceName='java.util.functional.Consumer', \
                                                generics=[java.lang.Integer], methodNames=[andThen, accept]]]]\
                                                """)
                        );
                classUnderTest.fxmlParameterizations = List.of(fxmlParameterized);

                // When
                assertThatNoException()
                        .isThrownBy(classUnderTest::execute);

                // Then
                ArgumentCaptor<String> sourceCodeCaptor = ArgumentCaptor.forClass(String.class);

                filesMockedStatic.verify(() -> Files.writeString(
                        Mockito.any(), sourceCodeCaptor.capture(), Mockito.eq(StandardCharsets.UTF_8)
                ), Mockito.times(1));

                assertThat(sourceCodeCaptor.getValue())
                        .isEqualToIgnoringNewLines("""
                                package test.package.generated;
                                
                                import java.lang.Integer;
                                import java.lang.String;
                                import java.util.ResourceBundle;
                                import java.util.functional.Consumer;
                                import javafx.event.EventHandler;
                                import javafx.scene.control.TableColumn.CellEditEvent;
                                import javafx.scene.control.TableColumn;
                                import javafx.scene.control.TableView;
                                import javax.annotation.processing.Generated;
                                import test.module.Student;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class MyTableView
                                        extends TableView<Student>
                                        implements Consumer<Integer> {
                                    private static final ResourceBundle RESOURCE_BUNDLE = test.project.MyClass.RESOURCE_BUNDLE;
                                
                                    protected final TableColumn<Student, test.data.String> ageColumn;
                                    protected final TableColumn<Student, String> nameColumn;
                                
                                    protected MyTableView() {
                                        nameColumn = new TableColumn<>();
                                        ageColumn = new TableColumn<>();
                                
                                        super();
                                
                                        this.setMaxHeight(1.7976931348623157E308);
                                        this.setMaxWidth(1.7976931348623157E308);
                                        this.setMinHeight(Double.NEGATIVE_INFINITY);
                                        this.setMinWidth(Double.NEGATIVE_INFINITY);
                                        this.setPrefHeight(440.0);
                                        this.setPrefWidth(800.0);
                                        nameColumn.setOnEditCommit(this::edit);
                                        nameColumn.setText("Name");
                                        ageColumn.setText("Age");
                                        this.getColumns().addAll(nameColumn, ageColumn);
                                    }
                                
                                    protected abstract void edit(CellEditEvent<Student, String> param0);
                                }
                                """.formatted(now));
            }
        }

        @Test
        void handleTableViewNoGenericsValidSourceCode() {
            // Given
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);

                String tableViewFxml = """
                         <?xml version="1.0" encoding="UTF-8"?>
                        
                         <?import javafx.scene.control.TableColumn?>
                         <?import javafx.scene.control.TableView?>
                        
                         <fx:root hgap="8.0" maxHeight="1.7976931348623157E308" maxWidth="1.7976931348623157E308"
                                  minHeight="-Infinity" minWidth="-Infinity" prefHeight="440.0" prefWidth="800.0"
                                  type="TableView"
                                  xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">
                            <columns>
                                <TableColumn fx:id="nameColumn" fx:prefWidth="100.0" text="Name" onEditCommit="#edit" />
                                <TableColumn fx:id="ageColumn" fx:prefWidth="100.0" text="Age" />
                            </columns>
                        </fx:root>
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("MyTableView.fxml");
                filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(tableViewFxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenReturn(new ByteArrayInputStream(tableViewFxml.getBytes(StandardCharsets.UTF_8)));

                FXMLParameterized fxmlParameterized = new FXMLParameterized("MyTableView");
                fxmlParameterized.setInterfaces(List.of(
                        new InterfacesWithMethod(),
                        new InterfacesWithMethod(
                                "java.util.functional.Consumer",
                                List.of("java.lang.Integer"),
                                Set.of("accept", "andThen")
                        )
                ));
                assertThat(fxmlParameterized.toString())
                        .satisfiesAnyOf(
                                toString -> assertThat(toString)
                                        .isEqualTo("""
                                                FXMLParameterized[className='MyTableView', \
                                                interfaces=[InterfacesWithMethod[interfaceName='null', generics=null, \
                                                methodNames=null], \
                                                InterfacesWithMethod[interfaceName='java.util.functional.Consumer', \
                                                generics=[java.lang.Integer], methodNames=[accept, andThen]]]]\
                                                """),
                                toString -> assertThat(toString)
                                        .isEqualTo("""
                                                FXMLParameterized[className='MyTableView', \
                                                interfaces=[InterfacesWithMethod[interfaceName='null', generics=null, \
                                                methodNames=null], \
                                                InterfacesWithMethod[interfaceName='java.util.functional.Consumer', \
                                                generics=[java.lang.Integer], methodNames=[andThen, accept]]]]\
                                                """)
                        );
                classUnderTest.fxmlParameterizations = List.of(fxmlParameterized);

                // When
                assertThatNoException()
                        .isThrownBy(classUnderTest::execute);

                // Then
                ArgumentCaptor<String> sourceCodeCaptor = ArgumentCaptor.forClass(String.class);

                filesMockedStatic.verify(() -> Files.writeString(
                        Mockito.any(), sourceCodeCaptor.capture(), Mockito.eq(StandardCharsets.UTF_8)
                ), Mockito.times(1));

                assertThat(sourceCodeCaptor.getValue())
                        .isEqualToIgnoringNewLines("""
                                package test.package.generated;
                                
                                import java.lang.Integer;
                                import java.util.ResourceBundle;
                                import java.util.functional.Consumer;
                                import javafx.event.EventHandler;
                                import javafx.scene.control.TableColumn.CellEditEvent;
                                import javafx.scene.control.TableColumn;
                                import javafx.scene.control.TableView;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="%s")
                                public abstract class MyTableView
                                        extends TableView
                                        implements Consumer<Integer> {
                                    private static final ResourceBundle RESOURCE_BUNDLE = test.project.MyClass.RESOURCE_BUNDLE;
                                
                                    protected final TableColumn ageColumn;
                                    protected final TableColumn nameColumn;
                                
                                    protected MyTableView() {
                                        nameColumn = new TableColumn();
                                        ageColumn = new TableColumn();
                                
                                        super();
                                
                                        this.setMaxHeight(1.7976931348623157E308);
                                        this.setMaxWidth(1.7976931348623157E308);
                                        this.setMinHeight(Double.NEGATIVE_INFINITY);
                                        this.setMinWidth(Double.NEGATIVE_INFINITY);
                                        this.setPrefHeight(440.0);
                                        this.setPrefWidth(800.0);
                                        nameColumn.setOnEditCommit(this::edit);
                                        nameColumn.setText("Name");
                                        ageColumn.setText("Age");
                                        this.getColumns().addAll(nameColumn, ageColumn);
                                    }
                                
                                    protected abstract void edit(CellEditEvent<?, ?> param0);
                                }
                                """.formatted(now));
            }
        }

        @Test
        void handleTableViewNonSequentialGenericsThrowIllegalStateException() {
            // Given
            String tableViewFxml = """
                     <?xml version="1.0" encoding="UTF-8"?>
                    
                     <?import javafx.scene.control.TableColumn?>
                     <?import javafx.scene.control.TableView?>
                    
                     <fx:root hgap="8.0" maxHeight="1.7976931348623157E308" maxWidth="1.7976931348623157E308"
                              minHeight="-Infinity" minWidth="-Infinity" prefHeight="440.0" prefWidth="800.0"
                              type="TableView"
                              xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">
                        <!-- generic 0: test.module.Student -->
                        <columns>
                            <TableColumn fx:id="nameColumn" fx:prefWidth="100.0" text="Name">
                                <!-- generic 0: test.module.Student -->
                                <!-- generic 1: java.lang.String -->
                            </TableColumn>
                            <TableColumn fx:id="ageColumn" fx:prefWidth="100.0" text="Age">
                                <!-- generic 1: test.data.String -->
                                <!-- generic 2: test.module.Student -->
                            </TableColumn>
                        </columns>
                    </fx:root>
                    """.strip();

            Path fxmlFile = classUnderTest.fxmlDirectory.resolve("MyTableView.fxml");
            filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                    .thenReturn(Stream.of(fxmlFile));
            filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                    .thenReturn(true);
            filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                    .thenReturn(tableViewFxml.lines());
            filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                    .thenReturn(new ByteArrayInputStream(tableViewFxml.getBytes(StandardCharsets.UTF_8)));

            // When & Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Generic type indices are not sequential after sort: 1");
        }

        @Test
        void handleTableViewIncompleteGenericsThrowIllegalStateException() {
            // Given
            String tableViewFxml = """
                     <?xml version="1.0" encoding="UTF-8"?>
                    
                     <?import javafx.scene.control.TableColumn?>
                     <?import javafx.scene.control.TableView?>
                    
                     <fx:root hgap="8.0" maxHeight="1.7976931348623157E308" maxWidth="1.7976931348623157E308"
                              minHeight="-Infinity" minWidth="-Infinity" prefHeight="440.0" prefWidth="800.0"
                              type="TableView"
                              xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">
                        <!-- generic 0: test.module.Student -->
                        <columns>
                            <TableColumn fx:id="nameColumn" fx:prefWidth="100.0" text="Name">
                                <!-- generic 0: test.module.Student -->
                                <!-- generic 1: java.lang.String -->
                            </TableColumn>
                            <TableColumn fx:id="ageColumn" fx:prefWidth="100.0" text="Age">
                                <!-- generic 0: test.module.Student -->
                            </TableColumn>
                        </columns>
                    </fx:root>
                    """.strip();

            Path fxmlFile = classUnderTest.fxmlDirectory.resolve("MyTableView.fxml");
            filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                    .thenReturn(Stream.of(fxmlFile));
            filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                    .thenReturn(true);
            filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                    .thenReturn(tableViewFxml.lines());
            filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                    .thenReturn(new ByteArrayInputStream(tableViewFxml.getBytes(StandardCharsets.UTF_8)));

            // When & Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Invalid number of generics provided for %s: %d, expecting: %d".formatted(
                            TableColumn.class, 1, 2
                    ));
        }

        @Test
        void minimalSourceCodeExample() {
            // Given
            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);

                String fxml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        
                        <String fx:value="Hello"
                                xmlns="http://javafx.com/javafx/17.0.12"
                                xmlns:fx="http://javafx.com/fxml/1" />
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestClass.fxml");
                filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(fxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
                classUnderTest.debugInternalModel = true;

                // When
                assertThatNoException()
                        .isThrownBy(classUnderTest::execute);

                // Then
                ArgumentCaptor<String> sourceCodeCaptor = ArgumentCaptor.forClass(String.class);

                filesMockedStatic.verify(() -> Files.writeString(
                        Mockito.any(), sourceCodeCaptor.capture(), Mockito.eq(StandardCharsets.UTF_8)
                ), Mockito.times(1));

                assertThat(sourceCodeCaptor.getValue())
                        .isEqualToIgnoringNewLines("""
                                package test.package.generated;
                                
                                import java.util.ResourceBundle;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="2025-01-01T00:00Z")
                                public class TestClass {
                                    private static final ResourceBundle RESOURCE_BUNDLE = test.project.MyClass.RESOURCE_BUNDLE;
                                
                                    private final String $internalVariable$000;
                                
                                    public TestClass() {
                                        $internalVariable$000 = "Hello";
                                
                                        super();
                                
                                    }
                                }
                                """);
            }
        }

        @Test
        void failSerializationParsedFXMLThrowRuntimeException() {
            try (MockedStatic<ObjectMapperProvider> objectMapperProviderMockedStatic = Mockito.mockStatic(ObjectMapperProvider.class)) {
                SimpleModule simpleModule = new SimpleModule()
                        .addSerializer(ParsedFXML.class, new JsonSerializer<>() {
                            @Override
                            public void serialize(ParsedFXML value, JsonGenerator gen, SerializerProvider serializers)
                                    throws IOException {
                                throw new JsonGenerationException("Test exception", gen);
                            }
                        });
                ObjectMapper objectMapper = new ObjectMapper().registerModule(simpleModule);
                objectMapperProviderMockedStatic.when(ObjectMapperProvider::getObjectMapper)
                        .thenReturn(objectMapper);

                String fxml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        
                        <String fx:value="Hello"
                                xmlns="http://javafx.com/javafx/17.0.12"
                                xmlns:fx="http://javafx.com/fxml/1" />
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestClass.fxml");
                filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(fxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
                classUnderTest.debugInternalModel = true;

                // When, Then
                assertThatThrownBy(classUnderTest::execute)
                        .isInstanceOf(RuntimeException.class)
                        .hasRootCauseInstanceOf(JsonGenerationException.class);
            }
        }

        @Test
        void failSerializationProcessedFXMLThrowRuntimeException() {
            try (MockedStatic<ObjectMapperProvider> objectMapperProviderMockedStatic = Mockito.mockStatic(ObjectMapperProvider.class)) {
                SimpleModule simpleModule = new SimpleModule()
                        .addSerializer(ProcessedFXML.class, new JsonSerializer<>() {
                            @Override
                            public void serialize(ProcessedFXML value, JsonGenerator gen, SerializerProvider serializers)
                                    throws IOException {
                                throw new JsonGenerationException("Test exception", gen);
                            }
                        });
                ObjectMapper objectMapper = new ObjectMapper().registerModule(simpleModule);
                objectMapperProviderMockedStatic.when(ObjectMapperProvider::getObjectMapper)
                        .thenReturn(objectMapper);

                String fxml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        
                        <String fx:value="Hello"
                                xmlns="http://javafx.com/javafx/17.0.12"
                                xmlns:fx="http://javafx.com/fxml/1" />
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestClass.fxml");
                filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                        .thenReturn(fxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                        .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
                classUnderTest.debugInternalModel = true;

                // When, Then
                assertThatThrownBy(classUnderTest::execute)
                        .isInstanceOf(RuntimeException.class)
                        .hasRootCauseInstanceOf(JsonGenerationException.class);
            }
        }

        @Test
        void minimalSourceCodeFailingFileWriteThrowsMojoExecutionException() {
            String fxml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <String fx:value="Hello"
                            xmlns="http://javafx.com/javafx/17.0.12"
                            xmlns:fx="http://javafx.com/fxml/1" />
                    """.strip();

            Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestClass.fxml");
            filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                    .thenReturn(Stream.of(fxmlFile));
            filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                    .thenReturn(true);
            filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                    .thenReturn(fxml.lines());
            filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                    .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
            filesMockedStatic.when(() -> Files.writeString(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(OpenOption[].class)))
                    .thenThrow(IOException.class);
            // When, Then
            assertThatThrownBy(classUnderTest::execute)
                    .isInstanceOf(MojoExecutionException.class)
                    .hasRootCauseInstanceOf(IOException.class);
        }

        @Test
        void complexFileNoErrors() {
            // Given
            String fxml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    
                    <?import javafx.geometry.Insets?>
                    <?import javafx.scene.control.Button?>
                    <?import javafx.scene.control.CheckBox?>
                    <?import javafx.scene.control.ChoiceBox?>
                    <?import javafx.scene.control.ColorPicker?>
                    <?import javafx.scene.control.ComboBox?>
                    <?import javafx.scene.control.Label?>
                    <?import javafx.scene.control.RadioButton?>
                    <?import javafx.scene.control.Spinner?>
                    <?import javafx.scene.control.TableColumn?>
                    <?import javafx.scene.control.TableView?>
                    <?import javafx.scene.control.TextField?>
                    <?import javafx.scene.control.ToggleGroup?>
                    <?import javafx.scene.layout.ColumnConstraints?>
                    <?import javafx.scene.layout.GridPane?>
                    <?import javafx.scene.layout.HBox?>
                    <?import javafx.scene.layout.Priority?>
                    <?import javafx.scene.layout.RowConstraints?>
                    
                    <fx:root hgap="8.0" maxHeight="1.7976931348623157E308" maxWidth="1.7976931348623157E308" minHeight="-Infinity"
                             minWidth="-Infinity" prefHeight="440.0" prefWidth="800.0" type="GridPane" vgap="1.0"
                             xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">
                        <String fx:value="ignore" />
                        <String fx:value="ignore" />
                        <columnConstraints>
                            <ColumnConstraints>
                                <hgrow>
                                    <Priority fx:constant="NEVER"/>
                                </hgrow>
                                <minWidth>
                                    <Double fx:value="10.0"/>
                                </minWidth>
                                <prefWidth/>
                            </ColumnConstraints>
                            <ColumnConstraints hgrow="SOMETIMES" minWidth="10.0" prefWidth="100.0"/>
                        </columnConstraints>
                        <rowConstraints>
                            <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="NEVER"/>
                            <RowConstraints minHeight="10.0" prefHeight="30.0" vgrow="SOMETIMES"/>
                        </rowConstraints>
                        <children>
                            <CheckBox fx:id="checkBox" GridPane.columnIndex="1" GridPane.rowIndex="9">
                                <padding>
                                    <Insets bottom="2.0" left="2.0" right="2.0" top="2.0"/>
                                </padding>
                            </CheckBox>
                        </children>
                        <padding>
                            <Insets bottom="2.0" left="2.0" right="2.0" top="2.0"/>
                        </padding>
                    </fx:root>
                    """.strip();

            Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestFile.fxml");
            filesMockedStatic.when(() -> Files.walk(Mockito.any()))
                    .thenReturn(Stream.of(fxmlFile));
            filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                    .thenReturn(true);
            filesMockedStatic.when(() -> Files.lines(Mockito.any(), Mockito.any()))
                    .thenReturn(fxml.lines());
            filesMockedStatic.when(() -> Files.newInputStream(Mockito.any()))
                    .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
            // When, Then
            assertThatNoException()
                    .isThrownBy(classUnderTest::execute);
        }

        @Test
        void includeSourceCodeInClassDiscovery() {
            // Given
            classUnderTest.includeSourceFilesInClassDiscovery = true;
            Mockito.when(mockProject.getCompileSourceRoots())
                    .thenReturn(List.of(rootTestFolderPath.toString()));

            ZonedDateTime now = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            try (MockedStatic<ZonedDateTime> zonedDateTimeMockedStatic = Mockito.mockStatic(ZonedDateTime.class)) {
                zonedDateTimeMockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC))
                        .thenReturn(now);

                String fxml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <?import javafx.test.MyButton?>
                        <?import javafx.scene.layout.BorderPane?>
                        <fx:root type="BorderPane" xmlns="http://javafx.com/javafx/11.0.1"
                                 xmlns:fx="http://javafx.com/fxml/1">
                            <center>
                                <MyButton fx:id="button" text="Hello" />
                            </center>
                        </fx:root>
                        """.strip();

                Path fxmlFile = classUnderTest.fxmlDirectory.resolve("TestClass.fxml");
                filesMockedStatic.when(() -> Files.walk(classUnderTest.fxmlDirectory))
                        .thenReturn(Stream.of(fxmlFile));
                filesMockedStatic.when(() -> Files.isRegularFile(fxmlFile))
                        .thenReturn(true);
                filesMockedStatic.when(() -> Files.lines(Mockito.eq(fxmlFile), Mockito.any()))
                        .thenReturn(fxml.lines());
                filesMockedStatic.when(() -> Files.newInputStream(fxmlFile))
                        .thenReturn(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
                classUnderTest.debugInternalModel = true;

                // When
                assertThatNoException()
                        .isThrownBy(classUnderTest::execute);

                // Then
                ArgumentCaptor<String> sourceCodeCaptor = ArgumentCaptor.forClass(String.class);

                filesMockedStatic.verify(() -> Files.writeString(
                        Mockito.any(), sourceCodeCaptor.capture(), Mockito.eq(StandardCharsets.UTF_8)
                ), Mockito.times(1));

                assertThat(sourceCodeCaptor.getValue())
                        .isEqualToIgnoringNewLines("""
                                package test.package.generated;
                                
                                import java.util.ResourceBundle;
                                import javafx.scene.layout.BorderPane;
                                import javafx.test.MyButton;
                                import javax.annotation.processing.Generated;
                                
                                
                                @Generated(value="com.github.bsels.javafx.maven.plugin.io.FXMLSourceCodeBuilder", date="2025-01-01T00:00Z")
                                public abstract class TestClass
                                        extends BorderPane {
                                    private static final ResourceBundle RESOURCE_BUNDLE = test.project.MyClass.RESOURCE_BUNDLE;
                                
                                    protected final MyButton button;
                                
                                    protected TestClass() {
                                        button = new MyButton();
                                
                                        super();
                                
                                        button.setText("Hello");
                                        this.setCenter(button);
                                    }
                                }
                                """);
            }
        }
    }
}
