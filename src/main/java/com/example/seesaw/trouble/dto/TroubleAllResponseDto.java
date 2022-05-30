package com.example.seesaw.trouble.dto;

import com.example.seesaw.trouble.model.Trouble;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TroubleAllResponseDto {

    private Long troubleId;
    private String title;
    private String contents;
    private String question;
    private String answer;
    private Long views;
    private Long commentCount;
    private String troubleImages;     // imageUrls -> troubleImages


    public TroubleAllResponseDto(TroubleDto troubleDto) {
        this.title = troubleDto.getTitle();
        this.contents = troubleDto.getContents();
        this.question = troubleDto.getQuestion();
        this.answer = troubleDto.getAnswer();
        this.troubleImages = troubleDto.getTroubleImages().get(0);
    }

    public TroubleAllResponseDto(Trouble trouble, String troubleImages) {
        this.troubleId = trouble.getId();
        this.title = trouble.getTitle();
        this.contents = trouble.getContents();
        this.question = trouble.getQuestion();
        this.answer = trouble.getAnswer();
        this.views = trouble.getViews();
        this.commentCount = (long) trouble.getTroubleComments().size();
        this.troubleImages = troubleImages;
    }
}