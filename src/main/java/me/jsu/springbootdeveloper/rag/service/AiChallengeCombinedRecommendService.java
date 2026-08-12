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
import me.jsu.springbootdeveloper.tag.domain.Tag;
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
import java.util.concurrent.ThreadLocalRandom;
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

            // 추천 이유 표시용: 태그 → #태그, 제목/태그 따옴표·역슬래시 제거
            Set<String> knownTagNames = collectKnownTagNames(tagToUsers, cardChallenges);
            Set<String> knownChallengeTitles = collectKnownChallengeTitles(
                    cardChallenges,
                    participants,
                    similarParticipatedTitles
            );

            /*
                추천 챌린지는 최대 3개(cardChallenges)이다.
                for 루프 1회 = 추천 카드 1장 → imageUrl도 카드마다 1개씩 붙인다.
             */
            List<AiChallengeCombinedRecommendItemResponse> items = new ArrayList<>();
            for (Challenge challenge : cardChallenges) {
                String reason = formatRecommendReasonForDisplay(
                        reasonByChallengeId.getOrDefault(challenge.getId(), ""),
                        knownTagNames,
                        knownChallengeTitles
                );
                // 이 챌린지 태그(보통 2개) 중 1개를 랜덤으로 골라 imageUrl을 정한다.
                String imageUrl = pickRandomTagImageUrl(challenge);

                items.add(new AiChallengeCombinedRecommendItemResponse(
                        toResponse(challenge),
                        challenge.getContent(),
                        reason,
                        imageUrl
                ));
            }

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
        사용자의 관심사 태그는 #요가 와 #운동 이며, 지금까지 참여한 챌린지는 매일 30분 러닝 입니다.
        이러한 정보를 바탕으로 추천할 수 있는 공개 챌린지는 다음과 같습니다.

        예2) 참여 이력이 없거나 비슷한 참여 챌린지가 없을 때
        사용자의 관심사 태그는 #요가 와 #운동 입니다.
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
            /*
                ASCII 큰따옴표(")는 JSON 직렬화 시 \" 로 보여 혼란을 주므로 쓰지 않는다.
                제목만 쉼표로 이어 붙인다.
             */
            String challengePart = String.join(", ", similarParticipatedTitles);
            sb.append(" 이며, 지금까지 참여한 챌린지는 ").append(challengePart)
                    .append(" 입니다.");
        } else {
            sb.append(" 입니다.");
        }

        sb.append(" 이러한 정보를 바탕으로 추천할 수 있는 공개 챌린지는 다음과 같습니다.");
        return sb.toString();
    }

    /*
        관심 태그를 "#태그1 와/과 #태그2" 형태로 만든다.
        앞 태그의 마지막 글자 받침 유무에 따라 연결 조사를 '과'(받침 있음) 또는
        '와'(받침 없음)로 구분한다. 태그가 없으면 "없음"을 반환한다.

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
        return joinHashedTagsWithWaOrGwa(hashed);
    }

    /*
        #태그 목록을 받침에 맞는 '와' / '과'로 이어 붙인다.
        예: #요가 와 #운동, #과제 와 #러닝

        @param hashedTags # 접두가 붙은 태그 문자열 목록이다.
        @return 배너 문장에 넣을 태그 구문이다.
     */
    private String joinHashedTagsWithWaOrGwa(List<String> hashedTags) {
        if (hashedTags == null || hashedTags.isEmpty()) {
            return "";
        }
        if (hashedTags.size() == 1) {
            return hashedTags.get(0);
        }

        StringBuilder joined = new StringBuilder(hashedTags.get(0));
        for (int i = 1; i < hashedTags.size(); i++) {
            String previousTag = hashedTags.get(i - 1);
            joined.append(' ')
                    .append(selectWaOrGwaParticle(previousTag))
                    .append(' ')
                    .append(hashedTags.get(i));
        }
        return joined.toString();
    }

    /*
        태그 문자열의 마지막 글자 받침 유무로 연결 조사 '와' / '과'를 고른다.
        한글은 유니코드 음절 블록으로 받침을 판별하고, 그 외 문자는
        마지막 글자가 모음이면 '와', 자음(또는 숫자)이면 '과'를 쓴다.

        @param hashedTag # 접두가 있을 수 있는 태그 문자열이다.
        @return 받침 없으면 "와", 있으면 "과"이다.
     */
    private String selectWaOrGwaParticle(String hashedTag) {
        if (hashedTag == null || hashedTag.isBlank()) {
            return "과";
        }

        String tagText = hashedTag.startsWith("#")
                ? hashedTag.substring(1)
                : hashedTag;
        if (tagText.isBlank()) {
            return "과";
        }

        char lastChar = tagText.charAt(tagText.length() - 1);
        if (lastChar >= '가' && lastChar <= '힣') {
            // (음절 - '가') % 28 == 0 이면 받침 없음, 0이 아니면 받침 있음이다.
            boolean hasBatchim = (lastChar - '가') % 28 != 0;
            return hasBatchim ? "과" : "와";
        }

        if (Character.isLetter(lastChar)) {
            char lower = Character.toLowerCase(lastChar);
            if ("aeiou".indexOf(lower) >= 0) {
                return "와";
            }
            return "과";
        }

        return "과";
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
        각 추천 챌린지마다 추천 이유 1개만 정확히 잘라 낸다.

        처리 순서:
        1) id 또는 제목으로 해당 챌린지 구간 시작 위치를 찾는다.
        2) 다음 챌린지 구간 직전까지를 한 섹션으로 자른다.
        3) 섹션 안에서 "추천 이유" 라벨 뒤 문장만 우선 사용한다.
        4) 라벨이 없으면 제목/id 줄을 제외한 본문 문장을 사용한다.

        @param prose AI 답변 본문이다.
        @param cardChallenges 카드로 내려줄 챌린지 목록이다.
        @return 챌린지 id → 추천 이유 문자열 맵이다.
     */
    private Map<Long, String> extractRecommendReasons(String prose, List<Challenge> cardChallenges) {
        if (prose == null || prose.isBlank() || cardChallenges.isEmpty()) {
            return Map.of();
        }

        record IndexedChallenge(int indexInAnswer, Challenge challenge) {
        }

        List<IndexedChallenge> indexed = new ArrayList<>();
        for (Challenge challenge : cardChallenges) {
            int idx = findChallengeSectionStart(prose, challenge);
            if (idx >= 0) {
                indexed.add(new IndexedChallenge(idx, challenge));
            }
        }
        indexed.sort(Comparator.comparingInt(IndexedChallenge::indexInAnswer));

        Map<Long, String> reasons = new HashMap<>();
        for (int i = 0; i < indexed.size(); i++) {
            IndexedChallenge current = indexed.get(i);
            int sectionStart = current.indexInAnswer();
            int sectionEnd = (i + 1 < indexed.size())
                    ? indexed.get(i + 1).indexInAnswer()
                    : prose.length();
            if (sectionStart >= sectionEnd) {
                reasons.put(current.challenge().getId(), "");
                continue;
            }

            String section = prose.substring(sectionStart, sectionEnd);
            String reason = extractSingleRecommendReason(section, current.challenge());
            reasons.put(current.challenge().getId(), reason);
        }

        // 본문에서 위치를 못 찾은 카드는 빈 이유로 채운다.
        for (Challenge challenge : cardChallenges) {
            reasons.putIfAbsent(challenge.getId(), "");
        }
        return reasons;
    }

    /*
        답변 본문에서 해당 챌린지 구간의 시작 인덱스를 찾는다.
        id 표기(id: 131)를 우선하고, 없으면 제목 문자열을 사용한다.

        @param prose AI 답변 본문이다.
        @param challenge 찾을 챌린지이다.
        @return 시작 인덱스. 없으면 -1이다.
     */
    private int findChallengeSectionStart(String prose, Challenge challenge) {
        if (challenge.getId() != null) {
            String idMarker = "id: " + challenge.getId();
            int idIdx = indexOfIgnoreCase(prose, idMarker);
            if (idIdx >= 0) {
                return idIdx;
            }
        }

        String title = challenge.getTitle();
        if (title == null || title.isBlank()) {
            return -1;
        }
        return prose.indexOf(title);
    }

    /*
        한 챌린지 섹션에서 추천 이유 문장 하나만 뽑는다.

        @param section 해당 챌린지 구간 원문이다.
        @param challenge 대상 챌린지이다.
        @return 정리된 추천 이유 1개이다.
     */
    private String extractSingleRecommendReason(String section, Challenge challenge) {
        if (section == null || section.isBlank()) {
            return "";
        }

        String normalized = section.replace("\r\n", "\n");

        // 1순위: "추천 이유" 라벨 뒤 텍스트
        int reasonLabelIdx = indexOfIgnoreCase(normalized, "추천 이유");
        if (reasonLabelIdx >= 0) {
            String afterLabel = normalized.substring(reasonLabelIdx + "추천 이유".length());
            afterLabel = afterLabel.replaceFirst("^\\s*[:：\\-–—]?\\s*", "");
            return cleanRecommendReason(takeFirstReasonParagraph(afterLabel));
        }

        // 2순위: 제목/id 헤더 줄을 제외한 나머지 본문
        String withoutHeader = stripChallengeHeaderLine(normalized, challenge);
        return cleanRecommendReason(takeFirstReasonParagraph(withoutHeader));
    }

    /*
        섹션 맨 앞의 id·제목 헤더 줄을 제거한다.

        @param section 챌린지 구간 원문이다.
        @param challenge 대상 챌린지이다.
        @return 헤더를 뺀 본문이다.
     */
    private String stripChallengeHeaderLine(String section, Challenge challenge) {
        String[] lines = section.split("\n", 2);
        if (lines.length == 0) {
            return "";
        }

        String firstLine = lines[0];
        boolean looksLikeHeader = false;
        if (challenge.getId() != null
                && indexOfIgnoreCase(firstLine, "id: " + challenge.getId()) >= 0) {
            looksLikeHeader = true;
        }
        if (challenge.getTitle() != null
                && !challenge.getTitle().isBlank()
                && firstLine.contains(challenge.getTitle())) {
            looksLikeHeader = true;
        }

        if (looksLikeHeader && lines.length == 2) {
            return lines[1];
        }
        if (looksLikeHeader) {
            return "";
        }
        return section;
    }

    /*
        추천 이유 본문에서 첫 의미 있는 문단(또는 문장)만 남긴다.
        다음 번호 목록(2. / 3.)이 나오면 그 앞에서 끊는다.

        @param text 추천 이유 후보 원문이다.
        @return 한 챌린지용 이유 초안이다.
     */
    private String takeFirstReasonParagraph(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String trimmed = text.trim();
        // 다음 추천 항목 번호(예: "\n2." / "\n3.") 앞에서 끊는다.
        java.util.regex.Matcher nextItem = java.util.regex.Pattern
                .compile("(?m)^\\s*\\d+\\s*[.)]")
                .matcher(trimmed);
        if (nextItem.find() && nextItem.start() > 0) {
            trimmed = trimmed.substring(0, nextItem.start());
        }

        // 빈 줄 기준으로 첫 문단만 사용한다.
        int blankLine = trimmed.indexOf("\n\n");
        if (blankLine >= 0) {
            trimmed = trimmed.substring(0, blankLine);
        }
        return trimmed.trim();
    }

    /*
        마크다운·번호·id/제목 잔여물을 제거하고 추천 이유 문장만 남긴다.

        @param rawReason 제목 직후 또는 라벨 뒤에서 자른 원문이다.
        @return UI에 보여주기 좋게 다듬은 추천 이유이다.
     */
    private String cleanRecommendReason(String rawReason) {
        if (rawReason == null || rawReason.isBlank()) {
            return "";
        }

        String cleaned = rawReason.trim();
        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*[-*•]\\s*", "");
        cleaned = cleaned.replaceFirst("(?i)^\\s*추천\\s*이유\\s*[:：\\-–—]?\\s*", "");
        // id / 제목 헤더 잔여물 제거
        cleaned = cleaned.replaceAll("(?i)id\\s*:\\s*\\d+\\s*,?\\s*", "");
        cleaned = cleaned.replaceAll("(?i)제목\\s*:\\s*", "");
        cleaned = cleaned.replaceFirst("^[\\s\\-–—:：)\\].0-9*]+", "");
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        cleaned = cleaned.replaceAll(" *\\n+ *", " ");
        return cleaned.trim();
    }

    /*
        추천 이유 표시용으로 따옴표·역슬래시를 정리한다.
        - 알려진 태그: "스터디" / \"스터디\" → #스터디
        - 알려진 챌린지 제목: "코테d-100" → 코테d-100 (따옴표만 제거)
        - 그 외 남은 ASCII 따옴표도 제거한다.

        @param reason 정리 전 추천 이유이다.
        @param knownTagNames 관심/추천 챌린지 태그 이름 집합이다.
        @param knownChallengeTitles 추천·참여 챌린지 제목 집합이다.
        @return UI에 바로 쓸 추천 이유이다.
     */
    private String formatRecommendReasonForDisplay(
            String reason,
            Set<String> knownTagNames,
            Set<String> knownChallengeTitles
    ) {
        if (reason == null || reason.isBlank()) {
            return "";
        }

        String formatted = reason;
        // JSON/원문에 섞인 이스케이프 따옴표를 일반 따옴표로 통일한다.
        formatted = formatted.replace("\\\"", "\"");

        // 긴 제목부터 치환해 부분 문자열 충돌을 줄인다.
        List<String> titlesLongestFirst = knownChallengeTitles.stream()
                .filter(title -> title != null && !title.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String title : titlesLongestFirst) {
            formatted = replaceQuotedOrPlainKeepUnquoted(formatted, title);
        }

        List<String> tagsLongestFirst = knownTagNames.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String tagName : tagsLongestFirst) {
            String bareTag = tagName.startsWith("#") ? tagName.substring(1) : tagName;
            if (bareTag.isBlank()) {
                continue;
            }
            formatted = replaceQuotedTagWithHash(formatted, bareTag);
        }

        // 남은 ASCII 큰따옴표는 모두 제거한다. (역슬래시 잔여 포함)
        formatted = formatted.replace("\"", "");
        formatted = formatted.replace("\\", "");
        return formatted.trim();
    }

    /*
        따옴표로 감싼 챌린지 제목을 따옴표 없는 제목으로 바꾼다.
        이미 따옴표 없이 있는 제목은 그대로 둔다.

        @param text 추천 이유 원문이다.
        @param title 챌린지 제목이다.
        @return 제목 따옴표가 제거된 문자열이다.
     */
    private String replaceQuotedOrPlainKeepUnquoted(String text, String title) {
        if (text == null || title == null || title.isBlank()) {
            return text;
        }
        String quoted = "\"" + title + "\"";
        return text.replace(quoted, title);
    }

    /*
        따옴표로 감싼 태그명을 #태그 형태로 바꾼다.
        예: "스터디" → #스터디

        @param text 추천 이유 원문이다.
        @param bareTag # 없는 태그 이름이다.
        @return 태그가 해시 형태로 바뀐 문자열이다.
     */
    private String replaceQuotedTagWithHash(String text, String bareTag) {
        if (text == null || bareTag == null || bareTag.isBlank()) {
            return text;
        }
        String hashed = bareTag.startsWith("#") ? bareTag : "#" + bareTag;
        String quoted = "\"" + bareTag + "\"";
        String quotedWithHash = "\"#" + bareTag + "\"";
        String result = text.replace(quotedWithHash, hashed);
        result = result.replace(quoted, hashed);
        return result;
    }

    /*
        관심 태그 + 추천 카드 태그 이름을 모은다.

        @param tagToUsers 사용자 관심 태그이다.
        @param cardChallenges 추천 카드 챌린지이다.
        @return # 없는 태그 이름 집합이다.
     */
    private Set<String> collectKnownTagNames(
            List<TagToUser> tagToUsers,
            List<Challenge> cardChallenges
    ) {
        Set<String> tags = new HashSet<>();
        if (tagToUsers != null) {
            for (TagToUser ttu : tagToUsers) {
                if (ttu.getTag() != null && ttu.getTag().getTagName() != null) {
                    String name = ttu.getTag().getTagName().trim();
                    if (name.startsWith("#")) {
                        name = name.substring(1);
                    }
                    if (!name.isBlank()) {
                        tags.add(name);
                    }
                }
            }
        }
        if (cardChallenges != null) {
            for (Challenge challenge : cardChallenges) {
                if (challenge.getTagMappings() == null) {
                    continue;
                }
                for (TagToChallenge tm : challenge.getTagMappings()) {
                    if (tm.getTag() == null || tm.getTag().getTagName() == null) {
                        continue;
                    }
                    String name = tm.getTag().getTagName().trim();
                    if (name.startsWith("#")) {
                        name = name.substring(1);
                    }
                    if (!name.isBlank()) {
                        tags.add(name);
                    }
                }
            }
        }
        return tags;
    }

    /*
        추천·참여 챌린지 제목을 모은다.

        @param cardChallenges 추천 카드이다.
        @param participants 참여 이력이다.
        @param similarParticipatedTitles 요약에 쓴 참여 제목이다.
        @return 챌린지 제목 집합이다.
     */
    private Set<String> collectKnownChallengeTitles(
            List<Challenge> cardChallenges,
            List<ChallengeParticipant> participants,
            List<String> similarParticipatedTitles
    ) {
        Set<String> titles = new HashSet<>();
        if (cardChallenges != null) {
            for (Challenge challenge : cardChallenges) {
                if (challenge.getTitle() != null && !challenge.getTitle().isBlank()) {
                    titles.add(challenge.getTitle().trim());
                }
            }
        }
        if (participants != null) {
            for (ChallengeParticipant cp : participants) {
                if (cp.getChallenge() != null
                        && cp.getChallenge().getTitle() != null
                        && !cp.getChallenge().getTitle().isBlank()) {
                    titles.add(cp.getChallenge().getTitle().trim());
                }
            }
        }
        if (similarParticipatedTitles != null) {
            for (String title : similarParticipatedTitles) {
                if (title != null && !title.isBlank()) {
                    titles.add(title.trim());
                }
            }
        }
        return titles;
    }

    /*
        대소문자 무시 부분 문자열 검색이다.

        @param source 검색 대상 문자열이다.
        @param target 찾을 문자열이다.
        @return 시작 인덱스. 없으면 -1이다.
     */
    private int indexOfIgnoreCase(String source, String target) {
        if (source == null || target == null || target.isBlank()) {
            return -1;
        }
        return source.toLowerCase(Locale.ROOT)
                .indexOf(target.toLowerCase(Locale.ROOT));
    }

    /*
        추천 카드 1장에 쓸 imageUrl을 고른다.

        cardChallenges.forEach(...) 안에서 챌린지마다 1번씩 호출된다.
        즉 추천 3개면 imageUrl도 3개(각 카드 1개)가 API로 내려간다.

        동작:
        1) 이 챌린지에 붙은 태그 목록을 꺼낸다 (보통 2개)
        2) 그중 1개를 랜덤으로 고른다
        3) 고른 태그의 tag.image_url 을 그대로 반환한다

        @param challenge 태그가 join fetch 된 추천 챌린지 1개이다.
        @return 랜덤 선택된 태그의 imageUrl. 태그/URL이 없으면 null이다.
     */
    private String pickRandomTagImageUrl(Challenge challenge) {
        if (challenge.getTagMappings() == null || challenge.getTagMappings().isEmpty()) {
            return null;
        }

        // tagMappings → Tag 엔티티만 추출 (null 태그는 제외)
        List<Tag> tags = new ArrayList<>();
        for (TagToChallenge mapping : challenge.getTagMappings()) {
            if (mapping.getTag() != null) {
                tags.add(mapping.getTag());
            }
        }
        if (tags.isEmpty()) {
            return null;
        }

        // 태그 2개 중 1개를 랜덤 선택 (태그가 1개면 항상 그 1개)
        Tag pickedTag = tags.get(ThreadLocalRandom.current().nextInt(tags.size()));

        String imageUrl = pickedTag.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
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
