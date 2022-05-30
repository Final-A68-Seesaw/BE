package com.example.seesaw.trouble.service;

import com.example.seesaw.dictionary.service.ConvertTimeService;
import com.example.seesaw.trouble.dto.TroubleAllResponseDto;
import com.example.seesaw.trouble.dto.TroubleCommentRequestDto;
import com.example.seesaw.trouble.dto.TroubleDetailResponseDto;
import com.example.seesaw.trouble.dto.TroubleDto;
import com.example.seesaw.trouble.model.*;
import com.example.seesaw.trouble.repository.*;
import com.example.seesaw.user.model.User;
import com.example.seesaw.user.repository.UserRepository;
import com.example.seesaw.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TroubleService {

    private final TroubleTagRepository troubleTagRepository;
    private final TroubleRepository troubleRepository;
    private final TroubleImageRepository troubleImageRepository;
    private final TroubleS3Service troubleS3Service;
    private final TroubleCommentRepository troubleCommentRepository;
    private final TroubleCommentLikeRepository troubleCommentLikeRepository;

    private final ConvertTimeService convertTimeService;
    private final UserService userService;
    private final UserRepository userRepository;

    //Trouble 글 등록
    public void registerTrouble(TroubleDto troubleDto, List<MultipartFile> files, User user) {
        checkTrouble(troubleDto);
        List<String> imagePaths = new ArrayList<>();

        if(files == null){
            imagePaths.add("https://myseesaw.s3.ap-northeast-2.amazonaws.com/TroubleBaicCard.svg");
        } else {
            imagePaths.addAll(troubleS3Service.upload(files));
        }
        Trouble trouble = new Trouble(
                troubleDto.getTitle(),
                troubleDto.getContents(),
                troubleDto.getQuestion(),
                troubleDto.getAnswer(),
                0L,
                user
        );
        troubleRepository.save(trouble);

        for (String imagePath : imagePaths) {
            TroubleImage troubleImage = new TroubleImage(imagePath, user, trouble);
            troubleImageRepository.save(troubleImage);
        }

        List<String> tags = troubleDto.getTagNames();
        for (String tag : tags) {
            if(!tag.equals("")){
                TroubleTag troubleTag = new TroubleTag(tag, user, trouble);
                troubleTagRepository.save(troubleTag);
            }
        }
    }

    //고민글 작성자 검사
    public Trouble checkTroubleAndWriter(Long troubleId, User user){

        Trouble trouble = troubleRepository.findById(troubleId).orElseThrow(
                () -> new IllegalArgumentException("고민 Id에 해당하는 글이 없습니다.")
        );

        Long troubleUserId = trouble.getUser().getId();
        if(!user.getId().equals(troubleUserId)){
            throw new IllegalArgumentException("작성자가 아니므로 고민글 수정이 불가합니다.");
        }
        return trouble;
    }

    //고민글 수정 시 정보조회
    public TroubleDto findTrouble(Long troubleId, User user) {

        Trouble trouble = checkTroubleAndWriter(troubleId, user);

        List<TroubleTag> troubleTags = troubleTagRepository.findAllByTroubleId(troubleId);

        List<String> troubleTagList = new ArrayList<>();
        for(TroubleTag troubleTag : troubleTags){
            troubleTagList.add(troubleTag.getTagName());
        }

        List<TroubleImage> troubleImages = troubleImageRepository.findAllByTroubleId(troubleId);
        if (troubleImages.isEmpty()){
            throw new IllegalArgumentException("고민 Id에 해당하는 이미지가 없습니다.");
        }
        List<String> troubleImageList = new ArrayList<>();
        for(TroubleImage troubleImage : troubleImages){
            if(troubleImage.getTroubleImage().equals("https://myseesaw.s3.ap-northeast-2.amazonaws.com/TroubleBaicCard.svg")){
                break;
            } else {
                troubleImageList.add(troubleImage.getTroubleImage());
            }
        }

        return new TroubleDto(trouble.getTitle(), trouble.getContents(), trouble.getQuestion(), trouble.getAnswer(), troubleTagList, troubleImageList);
    }

    //고민글 수정
    public void updateTrouble(TroubleDto troubleDto, List<MultipartFile> files, Long troubleId, User user) {

        Trouble trouble = checkTroubleAndWriter(troubleId, user);

        checkTrouble(troubleDto);

        trouble.update(troubleDto);

        List<String> imagePaths = new ArrayList<>();
        if(files == null && troubleDto.getTroubleImages().isEmpty()){
            imagePaths.add("https://myseesaw.s3.ap-northeast-2.amazonaws.com/TroubleBaicCard.svg");
            troubleS3Service.delete(troubleId, troubleDto.getTroubleImages());
            troubleImageRepository.deleteAllByTroubleId(troubleId);
        } else if(files!=null) {
            imagePaths.addAll(troubleS3Service.update(troubleId, troubleDto.getTroubleImages(), files));
        } else{
            imagePaths = troubleDto.getTroubleImages();
            troubleS3Service.delete(troubleId, troubleDto.getTroubleImages());
            troubleImageRepository.deleteAllByTroubleId(troubleId);
        }

        for (String imagePath : imagePaths) {
            TroubleImage troubleImage = new TroubleImage(imagePath, user, trouble);
            troubleImageRepository.save(troubleImage);
        }
        List<String> tags = troubleDto.getTagNames();
        troubleTagRepository.deleteAllByTroubleId(troubleId);
        for (String tag : tags) {
            if(!tag.equals("")){
                TroubleTag troubleTag = new TroubleTag(tag, user, trouble);
                troubleTagRepository.save(troubleTag);
            }
        }
    }

    //고민글 유효성 검사
    public void checkTrouble(TroubleDto troubleDto) {
        if (troubleDto.getTitle().isEmpty()) {
            throw new IllegalArgumentException("제목입력은 필수값입니다.");
        } else if (troubleDto.getContents().isEmpty()) {
            throw new IllegalArgumentException("내용입력은 필수값입니다.");
        } else if (troubleDto.getQuestion().isEmpty()) {
            throw new IllegalArgumentException("질문자세대 입력은 필수값입니다.");
        } else if (troubleDto.getAnswer().isEmpty()) {
            throw new IllegalArgumentException("답변자세대 입력은 필수값입니다.");
        }
    }

    // 고민 상세보기
    public TroubleDetailResponseDto findDetailTrouble(Long troubleId, int page, User user1) {
        Trouble trouble = troubleRepository.findById(troubleId).orElseThrow(
                () -> new IllegalArgumentException("고민 Id에 해당하는 글이 없습니다.")
        );
        TroubleDto troubleDto = findTrouble(troubleId, trouble.getUser());

        TroubleDetailResponseDto troubleDetailResponseDto = getTroubleDetailResponseDto(trouble, troubleDto, user1);

        Pageable pageable = PageRequest.of(page-1, 4);
        Page<TroubleComment> troubleCommentPage = troubleCommentRepository.findAllByTroubleIdOrderByCreatedAtDesc(troubleId, pageable);

        List<TroubleComment> troubleComments = troubleCommentRepository.findAllByTroubleId(troubleId);
        troubleDetailResponseDto.setCommentCount((long) troubleComments.size());

        List<TroubleCommentRequestDto> troubleCommentRequestDtos = new ArrayList<>();
        for(TroubleComment troubleComment:troubleCommentPage){
            TroubleCommentRequestDto troubleCommentRequestDto = new TroubleCommentRequestDto(troubleComment);
            User user = userRepository.findByNickname(troubleComment.getNickname()).orElseThrow(
                    () -> new IllegalArgumentException("고민댓글에 해당하는 사용자를 찾을 수 없습니다."));
            // commentTime
            String postCommentTime = convertTimeService.convertLocaldatetimeToTime(troubleComment.getCreatedAt());
            troubleCommentRequestDto.setCommentTime(postCommentTime);
            // commentLikeCount
            troubleCommentRequestDto.setCommentLikeCount(troubleComment.getLikeCount());
            // 현재 로그인한 사람의 댓글 좋아요 상태
            TroubleCommentLike savedPostCommentLike = troubleCommentLikeRepository.findByTroubleCommentAndUserId(troubleComment, user1.getId());
            troubleCommentRequestDto.setCommentLikeStatus(savedPostCommentLike != null); // status
            // 프로필이미지
            troubleCommentRequestDto.setProfileImages(userService.findUserProfiles(user));
            // 리스트에 넣기
            troubleCommentRequestDtos.add(troubleCommentRequestDto);
        }
        troubleDetailResponseDto.setTroubleComments(troubleCommentRequestDtos);

        return troubleDetailResponseDto;
    }

    // 고민글 상세조회 중복 메서드 추출
    private TroubleDetailResponseDto getTroubleDetailResponseDto(Trouble trouble, TroubleDto troubleDto, User user) {
        TroubleDetailResponseDto troubleDetailResponseDto = new TroubleDetailResponseDto(troubleDto);
        troubleDetailResponseDto.setWriter(trouble.getUser().getNickname());
        troubleDetailResponseDto.setNickname(user.getNickname());
        troubleDetailResponseDto.setProfileImages(userService.findUserProfiles(trouble.getUser()));
        String postTime = convertTimeService.convertLocaldatetimeToTime(trouble.getCreatedAt());
        troubleDetailResponseDto.setTroubleTime(postTime);
        troubleDetailResponseDto.setViews(trouble.getViews());
        trouble.setViews(trouble.getViews()+1);
        troubleRepository.save(trouble);
        return troubleDetailResponseDto;
    }


    public List<TroubleAllResponseDto> findAllTroubles(int page) {
        List<TroubleAllResponseDto> troubleAllResponseDtos = new ArrayList<>();
        Pageable pageable = PageRequest.of(page-1, 30);
        Page<Trouble> troubles = troubleRepository.findAllByOrderByCreatedAtDesc(pageable);
        for (Trouble trouble: troubles) {
            List<TroubleImage> troubleImages = troubleImageRepository.findAllByTroubleId(trouble.getId());
            troubleAllResponseDtos.add(new TroubleAllResponseDto(trouble, troubleImages.get(0).getTroubleImage()));
        }
        return troubleAllResponseDtos;
    }

    public List<TroubleAllResponseDto> findViewTroubles(User user) {
        List<Trouble> troubles = troubleRepository.findAllByAnswerOrderByViewsDesc(user.getGeneration());
        return getTroubles(troubles);
    }

    // 고민글 전체 리스트 조회
    public List<TroubleAllResponseDto> getTroubles(List<Trouble> troubles){
        if(troubles.isEmpty()){
            throw new IllegalArgumentException("작성된 고민글이 없습니다.");
        }
        List<TroubleAllResponseDto> troubleAllResponseDtos = new ArrayList<>();

        for(Trouble trouble:troubles){
            TroubleDto troubleDto = findTrouble(trouble.getId(), trouble.getUser());
            TroubleAllResponseDto troubleAllResponseDto = new TroubleAllResponseDto(troubleDto);
            troubleAllResponseDto.setTroubleId(trouble.getId());
            troubleAllResponseDto.setViews(trouble.getViews());
            List<TroubleComment> troubleComments = troubleCommentRepository.findAllByTroubleId(trouble.getId());
//            troubleAllResponseDto.setCommentCount((long) troubleComments.size());
            troubleAllResponseDtos.add(troubleAllResponseDto);
        }
        return troubleAllResponseDtos;
    }

    // 댓글 리스폰스용
    public TroubleCommentRequestDto getTroubleCommentDto(User user, TroubleComment troubleComment) {
        TroubleCommentRequestDto troubleCommentRequestDto = new TroubleCommentRequestDto(troubleComment);
        User commentUser = userRepository.findByNickname(troubleComment.getNickname()).orElseThrow(
                () -> new IllegalArgumentException("고민댓글에 해당하는 사용자를 찾을 수 없습니다."));
        troubleCommentRequestDto.setProfileImages(userService.findUserProfiles(commentUser));
        troubleCommentRequestDto.setCommentLikeCount(troubleComment.getLikeCount());
        String troubleCommentTime = convertTimeService.convertLocaldatetimeToTime(troubleComment.getCreatedAt());
        troubleCommentRequestDto.setCommentTime(troubleCommentTime);
        Long size = (long) troubleCommentRepository.findAllByTroubleId(troubleComment.getTrouble().getId()).size();
//        troubleCommentRequestDto.setCommentCount(size);
        TroubleCommentLike savedTroubleCommentLike = troubleCommentLikeRepository.findByTroubleCommentAndUserId(troubleComment, user.getId());
        troubleCommentRequestDto.setCommentLikeStatus(savedTroubleCommentLike != null);
        return troubleCommentRequestDto;
    }

    public void deleteTrouble(Long troubleId, User user) {
        checkTroubleAndWriter(troubleId, user);
        troubleRepository.deleteById(troubleId);
    }
}
