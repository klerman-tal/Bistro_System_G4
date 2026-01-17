package application;

import java.io.*;
import java.util.ArrayList;

import dto.NotificationDTO;
import dto.ResponseDTO;
import ocsf.client.AbstractClient;
import interfaces.ChatIF;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import network.ClientResponseHandler;

/**
 * OCSF client implementation for the Bistro application.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintains a TCP connection to the server using {@link AbstractClient}.</li>
 *   <li>Receives server messages and routes them by type:
 *     <ul>
 *       <li>{@link NotificationDTO}: shown as a JavaFX popup + optional simulated channel message in the UI logs.</li>
 *       <li>{@link ResponseDTO}: forwarded to the currently registered {@link ClientResponseHandler} (if set).</li>
 *       <li>Any other object: printed to the UI log via {@link ChatIF#display(String)}.</li>
 *     </ul>
 *   </li>
 *   <li>Allows controllers to register/unregister a {@link ClientResponseHandler} for async responses.</li>
 *   <li>Provides a simple API to send client UI messages to the server.</li>
 * </ul>
 * </p>
 *
 * <p><b>Threading:</b> Server callbacks may arrive on a non-JavaFX thread.
 * Any UI updates are wrapped with {@link Platform#runLater(Runnable)} to ensure they run
 * on the JavaFX Application Thread.</p>
 */
public class ChatClient extends AbstractClient {

  /** UI interface for logging and displaying messages (console/log area abstraction). */
  ChatIF clientUI;

  /**
   * Active response handler set by the current screen/controller.
   * When null, responses are logged to the UI instead of being handled by a controller.
   */
  private ClientResponseHandler responseHandler;

  /**
   * Creates a new ChatClient and opens the connection immediately.
   *
   * @param host server host/IP
   * @param port server port
   * @param clientUI UI logger/bridge used to display messages
   * @throws IOException if opening the connection fails
   */
  public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
    super(host, port);
    this.clientUI = clientUI;
    openConnection();
  }

  /**
   * Sets the response handler for incoming {@link ResponseDTO} messages.
   * <p>
   * Typically each JavaFX controller sets itself (or an adapter) as the handler
   * when it becomes active, and clears it on navigation back to avoid stale handlers.
   * </p>
   *
   * @param handler handler to receive responses; may be null to disable forwarding
   */
  public void setResponseHandler(ClientResponseHandler handler) {
      this.responseHandler = handler;
  }

  /**
   * Called automatically by OCSF when the server sends a message to this client.
   * <p>
   * Routing logic:
   * <ul>
   *   <li>If message is {@link NotificationDTO}: shows a popup and logs simulated channel message (if exists).</li>
   *   <li>If message is {@link ResponseDTO}: forwards to {@link #responseHandler} if set; otherwise logs it.</li>
   *   <li>Otherwise: logs {@code msg.toString()}.</li>
   * </ul>
   * </p>
   *
   * @param msg the received message object
   */
  public void handleMessageFromServer(Object msg) {

      // ✅ NotificationDTO (popup in app + simulated SMS text in logs)
      if (msg instanceof NotificationDTO n) {
          Platform.runLater(() -> showNotification(n));
          return;
      }

      // ✅ Standard response wrapper for requests
      if (msg instanceof ResponseDTO response) {
          if (responseHandler != null) {
              Platform.runLater(() -> responseHandler.handleResponse(response));
          } else {
              clientUI.display("Response: " + response.getMessage());
          }
          return;
      }

      // Fallback: print raw message
      clientUI.display(msg.toString());
  }

  /**
   * Sends a message from the client UI to the server.
   *
   * @param message payload sent to the server
   */
  public void handleMessageFromClientUI(ArrayList<String> message) {
    try {
      sendToServer(message);
    }
    catch(IOException e) {
      clientUI.display("Could not send message to server.  Terminating client.");
      quit();
    }
  }

  /**
   * Closes the connection (best effort) and terminates the application process.
   */
  public void quit() {
    try { closeConnection(); } catch(IOException e) {}
    System.exit(0);
  }

  /**
   * Displays an in-app notification popup for a {@link NotificationDTO}.
   * <p>
   * Behavior:
   * <ol>
   *   <li>Shows ONLY a safe user-facing message via JavaFX {@link Alert}.</li>
   *   <li>If channel simulation data exists (e.g., SMS/Email), logs it to the UI output area.</li>
   * </ol>
   * </p>
   *
   * @param n the notification data received from the server
   */
  private void showNotification(NotificationDTO n) {

      // 1) Show ONLY safe text in app popup (no code)
      Alert.AlertType alertType = switch (n.getType()) {
          case SUCCESS, INFO -> Alert.AlertType.INFORMATION;
          case WARNING -> Alert.AlertType.WARNING;
          case ERROR -> Alert.AlertType.ERROR;
      };

      Alert alert = new Alert(alertType);
      alert.setTitle("Bistro");
      alert.setHeaderText(null);
      alert.setContentText(n.getDisplayMessage());
      alert.showAndWait();

      // 2) Simulated SMS/Email content -> log area (contains code)
      if (n.getChannel() != null && n.getChannelMessage() != null && !n.getChannelMessage().isBlank()) {
          clientUI.display("[" + n.getChannel() + "-SIM] " + n.getChannelMessage());
      }
  }

  /**
   * OCSF hook: called when a connection-level exception occurs.
   * <p>
   * If a {@link ClientResponseHandler} is set, it is notified via {@link ClientResponseHandler#handleConnectionError(Exception)}.
   * The error is also logged to {@link #clientUI}.
   * </p>
   *
   * @param exception the connection exception
   */
  @Override
  protected void connectionException(Exception exception) {
      if (responseHandler != null) {
          Platform.runLater(() -> responseHandler.handleConnectionError(exception));
      }
      clientUI.display("Connection error: " + exception.getMessage());
  }

  /**
   * Returns the currently registered response handler (may be null).
   *
   * @return current {@link ClientResponseHandler}
   */
  public ClientResponseHandler getResponseHandler() {
      return responseHandler;
  }
}
