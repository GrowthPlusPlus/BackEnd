// 정성우

package me.jsu.springbootdeveloper.rag.dto

import io.swagger.v3.oas.annotations.media.Schema

/*
    질의 요청 데이터 모델이다.
    문서 관련 질의에서만 사용된다.
 */
@Schema(description = "질의 요청 데이터 모델")
data class QueryRequestDto(
    @Schema(description = "사용자 질문", example = "인공지능이란 무엇인가요?")
    val query: String,

    @Schema(description = "최대 검색 결과 수", example = "3", defaultValue = "3")
    val maxResults: Int = 3  // dto에서 값을 지정하지 않으면 기본 값은 3이다.
)

/*
    질의 응답 데이터이다.
 */
@Schema(description = "질의 응답 데이터")
data class QueryResponseDto(
    @Schema(description = "원본 질의")
    val query: String,

    @Schema(description = "생성된 답변")
    val answer: String,
)
