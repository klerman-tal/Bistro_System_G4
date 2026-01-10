package dto;

import java.io.Serializable;

//dto/GetReservationHistoryDTO.java
public class GetReservationHistoryDTO implements Serializable {
 private final int subscriberId;

 public GetReservationHistoryDTO(int subscriberId) {
     this.subscriberId = subscriberId;
 }

 public int getSubscriberId() {
     return subscriberId;
 }
}
