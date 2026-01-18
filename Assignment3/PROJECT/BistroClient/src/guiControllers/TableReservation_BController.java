package guiControllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import application.ChatClient;
import dto.ResponseDTO;
import entities.OpeningHouers;
import entities.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import network.ClientAPI;
import network.ClientResponseHandler;

public class TableReservation_BController implements ClientResponseHandler {

	private User user;
	private ChatClient chatClient;
	private ClientAPI api;
	private ArrayList<OpeningHouers> cachedOpeningHours;
	private String backFxml;

	@FXML
	private BorderPane rootPane;
	@FXML
	private DatePicker datePicker;
	@FXML
	private ComboBox<String> cmbHour;
	@FXML
	private TextField txtGuests;
	@FXML
	private Label lblMessage;

	/* ================= CONTEXT ================= */

	/**
	 * JavaFX controller for creating a table reservation.
	 * <p>
	 * Responsibilities:
	 * <ul>
	 * <li>Collects reservation details (date, time slot, guests) from the
	 * user.</li>
	 * <li>Requests opening hours and available time slots from the server.</li>
	 * <li>Sends a reservation creation request and handles server responses.</li>
	 * <li>Supports navigation back to the originating screen using an injected FXML
	 * path.</li>
	 * </ul>
	 * </p>
	 * This controller implements {@link ClientResponseHandler} to receive async
	 * responses via {@link ChatClient}.
	 */
	public void setBackFxml(String backFxml) {
		this.backFxml = backFxml;
	}

	/**
	 * Injects the session context and initializes server communication for this
	 * screen.
	 * <p>
	 * Also applies date restrictions to the DatePicker (from today to one month
	 * ahead) and triggers an opening-hours request.
	 * </p>
	 *
	 * @param user       the current user performing the reservation
	 * @param chatClient the active client connection to the server
	 */
	public void setClient(User user, ChatClient chatClient) {
		this.user = user;
		this.chatClient = chatClient;
		this.api = new ClientAPI(chatClient);

		if (this.chatClient != null) {
			this.chatClient.setResponseHandler(this);
		}

		// Date picker limits: today -> +1 month
		datePicker.setDayCellFactory(picker -> new DateCell() {
			@Override
			public void updateItem(LocalDate date, boolean empty) {
				super.updateItem(date, empty);

				if (empty || date == null) {
					setDisable(true);
					return;
				}

				LocalDate today = LocalDate.now();
				LocalDate maxDate = today.plusMonths(1);

				if (date.isBefore(today) || date.isAfter(maxDate)) {
					setDisable(true);
					setStyle("-fx-background-color: #eeeeee;");
				}
			}
		});

		try {
			api.getOpeningHours();
		} catch (IOException e) {
			showMessage("Failed to load opening hours", "red");
		}
	}

	/* ================= ACTIONS ================= */

	/**
	 * Triggered when the user selects a date.
	 * <p>
	 * Clears the time ComboBox and requests available time slots from the server
	 * using the selected date and the current number of guests (or a default
	 * value).
	 * </p>
	 */
	@FXML
	private void onDateSelected() {
		hideMessage();
		cmbHour.getItems().clear();
		cmbHour.setValue(null);

		LocalDate selected = datePicker.getValue();
		if (selected == null)
			return;

		int guests = parseGuestsOrDefault(1);

		try {
			api.getAvailableTimesForDate(selected, guests);
		} catch (IOException e) {
			showMessage("Failed to load available times", "red");
		}
	}

	/**
	 * Triggered when the user clicks the "Create" button.
	 * <p>
	 * Validates user input, enforces a minimum 1-hour advance rule for same-day
	 * reservations, and sends the reservation creation request to the server.
	 * </p>
	 */
	@FXML
	private void onCreateClicked() {
		hideMessage();

		if (!validateInputs())
			return;

		try {
			LocalDate date = datePicker.getValue();
			LocalTime time = LocalTime.parse(cmbHour.getValue());

			if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now().plusHours(1))) {
				showMessage("Reservations must be at least 1 hour in advance.", "red");
				return;
			}

			api.createReservation(date, time, Integer.parseInt(txtGuests.getText().trim()), user);

