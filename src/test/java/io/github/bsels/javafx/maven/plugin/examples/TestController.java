package io.github.bsels.javafx.maven.plugin.examples;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;

public class TestController {
    public Button myButton;

    private void onButtonClick(ActionEvent event) {
        System.out.println("Button clicked");
        System.out.println(event);
    }
}
