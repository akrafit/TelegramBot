import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;

@Getter
@Setter
public class ExaminationTicket {
    private Instant startTime;
    private ArrayList<Ticket> ticketsActual = new ArrayList<>();
    private ArrayList<Ticket> ticketsPassed = new ArrayList<>();

}
