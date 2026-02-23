package me.jsu.springbootdeveloper.challenge.dto.basic;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class CreateChallengeRequest {

		//챌린지 방 이름
    @NotBlank(message = "챌린지 방 이름을 입력해주세요.")
    private String title;

		//챌린지 시작일
    @NotNull(message = "챌린지 시작일을 선택해주세요.")
    private LocalDate startDate;

		//인증 기간
    @NotBlank(message = "인증 기간을 선택해주세요.") // 예: "30일"
    private Integer duration;

		//인증 빈도
    @NotBlank(message = "인증 빈도를 선택해주세요.") // 예: "매일"
    private Integer frequency;

    //챌린지 태그
    private List<Long> tags; // 태그는 여러 개일 수 있으므로 List

    //챌린지 설명
    @NotBlank(message = "챌린지 설명을 입력해주세요.")
    private String description;

    //인증 사진 필수 -> 체크박스
    private boolean photoRequired; 
    
    // 챌린지 공개 여부 -> 현재 필드 없음, 기본값 PUBLIC으로 설정
    private ChallengeVisibility challengeVisibility;

    //최대 참여자 수 -> 현재 필드 없음, 기본값 50으로 설정
    private Integer maxParticipantNumber;
}