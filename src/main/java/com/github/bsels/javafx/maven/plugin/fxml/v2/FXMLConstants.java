package com.github.bsels.javafx.maven.plugin.fxml.v2;

/// Constants for FXML elements, attributes, properties, and prefixes.
public final class FXMLConstants {
    // region: Attributes
    /// The `charset` attribute name.
    public static final String CHARSET_ATTRIBUTE = "charset";
    /// The `fx:constant` attribute name.
    public static final String FX_CONSTANT_ATTRIBUTE = "fx:constant";
    /// The `fx:controller` attribute name.
    public static final String FX_CONTROLLER_ATTRIBUTE = "fx:controller";
    /// The `fx:factory` attribute name.
    public static final String FX_FACTORY_ATTRIBUTE = "fx:factory";
    /// The `fx:id` attribute name.
    public static final String FX_ID_ATTRIBUTE = "fx:id";
    /// The `fx:value` attribute name.
    public static final String FX_VALUE_ATTRIBUTE = "fx:value";
    /// The `resources` attribute name.
    public static final String RESOURCES_ATTRIBUTE = "resources";
    /// The `source` attribute name.
    public static final String SOURCE_ATTRIBUTE = "source";
    /// The `type` attribute name.
    public static final String TYPE_ATTRIBUTE = "type";
    // endregion
    // region: Elements
    /// The `fx:copy` element name.
    public static final String FX_COPY_ELEMENT = "fx:copy";
    /// The `fx:include` element name.
    public static final String FX_INCLUDE_ELEMENT = "fx:include";
    /// The `fx:define` element name.
    public static final String FX_DEFINE_ELEMENT = "fx:define";
    /// The `fx:root` element name.
    public static final String FX_ROOT_ELEMENT = "fx:root";
    /// The `fx:script` element name.
    public static final String FX_SCRIPT_ELEMENT = "fx:script";
    /// The `fx:reference` element name.
    public static final String FX_REFERENCE_ELEMENT = "fx:reference";
    // endregion
    // region: Prefixes
    /// The `\` escape prefix.
    public static final String ESCAPE_PREFIX = "\\";
    /// The `$` expression prefix.
    public static final String EXPRESSION_PREFIX = "$";
    /// The `fx:` namespace prefix.
    public static final String FX_PREFIX = "fx:";
    /// The `@` location prefix.
    public static final String LOCATION_PREFIX = "@";
    /// The `#` method reference prefix.
    public static final String METHOD_REFERENCE_PREFIX = "#";
    /// The `%` translation prefix.
    public static final String TRANSLATION_PREFIX = "%";
    /// The `xmlns` namespace prefix.
    public static final String XML_NAMESPACE_PREFIX = "xmlns";
    // endregion
    // region: File Extensions
    /// The `.fxml` file extension.
    public static final String FXML_EXTENSION = ".fxml";
    // endregion

    /// Private constructor to prevent instantiation.
    private FXMLConstants() {
        // No instances needed;
    }
}
