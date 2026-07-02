// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.tag.repository.TagToChallengeRepository;
import me.jsu.springbootdeveloper.tag.repository.TagToUserRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
    사용자 정보를 기반으로 AI(RAG)가 챌린지를 추천하는 서비스이다.

    [전체 흐름]
    1) DB에서 사용자 관심 태그·참여 이력·진행 중 공개 챌린지를 읽는다.
    2) 위 정보를 하나의 텍스트 문서(content)로 만든 뒤 LLM에 넘긴다.
    3) LLM 답변 + 후보 챌린지 목록을 반환한다.
       (탐색 탭 API는 답변 속 제목을 후보 목록과 대조해 카드 id를 채운다.)

    [이번 개선의 핵심]
    - 참여 이력: 제목만 → 태그·진행/종료 상태까지 포함
    - 후보 선정: 관심 태그만 → 관심 태그 + 참여했던 챌린지의 태그(preferenceTagNames)
    - 후보 목록: 이미 참여한 챌린지는 LLM 컨텍스트에서 제외
    - DB 호출: 공개 챌린지 1회 조회 후 메모리에서 필터·정렬
 */
@RequiredArgsConstructor
@Service
public class AiChallengeRecommendService {

    // 챌린지 진행 여부·종료일 비교에 쓰는 기준 시간대이다.
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    /*
        LLM에 넣을 "참여 이력" 최대 개수이다.
        참여 챌린지가 수십 개여도 12개만 넣어 프롬프트 길이를 막는다.
     */
    private static final int MAX_PARTICIPATED_CHALLENGES_IN_CONTEXT = 12;

    /*
        LLM에게 보내는 고정 질문(프롬프트)이다.
        텍스트 추천 API(recommend)와 탐색 카드 API(discovery)가 동일 문자열을 쓴다.

        변경 요지:
        - "목록에 있는 챌린지만 추천" → id 0·없는 챌린지 환각 방지
        - "이미 참여·종료 챌린지 추천 금지" → 잘못된 추천 감소
        - "id·제목을 목록과 동일하게" → 앱의 제목/id 매칭 실패 방지
     */
    public static final String CHALLENGE_RECOMMEND_USER_QUERY = """
            사용자의 관심사 태그와 지금까지 참여한 챌린지(제목·태그)를 바탕으로,
            아래 "추천 가능한 공개 챌린지 정보" 목록에 있는 챌린지만 골라 추천해줘.
            목록에 없는 챌린지, 이미 참여한 챌린지, 종료된 챌린지는 추천하지 마.
            (목록은 오늘 기준 진행 중인 공개 챌린지만 포함한다.)
            추천 이유도 함께 설명해줘.
            추천하는 챌린지는 최대 3개까지야.
            각 추천 챌린지를 말할 때는 반드시 목록에 적힌 id와 제목을 그대로 써줘.
            id나 제목을 임의로 바꾸거나 새로 만들지 마. (앱에서 제목·id로 매칭한다.)
            """;

    private final RagService ragService;
    private final TagToUserRepository tagToUserRepository;
    // 참여했던 챌린지의 태그를 challengeId 목록으로 한 번에 조회할 때 사용한다.
    private final TagToChallengeRepository tagToChallengeRepository;
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
        RAG에 넣을 "사용자 맞춤 문서" 1건과, LLM 후보로 쓸 공개 챌린지 목록을 만든다.
        이 메서드가 추천 품질·성능 개선의 중심이다.

        @param userId 추천 대상 사용자 PK
        @return document(LLM 컨텍스트 문자열), publicSlice(미참여·태그 정렬된 전체 후보)
     */
    private RecommendContextBundle buildRecommendContextBundle(Long userId) {
        // ----- 1단계: DB에서 사용자 원천 데이터 조회 -----
        List<TagToUser> tagToUsers = tagToUserRepository.findWithTagByUserId(userId);
        List<ChallengeParticipant> challengeParticipants = challengeQueryService.findWithChallengeByUserId(userId);

        Set<Long> participatingChallengeIds = challengeParticipants.stream()
                .map(cp -> cp.getChallenge().getId())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now(SERVICE_ZONE);

        // ----- 2단계: 참여 이력을 LLM용으로 압축 (최대 12건 + 태그 일괄 조회) -----
        List<ChallengeParticipant> participantsForContext = selectParticipantsForContext(
                challengeParticipants,
                MAX_PARTICIPATED_CHALLENGES_IN_CONTEXT
        );
        Map<Long, List<String>> participatedTagNamesByChallengeId = buildParticipatedTagNamesByChallengeId(
                participantsForContext
        );

        // 선호 태그 = 관심사 + 참여 챌린지 태그 (후보 정렬에만 사용)
        Set<String> preferenceTagNames = tagToUsers.stream()
                .map(ttu -> ttu.getTag().getTagName())
                .collect(Collectors.toCollection(HashSet::new));
        for (List<String> tagNames : participatedTagNamesByChallengeId.values()) {
            preferenceTagNames.addAll(tagNames);
        }

        // ----- 3단계: 진행 중 공개 챌린지 1회 조회 후 메모리에서 필터·정렬 -----
        List<Challenge> allPublicWithTags = challengeRepository.findPublicChallengesWithTagMappings(today);
        List<Challenge> publicSlice = slicePublicChallengesForContext(
                allPublicWithTags,
                preferenceTagNames,
                participatingChallengeIds
        );

        // ----- 4단계: LLM이 읽을 본문(content) 조립 -----
        String content = buildContent(
                tagToUsers,
                participantsForContext,
                participatedTagNamesByChallengeId,
                publicSlice,
                today
        );

        /*
            metadata는 RagService가 LLM 프롬프트에 넣지 않는다. (content만 사용)
            참고 문서 푸터는 originalFilename 키만 쓰므로 비워 둔다.
         */
        DocumentSearchResultDto document = new DocumentSearchResultDto(
                "user-info-" + userId,
                content,
                Map.of(),
                1.0
        );

        return new RecommendContextBundle(document, publicSlice);
    }

