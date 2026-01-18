package guiControllers;

import application.ChatClient;
import entities.User;
import interfaces.ClientActions;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX controller for the reservation menu screen.
 *
 * <p>
 * This screen provides navigation shortcuts for common guest/subscriber flows:
 * creating a reservation, canceling a reservation, joining the waiting list,
 * and canceling a waiting entry.
 * </p>
 *
 * <p>
 * The controller preserves the current session by passing {@link User} and
 * {@link ChatClient} to the next screens, and performs navigation by swapping
 * the current scene root.
 * </p>
 */
public class ReservationMenu_BController {

	@FXML
	private BorderPane rootPane;

	private User user;
	private ChatClient chatClient;
	private ClientActions clientActions;

	/**
	 * Injects the current session context.
	 *
	 * @param user       the current logged-in user
	 * @param chatClient the client used to communicate with the server
	 */
	public void setClient(User user, ChatClient chatClient) {
		this.user = user;
		this.chatClient = chatClient;
	}

	/**
	 * Injects an optional API interface that other screens may require.
	 *
	 * @param clientActions a client-side actions interface used by some controllers
	 */
	public void setClientActions(ClientActions clientActions) {
		this.clientActions = clientActions;
	}

	/**
	 * Opens the "Create Reservation" screen and injects the current session
	 * context.
	 */
	@FXML
	private void onCreateReservation() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/TableReservation_B.fxml"));
			Parent root = loader.load();

			TableReservation_BController controller = loader.getController();
			if (controller != null) {
				controller.setClient(user, chatClient);
				controller.setBackFxml("/gui/ReservationMenu_B.fxml");
			}

			switchRoot(root, "Bistro - Create Reservation");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens the "Cancel Reservation" screen and injects the current session
	 * context.
	 */
	@FXML
	private void onCancelReservation() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CancelReservation_B.fxml"));
			Parent root = loader.load();

			CancelReservation_BController controller = loader.getController();
			if (controller != null) {
				controller.setClient(user, chatClient);
				controller.setBackFxml("/gui/ReservationMenu_B.fxml");
			}

			switchRoot(root, "Bistro - Cancel Reservation");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens the "Join Waiting List" screen and injects the current session context.
	 */
	@FXML
	private void onJoinWaiting() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/JoinWaiting_B.fxml"));
			Parent root = loader.load();

			JoinWaiting_BController controller = loader.getController();
			if (controller != null) {
				controller.setClient(user, chatClient);
				controller.setBackFxml("/gui/ReservationMenu_B.fxml");
			}

			switchRoot(root, "Bistro - Join Waiting List");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens the "Cancel Waiting" screen and injects the current session context.
	 */
	@FXML
	private void onCancelWaiting() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/CancelWaiting_B.fxml"));
			Parent root = loader.load();

			CancelWaiting_BController controller = loader.getController();
			if (controller != null) {
				controller.setClient(user, chatClient);
				controller.setBackFxml("/gui/ReservationMenu_B.fxml");
			}

			switchRoot(root, "Bistro - Cancel Waiting List");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Navigates back to the main menu screen.
	 */
	@FXML
	private void onBack() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/Menu_B.fxml"));
			Parent root = loader.load();

			Object controller = loader.getController();

			// ✅ ALWAYS pass the real logged-in user to Menu
			User logged = application.ClientSession.getLoggedInUser();

			if (controller != null && logged != null && chatClient != null) {
				try {
					controller.getClass().getMethod("setClient", User.class, ChatClient.class).invoke(controller,
							logged, chatClient);
				} catch (Exception ignored) {
				}
			}

			switchRoot(root, "Bistro - Main Menu");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads an FXML screen from {@code /gui/}, injects session context and optional
	 * {@link ClientActions}, then navigates by swapping the current scene root.
	 *
	 * @param fxmlName the FXML file name under {@code /gui/}
	 * @param title    the window title suffix
	 */
	private void openWindow(String fxmlName, String title) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
			Parent root = loader.load();

			Object controller = loader.getController();

			if (controller != null && clientActions != null) {
				try {
					controller.getClass().getMethod("setClientActions", ClientActions.class).invoke(controller,
							clientActions);
				} catch (Exception ignored) {
				}
			}

			if (controller != null && user != null && chatClient != null) {
				try {
					controller.getClass().getMethod("setClient", User.class, ChatClient.class).invoke(controller, user,
							chatClient);
				} catch (Exception ignored) {
				}
			}

			switchRoot(root, "Bistro - " + title);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Replaces the current scene root with the provided root node. This navigation
	 * approach keeps the same stage and preserves window state.
	 *
	 * @param root  the new root node to display
	 * @param title the window title to set
	 */
	private void switchRoot(Parent root, String title) {
		Stage stage = (Stage) rootPane.getScene().getWindow();
		Scene scene = stage.getScene();

		if (scene == null) {
			stage.setScene(new Scene(root));
		} else {
			scene.setRoot(root);
		}

		stage.setTitle(title);
		stage.setMaximized(true);
		stage.show();
	}
}
