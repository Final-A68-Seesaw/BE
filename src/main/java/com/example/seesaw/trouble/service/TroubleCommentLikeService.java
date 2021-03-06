package com.example.seesaw.trouble.service;

import com.example.seesaw.trouble.dto.TroubleCommentRequestDto;
import com.example.seesaw.trouble.model.TroubleComment;
import com.example.seesaw.trouble.model.TroubleCommentLike;
import com.example.seesaw.user.model.User;
import com.example.seesaw.trouble.repository.TroubleCommentRepository;
import com.example.seesaw.trouble.repository.TroubleCommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@RequiredArgsConstructor
@Service
public class TroubleCommentLikeService {

    private final TroubleCommentRepository troubleCommentRepository;
    private final TroubleCommentLikeRepository troubleCommentLikeRepository;
    private final TroubleService troubleService;

    @Transactional
    public TroubleCommentRequestDto getTroubleCommentLikes(Long commentId, User user) {
        Long userId= user.getId();
        TroubleComment troubleComment = troubleCommentRepository.findById(commentId).orElseThrow(
                () -> new IllegalArgumentException("해당하는 댓글이 없습니다.")
        );

        TroubleCommentLike savedLike = troubleCommentLikeRepository.findByTroubleCommentAndUserId(troubleComment, userId);

        if(savedLike != null){
            troubleCommentLikeRepository.deleteById(savedLike.getId());
            troubleComment.setLikeCount(troubleComment.getLikeCount()-1); //고민댓글 좋아요 수 -1
            troubleCommentRepository.save(troubleComment);
        } else{
            TroubleCommentLike troubleCommentLike = new TroubleCommentLike(user, troubleComment);
            troubleCommentLikeRepository.save(troubleCommentLike);
            troubleComment.setLikeCount(troubleComment.getLikeCount()+1); //고민댓글 좋아요 수 +1
            troubleCommentRepository.save(troubleComment);
        }
        return troubleService.getTroubleCommentDto(user, troubleComment);
    }
}