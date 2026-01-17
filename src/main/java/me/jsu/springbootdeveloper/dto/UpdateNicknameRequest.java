// 정성우

package me.jsu.springbootdeveloper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    클라이언트가 유저의 닉네임을 변경하고자 할 때, 같이 보내는 dto이다.
 */
@Getter
@NoArgsConstructor
public class UpdateNicknameRequest {
    @NotBlank
    private String nickname;
}
