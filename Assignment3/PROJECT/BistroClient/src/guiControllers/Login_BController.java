package guiControllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import application.ClientUI;

/**
 * JavaFX controller for the login choice screen.
 *
 * <p>
 * This controller routes the user to either the subscriber login screen or the
 * guest login screen, and injects the shared {@link application.ChatClient}
 * instance from {@link ClientUI} into the next controller. Navigation is
 * performed by swapping the current scene root (without creating a new
 * {@link Scene}).
 * </p>
 */
public class Login_BController {

	/**
	 * Navigates to the subscriber login screen and injects the shared client
	 * instance.
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleSubscriberChoice(ActionEvent event) {

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/SubscriberLogin.fxml"));
			Parent root = loader.load();

			SubscriberLoginController nextController = loader.getController();
			nextController.setClient(application.ClientUI.client);

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

			Scene scene = stage.getScene();
			if (scene == null) {
				stage.setScene(new Scene(root));
			} else {
				scene.setRoot(root);
			}

			stage.setMaximized(true);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Navigates to the guest login screen and injects the shared client instance.
	 *
	 * @param event the JavaFX action event
	 */
	@FXML
	private void handleGuestChoice(ActionEvent event) {

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/GuestLogin.fxml"));
			Parent root = loader.load();

			GuestLoginController nextController = loader.getController();
			nextController.setClient(application.ClientUI.client);

			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

			Scene scene = stage.getScene();
			if (scene == null) {
				stage.setScene(new Scene(root));
			} else {
				scene.setRoot(root);
			}

			stage.setMaximized(true);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
