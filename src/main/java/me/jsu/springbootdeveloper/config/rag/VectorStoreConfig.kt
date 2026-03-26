// 정성우

package me.jsu.springbootdeveloper.config.rag

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/*
    벡터 스토어를 인메모리가 아닌 postgresql에 저장하기 위한 설정 클래스이다.
 */
@Configuration
class VectorStoreConfig {
    @Bean
    open fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    @Bean
    open fun vectorStore(jdbcTemplate: JdbcTemplate, embeddingModel: EmbeddingModel): PgVectorStore {
        //vectorStore 객체의 재료로 이미 embeddingModel 을 빈으로 주입받고 있음.
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)  // 텍스트를 벡터 임베딩으로 변환해주는 서비스이다.
            .build()
    }
}