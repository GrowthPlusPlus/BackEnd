// 정성우

package me.jsu.springbootdeveloper.rag.context

import org.springframework.stereotype.Component;

/*
    RAG에서 항상 사용되는 공통 컨텍스트를 제공하는 클래스.
    서비스 개요
    핵심 개념 정의
    해석 기준

    검색 대상이 아니며, 항상 LLM 입력에 포함된다.
 */
@Component
class CommonContextProvider {
    val commonContext: String = """
        이 서비스는 유저가 다양한 챌린지를 수행하며 목표 달성을 돕는 플랫폼이다.

        [서비스 개요]

        [답변 규칙]
        답변 마지막에 사용한 정보의 출처 번호 [1], [2] 등을 반드시 포함해주세요.
        출처 번호는 실제 사용한 문서에만 붙이세요.
    """.trimIndent()
}