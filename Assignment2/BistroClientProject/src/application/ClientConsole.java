// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 
package application;

import java.io.*;
import java.util.ArrayList;

import ocsf.client.AbstractClient;
import interfaces.ChatIF;    // זה היחיד שאת צריכה כאן


/**
 * This class constructs the UI for a chat client.  It implements the
 * chat interface in order to activate the display() method.
 * Warning: Some of the code here is cloned in ServerConsole 
 *
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Dr Timothy C. Lethbridge  
 * @author Dr Robert Lagani&egrave;re
 * @version July 2000
 */
public class ClientConsole implements ChatIF 
{
  //Class variables *************************************************
  
  /**
   * The default port to connect on.
   */
  final public static int DEFAULT_PORT = 5556;
  
  //Instance variables **********************************************
  
  /**
   * The instance of the client that created this ConsoleChat.
   */
  ChatClient client;

  
  //Constructors ****************************************************

  /**
   * Constructs an instance of the ClientConsole UI.
   *
   * @param host The host to connect to.
   * @param port The port to connect on.
   */
  public ClientConsole(String host, int port) 
  {
    try 
    {
      client= new ChatClient(host, port, this);
    } 
    catch(IOException exception) 
    {
      System.out.println("Error: Can't setup connection!"
                + " Terminating client.");
      System.exit(1);
    }
  }

  
  //Instance methods ************************************************
  
  /**
   * This method waits for input from the console.  Once it is 
   * received, it sends it to the client's message handler.
   */
  public void accept() 
  {
      BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

      while (true) 
      {
          try 
          {
              // --- MENU ---
              System.out.println("\n===== Bistro Client Menu =====");
              System.out.println("1. Print all orders");
              System.out.println("2. Update order date");
              System.out.println("3. Update number of guests");
              System.out.println("4. Exit");
              System.out.print("Choose an option: ");

              String choice = console.readLine();

              switch (choice) 
              {
                  case "1": // PRINT ORDERS
                  {
                      ArrayList<String> msg = new ArrayList<>();
                      msg.add("PRINT_ORDERS");
                      client.handleMessageFromClientUI(msg);
                      break;
                  }

                  case "2": // UPDATE DATE
                  {
                      ArrayList<String> msg = new ArrayList<>();
                      msg.add("UPDATE_DATE");

                      System.out.print("Enter order number: ");
                      msg.add(console.readLine());

                      System.out.print("Enter new date (yyyy-mm-dd): ");
                      msg.add(console.readLine());

                      client.handleMessageFromClientUI(msg);
                      break;
                  }

                  case "3": // UPDATE GUEST COUNT
                  {
                      ArrayList<String> msg = new ArrayList<>();
                      msg.add("UPDATE_GUESTS");

                      System.out.print("Enter order number: ");
                      msg.add(console.readLine());

                      System.out.print("Enter new guest count: ");
                      msg.add(console.readLine());

                      client.handleMessageFromClientUI(msg);
                      break;
                  }

                  case "4":
                      System.out.println("Exiting client...");

                      // שולחים פקודת לוגאאוט לסרבר לפני שסוגרים
                      ArrayList<String> logoutMsg = new ArrayList<>();
                      logoutMsg.add("CLIENT_LOGOUT");
                      client.handleMessageFromClientUI(logoutMsg);

                      client.quit();
                      return;

                  default:
                      System.out.println("Invalid option. Please try again.");
              }

          } 
          catch (Exception ex) 
          {
              System.out.println("Unexpected error while reading from console!");
          }
      }
  }


  /**
   * This method overrides the method in the ChatIF interface.  It
   * displays a message onto the screen.
   *
   * @param message The string to be displayed.
   */
  public void display(String message) 
  {
    System.out.println("> " + message);
  }

  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of the Client UI.
   *
   * @param args[0] The host to connect to.
   */
  public static void main(String[] args) 
  {
    String host = "";
    int port = 0;  //The port number

    try
    {
      host = args[0];
    }
    catch(ArrayIndexOutOfBoundsException e)
    {
      host = "localhost";
    }
    ClientConsole chat= new ClientConsole(host, DEFAULT_PORT);
    chat.accept();  //Wait for console data
  }
}
//End of ConsoleChat class
