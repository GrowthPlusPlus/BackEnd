// 정성우

package me.jsu.springbootdeveloper.challenge.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    챌린지에서 유저를 추방하기 위해 보내는 dto이다.
 */
@Getter
@NoArgsConstructor
public class KickMemberRequest {
    @NotNull
    private Long targetUserId;
}
