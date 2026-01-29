// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
    유저가 로그아웃 할 수 있도록 리프레시 토큰을 서버 DB에서 삭제해주는 서비스이다.
    따라서 유저는 리프레시 토큰이 존재하지 않으므로 엑세스 토큰을 발급받을 수 없고, OAuth에서 다시 로그인 해야 한다.
    하지만 엑세스 토큰은 여전히 남아있을 수 있으므로 해당 사항은 클라이언트에서 제어한다.
 */
@Service
@RequiredArgsConstructor
public class LogoutService {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void logout(String email) {
        User user = userService.findByEmail(email);

        // DB의 리프레시 토큰을 삭제하여 서버 기준으로 유저는 로그아웃 한다.
        refreshTokenService.deleteByUser_Id(user.getId());
    }
}
