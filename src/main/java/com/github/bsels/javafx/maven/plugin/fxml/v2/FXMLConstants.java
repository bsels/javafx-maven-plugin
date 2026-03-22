package com.github.bsels.javafx.maven.plugin.fxml.v2;

/// Constants for FXML elements, attributes, properties, and prefixes.
public final class FXMLConstants {
    // region: Attributes
    /// The `charset` attribute.
    public static final String CHARSET_ATTRIBUTE = "charset";
    /// The `fx:constant` attribute.
    public static final String FX_CONSTANT_ATTRIBUTE = "fx:constant";
    /// The `fx:controller` attribute.
    public static final String FX_CONTROLLER_ATTRIBUTE = "fx:controller";
    /// The `fx:copy` attribute.
    public static final String FX_COPY_ATTRIBUTE = "fx:copy";
    /// The `fx:factory` attribute.
    public static final String FX_FACTORY_ATTRIBUTE = "fx:factory";
    /// The `fx:id` attribute.
    public static final String FX_ID_ATTRIBUTE = "fx:id";
    /// The `fx:include` attribute.
    public static final String FX_INCLUDE_ATTRIBUTE = "fx:include";
    /// The `fx:reference` attribute.
    public static final String FX_REFERENCE_ATTRIBUTE = "fx:reference";
    /// The `fx:value` attribute.
    public static final String FX_VALUE_ATTRIBUTE = "fx:value";
    /// The `source` attribute.
    public static final String SOURCE_ATTRIBUTE = "source";
    /// The `type` attribute.
    public static final String TYPE_ATTRIBUTE = "type";
    // endregion
    // region: Elements
    /// The `fx:root` element.
    public static final String FX_ROOT_ELEMENT = "fx:root";
    /// The `fx:define` element.
    public static final String FX_DEFINE_ELEMENT = "fx:define";
    /// The `fx:script` element.
    public static final String FX_SCRIPT_ELEMENT = "fx:script";
    // endregion
    // region: Prefixes
    /// The `\` prefix for escaping.
    public static final String ESCAPE_PREFIX = "\\";
    /// The `$` prefix for expressions.
    public static final String EXPRESSION_PREFIX = "$";
    /// The `@` prefix for locations.
    public static final String LOCATION_PREFIX = "@";
    /// The `#` prefix for method references.
    public static final String METHOD_REFERENCE_PREFIX = "#";
    /// The `%` prefix for translations.
    public static final String TRANSLATION_PREFIX = "%";
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
