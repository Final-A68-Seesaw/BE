package com.example.seesaw.repository;

import com.example.seesaw.model.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findAllByNickname(String nickname);

    Page<PostComment> findAllByPostIdOrderByLikeCountDesc(Long postId, Pageable pageable);

    List<PostComment> findAllByPostId(Long id);
}
