package com.github.bsels.javafx.maven.plugin.fxml.v2.scripts;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXMLScriptTest {

    @Nested
    class FXMLFileScriptTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLFileScript script = new FXMLFileScript("path/to/script.js", StandardCharsets.UTF_8);
            assertThat(script.path()).isEqualTo("path/to/script.js");
            assertThat(script.charset()).isEqualTo(StandardCharsets.UTF_8);
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLFileScript(null, StandardCharsets.UTF_8))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`path` must not be null");
            assertThatThrownBy(() -> new FXMLFileScript("path", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`charset` must not be null");
        }
    }

    @Nested
    class FXMLSourceScriptTest {
        @Test
        void shouldCreateWithValidParams() {
            FXMLSourceScript script = new FXMLSourceScript("println('hello')");
            assertThat(script.source()).isEqualTo("println('hello')");
        }

        @Test
        void shouldThrowNpeForNull() {
            assertThatThrownBy(() -> new FXMLSourceScript(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("`source` must not be null");
        }
    }
}
