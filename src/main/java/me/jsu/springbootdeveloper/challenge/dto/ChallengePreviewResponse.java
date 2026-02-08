// 정성우

package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/*
    챌린지 이름이 사진으로 검증하기에 적합한지 아닌지 여부를 클라이언트에게 반환하는 dto이다.
 */
@Getter
@Builder
public class ChallengePreviewResponse {
    boolean autoVerifiable;
}
