import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Ticket {
    private String title;
    private String image;
    private List<String> answers;
    private Integer correctAnswer;
    private Integer actualAnswer;
}
