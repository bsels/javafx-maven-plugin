package io.github.bsels.javafx.maven.plugin.io;

import io.github.bsels.javafx.maven.plugin.utils.Utils;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// Parses FXML files, extracting import statements and XML structure.
///
/// @param log The logger instance
public record FXMLReader(Log log) {
    /// Pattern to match FXML import statements.
    private static final Pattern IMPORT_PATTERN = Pattern.compile("<\\?import\\s+((\\w+\\.)*(\\w+|\\*))\\s*\\?>");
    /// Pattern to match FXML language declarations.
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("<\\?language\\s+(\\w+)\\s*\\?>");
    /// Pattern to match non-word characters.
    private static final Pattern NON_NAME_CHAR_PATTERN = Pattern.compile("\\W");
    /// Set of XML node types considered as text content.
    private static final Set<Short> TEXT_NODE_TYPES = Set.of(Node.TEXT_NODE, Node.CDATA_SECTION_NODE);

    /// Initializes a new [FXMLReader] instance.
    ///
    /// @param log The logger instance
    /// @throws NullPointerException If `log` is null
    public FXMLReader(Log log) {
        this.log = Objects.requireNonNull(log);
    }

    /// Reads and parses an FXML file using UTF-8 encoding.
    ///
    /// @param fxmlFile The path to the FXML file
    /// @return A [ParsedFXML] instance
    /// @throws MojoExecutionException If the file cannot be read or parsed
    /// @throws NullPointerException   If `fxmlFile` is null
    public ParsedFXML readFXML(Path fxmlFile) throws MojoExecutionException {
        return readFXML(fxmlFile, StandardCharsets.UTF_8);
    }

    /// Reads and parses an FXML file with the specified charset.
    ///
    /// @param fxmlFile The path to the FXML file
    /// @param charset  The character set to use
    /// @return A [ParsedFXML] instance
    /// @throws MojoExecutionException If the file cannot be read or parsed
    /// @throws NullPointerException   If `fxmlFile` or `charset` is null
    public ParsedFXML readFXML(Path fxmlFile, Charset charset) throws MojoExecutionException, NullPointerException {
        Objects.requireNonNull(fxmlFile, "`fxmlFile` must not be null");
        Objects.requireNonNull(charset, "`charset` must not be null");
        record FXMLHeaders(List<String> imports, Optional<String> language) {
        }

        FXMLHeaders headers;
        try (Stream<String> lines = Files.lines(fxmlFile, charset)) {
            Predicate<String> combinedPredicate = IMPORT_PATTERN.asPredicate()
                    .or(LANGUAGE_PATTERN.asPredicate());
            headers = lines.map(String::strip)
                    .filter(Predicate.not(String::isEmpty))
                    .dropWhile(combinedPredicate.negate())
                    .takeWhile(combinedPredicate)
                    .collect(Collectors.teeing(
                            Utils.collectPattern(IMPORT_PATTERN),
                            Collectors.collectingAndThen(
                                    Utils.collectPattern(LANGUAGE_PATTERN),
                                    Utils.singletonOrEmpty()
                            ),
                            FXMLHeaders::new
                    ));
            log.info("Found %d imports: %s".formatted(headers.imports().size(), headers.imports()));
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
        return new ParsedFXML(headers.language(), headers.imports(), data, deriveClassName(fxmlFile));
    }

    /// Parses an XML node and its subtree recursively.
    ///
    /// @param node  The XML `Node` to parse
    /// @param depth The current depth in the XML tree
    /// @return A [ParsedXMLStructure] representing the node and its subtree
    private ParsedXMLStructure readDocument(Node node, int depth) {
        String name = node.getNodeName();
        log.debug("Reading node: %s".formatted(name).indent(depth * 2).stripTrailing());
        NamedNodeMap attributes = node.getAttributes();
        Map<String, String> properties = Optional.ofNullable(attributes)
                .map(NamedNodeMap::getLength)
                .map(length -> IntStream.range(0, length)
                        .mapToObj(attributes::item)
                        .filter(Predicate.not(
                                attribute -> "xmlns".equals(attribute.getNodeName()) || attribute.getNodeName().startsWith(
                                        "xmlns:")
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
        Optional<String> textValue = getCombinedTextContent(children, childNodes);
        return new ParsedXMLStructure(name, properties, children, comments, textValue);
    }

    /// Combines text content from child nodes into a single string.
    ///
    /// @param children   The list of parsed child elements
    /// @param childNodes The raw list of child nodes
    /// @return An [Optional] containing the combined text content
    private Optional<String> getCombinedTextContent(List<ParsedXMLStructure> children, NodeList childNodes) {
        if (!children.isEmpty()) {
            return Optional.empty();
        }
        return IntStream.range(0, childNodes.getLength())
                .mapToObj(childNodes::item)
                .filter(childNode -> TEXT_NODE_TYPES.contains(childNode.getNodeType()))
                .map(Node::getNodeValue)
                .collect(Collectors.collectingAndThen(Collectors.joining(), Optional::of))
                .map(Utils::stripIndentNonBlankLines)
                .filter(Predicate.not(String::isBlank));
    }

    /// Derives a Java class name from the specified FXML file path.
    ///
    /// @param fxmlFile The path to the FXML file
    /// @return The derived class name
    private String deriveClassName(Path fxmlFile) {
        String fileName = fxmlFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        return NON_NAME_CHAR_PATTERN.matcher(baseName).replaceAll("_");
    }
}
