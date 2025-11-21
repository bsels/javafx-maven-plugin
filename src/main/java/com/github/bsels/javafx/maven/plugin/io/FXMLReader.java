package com.github.bsels.javafx.maven.plugin.io;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// Handles the parsing and processing of FXML files, extracting import statements and XML structure.
/// Instances of this class are initialized with a logging utility to facilitate debugging and tracking.
///
/// This class is designed to read FXML files, parse their XML content, and extract necessary information
/// such as custom import statements and the root XML element hierarchy.
public record FXMLReader(Log log) {
    /// A [Pattern] object used to match and validate custom import statements in the FXML file.
    /// An import statement must conform to the specified format:
    ///
    /// `<?import package.name.ClassName?>`
    /// The pattern ensures the following structure:
    /// - Begins with `<?import`, followed by whitespace.
    /// - Captures a fully qualified class name in the form of `package.name.ClassName`.
    /// - Allows optional whitespace before the closing `?>`.
    ///
    /// This pattern is intended to be used for extracting and processing import statements
    /// declared within the FXML file for dynamic source code generation.
    private static final Pattern IMPORT_PATTERN = Pattern.compile("<\\?import\\s+((\\w+\\.)*(\\w+|\\*))\\s*\\?>");
    /// A compiled regular expression used to match non-name characters within a string. Non-name characters are
    /// typically defined as characters that are not considered part of a valid name identifier, as indicated by the
    /// regex pattern "\W".
    ///
    /// This constant is used by the FXMLReader class, which deals with parsing FXML files and extracting relevant
    /// structured information. Its primary usage is likely to assist in identifying invalid characters in
    /// names or identifiers during processing.
    private static final Pattern NON_NAME_CHAR_PATTERN = Pattern.compile("\\W");
    /// A [Gatherer] instance designed to extract class or package names from "import" statements
    /// in lines of text during sequential processing. Specifically, this `Gatherer` uses
    /// a regular expression matcher to identify and extract substrings that match import patterns.
    ///
    /// The `integrator` implementation iterates through matches found within a line of text,
    /// extracts the relevant group from the matched text, and pushes it downstream for further processing.
    /// The operation halts if downstream processing requests termination.
    private static final Gatherer<String, Void, String> EXTRACT_IMPORT_FROM_LINE = Gatherer.ofSequential(
            (_, line, integrator) -> {
                Matcher matcher = IMPORT_PATTERN.matcher(line);
                while (matcher.find()) {
                    if (!integrator.push(matcher.group(1))) {
                        return false;
                    }
                }
                return true;
            }
    );

    /// Constructs an instance of the [FXMLReader] class.
    ///
    /// @param log a Log instance used for logging messages during the parsing of FXML files. Must not be null.
    /// @throws NullPointerException if the provided log is null.
    public FXMLReader(Log log) {
        this.log = Objects.requireNonNull(log);
    }

    /// Reads and parses the specified FXML file to extract its import statements and its XML structure. The resulting
    /// data includes a list of imports and the root XML element hierarchy represented as a [ParsedFXML] instance.
    ///
    /// @param fxmlFile the path to the FXML file to read and parse. Must not be null.
    /// @return a [ParsedFXML] instance containing the import statements and the  root XML structure of the FXML file.
    /// @throws MojoExecutionException if the FXML file cannot be read or parsed due to an I/O error or XML parsing issues.
    public ParsedFXML readFXML(Path fxmlFile) throws MojoExecutionException {
        List<String> imports;
        try (Stream<String> lines = Files.lines(fxmlFile, StandardCharsets.UTF_8)) {
            Predicate<String> importLinePredicate = IMPORT_PATTERN.asPredicate();
            imports = lines.map(String::strip)
                    .filter(Predicate.not(String::isEmpty))
                    .dropWhile(Predicate.not(importLinePredicate))
                    .takeWhile(importLinePredicate)
                    .gather(EXTRACT_IMPORT_FROM_LINE)
                    .toList();
            log.info("Found %d imports: %s".formatted(imports.size(), imports));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read FXML file", e);
        }

        ParsedXMLStructure data;
        try (InputStream reader = Files.newInputStream(fxmlFile)) {
            log.info("Converting FXML file: %s".formatted(fxmlFile));

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilderFactory.setIgnoringElementContentWhitespace(true);
            documentBuilderFactory.setIgnoringComments(false);
            Document document = documentBuilderFactory.newDocumentBuilder()
                    .parse(reader);
            data = readDocument(document.getDocumentElement(), 0);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read FXML file", e);
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
        return new ParsedFXML(imports, data, deriveClassName(fxmlFile));
    }

    /// Parses an XML node and its subtree recursively, constructing a [ParsedXMLStructure] instance that represents
    /// the name, attributes, and children of the node.
    ///
    /// @param node the XML `Node` to parse. Must not be null.
    /// @return a [ParsedXMLStructure] object representing the parsed structure of the given node and its subtree.
    /// The object includes the node's name, attributes as key-value pairs, and a list of parsed child nodes.
    private ParsedXMLStructure readDocument(Node node, int depth) {
        String name = node.getNodeName();
        log.debug("Reading node: %s".formatted(name).indent(depth * 2).stripTrailing());
        NamedNodeMap attributes = node.getAttributes();
        Map<String, String> properties = Optional.ofNullable(attributes)
                .map(NamedNodeMap::getLength)
                .map(length -> IntStream.range(0, length)
                        .mapToObj(attributes::item)
                        .filter(Predicate.not(
                                attribute -> "xmlns".equals(attribute.getNodeName()) || attribute.getNodeName().startsWith("xmlns:")
                        ))
                        .collect(Collectors.toMap(Node::getNodeName, Node::getNodeValue)))
                .orElseGet(Map::of);
        NodeList childNodes = node.getChildNodes();
        List<ParsedXMLStructure> children = IntStream.range(0, childNodes.getLength())
                .mapToObj(childNodes::item)
                .filter(childNode -> Node.ELEMENT_NODE == childNode.getNodeType())
                .map(childNode -> readDocument(childNode, depth + 1))
                .toList();
        List<String> comments = IntStream.range(0, childNodes.getLength())
                .mapToObj(childNodes::item)
                .filter(childNode -> Node.COMMENT_NODE == childNode.getNodeType())
                .map(Node::getNodeValue)
                .map(String::strip)
                .toList();
        log.debug(
                "Found %d comments: %s".formatted(comments.size(), comments).indent((depth + 1) * 2).stripTrailing()
        );
        return new ParsedXMLStructure(name, properties, children, comments);
    }

    /// Derives the class name corresponding to the given FXML file path.
    /// The method extracts the base name of the file, removes the file extension,
    /// and replaces characters that are not valid in class names with underscores.
    ///
    /// @param fxmlFile the path to the FXML file. Must not be null.
    /// @return a string representing the derived class name, with non-name characters replaced by underscores.
    private String deriveClassName(Path fxmlFile) {
        String fileName = fxmlFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        return NON_NAME_CHAR_PATTERN.matcher(baseName).replaceAll("_");
    }
}
