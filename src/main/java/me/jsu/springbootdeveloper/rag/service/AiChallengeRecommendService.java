// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.tag.repository.TagToUserRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
    사용자 정보를 기반으로 AI가 챌린지를 추천해주는 서비스이다.
    공개 챌린지만 추천 대상으로 한다.
 */
@RequiredArgsConstructor
@Service
public class AiChallengeRecommendService {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    // buildContent에 넣는 공개 챌린지 최대 개수이다.
    private static final int MAX_PUBLIC_CHALLENGES_IN_CONTEXT = 20;

    /*
        AI 챌린지 추천 시 유저 질문으로 쓰는 고정 프롬프트이다.
        (텍스트 응답 API와 카드형 응답 API가 동일하게 사용한다.)
     */
    public static final String CHALLENGE_RECOMMEND_USER_QUERY = """
            사용자의 관심사 태그와 지금까지 참여한 챌린지 정보를 바탕으로,
            공개 챌린지 중에서 사용자에게 적합한 챌린지를 추천해줘.
            추천 이유도 함께 설명해줘.
            추천하는 챌린지는 최대 3개까지야.
            추천하는 각 챌린지를 말할 때는 반드시 아래 "추천 가능한 공개 챌린지 정보"에 적힌
            제목과 글자 하나까지 동일하게 써줘. (앱에서 제목 문자열로 매칭한다.)
            """;

    private final RagService ragService;
    private final TagToUserRepository tagToUserRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryService challengeQueryService;

    /*
        RAG 응답 문자열과, 당시 문서에 실린 공개 후보 엔티티 목록을 묶어 반환할 때 쓰는 값 객체이다.
        이 record 자체는 DB를 호출하지 않는다.
        publicChallengesInContext는 callRecommendRagWithOutcome 내부의
        buildRecommendContextBundle → slicePublicChallengesForContext 에서
        challengeRepository.findPublicChallengesWithTagMappings 로 읽어 온 Challenge 리스트를
        그대로 담은 것이다.
     */
    public record ChallengeRecommendRagResult(String fullAnswer, List<Challenge> publicChallengesInContext) {
    }

    /*
        내부적으로 문서 1건과 공개 후보 슬라이스를 함께 만든다.
     */
    private record RecommendContextBundle(DocumentSearchResultDto document, List<Challenge> publicSlice) {
    }

    /*
        사용자 정보를 기반으로 AI가 챌린지를 추천해주는 메서드이다.
        @param user 추천 대상 사용자이다.
        @return AI가 생성한 챌린지 추천 답변
     */
    public QueryResponseDto recommend(User user) {
        ChallengeRecommendRagResult outcome = callRecommendRagWithOutcome(user);

        return new QueryResponseDto(CHALLENGE_RECOMMEND_USER_QUERY, outcome.fullAnswer());
    }

    /*
        챌린지 추천용 RAG 호출을 수행한다. 질문문·컨텍스트 구성은 recommend()와 동일하다.

        @param user 추천 대상 사용자이다.
        @return generateAnswerWithContexts 결과 전체(답변 + 참고 문서 블록)이다.
     */
    public String callRecommendRag(User user) {
        return callRecommendRagWithOutcome(user).fullAnswer();
    }

    /*
        한 번의 RAG 호출에 사용한 문서와 동일한 공개 챌린지 후보 슬라이스를 함께 반환한다.

        @param user 추천 대상 사용자이다.
        @return 답변 전체 문자열과, 그 답변 직전에 LLM에 넣었던 공개 후보 목록(순서 동일)이다.
     */
    public ChallengeRecommendRagResult callRecommendRagWithOutcome(User user) {
        RecommendContextBundle bundle = buildRecommendContextBundle(user.getId());
        String answer = ragService.generateAnswerWithContexts(
                CHALLENGE_RECOMMEND_USER_QUERY,
                List.of(bundle.document())
        );

        return new ChallengeRecommendRagResult(answer, bundle.publicSlice());
    }

    /*
        사용자 정보(관심 태그·참여 챌린지·공개 후보)로 RAG 문서 1건과 후보 슬라이스를 만든다.
     */
    private RecommendContextBundle buildRecommendContextBundle(Long userId) {
        List<TagToUser> tagToUsers = tagToUserRepository.findWithTagByUserId(userId);
        List<ChallengeParticipant> challengeParticipants = challengeQueryService.findWithChallengeByUserId(userId);

        Set<String> interestTagNames = tagToUsers.stream()
                .map(ttu -> ttu.getTag().getTagName())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(SERVICE_ZONE);
        List<Challenge> publicSlice = slicePublicChallengesForContext(today, interestTagNames);
        int totalPublicCount = challengeRepository.findPublicChallenges(today).size();

        String content = buildContent(tagToUsers, challengeParticipants, publicSlice, totalPublicCount);

        DocumentSearchResultDto document = new DocumentSearchResultDto(
                "user-info-" + userId,
                content,
                buildMetadata(tagToUsers, challengeParticipants),
                1.0
        );

        return new RecommendContextBundle(document, new ArrayList<>(publicSlice));
    }

