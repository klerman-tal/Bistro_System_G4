package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.TimeReportDTO;
import logicControllers.ReportsController;
import ocsf.server.ConnectionToClient;

/**
 * Server-side request handler responsible for generating and retrieving the
 * time-based reservations report.
 * <p>
 * This handler processes requests for a monthly time report, delegates the
 * report generation to the {@link ReportsController}, and returns aggregated
 * punctuality statistics to the client.
 * </p>
 */
public class GetTimeReportHandler implements RequestHandler {

	private final ReportsController reportsController;

	/**
	 * Constructs a handler with the required reports controller dependency.
	 */
	public GetTimeReportHandler(ReportsController reportsController) {
		this.reportsController = reportsController;
	}

	/**
	 * Handles a request to retrieve the time report.
	 * <p>
	 * The method extracts the requested year and month from the request DTO, builds
	 * the time report using the reports controller, and sends the resulting
	 * statistics back to the client.
	 * </p>
	 */
	@Override
	public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

		TimeReportDTO req = (TimeReportDTO) request.getData();

		TimeReportDTO result = reportsController.buildTimeReport(req.getYear(), req.getMonth());


		System.out.println("📤 Sending TimeReportDTO to client: " + "onTime=" + result.getOnTimeCount() + ", minor="
				+ result.getMinorDelayCount() + ", major=" + result.getSignificantDelayCount());

		System.out.println("Sending Time Reports to client");


		client.sendToClient(new ResponseDTO(true, "Time report loaded", result));
	}
}
