// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.rag.exception.DocumentProcessingException;
import me.jsu.springbootdeveloper.rag.exception.InvalidDocumentException;
import me.jsu.springbootdeveloper.rag.exception.InvalidDocumentFormatException;
import me.jsu.springbootdeveloper.rag.exception.InvalidQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/*
    문서를 벡터 스토어에 업로드 하거나, 벡터 스토어에 있는 내용을 바탕으로 AI 질의를 원할때 사용하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);  // 메세지를 출력하기 위한 객체이다.
    private final RagService ragService;
    // 업로드를 지원하는 문서 유형이다.
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "txt", "md", "json", "csv");

    /*
        문서를 벡터 스토어에 업로드하는 함수이다.
     */
    public void upload(MultipartFile file) {
        // 파일 존재 여부를 검사한다.
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentFormatException("파일이 비어있습니다.");
        }

        // 파일명을 검사한다.
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidDocumentFormatException("파일명이 없습니다.");
        }

        // 확장자 검사를 검사하여 사전에 허용하는 확장자인지 확인한다.
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidDocumentFormatException("지원하지 않는 파일 형식입니다.");
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", "." + extension);  // 임시 파일을 생성한다.
            file.transferTo(tempFile);  // 업로드한 Multipart 파일의 실제 내용을 디스크 파일로 복사한다.

            // 벡터 스토어에 저장한다.
            ragService.uploadDocumentFile(tempFile, originalFilename);
        } catch (IOException e) {
            throw new DocumentProcessingException("문서 파일 처리 중 오류가 발생했습니다.", e);
        } finally {
            // 생성한 임시 파일을 삭제한다.
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /*
        문서를 삭제하는 함수이다.
     */
    public void delete(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new InvalidDocumentException("문서 ID가 비어있습니다.");
        }

        ragService.deleteDocument(documentId);
    }

    /*
        파일 이름에서 확장자를 분리하여 리턴해주는 함수이다.
     */
    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase();
    }

    /*
        문서를 기반으로 RAG 질의를 수행하는 함수이다.
     */
    public QueryResponseDto query(String query, int maxResults) {
        // 질의가 존재하는지 확인한다.
        if (query == null || query.isBlank()) {
            throw new InvalidQueryException("질의가 비어있습니다.");
        }

        // 문서를 검색한다.
        List<DocumentSearchResultDto> documents = ragService.retrieve(query, maxResults);

        // RAG 기반으로 답변을 생성한다.
        String answer = ragService.generateAnswerWithContexts(query, documents);

        // 응답을 조립한다. 사용자 질문, 답변, 답변에 사용된 문서 출처를 반환한다.
        return new QueryResponseDto(query, answer);
    }
}
