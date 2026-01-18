package guiControllers;

import application.ChatClient;
import entities.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import javafx.scene.text.Text;

/**
 * JavaFX controller for the reports menu screen.
 *
 * <p>
 * This screen serves as an entry point to the different reports (e.g., time
 * report and subscribers report). It also presents a short summary describing
 * when monthly reports become available, based on the "last full month" rule
 * used across report screens.
 * </p>
 *
 * <p>
 * Navigation is performed by swapping the current scene root to keep the same
 * window instance and preserve the maximized state.
 * </p>
 */
public class ReportsMenuController {

	@FXML
	private BorderPane rootPane;

	@FXML
	private Label lblMessage;

	@FXML
	private Label lblReportsSummaryTitle;
	@FXML
	private Text txtReportsSummaryText;

	private User user;
	private ChatClient chatClient;

	/**
	 * Injects the current session context and updates the reports summary text.
	 *
	 * @param user       the current logged-in user
	 * @param chatClient the network client used to communicate with the server
	 */
	public void setClient(User user, ChatClient chatClient) {
		this.user = user;
		this.chatClient = chatClient;

		updateReportsSummaryText();
	}

	/**
	 * Updates the general reports explanation text. Uses the same logic as all
	 * report screens (last full month).
	 */
	private void updateReportsSummaryText() {

		YearMonth reportMonth = YearMonth.now().minusMonths(1);

		DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");
		DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		YearMonth nextReportMonth = reportMonth.plusMonths(1);
		String publishDate = nextReportMonth.plusMonths(1).atDay(1).format(dateFmt);

		txtReportsSummaryText.setText(
				"The " + nextReportMonth.format(monthFmt) + " reports will be available starting " + publishDate + ".");
	}

	/**
	 * Loads an FXML screen, injects the current session context into the target
	 * controller (when supported), and navigates by swapping the current scene
	 * root.
	 *
	 * @param fxmlName the FXML file name under {@code /gui/}
	 * @param title    the window title suffix
	 */
	private void openWindow(String fxmlName, String title) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + fxmlName));
			Parent root = loader.load();

			Object controller = loader.getController();

			if (controller instanceof TimeReportController) {
				((TimeReportController) controller).setClient(user, chatClient);

			} else if (controller instanceof SubscribersReportController) {
				((SubscribersReportController) controller).setClient(user, chatClient);
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
	 * Opens the time report screen.
	 */
	@FXML
	private void onTimeReportClicked() {
		openWindow("TimeReport.fxml", "Time Report");
	}

	/**
	 * Opens the subscribers report screen.
	 */
	@FXML
	private void onSubscribersReportClicked() {
		openWindow("SubscribersReport.fxml", "Subscribers Report");
	}

	/**
	 * Navigates back to the restaurant management screen.
	 */
	@FXML
	private void onBackClicked() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/RestaurantManagement_B.fxml"));
			Parent root = loader.load();

			Object controller = loader.getController();
			if (controller instanceof RestaurantManagement_BController) {
				((RestaurantManagement_BController) controller).setClient(user, chatClient);
			}

			Stage stage = (Stage) rootPane.getScene().getWindow();
			Scene scene = stage.getScene();

			if (scene == null) {
				stage.setScene(new Scene(root));
			} else {
				scene.setRoot(root);
			}

			stage.setTitle("Bistro - Restaurant Management");
			stage.setMaximized(true);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Failed to go back.");
		}
	}

	/**
	 * Displays a status/error message on the screen.
	 *
	 * @param msg the message to display
	 */
	private void showMessage(String msg) {
		lblMessage.setText(msg);
		lblMessage.setVisible(true);
		lblMessage.setManaged(true);
	}
}
