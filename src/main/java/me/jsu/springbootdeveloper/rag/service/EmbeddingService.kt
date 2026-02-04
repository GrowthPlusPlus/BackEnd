// 정성우

package me.jsu.springbootdeveloper.rag.service

import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.retry.RetryUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/*
    OpenAI의 임베딩 모델을 사용하여 텍스트를 벡터로 변환한다.
    Spring AI를 통해 임베딩 모델에 접근한다.
 */
@Service
class EmbeddingService(
    private val openAiApi: OpenAiApi  // openai의 api 엔드포인트와 직접 통신한다. 즉 임베딩 계산을 openai에 위임한다.
) {
    // 설정에 있는 openai 임베딩 모델을 사용한다. 현재는 text-embedding-3-small를 사용한다.
    @Value("\${spring.ai.openai.embedding.options.model}")
    private lateinit var embeddingModelName: String

    // OpenAI 임베딩 모델을 설정한다.
    val embeddingModel by lazy {  // 최초 접근 시에만 객체를 생성하고 이후에는 같은 인스턴스를 재사용한다.
        OpenAiEmbeddingModel(
            openAiApi,  // 실제로 openai api를 호출한다.
            MetadataMode.EMBED,  // 임베딩 시 메타데이터를 처리하는 모드로, 메타데이터는 임베딩 하지 않고 문서의 텍스트만 임베딩한다.
            OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)  // 설정한 임베딩 모델을 사용한다.
                .build(),
            RetryUtils.DEFAULT_RETRY_TEMPLATE  // 임베딩 실패 시 재시도 하도록 한다.
        )
    }
}