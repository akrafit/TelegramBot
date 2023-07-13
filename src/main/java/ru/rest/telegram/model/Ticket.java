package ru.rest.telegram.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    private Long id;
    private String title;
    private String image;
    private List<String> answers;
    private Integer correctAnswer;
    private Integer actualAnswer;
}