    /*
        AI에게 보낼 content를 생성한다.
        사용자의 관심사 태그와 참여한 챌린지 정보를 포함한다.
     */
    private String buildContent(
            List<TagToUser> tagToUsers,
            List<ChallengeParticipant> challengeParticipants,
            List<Challenge> publicChallenges,
            int totalPublicChallengeCount
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자 정보 요약\n");
        sb.append("==========\n\n");

        sb.append("관심사 태그 목록:\n");
        if (tagToUsers.isEmpty()) {
            sb.append("- 관심사 태그가 없습니다.\n");
        } else {
            for (TagToUser tagToUser : tagToUsers) {
                sb.append("- ").append(tagToUser.getTag().getTagName())
                        .append(" (").append(tagToUser.getTag().getTagCategory()).append(")\n");
            }
        }
        sb.append("\n");

        sb.append("지금까지 참여한 챌린지 목록:\n");
        if (challengeParticipants.isEmpty()) {
            sb.append("- 참여한 챌린지가 없습니다.\n");
        } else {
            Map<Long, Challenge> participatedById = new LinkedHashMap<>();
            for (ChallengeParticipant cp : challengeParticipants) {
                Challenge ch = cp.getChallenge();
                participatedById.putIfAbsent(ch.getId(), ch);
            }
            for (Challenge ch : participatedById.values()) {
                sb.append("- id: ").append(ch.getId())
                        .append(", 제목: ").append(ch.getTitle()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("추천 가능한 공개 챌린지 정보:\n");
        if (publicChallenges.isEmpty()) {
            sb.append("- 현재 진행 중인 공개 챌린지가 없습니다.\n");
        } else {
            for (Challenge challenge : publicChallenges) {
                sb.append("- id: ").append(challenge.getId())
                        .append(", 제목: ").append(challenge.getTitle())
                        .append(", 챌린지태그: ").append(formatChallengeTagNames(challenge))
                        .append(" (설명: ").append(challenge.getContent() != null ? challenge.getContent() : "설명 없음")
                        .append(")\n");
            }
            if (totalPublicChallengeCount > publicChallenges.size()) {
                sb.append("... 외 ").append(totalPublicChallengeCount - publicChallenges.size())
                        .append("개의 공개 챌린지가 더 있습니다. (관심 태그와 겹치는 후보를 우선 포함함)\n");
            }
        }

        return sb.toString();
    }

    /*
        DB에서 진행 중 공개 챌린지(+태그 fetch)를 읽어 온 뒤,
        관심 태그와 챌린지 태그의 교집합이 많은 순으로 정렬하고 상위 N개만 잘라 쓴다.
     */
    private List<Challenge> slicePublicChallengesForContext(LocalDate today, Set<String> interestTagNames) {
        List<Challenge> all = new ArrayList<>(challengeRepository.findPublicChallengesWithTagMappings(today));
        all.sort(buildPublicChallengeComparator(interestTagNames));
        int n = Math.min(MAX_PUBLIC_CHALLENGES_IN_CONTEXT, all.size());
        if (n == 0) {
            return List.of();
        }
        return new ArrayList<>(all.subList(0, n));
    }

    private Comparator<Challenge> buildPublicChallengeComparator(Set<String> interestTagNames) {
        return Comparator
                .comparingInt((Challenge c) -> countInterestTagOverlapOnChallenge(c, interestTagNames))
                .reversed()
                .thenComparing(Challenge::getStartDate, Comparator.reverseOrder());
    }

    private int countInterestTagOverlapOnChallenge(Challenge challenge, Set<String> interestTagNames) {
        if (interestTagNames.isEmpty() || challenge.getTagMappings() == null) {
            return 0;
        }
        int overlap = 0;
        for (TagToChallenge tm : challenge.getTagMappings()) {
            if (interestTagNames.contains(tm.getTag().getTagName())) {
                overlap++;
            }
        }
        return overlap;
    }

    private String formatChallengeTagNames(Challenge challenge) {
        if (challenge.getTagMappings() == null || challenge.getTagMappings().isEmpty()) {
            return "없음";
        }
        return challenge.getTagMappings().stream()
                .map(tm -> tm.getTag().getTagName())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /*
        AI에게 보낼 메타데이터를 생성한다.
     */
    private Map<String, Object> buildMetadata(List<TagToUser> tagToUsers, List<ChallengeParticipant> challengeParticipants) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("type", "user_challenge_recommendation");

        List<String> tagNames = tagToUsers.stream()
                .map(ttu -> ttu.getTag().getTagName())
                .collect(Collectors.toList());
        meta.put("interestTags", tagNames);
        meta.put("interestTagCount", tagNames.size());

        List<String> challengeTitles = challengeParticipants.stream()
                .map(cp -> cp.getChallenge().getTitle())
                .distinct()
                .collect(Collectors.toList());
        meta.put("participatedChallenges", challengeTitles);
        meta.put("participatedChallengeCount", challengeTitles.size());

        return meta;
    }
}
