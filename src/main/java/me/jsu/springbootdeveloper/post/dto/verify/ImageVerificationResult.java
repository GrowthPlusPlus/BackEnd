// 정성우

package me.jsu.springbootdeveloper.post.dto.verify;

import java.util.List;
import java.util.Map;

/*
    clip 서버로부터 이미지 검증 결과를 받는다.
 */
public record ImageVerificationResult (
    String title,
    List<String> categories,

    String bestPrompt,
    double score,

    String bestGenericPrompt,
    double genericScore,

    String bestNegativePrompt,
    double negativeScore,

    String passed,

    double threshold,
    double margin,

    Map<String, Double> promptScores,
    Map<String, Double> genericPromptScores,
    Map<String, Double> negativePromptScores,

    boolean yoloChecked,
    boolean yoloPass,

    List<String> detectedObjects,
    List<String> expectedObjects
) {}
