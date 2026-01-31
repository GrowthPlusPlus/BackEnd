// 정성우

package me.jsu.springbootdeveloper.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.dto.ArticleDetailResponse;
import me.jsu.springbootdeveloper.view.service.FriendFeedService;
import me.jsu.springbootdeveloper.view.service.PublicFeedService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
    피드 탭의 인증글을 조회하기 위한 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class FeedController {
    private final PublicFeedService publicFeedService;
    private final FriendFeedService friendFeedService;

    /*
        전체 인증글 중, 공개 범위의 인증글을 조회 한 뒤에 반환해주는 함수로, 챌린지 피드에서 모든 유저의 게시글을 최신순으로 보고 싶을 때 사용한다.
        또한 생성일을 기준으로 내림차순으로 검색하여 최신 -> 과거 순으로 게시글을 검색한다.
     */
    @Operation(
            summary = "공개 인증글 조회 (공개 피드 기능)",
            description = """
        전체 인증글을 최신순으로 페이징 조회하여 인증글 리스트를 20개씩 페이지 형태로 반환한다.
        현재 챌린지가 진행 중이며, 공개 상태인 글만 검색한다.
        ex) GET /api/feed/public?page=0
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/feed/public")
    public ResponseEntity<Page<ArticleDetailResponse>> findAllArticles(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam int page
    ) {
        Page<ArticleDetailResponse> response = publicFeedService.findFeed(userDetails.getUsername(), page);

        return ResponseEntity.ok(response);  // 조회가 성공적이면 얻은 정보를 반환한다.
    }

    /*
        전체 인증글 중, 친구 공개 범위의 인증글을 조회 한 뒤에 반환해주는 함수이다.
     */
    @Operation(
            summary = "친구 인증글 조회 (친구 피드 기능)",
            description = """
        친구 인증글을 최신순으로 페이징 조회하여 인증글 리스트를 20개씩 페이지 형태로 반환한다.
        현재 챌린지가 진행 중이며, 친구 공개 상태인 글만 검색한다.
        ex) GET /api/feed/friends?page=0
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/feed/friends")
    public ResponseEntity<Page<ArticleDetailResponse>> getFriendFeed(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam int page
    ) {
        Page<ArticleDetailResponse> response = friendFeedService.findFeed(userDetails.getUsername(), page);

        return ResponseEntity.ok(response);
    }
}
