package guiControllers;

import application.ChatClient;
import entities.Enums;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * JavaFX controller for the restaurant management main screen.
 *
 * <p>
 * This screen serves as a hub for management operations such as updating
 * tables, managing reservations, waiting list, opening hours, subscribers, and
 * current diners.
 * </p>
 *
 * <p>
 * Access to reports is restricted to {@link Enums.UserRole#RestaurantManager}
 * and is enforced both by UI visibility and by a runtime permission check.
 * </p>
 */
public class RestaurantManagement_BController {

	@FXML
	private BorderPane rootPane;
	@FXML
	private Label lblMessage;

	@FXML
	private Button btnReports;

	private User user;
	private ChatClient chatClient;

	/**
	 * Injects the current session context and applies role-based UI rules.
	 *
	 * @param user       the current logged-in user
	 * @param chatClient the client used to communicate with the server
	 */
	public void setClient(User user, ChatClient chatClient) {
		this.user = user;
		this.chatClient = chatClient;

		boolean isManager = user != null && user.getUserRole() == Enums.UserRole.RestaurantManager;

		if (btnReports != null) {
			btnReports.setVisible(isManager);
			btnReports.setManaged(isManager);
		}
	}

	/**
	 * Loads an FXML screen from {@code /gui/}, injects the session context if
	 * supported, and navigates by swapping the current scene root.
	 *
	 * @param fxmlName the FXML file name under {@code /gui/}
	 * @param title    the window title suffix
	 */
	private void openWindow(String fxmlName, String title) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
			Parent root = loader.load();

			Object controller = loader.getController();

			if (controller != null && user != null && chatClient != null) {
				try {
					controller.getClass().getMethod("setClient", User.class, ChatClient.class).invoke(controller, user,
							chatClient);
				} catch (Exception ignored) {
				}
			}

			Stage stage = (Stage) rootPane.getScene().getWindow();
			Scene scene = stage.getScene();

			if (scene == null) {
				stage.setScene(new Scene(root));
			} else {
				scene.setRoot(root);
			}

			stage.setTitle("Bistro - " + title);
			stage.setMaximized(true);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Failed to open: " + fxmlName);
		}
	}

	/**
	 * Navigates to the "Update Tables" screen.
	 */
	@FXML
	private void onUpdateTablesClicked() {
		openWindow("UpdateTables.fxml", "Update Tables");
	}

	/**
	 * Navigates to the "Waiting List" management screen.
	 */
	@FXML
	private void onWaitingListClicked() {
		openWindow("ManageWaitingList.fxml", "Waiting List");
	}

	/**
	 * Navigates to the opening hours management screen.
	 */
	@FXML
	private void onUpdateOpeningHoursClicked() {
		openWindow("opening.fxml", "Opening Hours");
	}

	/**
	 * Navigates to the subscribers management screen.
	 */
	@FXML
	private void onManageUsersClicked() {
		openWindow("manageSubscriber.fxml", "Manage Subscribers");
	}

	/**
	 * Navigates to the reservations management screen.
	 */
	@FXML
	private void onManageReservationClicked() {
		openWindow("ManageReservation.fxml", "Manage Reservations");
	}

	/**
	 * Navigates to the "Current Diners" management screen.
	 */
	@FXML
	private void onManageCurrentDinersClicked() {
		openWindow("ManageCurrentDiners.fxml", "Current Diners");
	}

	/**
	 * Navigates to the reports menu screen.
	 *
	 * <p>
	 * Includes a defense-in-depth permission check to prevent access by
	 * non-managers.
	 * </p>
	 */
	@FXML
	private void onReportsClicked() {
		if (user == null || user.getUserRole() != Enums.UserRole.RestaurantManager) {

			showMessage("Access denied. Managers only.");
			return;
		}

		openWindow("ReportsMenu.fxml", "Reports");
	}

	/**
	 * Navigates back to the main menu screen.
	 */
	@FXML
	private void onBackToMenuClicked() {
		openWindow("Menu_B.fxml", "Main Menu");
	}

	/**
	 * Displays a message on the screen.
	 *
	 * @param msg the message text to display
	 */
	private void showMessage(String msg) {
		if (lblMessage == null)
			return;
		lblMessage.setText(msg);
		lblMessage.setVisible(true);
		lblMessage.setManaged(true);
	}
}
