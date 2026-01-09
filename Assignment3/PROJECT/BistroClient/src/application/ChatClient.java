// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
package application;

import java.io.*;
import java.util.ArrayList;

import dto.ResponseDTO;
import entities.Notification;
import ocsf.client.AbstractClient;
import interfaces.ChatIF;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import network.ClientResponseHandler;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000
 */
public class ChatClient extends AbstractClient
{
  //Instance variables **********************************************
  
  /**
   * The interface type variable.  It allows the implementation of 
   * the display method in the client.
   */
  ChatIF clientUI; 
//✨ הוספה חדשה - handler לתגובות מובנות
  private ClientResponseHandler responseHandler;

  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the chat client.
   *
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   */
  
  public ChatClient(String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor
    this.clientUI = clientUI;
    openConnection();
  }
  
//✨ הוספה חדשה - setter ל-handler
  public void setResponseHandler(ClientResponseHandler handler) {
      this.responseHandler = handler;
  }

  
  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
      // אם זו התרעה – קופץ חלון
      if (msg instanceof Notification n) {
          Platform.runLater(() -> showNotification(n));
          return;
      }
      
      // 2️⃣ ✨ חדש: אם זו תגובת DTO – העבר ל-handler
      if (msg instanceof ResponseDTO response) {
          if (responseHandler != null) {
              Platform.runLater(() -> responseHandler.handleResponse(response));
          } else {
              // fallback - הצג בממשק הרגיל
              clientUI.display("Response: " + response.getMessage());
          }
          return;
      }

      clientUI.display(msg.toString());
  }

  /**
   * This method handles all data coming from the UI            
   *
   * @param message The message from the UI.    
   */
  public void handleMessageFromClientUI(ArrayList<String> message)  
  {
    try
    {
   	sendToServer(message);
    }
    catch(IOException e)
    {
      clientUI.display
        ("Could not send message to server.  Terminating client.");
      quit();
    }
  }
  
  /**
   * This method terminates the client.
   */
  public void quit()
  {
    try
    {
      closeConnection();
    }
    catch(IOException e) {}
    System.exit(0);
  }
  
  private void showNotification(Notification n) {

	    Alert.AlertType alertType = switch (n.getType()) {
	        case SUCCESS, INFO -> Alert.AlertType.INFORMATION;
	        case WARNING -> Alert.AlertType.WARNING;
	        case ERROR -> Alert.AlertType.ERROR;
	    };

	    Alert alert = new Alert(alertType);
	    alert.setTitle("Bistro");
	    alert.setHeaderText(null);
	    alert.setContentText(n.getMessage());
	    alert.showAndWait();
	}
  
//✨ חדש: טיפול בשגיאות חיבור
  @Override
  protected void connectionException(Exception exception) {
      if (responseHandler != null) {
          Platform.runLater(() -> responseHandler.handleConnectionError(exception));
      }
      clientUI.display("Connection error: " + exception.getMessage());
  }

}

