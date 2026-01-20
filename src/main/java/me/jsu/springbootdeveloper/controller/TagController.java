// 정성우

package me.jsu.springbootdeveloper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.TagResponse;
import me.jsu.springbootdeveloper.dto.TagSelectRequest;
import me.jsu.springbootdeveloper.service.tag.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/api/tags")
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
        ex) PUT /api/users/me/tags
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
            @RequestBody TagSelectRequest request
    ) {
        tagService.selectTags(userDetails.getUsername(), request.getTagIds());
        return ResponseEntity.noContent().build();
    }
}
