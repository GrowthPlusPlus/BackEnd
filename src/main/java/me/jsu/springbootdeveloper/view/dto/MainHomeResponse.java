// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Getter;
//import me.jsu.springbootdeveloper.domain.*;

import java.util.List;

/*
    메인화면을 구성하기 위한 값들을 저장하는 값이다.
    클라이언트는 해당 정보를 바탕으로 메인화면을 구성한다.
    또한 외부에 반환 규칙이 알려지면 안되므로 모두 서비스에서 정제한 뒤 List 형태로 반환한다.
 */
@Getter
public class MainHomeResponse {
    private final List<MyChallenge> myChallenges;  // 내가 참여한 챌린지의 정보이다.
    private final long notificationNumber;  // 내가 아직 읽지 않은 알람 수이다.

    public MainHomeResponse(List<MyChallenge> myChallenges, long notificationNumber) {
        this.myChallenges = myChallenges;
        this.notificationNumber = notificationNumber;
    }
}