    /*
        LLM 컨텍스트 본문을 만든다. 구조는 항상 아래 3블록이다.

        1) 관심사 태그 목록
        2) 참여한 챌린지 목록 (태그·상태 포함, 최대 12건)
        3) 추천 가능한 공개 챌린지 (진행 중·미참여 전체)

        @param tagToUsers 사용자 관심 태그 전체
        @param participantsForContext LLM에 넣을 참여 이력(선별된 목록)
        @param participatedTagNamesByChallengeId 챌린지 id → 태그 이름 목록
        @param publicChallenges LLM 후보로 실린 공개 챌린지(미참여·정렬된 전체)
        @param today 서울 기준 오늘(참여 상태 문구·formatParticipationStatus 와 동일 기준일)
        @return LLM에 전달할 평문 문자열
     */
    private String buildContent(
            List<TagToUser> tagToUsers,
            List<ChallengeParticipant> participantsForContext,
            Map<Long, List<String>> participatedTagNamesByChallengeId,
            List<Challenge> publicChallenges,
            LocalDate today
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

        sb.append("지금까지 참여한 챌린지 목록 (최근·진행 중 우선, 태그 포함):\n");
        if (participantsForContext.isEmpty()) {
            sb.append("- 참여한 챌린지가 없습니다.\n");
        } else {
            for (ChallengeParticipant cp : participantsForContext) {
                Challenge ch = cp.getChallenge();
                List<String> tags = participatedTagNamesByChallengeId.getOrDefault(ch.getId(), List.of());
                sb.append("- id: ").append(ch.getId())
                        .append(", 제목: ").append(ch.getTitle())
                        .append(", 상태: ").append(formatParticipationStatus(cp, today))
                        .append(", 태그: ").append(tags.isEmpty() ? "없음" : String.join(", ", tags))
                        .append("\n");
            }
        }
        sb.append("\n");

        sb.append("추천 가능한 공개 챌린지 정보 (오늘 기준 진행 중, 미참여만 포함):\n");
        if (publicChallenges.isEmpty()) {
            sb.append("- 추천할 수 있는 공개 챌린지가 없습니다.\n");
        } else {
            for (Challenge challenge : publicChallenges) {
                sb.append("- id: ").append(challenge.getId())
                        .append(", 제목: ").append(challenge.getTitle())
                        .append(", 기간: ").append(challenge.getStartDate())
                        .append(" ~ ").append(challenge.getEndDate())
                        .append(", 챌린지태그: ").append(formatChallengeTagNames(challenge))
                        .append("\n");
            }
        }

        return sb.toString();
    }

    /*
        참여 이력이 많을 때 LLM에 넣을 대표 샘플만 고른다.

        정렬 우선순위:
        1) status == IN_PROGRESS 인 챌린지를 먼저 (지금 하고 있는 습관이 더 중요)
        2) joinDate 최신순
        3) 같은 챌린지 id는 한 번만 (중복 참여 레코드 방지)

        @param challengeParticipants 사용자의 전체 참여 이력
        @param limit 컨텍스트에 실을 최대 건수 (보통 12)
        @return LLM 본문 "참여한 챌린지" 섹션에 쓸 목록
     */
    private List<ChallengeParticipant> selectParticipantsForContext(
            List<ChallengeParticipant> challengeParticipants,
            int limit
    ) {
        if (challengeParticipants.isEmpty()) {
            return List.of();
        }

        List<ChallengeParticipant> sorted = challengeParticipants.stream()
                .sorted(Comparator
                        .comparingInt((ChallengeParticipant cp) ->
                                cp.getStatus() == Status.IN_PROGRESS ? 0 : 1)
                        .thenComparing(ChallengeParticipant::getJoinDate, Comparator.reverseOrder()))
                .toList();

        List<ChallengeParticipant> selected = new ArrayList<>();
        Set<Long> seenChallengeIds = new HashSet<>();
        for (ChallengeParticipant cp : sorted) {
            Long challengeId = cp.getChallenge().getId();
            if (!seenChallengeIds.add(challengeId)) {
                continue;
            }
            selected.add(cp);
            if (selected.size() >= limit) {
                break;
            }
        }
        return selected;
    }

