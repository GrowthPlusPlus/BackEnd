// 정성우

package me.jsu.springbootdeveloper.dto.challenge;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    챌린지 방장을 위임하기 위해 클라이언트가 서버에게 보내는 dto이다.
 */
@Getter
@NoArgsConstructor
public class ChallengeOwnerDelegateRequest {
    @NotNull
    private Long delegateMemberId;   // 위임할 멤버 ID
    @NotNull
    private boolean leaveAfterDelegate;  // true: 위임 후 챌린지 탈퇴
}
