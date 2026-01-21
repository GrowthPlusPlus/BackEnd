// 정성우

package me.jsu.springbootdeveloper.service.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.repository.post.PostLikeRepository;
import org.springframework.stereotype.Service;

/*
    인증글의 좋아요를 처리하는 서비스 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;

    /*
        게시글 좋아요의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachLikeToUser(User user) {
        postLikeRepository.detachUser(user);
    }
}
