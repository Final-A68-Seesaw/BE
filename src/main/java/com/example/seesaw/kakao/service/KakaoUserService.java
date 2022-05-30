package com.example.seesaw.kakao.service;

import com.example.seesaw.kakao.dto.KakaoRequstDto;
import com.example.seesaw.kakao.dto.KakaoUserInfoDto;
import com.example.seesaw.user.model.User;
import com.example.seesaw.user.model.UserProfile;
import com.example.seesaw.user.model.UserProfileNum;
import com.example.seesaw.user.model.UserRoleEnum;
import com.example.seesaw.user.repository.MbtiRepository;
import com.example.seesaw.user.repository.UserProfileNumRepository;
import com.example.seesaw.user.repository.UserProfileRepository;
import com.example.seesaw.user.repository.UserRepository;
import com.example.seesaw.security.UserDetailsImpl;
import com.example.seesaw.security.UserDetailsServiceImpl;
import com.example.seesaw.security.jwt.JwtTokenUtils;
import com.example.seesaw.user.dto.MbtiRequestDto;
import com.example.seesaw.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class KakaoUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserProfileNumRepository userProfileNumRepository;
    private final UserProfileRepository userProfileRepository;
    private final MbtiRepository mbtiRepository;

    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final UserService userService;

    public List<String> kakaoLogin(String code) throws JsonProcessingException {
        List<String> infos = new ArrayList<>();

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(code);

        System.out.println("인가 코드 : " + code);
        System.out.println("엑세스 토큰 : " + accessToken);

        // 2. "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        KakaoUserInfoDto kakaoUserInfo = getKakaoUserInfo(accessToken);

        // 3. 회원가입이 안된 경우, token 없이 id, email 만 보낸다.
        User kakaoUser = userRepository.findByUsername(kakaoUserInfo.getKakaoId())
                .orElse(null);
        if(kakaoUser == null){
            infos.add(kakaoUserInfo.getKakaoId());
            infos.add(kakaoUserInfo.getEmail());
            infos.add("");
            infos.add("");
        } else{
            infos.add("");
            infos.add("");
            // 4. 로그인 JWT 토큰 발행
            infos.addAll(jwtTokenCreate(kakaoUser));
        }
        return infos;
    }


    private String getAccessToken(String code) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");

        // 클라이언트 아이디, url 확인
        body.add("client_id", "6f05e336898a8b021c45ac7c1f8770b8");
        body.add("redirect_uri", "https://www.play-seeso.com/user/kakao/callback");
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
                new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    private KakaoUserInfoDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
//      String nickname = jsonNode.get("properties")
//      .get("nickname").asText();
        String email = jsonNode.get("kakao_account")
                .get("email").asText();

        return new KakaoUserInfoDto(String.valueOf(id), email);
    }

    private User signUpKakaoUser(KakaoRequstDto kakaoRequstDto) {

        // DB 에 중복된 Kakao Id 가 있는지 확인
        String kakaoUsername = kakaoRequstDto.getKakaoId();
        String kakaoId = kakaoRequstDto.getUsername();

        System.out.println("input kakaoId -> KakaoUsername : " +kakaoUsername);
        System.out.println("input kakaoEmail -> kakaoId : " +kakaoId);

        User kakaoUser = userRepository.findByUsername(kakaoUsername)
                .orElse(null);

        if(kakaoUser != null) {
            throw new IllegalArgumentException("이미 카카오 로그인을 완료하였습니다.");
        }

            // 회원가입
             //password: random UUID(여기는 원래 코드임)
            String passwsord = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(passwsord);

            //nickname
            String nickname = kakaoRequstDto.getNickname();
            userService.checkNickName(nickname);

            //mbti
            String generation = kakaoRequstDto.getGeneration();
            MbtiRequestDto mbtiRequestDto = new MbtiRequestDto(kakaoRequstDto);
            String mbti = userService.checkMbti(mbtiRequestDto);

            //profile 저장
            List<Long> charIds = kakaoRequstDto.getCharId();
            if (charIds == null) {
                throw new IllegalArgumentException("charIds가 null 입니다.");
            }

            Long postCount = 0L;

            kakaoUser = new User(kakaoUsername, encodedPassword, nickname, generation, postCount, mbti, UserRoleEnum.USER, kakaoId);
            userRepository.save(kakaoUser); // DB 저장

            for (Long charId : charIds) {
                UserProfile userProfile = userProfileRepository.findByCharId(charId);
                UserProfileNum userProfileNum = new UserProfileNum(userProfile, kakaoUser);
                userProfileNumRepository.save(userProfileNum);
            }
        return kakaoUser;
    }

    //강제 로그인
    private List<String> jwtTokenCreate(User kakaoUser) {

        UserDetails userDetails = new UserDetailsImpl(kakaoUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl kakaoUserDetails = ((UserDetailsImpl) authentication.getPrincipal());

        System.out.println("kakaoUserDetails : " + kakaoUserDetails.toString());

        final String accessToken = JwtTokenUtils.generateJwtToken(kakaoUserDetails.getUser());
        final String refreshtoken = userDetailsServiceImpl.saveRefreshToken(kakaoUserDetails.getUser());

        List<String> tokens = new ArrayList<>();
        tokens.add(accessToken);
        tokens.add(refreshtoken);

        return tokens;
    }


    public List<String> signUpUser(KakaoRequstDto kakaoRequstDto) {

        // 3. 회원가입
        User kakaoUser = signUpKakaoUser(kakaoRequstDto);

        // 4. 로그인 JWT 토큰 발행
        return jwtTokenCreate(kakaoUser);
    }

//    public String checkKakaoMbti(KakaoMbtiRequestDto kakoMbtiRequestDto) {
//
//        //mbti 설정 시, 비밀번호 id가 없으면 가입을 못하도록 함.
//        if(kakoMbtiRequestDto.getCode() == null) {
//            throw new CustomException(ErrorCode.BLANK_USER_NAME);
//        }
//
//        String mbtiName = kakoMbtiRequestDto.getEnergy() + kakoMbtiRequestDto.getInsight() + kakoMbtiRequestDto.getJudgement() + kakoMbtiRequestDto.getLifePattern();
//        if (mbtiName.length() != 4 || mbtiName.contains("null")) {
//            throw new CustomException(ErrorCode.BLANK_USER_MBTI);
//        }
//        //MBTI table 만들어서 맞는 설명으로 치환해서 내려주기
//        String detail = mbtiRepository.findByMbtiName(mbtiName).getDetail();
//        if (detail.isEmpty()) {
//            throw new IllegalArgumentException("해당하는 MBTI가 없습니다.");
//        }
//        return detail;
//    }
}
