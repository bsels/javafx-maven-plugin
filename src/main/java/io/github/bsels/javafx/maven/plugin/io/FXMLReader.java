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

/// Parses FXML files, extracting import statements, language declarations, and the full XML structure.
///
/// This class handles the initial phase of FXML processing by reading the file's header information
/// (like imports and script language) and then performing a full XML parse of the document's content to build
/// an internal representation.
///
/// @param log The logger instance used for reporting parsing progress and debugging information
public record FXMLReader(Log log) {
    /// Pattern to match FXML import statements, supporting both single classes and wildcard imports.
    private static final Pattern IMPORT_PATTERN = Pattern.compile("<\\?import\\s+((\\w+\\.)*(\\w+|\\*))\\s*\\?>");
    /// Pattern to match FXML language declarations, identifying the script language used in the file.
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("<\\?language\\s+(\\w+)\\s*\\?>");
    /// Pattern to match characters that are not valid in a Java identifier, used when deriving class names.
    private static final Pattern NON_NAME_CHAR_PATTERN = Pattern.compile("\\W");
    /// Set of XML node types that are treated as textual content, including standard text and CDATA sections.
    private static final Set<Short> TEXT_NODE_TYPES = Set.of(Node.TEXT_NODE, Node.CDATA_SECTION_NODE);

    /// Initializes a new [FXMLReader] instance with the provided Maven logger.
    ///
    /// @param log The logger instance to use for all logging operations during FXML parsing
    /// @throws NullPointerException If the provided `log` is `null`
    public FXMLReader {
        Objects.requireNonNull(log, "`log` must not be null");
    }

    /// Reads and parses an FXML file from the specified path using the default UTF-8 encoding.
    ///
    /// This is a convenience method that delegates to [#readFXML(Path, Charset)] with [StandardCharsets#UTF_8].
    ///
    /// @param fxmlFile The path to the FXML file to be read and parsed
    /// @return A [ParsedFXML] instance containing the extracted headers, structure, and derived class name
    /// @throws MojoExecutionException If an I/O error occurs while reading the file or if XML parsing fails
    /// @throws NullPointerException   If the provided `fxmlFile` is `null`
    public ParsedFXML readFXML(Path fxmlFile) throws MojoExecutionException {
        return readFXML(fxmlFile, StandardCharsets.UTF_8);
    }

    /// Reads and parses an FXML file with the specified character set.
    ///
    /// This method performs the parsing in two distinct passes:
    /// 1. **Header Extraction**: It first streams the file lines to find and extract `<?import ... ?>`
    ///    and `<?language ... ?>` processing instructions.
    ///    It uses an optimized stream approach that skips empty lines and stops processing headers once it finds
    ///    the first non-header element.
    /// 2. **XML Structure Parsing**: It then performs a full XML parse using a `DocumentBuilder`.
    ///    The parser is configured to be namespace-aware and to preserve comments while ignoring element-content
    ///    whitespace.
    ///
    /// Finally, it derives a Java-compatible class name from the FXML filename and returns all information in
    /// a [ParsedFXML] record.
    ///
    /// @param fxmlFile The path to the FXML file to be read and parsed
    /// @param charset  The character set to use for reading the file content
    /// @return A [ParsedFXML] instance containing all the information extracted from the FXML document
    /// @throws MojoExecutionException If the file cannot be read, or if the XML content is malformed
    /// @throws NullPointerException   If either `fxmlFile` or `charset` is `null`
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
            documentBuilderFactory.setExpandEntityReferences(false);
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

    /// Parses a standard XML node and its entire subtree recursively into an internal model.
    ///
    /// The processing logic for each node follows these steps:
    /// 1. **Name Extraction**: Retrieves the XML tag name.
    /// 2. **Property Extraction**: Iterates through the node's attributes,
    ///    filtering out XML namespace declarations (`xmlns`), and maps them to a property map.
    /// 3. **Child Element Processing**: Recursively processes all child nodes that are of type `ELEMENT_NODE` to build
    ///    the hierarchy of [ParsedXMLStructure] objects.
    /// 4. **Comment Extraction**: Collects all comments directly nested within this node.
    /// 5. **Text Content Analysis**: If the node has no child elements,
    ///    it attempts to extract and combine all direct text and CDATA child nodes.
    ///
    /// @param node  The DOM `Node` representing the current XML element being parsed
    /// @param depth The current recursion depth, primarily used for formatted debug logging
    /// @return A [ParsedXMLStructure] representing the node, its attributes, child elements, comments, and text content
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

    /// Combines the text content from all direct child text nodes into a single string.
    ///
    /// This method is only applied to nodes that do not contain any child elements.
    /// It collects the values from all child nodes whose type is in [TEXT_NODE_TYPES] (standard text or CDATA),
    /// joins them together, and performs indentation stripping via [Utils#stripIndentNonBlankLines].
    ///
    /// If the node has child elements or if the resulting combined text is blank, an empty [Optional] is returned.
    ///
    /// @param children   The list of already parsed child elements (used to check if any exist)
    /// @param childNodes The full list of raw XML child nodes to scan for text content
    /// @return An [Optional] containing the combined, cleaned text content, or empty if none is available
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

    /// Derives a valid Java class name from the filename of the specified FXML file.
    ///
    /// The process involves:
    /// 1. Extracting the filename from the path.
    /// 2. Removing the file extension.
    /// 3. Replacing all non-alphanumeric characters (any character matching `\W`) with an underscore
    ///    to ensure the resulting name can be used as a Java identifier for the generated class.
    ///
    /// @param fxmlFile The path to the FXML file whose name will be used
    /// @return A string representing the derived Java-compatible class name
    private String deriveClassName(Path fxmlFile) {
        String fileName = fxmlFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        return NON_NAME_CHAR_PATTERN.matcher(baseName).replaceAll("_");
    }
}
