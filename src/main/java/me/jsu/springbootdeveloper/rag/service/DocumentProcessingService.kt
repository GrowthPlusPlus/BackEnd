// 정성우

package me.jsu.springbootdeveloper.rag.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.jsu.springbootdeveloper.rag.exception.DocumentProcessingException
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException

/*
    다양한 형식의 문서에서 텍스트를 추출하는 서비스이다.
    현재는 PDF 파일만 지원한다.
 */
@Service
class DocumentProcessingService {
    private val logger = KotlinLogging.logger {}

    /*
        PDF 파일로부터 텍스트를 추출합니다.

        @param pdfFile: PDF 파일 객체
        @return: 추출된 텍스트
        @throws DocumentProcessingException: 텍스트 추출 실패 시
     */
    fun extractTextFromPdf(pdfFile: File): String {
        logger.debug { "PDF 텍스트 추출 시작: ${pdfFile.name}" }

        // 추출한 문자열을 반환한다.
        return try {
            // Apache PDFBox를 사용하여 PDF에서 텍스트를 추출한다.
            PDDocument.load(pdfFile).use { document ->  // PDF 파일을 메모리 상의 PDF 문서 객체로 로드한다.
                logger.debug { "PDF 문서 로드 성공: ${document.numberOfPages}페이지" }
                PDFTextStripper().getText(document)  // PDF 내부 구조를 순회하며 텍스트만 추출한다.
            }.also {
                logger.debug { "PDF 텍스트 추출 완료: ${it.length} 문자" }
            }
        } catch (e: IOException) {
            logger.error(e) { "PDF 텍스트 추출 실패" }
            throw DocumentProcessingException("PDF에서 텍스트 추출 실패: ${e.message}", e)
        }
    }

    /*
        향후 다른 문서 형식 지원을 위한 메서드 추가 가능하다.
        fun extractTextFromDocx(docxFile: File): String { ... }
        hwp, pptx ...
     */
}