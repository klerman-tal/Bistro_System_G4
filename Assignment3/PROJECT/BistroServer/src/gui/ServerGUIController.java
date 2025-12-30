package gui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerGUIController {

    @FXML
    private TextArea txtLog;

    public void addLog(String msg) {
        txtLog.appendText(msg + "\n");
    }
}
