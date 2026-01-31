// 정성우

package me.jsu.springbootdeveloper.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.dto.*;
import me.jsu.springbootdeveloper.post.service.CommentService;
import me.jsu.springbootdeveloper.post.service.ImageVerificationService;
import me.jsu.springbootdeveloper.post.service.PostLikeService;
import me.jsu.springbootdeveloper.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
    클라이언트의 요청 중, 챌린지 인증글인 post와 관련된 모든 요청을 처리해주는 컨트롤러이다.
 */
@RequiredArgsConstructor
@RestController  //http response body에 객체 데이터를 JSON형식으로 직렬하여 반환해주는 컨트롤러이다.
public class PostApiController {
    private final PostService postService;  // 의존성을 주입하여 빈으로 등록된 PostService클래스의 함수를 사용한다.
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final ImageVerificationService imageVerificationService;

    /*
        서버에 사진을 올리기 전에, 해당 사진이 올바른 사진인지 검사하는 컨트롤러이다.
        사진 1장을 사용자가 올릴 때마다 수행한다.
     */
    @Operation(
            summary = "인증 사진 검사",
            description = """
        챌린지 인증을 위해 올린 사진을 검사한다. 사진 1장을 사용자가 올릴 때마다 수행한다.
        현재 하는 검사는 이미지 크기 검사, 이미지 포멧 검사, 손상된 이미지 업로드 차단 이다. AI 검사는 추후에 추가해야 한다.
        ex) POST /api/image/verify"
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이미지 요청 검증 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 검증 실패")
    })
    @PostMapping("/api/image/verify")
    public ResponseEntity<Void> verify(
            @Parameter(description = "검사할 이미지 파일", required = true)
            @RequestPart("images") MultipartFile image
    ) {
        imageVerificationService.verify(image);

        return ResponseEntity.noContent().build();  // 인증이 성공한 경우이다.
    }

    /*
       요청으로 온 http 메서드가 post이고, url이 아래와 같으면 매핑하여 함수를 실행한다.
       @RequestBody가 요청 본문 값인 JSON형태를 역직렬화하여 AddArticleRequest의 맴버변수에 매핑한다.
       @RestController가 있으므로 @RequestBody는 @PostMapping밑에 붙이지 않고 생략한다.

       해당 함수로 챌린지를 인증하기 위해 게시글을 만드는 역할을 한다.
     */
    @Operation(
            summary = "인증글 생성",
            description = """
        챌린지 인증을 위한 게시글을 생성한다.
        사진은 최대 5장 받을 수 있으며, 먼저 사진 검증을 받은 뒤 해당 컨트롤러로 인증글을 업로드 해야 한다.
        ex) POST /api/articles
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시글 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "500", description = "외부 이미지 스토리지 업로드 실패")
    })
    @PostMapping("/api/articles")  // post형태는 REST API에서는 생성하는 경우에 사용한다.
    /*
        AddArticleRequest 맴버변수에 맞지 않는 request값이 들어오면 무시한다.
        principal은 스프링 시큐리티 환경에서 인증된 사용자임을 나타내는 매개변수이다.
     */
    public ResponseEntity<ArticleDetailResponse> addArticle(
            // 안정성을 위하여 Principal 대신 사용한다.
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("data") @Valid AddArticleRequest request,
            @Parameter(description = "업로드할 인증 이미지 파일 목록", required = true)
            @RequestPart("image") List<MultipartFile> images
    ) {
        ArticleDetailResponse response = postService.save(userDetails.getUsername(), request.getContent(), request.getChallengeId(), images);

        // post가 성공하면, 게시글 정보를 응답 객체에 담아 JSON으로 반환하여 클라이언트가 바로 게시글 화면으로 넘어갈 수 있도록 한다.
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 리소스 생성 성공을 의미하는 상태 코드이다.
                .body(response);         // 정제한 dto값을 결과로 반환한다.
    }

    /*
        article id로 인증글 하나를 검색하여 반환하는 함수이다.
        url 경로에서 id값을 추출하여 사용한다.
     */
    @Operation(
            summary = "인증글 상세 조회",
            description = """
        게시글 ID로 인증글 상세 정보를 조회하여 반환한다.
        ex) GET /api/articles/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/api/articles/{postId}")
    public ResponseEntity<ArticleDetailResponse> findArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId) {
        ArticleDetailResponse response = postService.getPost(userDetails.getUsername(), postId);

        return ResponseEntity.ok(response);

    }

    /*
        게시글 id로 글을 삭제하는 함수이다.
     */
    @Operation(
            summary = "인증글 삭제",
            description = """
        게시글 ID로 인증글을 삭제한다.
        ex) DELETE /api/articles/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/api/articles/{postId}")
    public ResponseEntity<Void> deleteArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId) {
        postService.delete(userDetails.getUsername(), postId);

        return ResponseEntity.noContent().build();  // 삭제에 성공하면 204를 반환한다.
    }

    /*
        url에서 게시글 id를 받고, dto형태의 request를 받아 블로그 글을 수정한다.
        들어올 때는 json형태지만, 스프링이 자동으로 dto형태로 변환해준다.
     */
    @Operation(
            summary = "인증글 수정",
            description = """
        게시글 id로 인증글 내용을 수정한다.
        ex) PATCH /api/articles/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
            @ApiResponse(responseCode = "400", description = "클라이언트의 요청이 잘못됨"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/api/articles/{postId}")
    public ResponseEntity<ArticleDetailResponse> updateArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId,
            @RequestPart("request") @Valid UpdateArticleRequest request,
            @RequestPart("images") List<MultipartFile> images
    ) {
        ArticleDetailResponse response = postService.update(userDetails.getUsername(), postId, request.getContent(), request.getDeleteImageIds(), images);

        return ResponseEntity.ok(response);
    }

    /*
        article id로 인증글의 댓글 목록을 조회해주는 컨트롤러이다.
        GET /api/articles/{postId}/comments?page={page} 와 같이 클라이언트가 요청한다.
     */
    @Operation(
            summary = "댓글 목록 조회",
            description = """
        게시글의 댓글 목록을 페이지 단위로 20개씩 조회하여 반환한다.
        ex) GET /api/articles/1/comments?page=0
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @GetMapping("/api/articles/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam int page) {
        Page<CommentResponse> responses = commentService.getComments(userDetails.getUsername(), postId, page);

        return ResponseEntity.ok(responses);
    }

    /*
        인증글에 댓글을 작성해주는 컨트롤러이다.
        댓글 작성 후 화면 업데이트를 위해서 앱이 조회 컨트톨러를 다시 호출해야 한다.
     */
    @Operation(
            summary = "댓글 생성",
            description = """
        게시글에 댓글을 작성한다.
        댓글 작성 후에 화면 업데이트를 위해 앱이 전체 댓글을 다시 조회해야 한다.
        ex) POST /api/articles/1/comments
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
            @ApiResponse(responseCode = "400", description = "댓글 내용 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @PostMapping("/api/articles/{postId}/comments")
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request  // 해당 요청이 NotBlank 조건을 만족하는지 검사한다.
    ) {
        commentService.createComment(userDetails.getUsername(), postId, request.getContents());

        return ResponseEntity.status(HttpStatus.CREATED).build();  // 생성이 완료됐음을 알린다.
    }

    /*
        인증글의 댓글을 수정하는 컨트롤러이다.
     */
    @Operation(
            summary = "댓글 수정",
            description = """
        댓글 ID로 댓글 내용을 수정한다.
        ex) PATCH /api/comments/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "400", description = "댓글 내용 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request
    ) {
        commentService.updateComment(userDetails.getUsername(), commentId, request.getContents());

        return ResponseEntity.noContent().build();  // 수정 성공 메세지를 반환한다.
    }

    /*
        인증글의 댓글을 삭제하는 컨트롤러이다. 댓글 id로 삭제한다.
     */
    @Operation(
            summary = "댓글 삭제",
            description = """
        댓글 ID로 댓글을 삭제한다.
        ex) DELETE /api/comments/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userDetails.getUsername(), commentId);

        return ResponseEntity.noContent().build();  // 성공적으로 삭제되면 완료됐음을 알린다.
    }

    @Operation(
            summary = "인증글 좋아요",
            description = """
        인증글 id로 좋아요를 누른다.
        ex) POST /api/article/1/like
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "좋아요 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "인증글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 좋아요를 누른 인증글")
    })
    @PostMapping("/api/article/{articleId}/like")
    public ResponseEntity<Void> likeArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "인증글 id", required = true, example = "1")
            @PathVariable Long articleId
    ) {
        postLikeService.likePost(userDetails.getUsername(), articleId);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "인증글 좋아요 취소",
            description = """
        인증글 id로 좋아요를 취소한다.
        ex) DELETE /api/article/1/like
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "좋아요 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "좋아요를 찾을 수 없음")
    })
    @DeleteMapping("/api/article/{articleId}/like")
    public ResponseEntity<Void> unlikeArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "인증글 id", required = true, example = "1")
            @PathVariable Long articleId
    ) {
        postLikeService.unlikePost(userDetails.getUsername(), articleId);

        return ResponseEntity.noContent().build();
    }
}
