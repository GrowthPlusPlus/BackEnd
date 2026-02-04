// 정성우

package me.jsu.springbootdeveloper.rag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.stereotype.Service

/*
    OpenAI API를 사용하여 질의응답을 수행하는 서비스이다.
 */
@Service
class ChatService(private val openAiApi: OpenAiApi) {  // config에서 반환한 빈을 주입한다.
    // 메세지를 출력하기 위해 사용한다.
    private val logger = KotlinLogging.logger {}

    /*
        OpenAI 챗 API를 이용하여 응답을 생성한다.

        @param userInput: 사용자 입력 메시지로 우리 서버에선 유저의 통계 등을 보낸다.
        @param systemMessage: 시스템 프롬프트로 gpt에게 무슨 역할을 수행해야 하는지 결정한다. 즉 어떻게 말하게 할지 정한다.
        @param model: 사용할 LLM 모델명이다.
        @return: 챗 응답 객체로, 오류 시 null을 반환한다.
     */
    fun openAiChat(
        userInput: String,
        systemMessage: String,
        model: String = "gpt-4o-mini"  // 비용 감소를 위해 경량 모델을 사용한다.
    ): ChatResponse? {
        logger.debug { "OpenAI 챗 호출 시작 - 모델: $model" }
        try {
            /*
                LLM에 실제로 전달되는 대화 컨텍스트의 구성이다.
                시스템 메세지, 유저 메세지로 구성된다.
             */
            val messages = listOf(
                SystemMessage(systemMessage),
                UserMessage(userInput)
            )

            // 챗의 옵션을 설정한다.
            val chatOptions = ChatOptions.builder()
                .model(model)
                .temperature(0.3)  // 사실 기반 응답에 적합하도록 높은 정확성을 가진 답변만 생성한다.
                /*
                    생성되는 응답의 최대 토큰 수를 제한하여 비용 및 응답 길이를 통제한다.
                    정확한 토큰 사용 수를 알고 싶다면 OpenAI API 사용 또는 Spring AI에서 ChatResponse.getUsage()로 확인이 필요하다.
                 */
                .maxTokens(512)
                .build()

            /*
                LLM에게 보낼 프롬프트를 생성한다.
                즉 요청 데이터를 구조화 하는 것으로 위에서 설정한 메세지, 옵션을 하나로 포장한다.
             */
            val prompt = Prompt(messages, chatOptions)

            // 기존 빈에서 주입받은 값을 바탕으로 AI 챗 모델을 생성한다.
            val chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)  // config로 설정하여 빈으로 만든 값
                .build()

            /*
                미리 선택한 AI 모델을 호출하여 AI가 생성한 값을 ChatResponse 형태로 반환한다.
                ChatResponse에는 result, metadata, usage 등의 정보가 포함되어 있다.
             */
            return chatModel.call(prompt)
        } catch (e: Exception) {  // 오류가 발생한 경우이다.
            logger.error(e) { "OpenAI 챗 호출 중 오류 발생: ${e.message}" }
            return null
        }
    }
}