// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.dto.basic.SearchChallengeResponse;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.AiChallengeCombinedRecommendResponse;
import me.jsu.springbootdeveloper.rag.dto.AiChallengeCombinedRecommendResponse.AiChallengeCombinedRecommendItemResponse;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.tag.repository.TagToChallengeRepository;
import me.jsu.springbootdeveloper.tag.repository.TagToUserRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
    서술형 추천 요약과 탐색 카드 목록을 한 번에 내려주는 합본 서비스이다.

    기존 AiChallengeRecommendService / AiChallengeDiscoveryRecommendService 를
    직접 합치지 않고, RAG 1회 호출 결과를 재사용해 배너 요약·카드·추천 이유를 조립한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChallengeCombinedRecommendService {

    // 챌린지 진행 여부·종료일 비교에 쓰는 기준 시간대이다.
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /*
        배너 요약에 넣을 참여 챌린지 제목 최대 개수이다.
        참여 이력이 많아도 문장이 너무 길어지지 않게 제한한다.
     */
    private static final int MAX_PARTICIPATED_TITLES_IN_SUMMARY = 3;

    private final AiChallengeRecommendService aiChallengeRecommendService;
    private final UserService userService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryService challengeQueryService;
    // 배너 요약의 관심 태그(#태그)를 만들 때 사용한다.
    private final TagToUserRepository tagToUserRepository;
    // 참여 챌린지와 추천 챌린지의 태그 유사도를 계산할 때 사용한다.
    private final TagToChallengeRepository tagToChallengeRepository;

    /*
        로그인 이메일 기준으로 서술형 요약 + 추천 카드(추천 이유 포함)를 함께 반환한다.

        @param email 인증된 사용자 이메일이다.
        @return 배너용 summary 와 카드 목록(challenges)을 담은 합본 응답이다.
     */
    @Transactional(readOnly = true)
    public AiChallengeCombinedRecommendResponse recommendCombined(String email) {
        try {
            User user = userService.findByEmail(email);
            LocalDate today = LocalDate.now(SERVICE_ZONE);

            // RAG는 1회만 호출한다. (서술·카드 매칭이 같은 답변을 공유한다.)
            AiChallengeRecommendService.ChallengeRecommendRagResult ragOutcome =
                    aiChallengeRecommendService.callRecommendRagWithOutcome(user);
            String prose = stripReferenceDocumentBlock(ragOutcome.fullAnswer());

            List<Challenge> picked = pickChallengesMentionedInProse(
                    prose,
                    ragOutcome.publicChallengesInContext()
            );

            // 참여 이력은 카드 필터와 배너 요약에 함께 쓴다.
            List<ChallengeParticipant> participants =
                    challengeQueryService.findWithChallengeByUserId(user.getId());
            Set<Long> participatingIds = participants.stream()
                    .map(ChallengeParticipant::getChallenge)
                    .map(Challenge::getId)
                    .collect(Collectors.toSet());

            /*
                카드 필드는 오늘 기준 진행 중 공개 챌린지 엔티티로만 채운다.
                답변에 이름이 나와도 종료·비공개면 맵에 없어 스킵된다.
             */
            Map<Long, Challenge> publicWithTags = challengeRepository
                    .findPublicChallengesWithTagMappings(today)
                    .stream()
                    .collect(Collectors.toMap(Challenge::getId, c -> c, (a, b) -> a));

            List<Challenge> cardChallenges = new ArrayList<>();
            for (Challenge brief : picked) {
                if (participatingIds.contains(brief.getId())) {
                    continue;
                }
                Challenge full = publicWithTags.get(brief.getId());
                if (full == null) {
                    continue;
                }
                cardChallenges.add(full);
                if (cardChallenges.size() >= 3) {
                    break;
                }
            }

            // 카드별 추천 이유: 해당 제목 구간 ~ 다음 제목 직전까지.
            Map<Long, String> reasonByChallengeId = extractRecommendReasons(prose, cardChallenges);

            List<AiChallengeCombinedRecommendItemResponse> items = new ArrayList<>();
            for (Challenge challenge : cardChallenges) {
                String reason = reasonByChallengeId.getOrDefault(challenge.getId(), "");
                items.add(new AiChallengeCombinedRecommendItemResponse(
                        toResponse(challenge),
                        reason
                ));
            }

            /*
                배너 요약은 LLM 본문을 쓰지 않는다.
                관심 태그 + (추천 결과·추천 이유와 비슷한 참여 챌린지 최대 3개)로 문장을 만든다.
                비슷한 참여 챌린지가 없으면 참여 문장 전체를 생략한다.
             */
            List<TagToUser> tagToUsers = tagToUserRepository.findWithTagByUserId(user.getId());
            List<String> similarParticipatedTitles = selectSimilarParticipatedTitles(
                    participants,
                    cardChallenges,
                    reasonByChallengeId
            );
            String summary = buildBannerSummary(tagToUsers, similarParticipatedTitles);

            if (items.isEmpty() && !picked.isEmpty()) {
                log.debug("합본 추천: 답변 매칭은 됐으나 공개·참여 필터 후 카드가 비었습니다.");
            }

            return new AiChallengeCombinedRecommendResponse(summary, items);
        } catch (Exception e) {
            log.error("합본 챌린지 추천 처리 중 오류가 발생했습니다. email={}", email, e);
            throw e;
        }
    }

    /*
        RagService가 덧붙인 "참고 문서:" 블록을 제거해 본문만 남긴다.

        @param fullAnswer RAG 전체 답변 문자열이다.
        @return 참고 문서 블록이 제거된 본문이다.
     */
    private String stripReferenceDocumentBlock(String fullAnswer) {
        if (fullAnswer == null) {
            return "";
        }
        int cut = fullAnswer.indexOf("\n\n참고 문서:");
        if (cut >= 0) {
            return fullAnswer.substring(0, cut).trim();
        }
        return fullAnswer.trim();
    }

    /*
        candidates에 있는 챌린지(실제 DB 행)만 본문 제목과 대조한다.
        본문에만 있고 DB 후보에 없는 이름은 걸러진다.

        @param prose 참고 문서가 제거된 AI 답변 본문이다.
        @param candidates RAG 컨텍스트에 실렸던 공개 후보 목록이다.
        @return 본문에 언급된 순서의 챌린지 목록(최대 3개)이다.
     */
    private List<Challenge> pickChallengesMentionedInProse(String prose, List<Challenge> candidates) {
        record IndexedChallenge(int indexInAnswer, Challenge challenge) {
        }

        List<IndexedChallenge> matches = new ArrayList<>();
        for (Challenge c : candidates) {
            String title = c.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }
            int idx = prose.indexOf(title);
            if (idx >= 0) {
                matches.add(new IndexedChallenge(idx, c));
            }
        }

        matches.sort(Comparator.comparingInt(IndexedChallenge::indexInAnswer));

        List<Challenge> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (IndexedChallenge m : matches) {
            if (out.size() >= 3) {
                break;
            }
            if (seen.add(m.challenge().getId())) {
                out.add(m.challenge());
            }
        }
        return out;
    }

    /*
        배너용 서술형 요약을 고정 문장으로 만든다.

        예1) 비슷한 참여 챌린지가 있을 때
        사용자의 관심사 태그는 #운동 과 #러닝 이며, 지금까지 참여한 챌린지는 "매일 30분 러닝" 입니다.
        이러한 정보를 바탕으로 추천할 수 있는 공개 챌린지는 다음과 같습니다.

        예2) 참여 이력이 없거나 비슷한 참여 챌린지가 없을 때
        사용자의 관심사 태그는 #운동 과 #러닝 입니다.
        이러한 정보를 바탕으로 추천할 수 있는 공개 챌린지는 다음과 같습니다.

        @param tagToUsers 사용자 관심 태그 목록이다.
        @param similarParticipatedTitles 추천 결과와 비슷한 참여 챌린지 제목(최대 3개)이다.
        @return 배너에 표시할 서술형 요약이다.
     */
    private String buildBannerSummary(
            List<TagToUser> tagToUsers,
            List<String> similarParticipatedTitles
    ) {
        String tagPart = formatInterestTagsForSummary(tagToUsers);
        StringBuilder sb = new StringBuilder();
        sb.append("사용자의 관심사 태그는 ").append(tagPart);

        if (similarParticipatedTitles != null && !similarParticipatedTitles.isEmpty()) {
            String challengePart = similarParticipatedTitles.stream()
                    .map(title -> "\"" + title + "\"")
                    .collect(Collectors.joining(", "));
            sb.append(" 이며, 지금까지 참여한 챌린지는 ").append(challengePart)
                    .append(" 입니다.");
        } else {
            sb.append(" 입니다.");
        }

        sb.append(" 이러한 정보를 바탕으로 추천할 수 있는 공개 챌린지는 다음과 같습니다.");
        return sb.toString();
    }

    /*
        관심 태그를 "#태그1 과 #태그2" 형태로 만든다.
        태그가 없으면 "없음"을 반환한다.

        @param tagToUsers 사용자 관심 태그 목록이다.
        @return 배너 문장에 넣을 태그 구문이다.
     */
    private String formatInterestTagsForSummary(List<TagToUser> tagToUsers) {
        if (tagToUsers == null || tagToUsers.isEmpty()) {
            return "없음";
        }

        List<String> tagNames = tagToUsers.stream()
                .map(ttu -> ttu.getTag().getTagName())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
        if (tagNames.isEmpty()) {
            return "없음";
        }

        List<String> hashed = tagNames.stream()
                .map(name -> name.startsWith("#") ? name : "#" + name)
                .toList();
        return String.join(" 과 ", hashed);
    }

    /*
        추천 카드·추천 이유와 비슷한 실제 참여 챌린지 제목을 고른다.
        유사도 기준:
        1) 추천 이유 본문에 참여 챌린지 제목이 직접 언급되면 가산
        2) 추천 챌린지와 태그 이름이 겹치면 가산
        3) 추천 챌린지 제목/추천 이유와 단어가 겹치면 가산
        점수가 0인 항목은 제외하고, 높은 점수 순으로 최대 3개만 반환한다.

        @param participants 사용자 참여 이력이다.
        @param cardChallenges 이번에 추천된 챌린지 카드 목록이다.
        @param reasonByChallengeId 추천 챌린지 id → 추천 이유 맵이다.
        @return 배너에 넣을 참여 챌린지 제목 목록(최대 3개, 없으면 빈 목록)이다.
     */
    private List<String> selectSimilarParticipatedTitles(
            List<ChallengeParticipant> participants,
            List<Challenge> cardChallenges,
            Map<Long, String> reasonByChallengeId
    ) {
        if (participants == null || participants.isEmpty() || cardChallenges == null
                || cardChallenges.isEmpty()) {
            return List.of();
        }

        String combinedReasons = reasonByChallengeId == null
                ? ""
                : String.join(" ", reasonByChallengeId.values()).toLowerCase(Locale.ROOT);

        Set<String> recommendedTagNames = new HashSet<>();
        Set<String> recommendedTitleTokens = new HashSet<>();
        for (Challenge recommended : cardChallenges) {
            if (recommended.getTagMappings() != null) {
                for (TagToChallenge tm : recommended.getTagMappings()) {
                    if (tm.getTag() != null && tm.getTag().getTagName() != null) {
                        recommendedTagNames.add(tm.getTag().getTagName().toLowerCase(Locale.ROOT));
                    }
                }
            }
            recommendedTitleTokens.addAll(tokenize(recommended.getTitle()));
        }
        recommendedTitleTokens.addAll(tokenize(combinedReasons));

        Map<Long, List<String>> participatedTagsByChallengeId =
                buildParticipatedTagNamesByChallengeId(participants);

        record ScoredTitle(String title, int score, LocalDate joinDate, boolean inProgress) {
        }

        Map<String, ScoredTitle> bestByTitle = new HashMap<>();
        for (ChallengeParticipant cp : participants) {
            Challenge participated = cp.getChallenge();
            if (participated == null || participated.getTitle() == null
                    || participated.getTitle().isBlank()) {
                continue;
            }
            String title = participated.getTitle().trim();
            int score = 0;

            // 1) 추천 이유에 참여 제목이 직접 언급되면 강하게 가산한다.
            if (!combinedReasons.isBlank()
                    && combinedReasons.contains(title.toLowerCase(Locale.ROOT))) {
                score += 100;
            }

            // 2) 추천 챌린지와 태그 겹침 개수를 가산한다.
            List<String> participatedTags = participatedTagsByChallengeId
                    .getOrDefault(participated.getId(), List.of());
            for (String tagName : participatedTags) {
                if (recommendedTagNames.contains(tagName.toLowerCase(Locale.ROOT))) {
                    score += 10;
                }
            }

            // 3) 제목/추천 이유 토큰 겹침을 가산한다.
            Set<String> participatedTokens = tokenize(title);
            for (String token : participatedTokens) {
                if (recommendedTitleTokens.contains(token)) {
                    score += 3;
                }
            }

            if (score <= 0) {
                continue;
            }

            boolean inProgress = cp.getStatus() == Status.IN_PROGRESS;
            LocalDate joinDate = cp.getJoinDate();
            ScoredTitle current = new ScoredTitle(title, score, joinDate, inProgress);
            ScoredTitle previous = bestByTitle.get(title);
            if (previous == null
                    || current.score() > previous.score()
                    || (current.score() == previous.score() && current.inProgress() && !previous.inProgress())
                    || (current.score() == previous.score()
                    && current.inProgress() == previous.inProgress()
                    && current.joinDate() != null
                    && (previous.joinDate() == null || current.joinDate().isAfter(previous.joinDate())))) {
                bestByTitle.put(title, current);
            }
        }

        return bestByTitle.values().stream()
                .sorted(Comparator
                        .comparingInt(ScoredTitle::score).reversed()
                        .thenComparing((ScoredTitle s) -> s.inProgress() ? 0 : 1)
                        .thenComparing(
                                ScoredTitle::joinDate,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(MAX_PARTICIPATED_TITLES_IN_SUMMARY)
                .map(ScoredTitle::title)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /*
        참여 이력 챌린지들의 태그 이름을 챌린지 id 기준으로 모은다.

        @param participants 사용자 참여 이력이다.
        @return 챌린지 id → 태그 이름 목록 맵이다.
     */
    private Map<Long, List<String>> buildParticipatedTagNamesByChallengeId(
            List<ChallengeParticipant> participants
    ) {
        List<Long> challengeIds = participants.stream()
                .map(cp -> cp.getChallenge() == null ? null : cp.getChallenge().getId())
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (challengeIds.isEmpty()) {
            return Map.of();
        }

        List<TagToChallenge> mappings =
                tagToChallengeRepository.findWithTagByChallengeIdsByOnce(challengeIds);
        Map<Long, List<String>> tagNamesByChallengeId = new HashMap<>();
        for (TagToChallenge mapping : mappings) {
            Long challengeId = mapping.getChallenge().getId();
            tagNamesByChallengeId
                    .computeIfAbsent(challengeId, ignored -> new ArrayList<>())
                    .add(mapping.getTag().getTagName());
        }
        return tagNamesByChallengeId;
    }

    /*
        제목·추천 이유에서 유사도 비교용 토큰을 뽑는다.
        한글/영문/숫자만 남기고, 너무 짧은 토큰은 제외한다.

        @param text 원문이다.
        @return 소문자 토큰 집합이다.
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        String[] parts = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^0-9a-zA-Z가-힣\\s]", " ")
                .split("\\s+");
        Set<String> tokens = new LinkedHashSet<>();
        for (String part : parts) {
            if (part != null && part.length() >= 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    /*
        각 챌린지 제목 구간에서 추천 이유 문장을 잘라 낸다.
        해당 제목 끝 ~ 다음 제목 시작(또는 본문 끝)까지를 이유 텍스트로 본다.

        @param prose AI 답변 본문이다.
        @param cardChallenges 카드로 내려줄 챌린지 목록이다.
        @return 챌린지 id → 추천 이유 문자열 맵이다.
     */
    private Map<Long, String> extractRecommendReasons(String prose, List<Challenge> cardChallenges) {
        if (prose == null || prose.isBlank() || cardChallenges.isEmpty()) {
            return Map.of();
        }

        record IndexedTitle(int indexInAnswer, Long challengeId, String title) {
        }

        List<IndexedTitle> indexed = new ArrayList<>();
        for (Challenge challenge : cardChallenges) {
            String title = challenge.getTitle();
            if (title == null || title.isBlank()) {
                continue;
            }
            int idx = prose.indexOf(title);
            if (idx >= 0) {
                indexed.add(new IndexedTitle(idx, challenge.getId(), title));
            }
        }
        indexed.sort(Comparator.comparingInt(IndexedTitle::indexInAnswer));

        Map<Long, String> reasons = new java.util.HashMap<>();
        for (int i = 0; i < indexed.size(); i++) {
            IndexedTitle current = indexed.get(i);
            int reasonStart = current.indexInAnswer() + current.title().length();
            int reasonEnd = (i + 1 < indexed.size())
                    ? indexed.get(i + 1).indexInAnswer()
                    : prose.length();

            if (reasonStart >= reasonEnd) {
                reasons.put(current.challengeId(), "");
                continue;
            }

            String rawReason = prose.substring(reasonStart, reasonEnd).trim();
            reasons.put(current.challengeId(), cleanRecommendReason(rawReason));
        }
        return reasons;
    }

    /*
        제목 뒤에 붙은 번호·기호·불필요한 공백을 정리한다.

        @param rawReason 제목 직후부터 자른 원문이다.
        @return UI에 보여주기 좋게 다듬은 추천 이유이다.
     */
    private String cleanRecommendReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return "";
        }

        String cleaned = rawReason.trim();
        // 앞쪽의 번호/구두점/줄바꿈 잔여물을 가볍게 제거한다.
        cleaned = cleaned.replaceFirst("^[\\s\\-–—:：)\\].0-9]+", "");
        cleaned = cleaned.replaceFirst("(?i)^(추천\\s*이유)\\s*[:：]?\\s*", "");
        return cleaned.trim();
    }

    /*
        엔티티를 탐색 탭 카드 DTO로 변환한다.

        @param challenge 태그 매핑이 포함된 공개 챌린지이다.
        @return 카드 UI용 SearchChallengeResponse 이다.
     */
    private SearchChallengeResponse toResponse(Challenge challenge) {
        List<TagResponse> tags = List.of();
        if (challenge.getTagMappings() != null) {
            Map<Long, TagResponse> tagMap = challenge.getTagMappings().stream()
                    .collect(Collectors.toMap(
                            tm -> tm.getTag().getId(),
                            tm -> new TagResponse(
                                    tm.getTag().getId(),
                                    tm.getTag().getTagName(),
                                    tm.getTag().getTagCategory()
                            ),
                            (left, right) -> left
                    ));
            tags = new ArrayList<>(tagMap.values());
        }

        return new SearchChallengeResponse(challenge, tags);
    }
}
