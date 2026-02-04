// 정성우

package me.jsu.springbootdeveloper.rag.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto
import me.jsu.springbootdeveloper.rag.exception.DocumentProcessingException
import me.jsu.springbootdeveloper.rag.service.DocumentProcessingService
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Repository
import java.io.File
import org.springframework.ai.vectorstore.SearchRequest

/*
    문서를 벡터화하여 저장하고, 벡터 유사도 검색을 제공한다.
    Spring AI의 SimpleVectorStore를 활용한다.
 */
@Repository
class DocumentVectorStore(
    // Spring AI의 PgVectorStore를 주입받는다. 문서를 임베딩하여 내부적으로 List<Document> 형태로 저장하거나, 쿼리를 임베딩 하여 검색한다.
    private val vectorStore: VectorStore,
    private val documentProcessingService: DocumentProcessingService,  // PDF 파일에서 텍스트를 추출해주는 서비스이다. 다른 파일 유형은 구현되어 있지 않다.
) {
    private val logger = KotlinLogging.logger {}

    /*
        이미 텍스트 상태인 문서를 벡터 스토어에 추가한다.

        @param id: 문서 식별자
        @param fileText: 문서 내용
        @param metadata: 문서 메타데이터
     */
    fun addDocument(id: String, fileText: String, metadata: Map<String, Any>) {
        logger.debug { "문서 추가 시작 - ID: $id, 내용 길이: ${fileText.length}" }

        try {
            // Spring AI Document 객체를 생성한다.
            val document = Document(fileText, metadata + mapOf("id" to id))  // 메타데이터에 id를 포함시킨다.
            // 긴 문서를 임베팅 하기 전에 여러개의 작은 Document로 분할하기 위한 작업이다.
            val textSplitter = TokenTextSplitter.builder()
                .withChunkSize(512)  // 하나의 청크에 포함될 최대 토큰 수
                .withMinChunkSizeChars(350)  // 최소 청크 크기 (너무 짧은 청크는 버린다.)
                .withMinChunkLengthToEmbed(5)  // 임베딩할 최소 청크 길이
                .withMaxNumChunks(10000)  // 하나의 원본 문서에서 생성 가능한 최대 청크 수
                .withKeepSeparator(true)  // 구분자 유지 여부
                .build()
            val chunks = textSplitter.split(document)  // 위 설정을 바탕으로 문서를 여러개의 document 청크로 만든다.

            // 벡터 스토어에 문서 청크를 추가한다. (내부적으로 임베딩 변환 수행)
            vectorStore.add(chunks)

            logger.info { "문서 추가 완료 - ID: $id" }
        } catch (e: Exception) {
            logger.error(e) { "문서 추가 실패 - ID: $id" }
            throw DocumentProcessingException("문서 임베딩 및 저장 실패: ${e.message}", e)
        }
    }

    /*
        파일을 텍스트로 변환하여 addDocument에 넘겨준다.

        @param id: 문서 식별자
        @param file: 파일 객체
        @param metadata: 문서 메타데이터
     */
    fun addDocumentFile(id: String, file: File, metadata: Map<String, Any>) {
        logger.debug { "파일 문서 추가 시작 - ID: $id, 파일: ${file.name}" }

        try {
            // 파일에서 텍스트를 추출한다.
            val fileText =
            if (file.extension.lowercase() == "pdf") {  // pdf 파일에서 텍스트를 추출한다.
                documentProcessingService.extractTextFromPdf(file)
            } else {  // txt, md, json, csv, xml 파일에서 텍스트를 추출한다.
                file.readText()
            }
            logger.debug { "파일 텍스트 추출 완료 - 길이: ${fileText.length}" }

            addDocument(id, fileText, metadata)  // 추출한 텍스트를 벡터 스토어에 추가하기 위해 넘겨준다.
        } catch (e: Exception) {
            logger.error(e) { "파일 처리 실패 - ID: $id, 파일: ${file.name}" }
            throw DocumentProcessingException("파일 처리 실패: ${e.message}", e)
        }
    }

    /*
        질의와 유사한 문서를 검색한다.

        @param query: 검색 질의
        @param maxResults: 최대 결과 수
        @return: 유사도 순으로 정렬된 검색 결과 목록
     */
    fun similaritySearch(query: String, maxResults: Int): List<DocumentSearchResultDto> {
        logger.debug { "유사도 검색 시작 - 질의: '$query', 최대 결과: $maxResults" }

        try {
            // 검색 요청 구성으로 자연어 질의, 반환할 최대 결과 수로 구성된다.
            val request = SearchRequest.builder()
                .query(query)
                .topK(maxResults)
                .build()

            // 유사성 검색을 실행한다. score 기준 내림차순으로 정렬한다.
            val results = vectorStore.similaritySearch(request) ?: emptyList()

            logger.debug { "유사도 검색 완료 - 결과 수: ${results.size}" }

            // 결과를 매핑하여 dto로 반환한다.
            return results.map { result ->
                DocumentSearchResultDto(
                    id = (result.metadata["id"] ?: "unknown").toString(),  // id 매핑
                    content = result.text ?: "",
                    metadata = result.metadata.filter { it.key != "id" },
                    score = result.score ?: 0.0  // 검색 유사도
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "유사도 검색 실패 - 질의: '$query'" }

            throw DocumentProcessingException("유사도 검색 중 오류 발생: ${e.message}", e)
        }
    }

    /*
        문서 ID를 기준으로 벡터 스토어에서 문서를 삭제한다.

        @param id: 문서 식별자
     */
    fun deleteDocument(id: String) {
        logger.debug { "문서 삭제 시작 - ID: $id" }

        try {
            // metadata의 id를 기준으로 삭제한다.
            vectorStore.delete("id == '$id'")

            logger.info { "문서 삭제 완료 - ID: $id" }
        } catch (e: Exception) {
            logger.error(e) { "문서 삭제 실패 - ID: $id" }
            throw DocumentProcessingException("문서 삭제 실패: ${e.message}", e)
        }
    }
}