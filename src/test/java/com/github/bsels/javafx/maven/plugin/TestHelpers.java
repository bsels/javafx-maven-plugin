package com.github.bsels.javafx.maven.plugin;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

public class TestHelpers {

    public static Path getTestResourcePath(String path) {
        try {
            return Path.of(
                    Objects.requireNonNull(TestHelpers.class.getResource(path))
                            .toURI()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
