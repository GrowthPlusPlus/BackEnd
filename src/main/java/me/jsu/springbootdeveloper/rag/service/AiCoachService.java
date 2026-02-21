// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    유저 정보를 기반으로 AI가 어드바이스를 해주는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class AiCoachService {
    private final RagService ragService;
    private final UserService userService;
    private final UserStatisticsService userStatisticsService;
    private final ChallengeQueryService challengeQueryService;

    public QueryResponseDto coach(String email) {
        User user = userService.findByEmail(email);

        // AI에게 보낼 질문이다.
        String query = """
                유저 통계 정보를 기반으로 적절한 조언을 부탁해.
                어떤 점이 잘 했고, 어떤 점은 개선해 나가야 하고 등
                """;

        // AI에게 보낼 유저 통계 기반 가짜 문서를 생성한다.
        DocumentSearchResultDto userStatsDoc = buildUserStatisticsDocument(user.getId());
        List<DocumentSearchResultDto> contexts = List.of(userStatsDoc);

        String answer = ragService.generateAnswerWithContexts(query, contexts);  // 답변을 생성한다.

        return new QueryResponseDto(query, answer);  // 생성한 답변을 반환한다.
    }

    /*
        유저 통계를 기반으로 RAG에 전달할 문서 DTO를 생성한다.
     */
    private DocumentSearchResultDto buildUserStatisticsDocument(Long userId) {
        UserStatistics stats = userStatisticsService.findByUser_IdStatistics(userId);

        // 문서의 내용을 생성한다.
        String content = buildContent(stats);

        return new DocumentSearchResultDto(
                "user-statistics-" + userId,  // 문서 ID
                content,  // 실제 LLM에 들어갈 텍스트
                buildMetadata(stats),  // 참고용 메타데이터
                1.0  // score는 고정
        );
    }

    /*
        AI에게 보낼 content를 생성한다.
     */
    private String buildContent(UserStatistics s) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자 활동 요약 통계\n");
        sb.append("- 총 참여 챌린지 수: ").append(s.getTotalChallengeCount()).append("개\n");
        sb.append("- 완료한 챌린지 수: ").append(s.getCompletedChallengeCount()).append("개\n");
        sb.append("- 실패한 챌린지 수: ").append(s.getFailedChallengeCount()).append("개\n");
        sb.append("- 현재 연속 활동 일수: ").append(s.getCurrentStreakLength()).append("일\n");
        sb.append("- 최대 연속 활동 기록: ").append(s.getMaxStreakLength()).append("일\n");
        sb.append("- 최근 7일 활동 일수: ").append(s.getWeeklyActiveDays()).append("일\n");
        sb.append("- 최근 30일 활동 일수: ").append(s.getMonthlyActiveDays()).append("일\n");

        if (s.getDominantChallengeId() != null) {
            Challenge dominant = challengeQueryService.findById(s.getDominantChallengeId());

            sb.append("- 진행중인 챌린지 중 가장 많이 수행한 챌린지: ").append(dominant.getTitle())
                    .append(" (수행횟수 ")
                    .append(String.format("%d", s.getDominantChallengeScore()))
                    .append("\n");
        }

        if (s.getMostFailedChallengeId() != null) {
            Challenge failed = challengeQueryService.findById(s.getMostFailedChallengeId());

            sb.append("- 최근 실패한 챌린지 중 실패율 높았던 챌린지: ")
                    .append(failed.getTitle())
                    .append(" (실패율 ")
                    .append(String.format("%.2f", s.getMostFailedChallengeFailureRate()))
                    .append(")\n");
        }

        if (s.getMostUnstableChallengeId() != null) {
            Challenge unstable = challengeQueryService.findById(s.getMostUnstableChallengeId());

            sb.append("- 수행 패턴 변동성이 큰 챌린지: ")
                    .append(unstable.getTitle())
                    .append(" (분산 ")
                    .append(String.format("%.2f", s.getMostUnstableChallengeVariance()))
                    .append(")\n");
        }

        return sb.toString();
    }

    /*
        AI에게 보낼 메타데이터를 생성한다.
     */
    private Map<String, Object> buildMetadata(UserStatistics s) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("type", "user_statistics");
        meta.put("totalChallenges", s.getTotalChallengeCount());
        meta.put("completedChallenges", s.getCompletedChallengeCount());
        meta.put("failedChallenges", s.getFailedChallengeCount());

        meta.put("currentStreakDays", s.getCurrentStreakLength());
        meta.put("maxStreakDays", s.getMaxStreakLength());

        meta.put("weeklyActiveDays", s.getWeeklyActiveDays());
        meta.put("monthlyActiveDays", s.getMonthlyActiveDays());

        if (s.getDominantChallengeId() != null) {
            meta.put("dominantChallengeId", s.getDominantChallengeId());
            meta.put("dominantChallengeScore", s.getDominantChallengeScore());
        }

        if (s.getMostFailedChallengeId() != null) {
            meta.put("mostFailedChallengeId", s.getMostFailedChallengeId());
            meta.put("mostFailedFailureRate", s.getMostFailedChallengeFailureRate());
        }

        if (s.getMostUnstableChallengeId() != null) {
            meta.put("mostUnstableChallengeId", s.getMostUnstableChallengeId());
            meta.put("mostUnstableVariance", s.getMostUnstableChallengeVariance());
        }

        return meta;
    }
}
