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
    유저의 프로필 사진에 대한 도메인 클래스이다.
    클라우디너리의 링크를 통해 사진을 저장할 수 있도록 한다.
 */
@Table(
        name = "user_profile_picture",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저 하나 당 하나의 사진만 가질 수 있다.
                        name = "uk_user_cloudinary",
                        columnNames = {"user_id"}
                )
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfilePicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)  // 유저랑 사진이랑 하나씩만 대응된다.
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "cloudinary_public_id", nullable = false)
    private String cloudinaryPublicId;  // 클라우디너리에 저장된 사진의 id이다.

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;  // 실제로 클라우디너리에서 사진을 가져오는 경로이다.

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserProfilePicture(User user, String cloudinaryPublicId, String imageUrl) {
        this.user = user;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.imageUrl = imageUrl;
    }
}
