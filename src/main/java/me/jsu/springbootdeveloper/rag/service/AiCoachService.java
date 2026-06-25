// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.rag.dto.DocumentSearchResultDto;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.dto.DailyGraphResponse;
import me.jsu.springbootdeveloper.user.dto.TagGraphResponse;
import me.jsu.springbootdeveloper.user.dto.UserYearlyActivityResponse;
import me.jsu.springbootdeveloper.user.dto.WeeklyGraphResponse;
import me.jsu.springbootdeveloper.user.service.UserProgressStatisticsService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import me.jsu.springbootdeveloper.user.service.UserTagStatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
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
    private final UserStatisticsService userStatisticsService;
    private final UserProgressStatisticsService userProgressStatisticsService;
    private final UserTagStatisticsService userTagStatisticsService;
    private final ChallengeQueryService challengeQueryService;

    public QueryResponseDto coach(User user) {
        UserStatistics stats = userStatisticsService.findByUser_IdStatistics(user.getId());  // 통계를 조회한다.

        // AI에게 보낼 질문이다.
        String query = """
                유저의 누적 통계, 잔디 활동, 최근 4주 활동, 최근 1주 활동, 태그별 활동을 기반으로
                개인화된 챌린지 조언을 해줘.
                
                잘하고 있는 점, 개선할 점, 추천 행동을 구체적으로 말해줘.
                """;

        if (stats.getTotalChallengeCount() == 0) {  // 만약 참여한 챌린지가 하나도 없다면 예외처리한다.
            return new QueryResponseDto(query, "챌린지에 참여해서 목표를 이루어보세요!");
        }

        String email = user.getEmail();
        int currentYear = LocalDate.now(ZoneId.of("Asia/Seoul")).getYear();

        // 유저의 잔디, 4주간 활동, 1주 활동, 태그별 활동을 검색한다.
        UserYearlyActivityResponse grass = userStatisticsService.searchYearGrass(email, currentYear);
        WeeklyGraphResponse recent4Weeks = userProgressStatisticsService.getRecent4WeeksGraph(email);
        DailyGraphResponse weekly = userProgressStatisticsService.getWeeklyGraph(email);
        List<TagGraphResponse> tagStats = userTagStatisticsService.getTagGraph(email);

        // AI에게 보낼 유저 통계 기반 가짜 문서를 생성한다.
        DocumentSearchResultDto userActivityDoc = buildUserActivityDocument(stats, grass, recent4Weeks, weekly, tagStats, user.getId());
        List<DocumentSearchResultDto> contexts = List.of(userActivityDoc);

        String answer = ragService.generateAnswerWithContexts(query, contexts);  // 답변을 생성한다.

        return new QueryResponseDto(query, removeCitation(answer));  // 생성한 답변을 반환한다.
    }

    /*
        유저 통계를 기반으로 RAG에 전달할 문서 DTO를 생성한다.
     */
    private DocumentSearchResultDto buildUserActivityDocument(
            UserStatistics stats,
            UserYearlyActivityResponse grass,
            WeeklyGraphResponse recent4Weeks,
            DailyGraphResponse weekly,
            List<TagGraphResponse> tagStats,
            Long userId
    ) {
        // 문서의 내용을 생성한다.
        String content = buildContent(stats, grass, recent4Weeks, weekly, tagStats);

        return new DocumentSearchResultDto(
                "user-statistics-" + userId,  // 문서 ID
                content,  // 실제 LLM에 들어갈 텍스트
                buildMetadata(stats, recent4Weeks, weekly, tagStats),  // 참고용 메타데이터
                1.0  // score는 고정
        );
    }

    /*
        AI에게 보낼 content를 생성한다.
     */
    private String buildContent(
            UserStatistics s,
            UserYearlyActivityResponse grass,
            WeeklyGraphResponse recent4Weeks,
            DailyGraphResponse weekly,
            List<TagGraphResponse> tagStats
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("사용자 챌린지 활동 종합 분석 문서\n\n");

        sb.append("[1. 누적 통계]\n");
        sb.append("- 총 참여 챌린지 수: ").append(s.getTotalChallengeCount()).append("개\n");
        sb.append("- 완료한 챌린지 수: ").append(s.getCompletedChallengeCount()).append("개\n");
        sb.append("- 실패한 챌린지 수: ").append(s.getFailedChallengeCount()).append("개\n");
        sb.append("- 현재 연속 활동 일수: ").append(s.getCurrentStreakLength()).append("일\n");
        sb.append("- 최대 연속 활동 기록: ").append(s.getMaxStreakLength()).append("일\n");
        sb.append("- 최근 7일 활동 일수: ").append(s.getWeeklyActiveDays()).append("일\n");
        sb.append("- 최근 30일 활동 일수: ").append(s.getMonthlyActiveDays()).append("일\n");

        // 챌린지가 삭제된 경우에 대한 방어 코드들이다.
        if (challengeQueryService.findByIdOrNull(s.getDominantChallengeId()) == null) {
            s.removeDominantChallenge();
        }
        if (challengeQueryService.findByIdOrNull(s.getMostFailedChallengeId()) == null) {
            s.removeMostFailedChallenge();
        }
        if (challengeQueryService.findByIdOrNull(s.getMostUnstableChallengeId()) == null) {
            s.removeMostUnstableChallenge();
        }

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

        sb.append("\n[2. 잔디 활동]\n");
        sb.append("- 총 활동일 수: ").append(grass.getSuccessDays()).append("일\n");
        sb.append("- 현재 연속 활동일 수: ").append(grass.getCurrentStreak()).append("일\n");
        sb.append("- 올해 일별 활동 배열: ")
                .append(Arrays.toString(grass.getActivity()))
                .append("\n");

        sb.append("\n[3. 최근 4주 활동 추이]\n");
        sb.append("- 이번 기준 4주 성공 횟수: ").append(recent4Weeks.getThisMonth()).append("\n");
        sb.append("- 이전 기준 4주 성공 횟수: ").append(recent4Weeks.getLastMonth()).append("\n");

        sb.append("\n[4. 최근 1주 활동 추이]\n");
        sb.append("- 이번 주 일별 성공 횟수: ").append(weekly.getThisWeek()).append("\n");
        sb.append("- 저번 주 일별 성공 횟수: ").append(weekly.getLastWeek()).append("\n");

        sb.append("\n[5. 태그별 활동]\n");
        for (TagGraphResponse tag : tagStats) {
            sb.append("- ")
                    .append(tag.getTagName())
                    .append(": ")
                    .append(tag.getCount())
                    .append("회\n");
        }

        return sb.toString();
    }

    /*
        AI에게 보낼 메타데이터를 생성한다.
     */
    private Map<String, Object> buildMetadata(
            UserStatistics s,
            WeeklyGraphResponse recent4Weeks,
            DailyGraphResponse weekly,
            List<TagGraphResponse> tagStats
    ) {
        Map<String, Object> meta = new HashMap<>();

        meta.put("type", "user_activity_summary");

        // 누적 통계
        meta.put("totalChallenges", s.getTotalChallengeCount());
        meta.put("completedChallenges", s.getCompletedChallengeCount());
        meta.put("failedChallenges", s.getFailedChallengeCount());

        meta.put("currentStreakDays", s.getCurrentStreakLength());
        meta.put("maxStreakDays", s.getMaxStreakLength());

        meta.put("weeklyActiveDays", s.getWeeklyActiveDays());
        meta.put("monthlyActiveDays", s.getMonthlyActiveDays());

        // 최근 4주
        meta.put("recent4WeeksThisMonth", recent4Weeks.getThisMonth());
        meta.put("recent4WeeksLastMonth", recent4Weeks.getLastMonth());

        // 최근 1주
        meta.put("thisWeekActivity", weekly.getThisWeek());
        meta.put("lastWeekActivity", weekly.getLastWeek());

        // 태그 활동
        Map<String, Integer> tags = new HashMap<>();
        for (TagGraphResponse tag : tagStats) {
            tags.put(tag.getTagName(), tag.getCount());
        }

        meta.put("tagStats", tags);

        // 대표 챌린지
        if (s.getDominantChallengeId() != null) {
            meta.put("dominantChallengeId", s.getDominantChallengeId());
            meta.put("dominantChallengeScore", s.getDominantChallengeScore());
        }

        // 실패 챌린지
        if (s.getMostFailedChallengeId() != null) {
            meta.put("mostFailedChallengeId", s.getMostFailedChallengeId());
            meta.put("mostFailedFailureRate", s.getMostFailedChallengeFailureRate());
        }

        // 변동성 챌린지
        if (s.getMostUnstableChallengeId() != null) {
            meta.put("mostUnstableChallengeId", s.getMostUnstableChallengeId());
            meta.put("mostUnstableVariance", s.getMostUnstableChallengeVariance());
        }

        return meta;
    }

    /*
        답변의 참고 문헌을 제거한다.
     */
    private String removeCitation(String answer) {
        return answer.replaceAll("\\[1].*", "").trim();
    }
}
