package network;

import dto.RequestDTO;
import dto.ResponseDTO;
import dto.SubscribersReportDTO;
import logicControllers.ReportsController;
import ocsf.server.ConnectionToClient;

public class GetSubscribersReportHandler implements RequestHandler {

    private final ReportsController reportsController;

    public GetSubscribersReportHandler(ReportsController reportsController) {
        this.reportsController = reportsController;
    }

    @Override
    public void handle(RequestDTO request, ConnectionToClient client) throws Exception {

        SubscribersReportDTO req = (SubscribersReportDTO) request.getData();

        SubscribersReportDTO result =
                reportsController.buildSubscribersReport(
                        req.getYear(),
                        req.getMonth()
                );

        System.out.println("📤 Sending SubscribersReportDTO to client: "
                + "active=" + result.getActiveSubscribersCount()
                + ", inactive=" + result.getInactiveSubscribersCount()
                + ", waitingDays=" + (result.getWaitingListPerDay() == null ? 0 : result.getWaitingListPerDay().size())
                + ", reservationDays=" + (result.getReservationsPerDay() == null ? 0 : result.getReservationsPerDay().size())
        );

        client.sendToClient(
                new ResponseDTO(
                        true,
                        "Subscribers report loaded",
                        result
                )
        );
    }
}
