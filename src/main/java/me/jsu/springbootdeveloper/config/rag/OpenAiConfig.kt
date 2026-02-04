// 정성우

package me.jsu.springbootdeveloper.config.rag

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/*
    OpenAI API를 설정하는 클래스이다.
    OpenAiApi를 스프링 빈으로 등록한다.
 */
@Configuration
class OpenAiConfig {
    /*
        logger 객체를 생성한다. logger.debug, logger.error(e) 등으로 사용하여 콘솔에 메세지를 출력할 수 있다.
        즉 비즈니스 흐름과는 상관 없이 디버깅, 오류 추적을 위해 사용한다.
     */
    private val logger = KotlinLogging.logger {}

    // application.yml에 정의된 값을 주입한다. openai에서 프로젝트를 만들어 반환받은 값을 주입받는다.
    @Value("\${spring.ai.openai.api-key}")
    private lateinit var apiKey: String

    /*
        OpenAI API 클라이언트를 빈에 등록한다.
        즉 OpenAiApi 객체를 생성하여 api key를 설정한 뒤, 스프링 컨텍스트에 싱글턴 빈으로 등록한다.
     */
    @Bean
    fun openAiApi(): OpenAiApi {
        logger.debug { "OpenAI API 클라이언트 초기화" }
        return OpenAiApi.builder()
            .apiKey(apiKey)
            .build()
    }
}