			showMessage("Sending request...", "blue");

		} catch (Exception e) {
			showMessage("Error creating reservation.", "red");
		}
	}

	/* ================= BACK ================= */

	/**
	 * Navigates back to the previous screen defined by {@code backFxml}.
	 * <p>
	 * Clears this controller as the current response handler before navigating.
	 * Reuses the existing {@link Scene} and replaces only the root node.
	 * </p>
	 */
	@FXML
	private void onBackClicked() {
		if (backFxml == null || backFxml.isBlank())
			return;

		if (chatClient != null) {
			chatClient.setResponseHandler(null);
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(backFxml));
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

			scene.setRoot(root);
			stage.centerOnScreen();
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* ================= SERVER RESPONSE ================= */

	/**
	 * Handles responses from the server related to:
	 * <ul>
	 * <li>Opening hours loading</li>
	 * <li>Available time slots loading</li>
	 * <li>Reservation creation success/failure</li>
	 * </ul>
	 * Updates UI safely via {@link Platform#runLater(Runnable)}.
	 *
	 * @param response the server response wrapper
	 */
	@Override
	public void handleResponse(ResponseDTO response) {
		Platform.runLater(() -> {
			if (response == null)
				return;

			Object data = response.getData();

			// 1) OpeningHours list
			if (response.isSuccess() && data instanceof ArrayList<?> list && isOpeningHoursList(list)) {
				@SuppressWarnings("unchecked")
				ArrayList<OpeningHouers> oh = (ArrayList<OpeningHouers>) data;
				cachedOpeningHours = oh;
				return;
			}

			// 2) Available times list (success)
			if (response.isSuccess() && data instanceof ArrayList<?> list && isLocalTimeList(list)) {
				@SuppressWarnings("unchecked")
				ArrayList<LocalTime> times = (ArrayList<LocalTime>) data;

				updateHoursComboFromLocalTimes(times);

				if (times.isEmpty()) {
					showMessage("No available tables match the selected day requirements.", "red");
				} else {
					hideMessage();
				}
				return;
			}

			// 3) Reservation created
			if (response.isSuccess()) {
				String msg = "Reservation Confirmed";
				if (response.getData() instanceof String) {
					msg += "\nConfirmation Code: " + response.getData();
				}
				showSuccessAlert("Reservation Confirmed", msg);
				return;
			}

			// 4) Reservation failed but got suggested times (LocalTime list)
			if (!response.isSuccess() && data instanceof ArrayList<?> list && isLocalTimeList(list)) {
				@SuppressWarnings("unchecked")
				ArrayList<LocalTime> times = (ArrayList<LocalTime>) data;

				updateHoursComboFromLocalTimes(times);

				if (times.isEmpty()) {
					showMessage("No available tables match the selected day requirements.", "red");
				} else {
					showMessage("The available time slots have been updated based on your selection.", "blue");
				}
				return;
			}

			// fallback
			showMessage(response.getMessage(), "red");
		});
	}

	/**
	 * Updates the hour ComboBox options based on the provided list of
	 * {@link LocalTime}.
	 *
	 * @param times list of available time slots (may be null or empty)
	 */
	private void updateHoursComboFromLocalTimes(ArrayList<LocalTime> times) {
		cmbHour.getItems().clear();
		cmbHour.setValue(null);

		if (times == null)
			return;

		for (LocalTime t : times) {
			cmbHour.getItems().add(t.toString());
		}
	}

	/**
	 * Checks whether the given list contains {@link OpeningHouers} elements.
	 *
	 * @param list a list returned from the server
	 * @return true if the list is non-empty and contains opening-hours objects
	 */
	private boolean isOpeningHoursList(ArrayList<?> list) {
		if (list == null || list.isEmpty())
			return false;
		return list.get(0) instanceof OpeningHouers;
	}

	/**
	 * Checks whether the given list is a {@link LocalTime} list.
	 * <p>
	 * Empty lists are considered valid time lists to support "no available times"
	 * responses.
	 * </p>
	 *
	 * @param list a list returned from the server
	 * @return true if the list is empty or contains {@link LocalTime} elements
	 */
	private boolean isLocalTimeList(ArrayList<?> list) {
		if (list == null)
			return true;
		if (list.isEmpty())
			return true;
		return list.get(0) instanceof LocalTime;
	}

	/* ================= HELPERS ================= */

	/**
	 * Parses the guests input field and returns a valid guest count. If parsing
	 * fails or the value is non-positive, returns the provided default.
	 *
	 * @param def default guest count to use when input is invalid
	 * @return parsed guests count or {@code def}
	 */
	private int parseGuestsOrDefault(int def) {
		try {
			String s = txtGuests.getText();
			if (s == null || s.isBlank())
				return def;
			int g = Integer.parseInt(s.trim());
			return (g > 0) ? g : def;
		} catch (Exception e) {
			return def;
		}
	}

	/**
	 * Validates user inputs (date, hour, and guest count). Shows an error message
	 * when validation fails.
	 *
	 * @return true if all inputs are valid; otherwise false
	 */
	private boolean validateInputs() {
		if (datePicker.getValue() == null) {
			showMessage("Please select a date.", "red");
			return false;
		}
		if (cmbHour.getValue() == null) {
			showMessage("Please select an hour.", "red");
			return false;
		}
		try {
			int g = Integer.parseInt(txtGuests.getText().trim());
			if (g <= 0)
				throw new Exception();
		} catch (Exception e) {
			showMessage("Invalid number of guests.", "red");
			return false;
		}
		return true;
	}

	/**
	 * Displays a message in the UI with the provided text color.
	 *
	 * @param msg   the message text to display
	 * @param color a JavaFX color name/string (e.g., "red", "blue")
	 */
	private void showMessage(String msg, String color) {
		lblMessage.setText(msg);
		lblMessage.setStyle("-fx-text-fill: " + color);
		lblMessage.setVisible(true);
		lblMessage.setManaged(true);
	}

	/**
	 * Hides the message label from the UI and removes it from layout calculations.
	 */
	private void hideMessage() {
		lblMessage.setVisible(false);
		lblMessage.setManaged(false);
	}

	/**
	 * Shows an informational success alert dialog.
	 *
	 * @param title   alert window title
	 * @param content alert content message
	 */
	private void showSuccessAlert(String title, String content) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * Called when a connection error occurs while this controller is active.
	 *
	 * @param e the connection exception
	 */
	@Override
	public void handleConnectionError(Exception e) {
		Platform.runLater(() -> showMessage("Connection lost.", "red"));
	}

	/**
	 * Called when the connection is closed while this controller is active.
	 */
	@Override
	public void handleConnectionClosed() {
	}
}
