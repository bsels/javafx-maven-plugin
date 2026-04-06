package io.github.bsels.javafx.maven.plugin.examples;

import javafx.event.ActionEvent;

public class TestController {
    private void onButtonClick(ActionEvent event) {
        System.out.println("Button clicked");
        System.out.println(event);
    }
}
