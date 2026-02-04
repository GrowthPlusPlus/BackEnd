// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;

/*
    유저 정보를 기반으로 AI가 어드바이스를 해주는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class AiCoachService {
    private final RagService ragService;
    private final UserService userService;

    public QueryResponseDto coach(String email) {
        User user = userService.findByEmail(email);

        return new QueryResponseDto("", "");  // 생성한 답변을 반환한다.
    }
}