    /*
        참여 이력에 붙은 태그를 챌린지 id 기준으로 모은다.

        챌린지마다 findWithTagByChallengeId 를 반복 호출하면 N+1이 되므로,
        TagToChallengeRepository.findWithTagByChallengeIdsByOnce 로 한 번에 조회한다.

        @param participantsForContext selectParticipantsForContext 로 고른 참여 목록
        @return Map<챌린지id, 태그이름 리스트>
     */
    private Map<Long, List<String>> buildParticipatedTagNamesByChallengeId(
            List<ChallengeParticipant> participantsForContext
    ) {
        if (participantsForContext.isEmpty()) {
            return Map.of();
        }

        List<Long> challengeIds = participantsForContext.stream()
                .map(cp -> cp.getChallenge().getId())
                .distinct()
                .toList();

        List<TagToChallenge> mappings = tagToChallengeRepository.findWithTagByChallengeIdsByOnce(challengeIds);
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
        LLM에 실을 "추천 후보" 공개 챌린지 목록을 만든다.

        처리 순서:
        1) participatingChallengeIds 에 있으면 제거 (이미 참여 → 추천 불필요)
        2) preferenceTagNames 와 챌린지 태그 겹침 개수로 내림차순 정렬
        3) 겹침이 같으면 startDate 최신순
        4) 정렬된 미참여 후보 전체 반환

        @param allPublicWithTags DB에서 이미 읽은 진행 중 공개 챌린지 전체
        @param preferenceTagNames 관심사+참여 이력 태그 합집합
        @param excludeChallengeIds 제외할 챌린지 id (보통 참여 중인 id 전체)
        @return LLM 컨텍스트·Discovery 제목 매칭에 쓰는 후보 목록
     */
    private List<Challenge> slicePublicChallengesForContext(
            List<Challenge> allPublicWithTags,
            Set<String> preferenceTagNames,
            Set<Long> excludeChallengeIds
    ) {
        //1. 공개 챌린지 중 미참여 챌린지만 다시 모음
        List<Challenge> candidates = allPublicWithTags.stream()
                .filter(c -> !excludeChallengeIds.contains(c.getId())) //excludeChallengeIds에 있는 챌린지는 제외
                .collect(Collectors.toCollection(ArrayList::new));
        //2. 필터링된 미참여 챌린지를 태그 겹침 개수 내림차순으로 정렬
        candidates.sort(Comparator
                .comparingInt((Challenge c) -> countTagOverlapOnChallenge(c, preferenceTagNames))
                    //선호 태그와 겹치는 태그 개수가 많을수록 위 
                .reversed()
                    //원래 낮은 숫자가 위로 가는데 reverseOrder()를 써서 높은 숫자가 위로 가도록 함
                .thenComparing(Challenge::getStartDate, Comparator.reverseOrder()));
                    //겹침이 같으면 시작일이 최근인 챌린지가 위 -> Comparator.reverseOrder()를 써서 최근인 챌린지가 위로 가도록 함
        return candidates;
    }

    /*
        한 챌린지의 태그 중 preferenceTagNames 와 이름이 같은 것이 몇 개인지 센다.
        겹침이 0이면 관심과 무관한 후보로 간주되어 정렬에서 뒤로 밀린다.

        @param challenge 태그 매핑이 fetch 된 공개 챌린지
        @param preferenceTagNames 선호 태그 집합
        @return 겹치는 태그 개수
     */
    private int countTagOverlapOnChallenge(Challenge challenge, Set<String> preferenceTagNames) {
        if (preferenceTagNames.isEmpty() || challenge.getTagMappings() == null) {
            return 0;
        }
        int overlap = 0;
        for (TagToChallenge tm : challenge.getTagMappings()) {
            if (preferenceTagNames.contains(tm.getTag().getTagName())) {
                overlap++;
            }
        }
        return overlap;
    }

    /*
        참여 이력 한 줄에 붙일 "상태" 문구를 만든다.
        LLM이 종료된 챌린지를 다시 추천하지 않도록 힌트를 준다.

        @param participant 참여 레코드 (status, challenge 포함)
        @param today 서울 기준 오늘
        @return 예: "진행중", "종료(참여기록)", "COMPLETED"
     */
    private String formatParticipationStatus(ChallengeParticipant participant, LocalDate today) {
        Challenge challenge = participant.getChallenge();
        if (participant.getStatus() != Status.IN_PROGRESS) {
            return participant.getStatus().name();
        }
        if (challenge.getEndDate().isBefore(today)) {
            return "종료(참여기록)";
        }
        return "진행중";
    }

    /*
        공개 후보 챌린지의 태그 이름을 ", " 로 이어 붙인다.

        @param challenge tagMappings 가 fetch 된 챌린지
        @return 태그 문자열 또는 "없음"
     */
    private String formatChallengeTagNames(Challenge challenge) {
        if (challenge.getTagMappings() == null || challenge.getTagMappings().isEmpty()) {
            return "없음";
        }
        return challenge.getTagMappings().stream()
                .map(tm -> tm.getTag().getTagName())
                .distinct()
                .collect(Collectors.joining(", "));
    }

}
