package network;

import dto.RequestDTO;
import dto.ResponseDTO;
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
        System.out.println("📤 Sending TimeReportDTO to client: "
                + "onTime=" + result.getOnTimeCount()
                + ", minor=" + result.getMinorDelayCount()
                + ", major=" + result.getSignificantDelayCount());

        client.sendToClient(
            new ResponseDTO(
                true,
                "Time report loaded",
                result
            )
        );
    }
}
