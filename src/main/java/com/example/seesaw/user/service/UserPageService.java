package com.example.seesaw.user.service;

import com.example.seesaw.dictionary.model.Post;
import com.example.seesaw.dictionary.model.PostComment;
import com.example.seesaw.dictionary.model.PostImage;
import com.example.seesaw.dictionary.model.PostScrap;
import com.example.seesaw.dictionary.repository.PostCommentRepository;
import com.example.seesaw.dictionary.repository.PostImageRepository;
import com.example.seesaw.dictionary.repository.PostRepository;
import com.example.seesaw.dictionary.repository.PostScrapRepository;
import com.example.seesaw.trouble.model.Trouble;
import com.example.seesaw.trouble.model.TroubleComment;
import com.example.seesaw.trouble.model.TroubleImage;
import com.example.seesaw.trouble.repository.TroubleCommentRepository;
import com.example.seesaw.trouble.repository.TroubleImageRepository;
import com.example.seesaw.trouble.repository.TroubleRepository;
import com.example.seesaw.user.dto.*;
import com.example.seesaw.user.model.User;
import com.example.seesaw.user.model.UserProfile;
import com.example.seesaw.user.model.UserProfileNum;
import com.example.seesaw.user.repository.UserProfileNumRepository;
import com.example.seesaw.user.repository.UserProfileRepository;
import com.example.seesaw.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPageService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileNumRepository userProfileNumRepository;
    private final TroubleCommentRepository troubleCommentRepository;

    private final PostScrapRepository postScrapRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostCommentRepository postCommentRepository;
    private final TroubleRepository troubleRepository;
    private final TroubleImageRepository troubleImageRepository;


    // 프로필 수정
    public void updateProfile(ProfileRequestDto profileRequestDto, User user) {
        if(!profileRequestDto.getNickname().equals(user.getNickname())) {
            //닉네임 유효성 검사 후 저장
            String nickname = userService.checkNickName(profileRequestDto.getNickname());
            //고민댓글 nickname 변경
            List<TroubleComment> troubleComments = troubleCommentRepository.findAllByNickname(user.getNickname());
            for (TroubleComment troubleComment : troubleComments) {
                troubleComment.setNickname(profileRequestDto.getNickname());
                troubleCommentRepository.save(troubleComment);
            }
            List<PostComment> postComments = postCommentRepository.findAllByNickname(user.getNickname());
            for (PostComment postComment : postComments) {
                postComment.setNickname(profileRequestDto.getNickname());
                postCommentRepository.save(postComment);
            }
            user.setNickname(nickname);
            userRepository.save(user);
        }

        //IDs 유효성 검사 후 IDs 저장
        List<Long> profileImageCharIds = profileRequestDto.getProfileImages();
        List<UserProfileNum> userProfileNums = new ArrayList<>();
        for (Long charId : profileImageCharIds) {
            UserProfile userProfile = userProfileRepository.findByCharId(charId);
            UserProfileNum userProfileNum = new UserProfileNum(userProfile, user);
            userProfileNums.add(userProfileNum);
        }
        userProfileNumRepository.deleteAllByUserId(user.getId());
        userProfileNumRepository.saveAll(userProfileNums);
    }

    public ProfileResponseDto findProfiles() {
        List<ProfileListDto> faceUrl = new ArrayList<>();
        List<ProfileListDto> accessoryUrl = new ArrayList<>();
        List<ProfileListDto> backgroudUrl = new ArrayList<>();
        List<UserProfile> userProfile = userProfileRepository.findAll();
        for (UserProfile profile : userProfile) {
            if (profile.getCategory().equals("faceUrl")) {
                faceUrl.add(new ProfileListDto(profile.getCharId(), profile.getUserProfileImage()));
            } else if (profile.getCategory().equals("accessoryUrl")) {
                accessoryUrl.add(new ProfileListDto(profile.getCharId(), profile.getUserProfileImage()));
            } else if (profile.getCategory().equals("backgroundUrl")) {
                backgroudUrl.add(new ProfileListDto(profile.getCharId(), profile.getUserProfileImage()));
            }
        }
        return new ProfileResponseDto(faceUrl, accessoryUrl, backgroudUrl);
    }

    // 내가 스크랩한 게시물 조회 (마이페이지)
    public List<MyScrapResponseDto> getMyScraps(User user) {
        List<PostScrap> postScraps = postScrapRepository.findAllByUserId(user.getId());
        List<Post> posts = postRepository.findAll();

        List<MyScrapResponseDto> myScrapResponseDtos = new ArrayList<>();

        for (PostScrap postScrap : postScraps) {
            for (Post post : posts) {
                // postScrap테이블 postId 와 post테이불 postId가 일치할때
                if (post.getId().equals(postScrap.getPost().getId())) {
                    // postId에 해당하는 postImage 가져오기
                    List<PostImage> postImages = postImageRepository.findAllByPostId(post.getId());
                    // 첫번째 이미지만 가져오기
                    PostImage postImage = postImages.get(0);
                    // 일치한 postId에 해당하는 scrap 횟수만 가져오기
                    long scrapCount = postScrapRepository.countByPostId(post.getId());
                    // 일치한 postId 에 해당하는 comment 개수만 가져오기
                    long commentCount = postCommentRepository.countByPostId(post.getId());
                    // 스크랩 상태
                    PostScrap savedPostScrap = postScrapRepository.findByUserAndPost(user, post);
                    boolean scrapStatus = savedPostScrap != null;

                    myScrapResponseDtos.add(new MyScrapResponseDto(postScrap, post, scrapCount,
                            commentCount, postImage, scrapStatus));
                }

            }
        }
        return myScrapResponseDtos;
    }

    // 내가 등록한 단어장 조회 (마이페이지)
    public List<MyPostResponseDto> getMyPosts(User user){
        // 내가 첫 작성자인 단어글 가져오기
        List<Post> posts = postRepository.findAllByFirstWriter(user.getNickname());

        List<MyPostResponseDto> myPostResponseDtos = new ArrayList<>();
        for (Post post: posts
             ) {
            // postId에 해당하는 postImage 가져오기
            List<PostImage> postImages = postImageRepository.findAllByPostId(post.getId());
            // 첫번째 이미지만 가져오기
            PostImage postImage = postImages.get(0);
            // 일치한 postId에 해당하는 scrap 횟수만 가져오기
            long scrapCount = postScrapRepository.countByPostId(post.getId());
            // 일치한 postId 에 해당하는 comment 개수만 가져오기
            long commentCount = postCommentRepository.countByPostId(post.getId());
            // 스크랩 상태
            PostScrap savedPostScrap = postScrapRepository.findByUserAndPost(user, post);
            boolean scrapStatus = savedPostScrap != null;

            myPostResponseDtos.add(new MyPostResponseDto(post,scrapCount,commentCount,postImage, scrapStatus));
        }
        return  myPostResponseDtos;
    }

    // 내가 등록한 고민글 조회 (마이페이지)
    public List<MyTroublesResponseDto> getMyTroubles(User user) {
        List<Trouble> troubles = troubleRepository.findAllByUserId(user.getId());

        List<MyTroublesResponseDto> myTroublesResponseDtos = new ArrayList<>();
        for (Trouble trouble : troubles) {
            // troubleId에 해당하는 troulbleImage 가져오기
            List<TroubleImage> troubleImages = troubleImageRepository.findAllByTroubleId(trouble.getId());
            TroubleImage troubleImage = troubleImages.get(0);
            // 고민글 댓글 수
            long commentCount = troubleCommentRepository.countByTroubleId(trouble.getId());

            myTroublesResponseDtos.add(new MyTroublesResponseDto(trouble,commentCount,troubleImage));
        }
        return myTroublesResponseDtos;
    }
}


