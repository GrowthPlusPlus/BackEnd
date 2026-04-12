// 정성우

package me.jsu.springbootdeveloper.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.ChallengeDiscoveryRecommendItemResponse;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
    AiChallengeRecommendService와 동일한 사용자·공개 챌린지 컨텍스트로 RAG(LLM)를 호출하고,
    모델이 고른 challengeId 목록을 DB로 보강하여 탐색 탭용 DTO 리스트로 만든다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChallengeDiscoveryRecommendService {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final RagService ragService;
    private final AiChallengeRecommendService aiChallengeRecommendService;
    private final UserService userService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryService challengeQueryService;
    private final ObjectMapper objectMapper;

    /*
        로그인 이메일로 AI 추천 챌린지를 구한 뒤 카드 응답 형태로 반환한다.

        @param email 인증된 사용자 이메일이다.
        @return AI가 선택한 순서를 유지한 추천 항목(유효 id만). 파싱 실패 시 빈 목록이다.
     */
    @Transactional(readOnly = true)
    public List<ChallengeDiscoveryRecommendItemResponse> recommendForDiscovery(String email) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now(SERVICE_ZONE);

        DocumentSearchResultDto contextDoc = aiChallengeRecommendService.getRecommendationContextDocument(user.getId());
        List<DocumentSearchResultDto> contexts = List.of(contextDoc);

        String query = """
                위 정보의 "추천 가능한 공개 챌린지 정보"에 적힌 챌린지 중에서만 골라,
                사용자에게 맞는 챌린지를 최대 3개 추천하라.
                "지금까지 참여한 챌린지 목록"에 있는 챌린지는 추천에 넣지 마라.
                목록에 없는 id는 절대 사용하지 마라.

                응답은 JSON 배열만 출력한다. 설명·마크다운·코드펜스는 넣지 않는다.
                형식 예: [{"challengeId": 1}, {"challengeId": 2}]
                각 원소는 challengeId 숫자 필드를 반드시 가진다.
                """;

        String aiRaw = ragService.generateAnswerWithContextsWithoutSourceFooter(query, contexts);

        List<Long> orderedIds;
        try {
            orderedIds = parseRecommendedChallengeIds(aiRaw);
        } catch (Exception e) {
            log.warn("AI 추천 JSON 파싱 실패, 빈 목록 반환. raw={}", aiRaw, e);
            return List.of();
        }

        Set<Long> participatingIds = challengeQueryService.findWithChallengeByUserId(user.getId()).stream()
                .map(ChallengeParticipant::getChallenge)
                .map(Challenge::getId)
                .collect(Collectors.toSet());

        Map<Long, Challenge> publicWithTags = challengeRepository.findPublicChallengesWithTagMappings(today).stream()
                .collect(Collectors.toMap(Challenge::getId, c -> c, (a, b) -> a));

        List<ChallengeDiscoveryRecommendItemResponse> result = new ArrayList<>();
        for (Long id : orderedIds) {
            if (id == null || participatingIds.contains(id)) {
                continue;
            }
            Challenge challenge = publicWithTags.get(id);
            if (challenge == null) {
                continue;
            }
            result.add(toResponse(challenge, today));
            if (result.size() >= 3) {
                break;
            }
        }

        return result;
    }

    /*
        LLM이 반환한 문자열에서 JSON 배열을 추출하고 challengeId 순서를 담는다.
        마크다운 코드펜스가 있으면 제거한다.
     */
    private List<Long> parseRecommendedChallengeIds(String aiRaw) throws Exception {
        String json = stripMarkdownCodeFence(aiRaw);
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (JsonNode n : root) {
            if (n.isObject() && n.has("challengeId") && n.get("challengeId").isNumber()) {
                ids.add(n.get("challengeId").asLong());
            } else if (n.isIntegralNumber()) {
                ids.add(n.asLong());
            }
        }
        return dedupePreserveOrder(ids);
    }

    /*
        동일 id가 연속·중복으로 나온 경우 앞 순서만 남긴다.
     */
    private List<Long> dedupePreserveOrder(List<Long> ids) {
        Set<Long> seen = new HashSet<>();
        List<Long> out = new ArrayList<>();
        for (Long id : ids) {
            if (id == null || !seen.add(id)) {
                continue;
            }
            out.add(id);
        }
        return out;
    }

    private String stripMarkdownCodeFence(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (!t.startsWith("```")) {
            return t;
        }
        int firstNl = t.indexOf('\n');
        if (firstNl >= 0) {
            t = t.substring(firstNl + 1);
        }
        int endFence = t.lastIndexOf("```");
        if (endFence >= 0) {
            t = t.substring(0, endFence);
        }
        return t.trim();
    }

    /*
        엔티티를 탐색 탭 응답 DTO로 변환한다.
     */
    private ChallengeDiscoveryRecommendItemResponse toResponse(Challenge challenge, LocalDate today) {
        long daysLeft = ChronoUnit.DAYS.between(today, challenge.getEndDate());
        if (daysLeft < 0) {
            daysLeft = 0;
        }
        String remainingLabel = String.format("D-%03d", daysLeft);

        List<String> tagNames = List.of();
        if (challenge.getTagMappings() != null) {
            tagNames = challenge.getTagMappings().stream()
                    .map(tm -> tm.getTag().getTagName())
                    .distinct()
                    .toList();
        }

        return new ChallengeDiscoveryRecommendItemResponse(
                challenge.getTitle(),
                challenge.getParticipantNumber(),
                remainingLabel,
                tagNames,
                challenge.getId()
        );
    }
}
