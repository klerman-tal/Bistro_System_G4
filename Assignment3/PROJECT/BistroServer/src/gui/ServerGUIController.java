package gui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * JavaFX controller responsible for displaying server-side log messages
 * in the server GUI.
 * <p>
 * This controller provides a simple visual log output area that allows
 * the server to append runtime messages, errors, and debug information
 * for monitoring purposes.
 * </p>
 */
public class ServerGUIController {

    /**
     * Text area used to display server log messages.
     * Injected from the corresponding FXML file.
     */
    @FXML
    private TextArea txtLog;

    /**
     * Appends a log message to the server log text area.
     * <p>
     * Each message is added on a new line, preserving the chronological
     * order of server events.
     * </p>
     *
     * @param msg the log message to display
     */
    public void addLog(String msg) {
        txtLog.appendText(msg + "\n");
    }
}
