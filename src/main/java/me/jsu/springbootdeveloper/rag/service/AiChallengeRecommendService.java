// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.tag.repository.TagToUserRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    사용자 정보를 기반으로 AI가 챌린지를 추천해주는 서비스이다.
    공개 챌린지만 추천 대상으로 한다.
 */
@RequiredArgsConstructor
@Service
public class AiChallengeRecommendService {
    private final RagService ragService;
    private final UserService userService;
    private final TagToUserRepository tagToUserRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeQueryService challengeQueryService;

    /*
        사용자 정보를 기반으로 AI가 챌린지를 추천해주는 메서드이다.
        @param email: 사용자 이메일
        @return: AI가 생성한 챌린지 추천 답변
     */
    public QueryResponseDto recommend(User user) {
        //User user = userService.findByEmail(email);

        // AI에게 보낼 질문이다.
        String query = """
                사용자의 관심사 태그와 지금까지 참여한 챌린지 정보를 바탕으로,
                공개 챌린지 중에서 사용자에게 적합한 챌린지를 추천해줘.
                추천 이유도 함께 설명해줘.
                추천하는 챌린지는 최대 3개까지야.
                """;

        // AI에게 보낼 사용자 정보 기반 문서를 생성한다.
        DocumentSearchResultDto userInfoDoc = buildUserInfoDocument(user.getId());
        List<DocumentSearchResultDto> contexts = List.of(userInfoDoc);

        String answer = ragService.generateAnswerWithContexts(query, contexts);  // 답변을 생성한다.

        return new QueryResponseDto(query, answer);  // 생성한 답변을 반환한다.
    }

    /*
        사용자 정보(관심사 태그, 참여한 챌린지)를 기반으로 RAG에 전달할 문서 DTO를 생성한다.
     */
    private DocumentSearchResultDto buildUserInfoDocument(Long userId) {
        // 사용자의 관심사 태그 목록을 가져온다.
        List<TagToUser> tagToUsers = tagToUserRepository.findWithTagByUserId(userId);

        // 사용자가 참여한 챌린지 목록을 가져온다.
        List<ChallengeParticipant> challengeParticipants = challengeQueryService.findWithChallengeByUserId(userId);

        // 문서의 내용을 생성한다.
        String content = buildContent(tagToUsers, challengeParticipants);

        return new DocumentSearchResultDto(
                "user-info-" + userId,  // 문서 ID
                content,  // 실제 LLM에 들어갈 텍스트
                buildMetadata(tagToUsers, challengeParticipants),  // 참고용 메타데이터
                1.0  // score는 고정
        );
    }

    /*
        AI에게 보낼 content를 생성한다.
        사용자의 관심사 태그와 참여한 챌린지 정보를 포함한다.
     */
    private String buildContent(List<TagToUser> tagToUsers, List<ChallengeParticipant> challengeParticipants) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자 정보 요약\n");
        sb.append("==========\n\n");

        // 관심사 태그 정보를 추가한다.
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

        // 참여한 챌린지 정보를 추가한다.
        sb.append("지금까지 참여한 챌린지 목록:\n");
        if (challengeParticipants.isEmpty()) { //
            sb.append("- 참여한 챌린지가 없습니다.\n");
        } else {
            // 챌린지 이름만 추출하여 중복 제거한다.
            List<String> challengeTitles = challengeParticipants.stream()
                    .map(cp -> cp.getChallenge().getTitle())
                    .distinct()
                    .collect(Collectors.toList());

            for (String title : challengeTitles) {
                sb.append("- ").append(title).append("\n");
            }
        }
        sb.append("\n");

        // 공개 챌린지 정보를 추가한다.
        sb.append("추천 가능한 공개 챌린지 정보:\n");
        LocalDate today = LocalDate.now();
        List<Challenge> publicChallenges = challengeRepository.findPublicChallenges(today);

        if (publicChallenges.isEmpty()) {
            sb.append("- 현재 진행 중인 공개 챌린지가 없습니다.\n");
        } else {
            // 공개 챌린지 목록을 제한하여 추가한다. (너무 많으면 컨텍스트가 길어지므로)
            int maxChallenges = Math.min(20, publicChallenges.size());
            for (int i = 0; i < maxChallenges; i++) {
                Challenge challenge = publicChallenges.get(i);
                sb.append("- ").append(challenge.getTitle())
                        .append(" (설명: ").append(challenge.getContent() != null ? challenge.getContent() : "설명 없음")
                        .append(")\n");
            }
            if (publicChallenges.size() > maxChallenges) {
                sb.append("... 외 ").append(publicChallenges.size() - maxChallenges)
                        .append("개의 공개 챌린지가 더 있습니다.\n");
            }
        }

        return sb.toString();
    }

    /*
        AI에게 보낼 메타데이터를 생성한다.
     */
    private Map<String, Object> buildMetadata(List<TagToUser> tagToUsers, List<ChallengeParticipant> challengeParticipants) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("type", "user_challenge_recommendation");

        // 관심사 태그 정보를 메타데이터에 추가한다.
        List<String> tagNames = tagToUsers.stream()
                .map(ttu -> ttu.getTag().getTagName())
                .collect(Collectors.toList());
        meta.put("interestTags", tagNames);
        meta.put("interestTagCount", tagNames.size());

        // 참여한 챌린지 정보를 메타데이터에 추가한다.
        List<String> challengeTitles = challengeParticipants.stream()
                .map(cp -> cp.getChallenge().getTitle())
                .distinct()
                .collect(Collectors.toList());
        meta.put("participatedChallenges", challengeTitles);
        meta.put("participatedChallengeCount", challengeTitles.size());

        return meta;
    }
}
