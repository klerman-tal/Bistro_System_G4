package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SubscribersReportDTO;
import logicControllers.ReportsController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for generating and retrieving the
 * subscribers activity report.
 * <p>
 * This handler processes report requests for a specific month and year,
 * delegates the report-building logic to the {@link ReportsController}, and
 * returns the aggregated report data to the client.
 * </p>
 */
public class GetSubscribersReportHandler implements RequestHandler {

	private final ReportsController reportsController;

	/**
	 * Constructs a handler with the required reports controller dependency.
	 */
	public GetSubscribersReportHandler(ReportsController reportsController) {
		this.reportsController = reportsController;
	}

	/**
	 * Handles a request to retrieve the subscribers report.
	 * <p>
	 * The method extracts the requested year and month from the DTO, builds the
	 * subscribers report using the reports controller, and sends the resulting
	 * report data back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		SubscribersReportDTO req = (SubscribersReportDTO) request.getData();

		SubscribersReportDTO result = reportsController.buildSubscribersReport(req.getYear(), req.getMonth());

		System.out.println("Sending Subscribers Reports to client");

		client.sendToClient(new ResponseDTO(true, "Subscribers report loaded", result));
	}
}
