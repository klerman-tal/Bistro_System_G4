package network;

import dto.RequestDTO;
import dto.TimeReportDTO;
import logicControllers.ReportsController;
import ocsf.server.ConnectionToClient;

public class GetTimeReportHandler implements RequestHandler {

    private final ReportsController reportsController;

    public GetTimeReportHandler(ReportsController reportsController) {
        this.reportsController = reportsController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        TimeReportDTO req = (TimeReportDTO) request.getData();

        TimeReportDTO result =
                reportsController.buildTimeReport(
                        req.getYear(),
                        req.getMonth()
                );

        client.sendToClient(result);
    }
}
