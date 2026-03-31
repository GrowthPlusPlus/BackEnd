// 정성우

package me.jsu.springbootdeveloper.rag.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    AI rag 결과를 저장하는 테이블이다.
    하루 1회 제한을 위해 userId + type + date 조합으로 조회한다.
    만약 3개의 값이 일치하면 기존 값 반환을, 한 개라도 다르다면 기존 값에 덮어쓴다.
 */
@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "gpt_response",
        uniqueConstraints = {  // 하루 1회 강제
                @UniqueConstraint(name = "uk_user_type_date", columnNames = {"user_id", "type", "created_date"})
        },
        indexes = {  // 조회 성능을 위해 인덱스 추가
                @Index(name = "idx_user_type_date", columnList = "user_id, type")
        })
public class GptResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청한 유저이다.
    @ManyToOne(fetch = FetchType.LAZY)  // 유저랑 사진이랑 하나씩만 대응되지만 쿼리를 위해 many로 사용한다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 맞춤형 코칭, 챌린지 추천 등 ai 사용의 종류이다.
    @Column(name = "type", nullable = false)
    private String type;

    // ai 사용 결과이다.
    @Column(name = "response", nullable = false, columnDefinition = "TEXT")
    private String response;

    // 날짜 기준으로 하루 1회로 AI 사용을 제한한다.
    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public GptResponse(User user, String type, String response, LocalDate createdDate) {
        this.user = user;
        this.type = type;
        this.response = response;
        this.createdDate = createdDate;
    }

    /*
        다음 날이 되면 AI 답변을 업데이트 할 수 있도록 하는 함수이다.
     */
    public void updateResponse(String response, LocalDate today) {
        this.response = response;
        this.createdDate = today;
    }
}
