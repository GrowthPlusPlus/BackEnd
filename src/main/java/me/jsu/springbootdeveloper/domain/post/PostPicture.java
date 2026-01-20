// 정성우

package me.jsu.springbootdeveloper.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/*
    인증 게시글의 사진을 저장하는 도메인 클래스이다.
    유저 프로필 사진과는 다르게 한 게시글에 여러 사진이 올 수 있으므로 유니크 대신 인덱싱만 사용한다.
 */
@Table(
        name = "post_picture",
        indexes = {
                @Index(
                        name = "idx_article_picture",
                        columnList = "article_id"  // post id로 인덱싱 한다.
                )
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPicture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 게시글에는 여러개의 인증 사진이 존재할 수 있으므로 many to one 관계이다.
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;  // 사진이 저장될 게시글

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    public PostPicture(Article article, String cloudinaryPublicId, String imageUrl) {
        this.article = article;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.imageUrl = imageUrl;
    }
}
