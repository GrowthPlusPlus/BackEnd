// 정성우

package me.jsu.springbootdeveloper.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.tag.dto.*;
import me.jsu.springbootdeveloper.tag.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    /*
        태그 전체를 조회한다.
        태그 전체를 조회하므로 카테고리 별 분류는 클라이언트에서 수행한다.
     */
    @Operation(
            summary = "전체 태그 조회",
            description = """
        시스템에 등록된 모든 태그를 조회한다. 카테고리 분류는 클라이언트에서 수행한다.
        ex) GET /api/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "태그 목록 조회 성공")
    })
    @GetMapping("/api/tags/all")
    public ResponseEntity<List<TagResponse>> getTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    /*
        유저가 선택한 여러 태그가 DB상에서 연결되도록 한다.
     */
    @Operation(
            summary = "유저 태그 선택",
            description = """
        로그인한 사용자가 선택한 태그들을 자신의 프로필과 연결한다.
        ex) POST /api/users/me/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "태그 선택 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 태그를 찾을 수 없음")
    })
    @PostMapping("/api/users/me/tags")
    public ResponseEntity<Void> selectTags(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AddUserTagRequest request
    ) {
        tagService.selectTags(userDetails.getUsername(), request.getTagIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "유저 태그 삭제",
            description = """
        로그인한 사용자가 선택한 태그들을 챌린지와 해제한다.
        ex) DELETE /api/users/me/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "태그 선택 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 태그를 찾을 수 없음")
    })
    @DeleteMapping("/api/users/me/tags")
    public ResponseEntity<Void> deleteUserTags(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid DeleteUserTagRequest request
    ) {
        tagService.deleteTags(userDetails.getUsername(), request.getTagIds());

        return ResponseEntity.noContent().build();
    }

    /*
        유저가 선택한 태그가 챌린지와 연결되도록하는 컨트롤러이다.
     */
    @Operation(
            summary = "챌린지 태그 선택",
            description = """
        로그인한 사용자가 선택한 태그들을 챌린지와 연결한다.
        ex) POST /api/challenge/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "태그 선택 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 태그를 찾을 수 없음")
    })
    @PostMapping("/api/challenge/tags")
    public ResponseEntity<Void> selectChallengeTags(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AddChallengeTagRequest request
    ) {
        tagService.selectChallengeTags(userDetails.getUsername(), request.getChallengeId(), request.getTagIds());

        return ResponseEntity.ok().build();
    }

    /*
        유저가 선택한 태그가 챌린지와 연결이 해제되도록 하는 컨트롤러이다.
     */
    @Operation(
            summary = "챌린지 태그 삭제",
            description = """
        로그인한 사용자가 선택한 태그들을 챌린지와 연결해제 한다.
        ex) DELETE /api/challenge/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "태그 선택 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 태그를 찾을 수 없음")
    })
    @DeleteMapping("/api/challenge/tags")
    public ResponseEntity<Void> deleteChallengeTags(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid DeleteChallengeTagRequest request
    ) {
        tagService.deleteChallengeTags(userDetails.getUsername(), request.getChallengeId(), request.getTagIds());

        return ResponseEntity.noContent().build();
    }

    //태그 생성하기
    @Operation(
            summary = "태그 생성하기",
            description = """
        새로운 태그를 생성한다. 태그 이름과 카테고리를 입력받는다.
        ex) POST /api/tags
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "태그 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이름 누락, 중복 등)")
    })
    @PostMapping("/api/tags/create")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        TagResponse response = tagService.createTag(request.getTagName(), request.getTagCategory());
        return ResponseEntity.status(201).body(response);
    }

}
