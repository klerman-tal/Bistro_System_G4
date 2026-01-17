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

public class ChatClient extends AbstractClient
{ 
  ChatIF clientUI;
  private ClientResponseHandler responseHandler;

  public ChatClient(String host, int port, ChatIF clientUI) throws IOException 
  {
    super(host, port);
    this.clientUI = clientUI;
    openConnection();
  }

  public void setResponseHandler(ClientResponseHandler handler) {
      this.responseHandler = handler;
  }

  public void handleMessageFromServer(Object msg) 
  {
      // ✅ NotificationDTO (popup in app + simulated SMS text in logs)
      if (msg instanceof NotificationDTO n) {
          Platform.runLater(() -> showNotification(n));
          return;
      }

      if (msg instanceof ResponseDTO response) {
          if (responseHandler != null) {
              Platform.runLater(() -> responseHandler.handleResponse(response));
          } else {
              clientUI.display("Response: " + response.getMessage());
          }
          return;
      }

      clientUI.display(msg.toString());
  }

  public void handleMessageFromClientUI(ArrayList<String> message)  
  {
    try
    {
      sendToServer(message);
    }
    catch(IOException e)
    {
      clientUI.display("Could not send message to server.  Terminating client.");
      quit();
    }
  }

  public void quit()
  {
    try { closeConnection(); } catch(IOException e) {}
    System.exit(0);
  }

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

  @Override
  protected void connectionException(Exception exception) {
      if (responseHandler != null) {
          Platform.runLater(() -> responseHandler.handleConnectionError(exception));
      }
      clientUI.display("Connection error: " + exception.getMessage());
  }

  public ClientResponseHandler getResponseHandler() {
      return responseHandler;
  }
}
