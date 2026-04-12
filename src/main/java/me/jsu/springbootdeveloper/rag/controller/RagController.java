// 정성우

package me.jsu.springbootdeveloper.rag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.rag.dto.QueryRequestDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.rag.dto.ChallengeDiscoveryRecommendItemResponse;
import me.jsu.springbootdeveloper.rag.service.AiChallengeDiscoveryRecommendService;
import me.jsu.springbootdeveloper.rag.service.DocumentService;
import me.jsu.springbootdeveloper.rag.service.GptService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
    RAG와 관련된 모든 일들을 처리하는 컨트롤러이다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {
    private final DocumentService documentService;
    private final GptService gptService;
    private final AiChallengeDiscoveryRecommendService aiChallengeDiscoveryRecommendService;
    /*
        PDF 문서를 업로드하여 벡터 스토어에 저장하는 컨트롤러이다.
     */
    @Operation(
            summary = "문서 업로드",
            description = """
        PDF, txt, md, json, csv 파일을 업로드하여 벡터 스토어에 저장한다. 추후 질의에 활용된다.
        ex) POST /api/v1/rag/documents
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문서 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (빈 파일 또는 지원하지 않는 파일)"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadDocument(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        documentService.upload(file);  // 문서를 업로드 한다.

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "문서 삭제",
            description = """
        업로드된 문서를 벡터 스토어에서 삭제한다.
        ex) DELETE /api/v1/rag/documents/{documentId}
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "문서 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String documentId
    ) {
        documentService.delete(documentId);

        return ResponseEntity.noContent().build();
    }

    /*
        사용자 질의에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성한다.
     */
    @Operation(
            summary = "문서를 통한 RAG 질의 수행",
            description = """
        사용자 질문에 대해 관련 문서를 검색하고, RAG 기반 응답을 생성한다.
        ex) POST /api/v1/rag/documents/query
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질의 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/documents/query")
    public ResponseEntity<QueryResponseDto> queryWithRag(
            @RequestBody QueryRequestDto request
    ) {
        // 질의를 통해 답변을 생성한다.
        QueryResponseDto response = documentService.query(request.getQuery(), request.getMaxResults());

        return ResponseEntity.ok(response);
    }

    /*
    사용자 질의에 대해 관련 문서를 검색하고 RAG 기반 응답을 생성한다.
 */
    @Operation(
            summary = "유저 정보를 바탕으로 맞춤형 코칭 수횅",
            description = """
        로그인한 유저 정보를 바탕으로 맞춤형 코칭을 수행해준다.
        ex) POST /api/v1/rag/coach/query
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질의 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/coach/query")
    public ResponseEntity<String> queryWithRag(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 유저 정보를 통해 코칭 답변을 생성한다.
        String response = gptService.coach(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "AI 챌린지 추천",
            description = """
        로그인한 유저의 관심사 태그와 참여 중인 챌린지를 바탕으로 챌린지를 추천한다.
        ex) POST /api/v1/rag/recommend/query
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질의 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/recommend/query")
    public ResponseEntity<String> recommendChallenge(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 유저 정보를 통해 코칭 답변을 생성한다.
        String response = gptService.challengeRecommend(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }

    /*
        AiChallengeRecommendService와 동일한 RAG 컨텍스트로 LLM이 challengeId를 고르고,
        서버가 DB로 카드 필드를 채워 탐색 탭에 넘긴다.
     */
    @Operation(
            summary = "탐색 탭용 AI 챌린지 추천(구조화)",
            description = """
        로그인 유저 기준으로 RAG/LLM이 공개 챌린지 id를 최대 3개 선택한다.
        응답은 챌린지 이름, 참여 인원, D-000 형식 남은 일수, 태그, id를 포함한다.
        ex) GET /api/v1/rag/recommend/discovery
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/recommend/discovery")
    public ResponseEntity<List<ChallengeDiscoveryRecommendItemResponse>> recommendDiscovery(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<ChallengeDiscoveryRecommendItemResponse> items = aiChallengeDiscoveryRecommendService
                .recommendForDiscovery(userDetails.getUsername());

        return ResponseEntity.ok(items);
    }
}