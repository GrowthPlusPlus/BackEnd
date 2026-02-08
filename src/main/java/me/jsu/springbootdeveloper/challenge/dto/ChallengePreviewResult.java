// 정성우

package me.jsu.springbootdeveloper.challenge.dto;

import java.util.List;

/*
    CLIP 서버에서 반환한 값이 들어있는 dto이다.
 */
public record ChallengePreviewResult (
    String title,
    List<String> categories,
    boolean autoVerifiable,
    String reason
) {}
