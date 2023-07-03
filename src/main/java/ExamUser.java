import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ExamUser {
    private String name;
    private Long chatId;
    private ExaminationTicket examinationTickets;
    private Instant examinationStartTime;
    private Long lastMassageToUser;
    private Ticket lastTicket;
}
