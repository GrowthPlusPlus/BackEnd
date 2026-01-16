// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    유저가 관심이 있는 태그들을 저장하는 클래스 도메인이다.
 */
@Table(
        name = "tag_to_user",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저는 동일한 태그를 2개 이상 가질 수 없다.
                        name = "uk_user_tag",
                        columnNames = {"user_id", "tag_id"}
                )
        },
        indexes = {
                @Index(  // 유저 id를 통한 챌린지 태그 검색을 인덱싱 한다.
                        name = "idx_user",
                        columnList = "user_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagToUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public TagToUser(User user, Tag tag) {
        this.user = user;
        this.tag = tag;
    }
}
