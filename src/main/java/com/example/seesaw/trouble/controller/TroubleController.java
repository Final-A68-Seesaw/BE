package com.example.seesaw.trouble.controller;

import com.example.seesaw.trouble.dto.TroubleAllResponseDto;
import com.example.seesaw.trouble.dto.TroubleDetailResponseDto;
import com.example.seesaw.trouble.dto.TroubleDto;
import com.example.seesaw.trouble.repository.TroubleRepository;
import com.example.seesaw.security.UserDetailsImpl;
import com.example.seesaw.trouble.service.TroubleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TroubleController {

    private final TroubleService troubleService;
    private final TroubleRepository troubleRepository;

    //고민글 등재
    @PostMapping("/api/trouble")
    public ResponseEntity<String> registerTrouble(
            @RequestPart(value = "troubleRequestDto") TroubleDto troubleDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        troubleService.registerTrouble(troubleDto, files, userDetails.getUser());
        return ResponseEntity.ok()
                .body("고민글 등재완료");
    }

    //고민글 수정 시 고민글 조회
    @GetMapping("api/trouble/{troubleId}")
    public ResponseEntity<TroubleDto> updateTrouble(@PathVariable Long troubleId, @AuthenticationPrincipal UserDetailsImpl userDetails){
        TroubleDto troubleDto = troubleService.findTrouble(troubleId, userDetails.getUser());
        return ResponseEntity.ok()
                .body(troubleDto);
    }
    //고민글 수정
    @PutMapping("api/trouble/{troubleId}")
    public ResponseEntity<String> updateTrouble(
            @RequestPart(value = "troubleRequestDto") TroubleDto troubleDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @PathVariable Long troubleId,
            @AuthenticationPrincipal UserDetailsImpl userDetails){

        troubleService.updateTrouble(troubleDto, files, troubleId, userDetails.getUser());
        return ResponseEntity.ok()
                .body("고민글 수정완료");
    }
    //고민글 삭제
    @DeleteMapping("/api/trouble/{troubleId}")
    public ResponseEntity<String> deleteTrouble(@PathVariable Long troubleId, @AuthenticationPrincipal UserDetailsImpl userDetails){
        troubleService.deleteTrouble(troubleId, userDetails.getUser());
        return ResponseEntity.ok()
                .body("고민글 삭제완료");
    }

    //고민글 상세조회
    @GetMapping("/api/trouble/{troubleId}/detail")
    public ResponseEntity<TroubleDetailResponseDto> findDetailTrouble(
            @RequestParam(value = "page") int page,
            @PathVariable Long troubleId, @AuthenticationPrincipal UserDetailsImpl userDetails){
        TroubleDetailResponseDto troubleDetailResponseDto = troubleService.findDetailTrouble(troubleId, page, userDetails.getUser());
        return ResponseEntity.ok()
                .body(troubleDetailResponseDto);
    }

    //고민글 전체 조회(최근 작성 순)
    @GetMapping("/api/trouble/list")
    public ResponseEntity<List<TroubleAllResponseDto>> findAllTroubles(
            @RequestParam(value = "page") int page){
        List<TroubleAllResponseDto> troubleAllResponseDto = troubleService.findAllTroubles(page);
        return ResponseEntity.ok()
                .body(troubleAllResponseDto);
    }

    //고민글 전체 조회(조회수 순)
    @GetMapping("/api/main/trouble/list")
    public ResponseEntity<List<TroubleAllResponseDto>> findViewTroubles(
            @AuthenticationPrincipal UserDetailsImpl userDetails){
        List<TroubleAllResponseDto> troubleAllResponseDto = troubleService.findViewTroubles(userDetails.getUser());
        return ResponseEntity.ok()
                .body(troubleAllResponseDto);
    }

}
