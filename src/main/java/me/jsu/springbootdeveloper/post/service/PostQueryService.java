// 정성우

package me.jsu.springbootdeveloper.post.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.PostPicture;
import me.jsu.springbootdeveloper.post.exception.ArticleNotFoundException;
import me.jsu.springbootdeveloper.post.repository.PostPictureRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/*
    인증글에 관련하여 간단한 조회를 담당하는 서비스 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class PostQueryService {
    private final PostRepository postRepository;
    private final PostPictureRepository postPictureRepository;

    /*
        게시글 단일 정보를 게시글 id로 조회하는 함수이다.
     */
    public Article findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));
    }

    /*
        게시글 단일 정보를 게시글 id로 조회하면서 챌린지 정보도 함께 조회하는 함수이다.
     */
    public Article findWithChallengeById(Long postId) {
        return postRepository.findWithChallengeById(postId)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));
    }

    /*
        공개 인증글 목록을 조회한다. 오늘 날짜, 페이지 정보를 넘겨준다.
        피드 생성에 사용된다.
     */
    public Page<Article> findPublicFeed(LocalDate targetDate, Pageable pageable) {
        return postRepository.findPublicFeed(pageable);
    }

    /*
        친구 인증글 목록을 조회한다. 자신의 id, 오늘 날짜, 페이지 정보를 넘겨준다.
        피드 생성에 사용된다.
     */
    public Page<Article> findFriendFeed(Long userId, LocalDate targetDate, Pageable pageable) {
        return postRepository.findFriendFeed(userId, pageable);
    }

    /*
        챌린지 맴버들의 인증글 목록을 조회한다.
        맴버 현황에 사용된다. (피드)
     */
    public Page<Article> findChallengeFeed(Long challengeId, Pageable pageable) {
        return postRepository.findChallengeFeed(challengeId, pageable);
    }

    /*
        인증글 id 여러 개로 그 인증글과 관련된 이미지들을 구한다.
        피드 생성에 사용된다.
     */
    public List<Object[]> findImagesByPostIds(List<Long> postIds) {
        return postPictureRepository.findImagesByPostIds(postIds);
    }

    /*
        캘린더를 조회하기 위해 사용하는 함수이다.
        DB 검색에서 자동으로 DTO를 생성하여 반환한다.
     */
    public List<ChallengeCalendarPhotoResponse> findCalendarPhotos(Long userId, Long challengeId, LocalDate start, LocalDate end) {
        return postRepository.findCalendarPhotos(userId, challengeId, start, end);
    }
    /*
        특정 챌린지의 해당 연 월에 내가 올린 인증글을 최신순으로 조회하여 반환한다.
        캘린더 뷰 화면에 필요한 정보이다.
     */
    public Page<ChallengeCalendarPostResponse> findCalendarPosts(Long userId, Long challengeId, LocalDate start, LocalDate end, Pageable pageable) {
        return postRepository.findCalendarPosts(userId, challengeId, start, end, pageable);
    }

    /*
        캘린더 공유 이미지를 생성한 이후에 추가적인 인증이나 수정사항이 존재하는지 검사하는 함수이다.
     */
    public boolean existsNewProof(Long userId, Long challengeId, LocalDateTime time) {
        return postRepository.existsNewProof(userId, challengeId, time);
    }

    /*
        캘린더 공유 이미지 생성을 위해, 인증글과 사진을 가져오는 함수이다.
     */
    public List<Article> findMonthlyProofArticles(Long userId, Long challengeId, LocalDate start, LocalDate end) {
        return postRepository.findMonthlyProofArticles(userId, challengeId, start, end);
    }
}
