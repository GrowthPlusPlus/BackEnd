// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.challenge.dto.basic.SearchChallengeResponse;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
    AiChallengeRecommendService.callRecommendRagWithOutcome 과 동일한 RAG 1회만 쓴다.
    카드 데이터는 가짜로 만들지 않는다.
    (1) 본문에 나온 제목은 반드시 DB에서 꺼낸 후보(publicChallengesInContext)와만 대조한다.
    (2) 카드 필드는 오늘 기준 진행 중 공개 챌린지를 DB에서 다시 조회한 엔티티로만 채운다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChallengeDiscoveryRecommendService {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final AiChallengeRecommendService aiChallengeRecommendService;
    private final UserService userService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryService challengeQueryService;

    /*
        로그인 이메일로 recommend와 동일한 AI 추천을 받은 뒤, 카드 목록 형태로 반환한다.

        @param email 인증된 사용자 이메일이다.
        @return 답변에 제목이 언급된 순서의 추천 카드. 매칭 실패 시 빈 목록이다.
     */
    @Transactional(readOnly = true)
    public List<SearchChallengeResponse> recommendForDiscovery(String email) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now(SERVICE_ZONE);

        AiChallengeRecommendService.ChallengeRecommendRagResult ragOutcome = aiChallengeRecommendService
                .callRecommendRagWithOutcome(user);
        String prose = stripReferenceDocumentBlock(ragOutcome.fullAnswer());
        /*
            제목 매칭 후보는 전부 DB에서 이미 읽어온 Challenge 엔티티이다.
            (AiChallengeRecommendService가 RAG 문서를 만들 때 같은 쿼리로 채운 목록과 동일하다.)
            답변에 아무 이름이나 썼다고 해서 목록에 없는 챌린지는 picked에 들어가지 않는다.
         */
        List<Challenge> picked = pickChallengesMentionedInProse(prose, ragOutcome.publicChallengesInContext());

        Set<Long> participatingIds = challengeQueryService.findWithChallengeByUserId(user.getId()).stream()
                .map(ChallengeParticipant::getChallenge)
                .map(Challenge::getId)
                .collect(Collectors.toSet());

        /*
            오늘(서울) 기준 진행 중인 공개 챌린지만 다시 DB에서 조회한다.
            잔여일·참여인원·태그 등 카드 필드는 이 맵에 들어 있는 엔티티로만 만든다.
            답변에 이름이 나왔어도 이미 종료·비공개가 되면 full이 null이 되어 스킵된다.
         */
        Map<Long, Challenge> publicWithTags = challengeRepository.findPublicChallengesWithTagMappings(today).stream()
                .collect(Collectors.toMap(Challenge::getId, c -> c, (a, b) -> a));

        List<SearchChallengeResponse> result = new ArrayList<>();
        for (Challenge brief : picked) {
            if (participatingIds.contains(brief.getId())) {
                continue;
            }
            Challenge full = publicWithTags.get(brief.getId());
            if (full == null) {
                continue;
            }
            result.add(toResponse(full));
            if (result.size() >= 3) {
                break;
            }
        }

        if (result.isEmpty() && !picked.isEmpty()) {
            log.debug("답변에서 챌린지는 매칭됐으나 공개·태그 조회 또는 참여 필터 후 비었습니다.");
        }

        return result;
    }

    /*
        RagService가 덧붙인 "참고 문서:" 블록을 제거해 본문만 남긴다.
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
        candidates에 있는 챌린지(실제 DB 행에 대응)만 본문과 제목 문자열로 대조한다.
        본문에만 있고 DB 후보에 없는 이름은 여기서 걸러진다.
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
        엔티티를 탐색 탭 응답 DTO로 변환한다.
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
