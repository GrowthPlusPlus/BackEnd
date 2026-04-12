// 정성우

package me.jsu.springbootdeveloper.rag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.jsu.springbootdeveloper.rag.context.CommonContextProvider
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto
import me.jsu.springbootdeveloper.rag.exception.AiGenerationException
import me.jsu.springbootdeveloper.rag.exception.DocumentProcessingException
import me.jsu.springbootdeveloper.rag.repository.DocumentVectorStore
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

/*
    문서 업로드, 검색, 그리고 검색 결과를 활용한 LLM 응답 생성을 담당한다.
    rag는 업로드 된 문서 내용을 바탕으로 질의에 응답한다.
 */
@Service
class RagService(
    private val vectorStore: DocumentVectorStore,  // 문서를 저장하고 벡터화 된 문서를 검색하는 것을 담당하는 객체이다.
    private val chatService: ChatService,  // LLM 호출을 담당하는 객체이다.
    private val commonContextProvider: CommonContextProvider  // 공통 문서를 가져오기 위한 객체이다.
) {
    // 디버깅을 위해 사용한다.
    private val logger = KotlinLogging.logger {}

    /*
        파일을 업로드하여 벡터 스토어에 추가한다.

        @param file: 업로드 할 파일
        @param originalFilename: 원본 파일명
        @return: 생성된 문서 ID
     */
    fun uploadDocumentFile(
        file: File,  // 업로드 가능한 파일의 종류는 vectorStore.addDocumentFile에 구현된 파일 종류에 달려있다.
        originalFilename: String?  // 파일 이름이다.
    ): String {
        val documentId = UUID.randomUUID().toString()  // 문서의 고유한 id를 생성한다.
        logger.info { "문서 업로드 시작. 파일: $originalFilename, ID: $documentId" }

        // 메타데이터를 생성한다.
        val docMetadata = HashMap<String, Any>().apply {
            // 업로드 된 파일의 원래 파일명을 저장한다. 이름이 없다면 빈 문자열을 등록한다.
            put("originalFilename", originalFilename ?: "")
            // 문서가 업로드 된 시각을 저장한다.
            put("uploadTime", System.currentTimeMillis())
        }

        // 벡터 스토어에 문서를 추가한다.
        try {
            vectorStore.addDocumentFile(documentId, file, docMetadata)  // 비정형 문서를 벡터로 변환하여 저장한다.
            logger.info { "문서 업로드 완료. ID: $documentId" }

            return documentId
        } catch (e: Exception) {
            logger.error(e) { "문서 처리 중 오류 발생: ${e.message}" }

            throw DocumentProcessingException("문서 처리 중 오류: ${e.message}", e)
        }
    }

    /*
        질의와 관련된 문서를 검색한다.

        @param question: 사용자 질문
        @param maxResults: 최대 검색 결과 수
        @return: 유사도 순으로 정렬된 문서 목록
     */
    fun retrieve(question: String, maxResults: Int): List<DocumentSearchResultDto> {
        logger.debug { "검색 시작: '$question', 최대 결과 수: $maxResults" }

        // 사용자 질문, 즉 서버에서 보내는 질문을 임베딩하여 벡터 유사도 검색을 통해 관련 문서 상위 n개를 반환한다.
        return vectorStore.similaritySearch(question, maxResults)
    }

    /*
        문서를 삭제한다.
     */
    fun deleteDocument(documentId: String) {
        vectorStore.deleteDocument(documentId)
    }

    /*
        질문에 대한 답변을 생성하며, 참고한 정보 출처도 함께 제공한다.

        @param question: 사용자 질문
        @param relevantDocs: 이미 검색된 관련 문서
        @param model: 사용할 LLM 모델명
        @return: 참고 출처가 포함된 응답
     */
    @JvmOverloads
    fun generateAnswerWithContexts(
        question: String,
        relevantDocs: List<DocumentSearchResultDto>,  // 벡터 검색의 결과로, 질문과 의미적으로 유사한 문서 조각들이다. (content, metadata 포함)
        model: String = "gpt-4o"
    ): String {
        return generateAnswerWithContextsInternal(question, relevantDocs, model, appendSourceFooter = true)
    }

    /*
        generateAnswerWithContexts와 동일하나, 응답 끝에 참고 문서 출처 블록을 붙이지 않는다.
        JSON 등 기계가 읽을 출력만 필요할 때 사용한다.
     */
    @JvmOverloads
    fun generateAnswerWithContextsWithoutSourceFooter(
        question: String,
        relevantDocs: List<DocumentSearchResultDto>,
        model: String = "gpt-4o"
    ): String {
        return generateAnswerWithContextsInternal(question, relevantDocs, model, appendSourceFooter = false)
    }

    private fun generateAnswerWithContextsInternal(
        question: String,
        relevantDocs: List<DocumentSearchResultDto>,
        model: String,
        appendSourceFooter: Boolean
    ): String {
        logger.debug { "RAG 응답 생성 시작: '$question', 모델: $model" }

        // 검색결과를 검증하여 관련 문서가 비어있는 경우 리턴한다.
        if (relevantDocs.isEmpty()) {
            logger.info { "관련 정보를 찾을 수 없음: '$question'" }

            return "관련 정보를 찾을 수 없습니다. 다른 질문을 시도하거나 관련 문서를 업로드해 주세요."
        }

        // 공통 문서를 참조한다.
        val commonContext = commonContextProvider.commonContext

        // reranking 작업으로 문서 검색 결과 중, 상위 n개만 사용한다.
        val rerankedDocs = relevantDocs.take(5)

        // 문서들에 번호를 붙여 하나의 컨텍스트 문자열로 결합한다.
        val context = buildContextWithLimit(rerankedDocs, maxChars = 4000)

        // 공통 문서와 도메인 문서를 합친다.
        val fullContext = """
            $commonContext
            ---
            $context
        """.trimIndent()

        // 컨텍스트를 포함하는 시스템 프롬프트를 생성한다.
        val systemPromptText = """
            당신은 지식 기반 Q&A 시스템입니다.
            아래 정보를 기반으로만 답변하세요.

            정보:
            $fullContext
        """.trimIndent()

        try {
            val response = chatService.openAiChat(question, systemPromptText, model)
            logger.debug { "AI 응답 생성: ${response}" }

            val aiAnswer = response?.result?.output?.text ?: "응답을 생성할 수 없습니다."

            if (!appendSourceFooter) {
                return aiAnswer
            }

            val sourceInfo = buildString {
                appendLine("\n\n참고 문서:")
                rerankedDocs.forEachIndexed { index, doc ->
                    val originalFilename = doc.metadata["originalFilename"]?.toString() ?: "Unknown file"
                    appendLine("[${index + 1}] $originalFilename")
                }
            }

            return aiAnswer + sourceInfo
        } catch (e: Exception) {
            logger.error(e) { "AI 모델 호출 중 오류 발생: ${e.message}" }

            throw AiGenerationException("AI 답변 생성 도중 오류: ${e.message}", e)
        }
    }

    /*
        해당 함수는 LLM에게 전달할 컨텍스트의 길이를 제한하는 함수이다.
     */
    private fun buildContextWithLimit(
        docs: List<DocumentSearchResultDto>,
        maxChars: Int  // 최종 완성되는 컨텍스트 문자열의 최대 문자 수이다.
    ): String {
        val sb = StringBuilder()  // 문자열을 만들기 위한 객체이다.
        for ((index, doc) in docs.withIndex()) {  // 문서 목록을 하나씩 처리한다.
            val block = "[${index + 1}] ${doc.content}\n\n"  // 문서에 번호를 붙이고 문서 간 구분을 위해 줄바꿈을 한다.
            // 지금까지 누적된 문자열 길이 + 새로 추가하려는 문서 블록의 길이가 최대 문자 수를 초과하면 문서를 추가하지 않고 종료한다.
            if (sb.length + block.length > maxChars) break
            sb.append(block)  // 초과하지 않는다면 더한다.
        }
        return sb.toString()  // LLM에 전달할 컨텍스트이다.
    }
}