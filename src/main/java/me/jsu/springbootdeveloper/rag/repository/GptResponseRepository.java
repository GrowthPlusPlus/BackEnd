// 정성우

package me.jsu.springbootdeveloper.rag.repository;

import me.jsu.springbootdeveloper.rag.domain.GptResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    GPT 응답을 관리하는 리포지토리이다.
 */
public interface GptResponseRepository extends JpaRepository<GptResponse, Long> {

    /*
        userId + type 기준으로 조회한다.
     */
    Optional<GptResponse> findByUser_IdAndType(Long userId, String type);
}
