//여하늘
package me.jsu.springbootdeveloper.dto;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class ChallengeRequestDto {

		//챌린지 방 이름
    @NotBlank(message = "챌린지 방 이름을 입력해주세요.")
    private String title;

		//챌린지 시작일
    @NotNull(message = "챌린지 시작일을 선택해주세요.")
    private LocalDate startDate;

		//인증 기간
    @NotBlank(message = "인증 기간을 선택해주세요.") // 예: "30일"
    private String duration;

		//인증 빈도
    @NotBlank(message = "인증 빈도를 선택해주세요.") // 예: "매일"
    private String frequency;

    //챌린지 태그
    private List<String> tags; // 태그는 여러 개일 수 있으므로 List

    //챌린지 설명
    @NotBlank(message = "챌린지 설명을 입력해주세요.")
    private String description;

    //인증 사진 필수 -> 체크박스
    private boolean photoRequired; 
    
    // 챌린지 공개 여부 -> 임시로 기본값을 PUBLIC으로 설정
    private ChallengeVisibility challengeVisibility = ChallengeVisibility.PUBLIC;
}