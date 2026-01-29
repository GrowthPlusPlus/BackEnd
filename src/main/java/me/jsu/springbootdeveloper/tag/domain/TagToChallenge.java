// 정성우

package me.jsu.springbootdeveloper.tag.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    챌린지의 태그를 저장하도록 하는 도메인 클래스이다.
 */
@Table(
        name = "tag_to_challenge",
        uniqueConstraints = {
                @UniqueConstraint(  // 한 챌린지는 동일한 태그를 2개 이상 가질 수 없다.
                        name = "uk_challenge_tag",
                        columnNames = {"challenge_id", "tag_id"}
                )
        },
        indexes = {
                @Index(  // 챌린지 id를 통한 태그 검색을 인덱싱한다.
                        name = "idx_challenge",
                        columnList = "challenge_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagToChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public TagToChallenge(Challenge challenge, Tag tag) {
        this.challenge = challenge;
        this.tag = tag;
    }
}
