package com.example.seesaw.dto;

import com.example.seesaw.model.Crossword;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossWordResponseDto {
    private Long id;
    private int x;                  // x좌표
    private int y;                 // y좌표
    private String word;         // 단어
    private String contents;  // 내용
    private int wordCount; // 글자수
    private boolean isOriental; // 가로면 true, 세로면 false
    private int quizId; // 퀴즈 번호

    public CrossWordResponseDto(Crossword crossword){
        this.id = crossword.getId();
        this.x = crossword.getX();
        this.y = crossword.getY();
        this.word = crossword.getWord();
        this.contents = crossword.getContents();
        this.wordCount = crossword.getWordCount();
        this.isOriental = crossword.isOriental();
    }

}
