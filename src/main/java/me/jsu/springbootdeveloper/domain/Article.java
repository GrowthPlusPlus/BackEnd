// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    DB에 관리될 post에 관한 정보가 저장되는 클래스이다.
 */
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @CreatedDate  // 게시글이 생성된 시간이다.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate  // 게시글이 수정된 시간이다.
    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    @Column(name = "author", nullable = false)
    private String author;

    /*
        @Builder로 빌더 패턴으로 객체를 생성할 수 있게 해준다.
        ex)
        Article article = Article.builder()
            .title("제목")
            .content("내용")
            .author("홍길동")
            .build();
     */
    @Builder
    public Article(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public void update(String title, String content) {
        if (title != null) {  // 게시글을 업데이트하는 경우, 내용만 수정해도 제목에는 null값이 들어가지 않도록 한다.
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }
}
