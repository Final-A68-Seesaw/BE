package com.example.seesaw.chat.model;

import com.example.seesaw.chat.dto.ChatMessageDto;
import com.example.seesaw.dictionary.model.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@NoArgsConstructor
@Getter
@Entity
public class ChatMessage extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String senderName;

    @Column
    private String message;

    @Column
    private String roomId;

    @ManyToOne
    @JoinColumn(name = "CHATROOM_ID")
    private ChatRoom chatRoom;


    public ChatMessage(ChatMessageDto chatMessageDto, ChatRoom chatRoom) {
        this.message = chatMessageDto.getMessage();
        this.senderName = chatMessageDto.getSenderName();
        this.chatRoom = chatRoom;
    }
}
