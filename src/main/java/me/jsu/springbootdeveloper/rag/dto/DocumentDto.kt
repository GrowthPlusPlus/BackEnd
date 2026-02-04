// 정성우

package me.jsu.springbootdeveloper.rag.dto

import io.swagger.v3.oas.annotations.media.Schema

/*
    문서 검색 결과 정보를 포함하는 dto이다.
 */
@Schema(description = "문서 검색 결과")
data class DocumentSearchResultDto(
    @Schema(description = "문서 ID")
    val id: String,
    
    @Schema(description = "문서 내용")
    val content: String,
    
    @Schema(description = "문서 메타데이터")
    val metadata: Map<String, Any>,
    
    @Schema(description = "유사도 점수")
    val score: Double
)

/*
    문서 검색을 하지 않는 경우 retrieve를 대체하여 dto를 직접 생성하고 rag에 전달한다.

    유저 통계를 요약하기 위해 만들어야 하는 dto
    val statsDoc = DocumentSearchResultDto(
        id = "stats",
        content = userStatsSummary, // 통계 요약 문자열
        metadata = emptyMap(),
        score = 1.0
    )

    val answer = ragService.generateAnswer(question, listOf(statsDoc))

    챌린지 추천을 위해 만들어야 하는 dto
    val challengeDoc = DocumentSearchResultDto(
    id = "challenge",
    content = challengeHistorySummary,
    metadata = emptyMap(),
    score = 1.0
    )

    val answer = ragService.generateAnswer(question, listOf(challengeDoc))
 */
