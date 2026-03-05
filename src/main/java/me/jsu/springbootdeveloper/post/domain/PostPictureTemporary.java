// 정성우

package me.jsu.springbootdeveloper.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/*
    인증 게시글의 사진을 임시로 저장하는 도메인 클래스이다.
    생성된지 오래된 임시 사진은 스케줄러가 삭제한다.
 */
@Table(
        name = "post_picture_temporary",
        indexes = {
                @Index(
                        name = "idx_temporary_in_created",
                        columnList = "created_at"  // 생성 시간으로 인덱싱한다.
                )
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPictureTemporary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사진을 올린 유저로, 해당 사진을 사용하려는 유저가 올린 유저와 같은지 확인한다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;  // 사진을 올린 챌린지 종류로, 사용하려고 업로드한 사진이 해당 챌린지와 동일한지 확인한다.

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;  // 임시 사진이 생성된 시각이다.

    @Builder
    public PostPictureTemporary(User user, Challenge challenge, String cloudinaryPublicId, String imageUrl) {
        this.user = user;
        this.challenge = challenge;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }
}
