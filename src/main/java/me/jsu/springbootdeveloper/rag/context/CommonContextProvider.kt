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
        1. 인공지능 코칭 서비스와 소셜에 공유할 수 있는 챌린지 캘린더 서비스를 제공함으로써 성취감을 높인다.
        2. 다른 사람들과의 상호작용 환경과 챌린지 인증률 제공 서비스를 통해 혼자 할 때는 지속하기 힘들었던 챌린지를 지속할 수 있도록 한다.
        3. 다른 사람들과 함께 같은 챌린지를 수행하고 성공 여부를 공유함으로써 집단 동기부여 효과를 유도한다.
        
        해당 서비스의 타겟 유저는 사람들과 상호작용하며 습관을 형성하고 싶은 사람들이다.
    """.trimIndent()
}