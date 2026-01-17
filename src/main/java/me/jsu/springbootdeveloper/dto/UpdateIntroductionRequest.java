// 정성우

package me.jsu.springbootdeveloper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    클라이언트가 한 줄 소개를 업데이트할때 같이 보내는 dto이다.
 */
@Getter
@NoArgsConstructor
public class UpdateIntroductionRequest {
    @Size(max = 50)
    private String introduction;
}
