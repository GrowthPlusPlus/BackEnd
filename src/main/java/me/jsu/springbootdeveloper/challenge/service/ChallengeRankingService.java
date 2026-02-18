// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.dto.RankingResponse;
import me.jsu.springbootdeveloper.challenge.dto.RankingResultResponse;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotFoundException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    챌린지 랭킹을 집계하거나, 조회하게 해주는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class ChallengeRankingService {
    private static final int BATCH_SIZE = 200;  // 배치 사이즈 단위로 처리한다.

    private final UserService userService;
    private final UserStatisticsRepository userStatisticsRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;

    /*
        해당 챌린지의 랭킹 순위, 사용자의 랭킹 순위를 조회하여 반환하는 함수이다.
     */
    public RankingResultResponse getRanking(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(
                0,           // page
                100             // 한 페이지 당 읽을 인증글 개수
        );

        // 상위 랭커를 조회한다.
        List<RankingResponse> topParticipants = challengeParticipantRepository.findTopRankers(challengeId, pageable);

        // 랭커의 등수를 순서대로 넣는다.
        for (int i = 0; i < topParticipants.size(); i++) {
            topParticipants.get(i).setRank(i + 1);
        }

        // 내 정보를 검색하고 랭킹을 계산한다.
        ChallengeParticipant challengeParticipant = challengeParticipantRepository.findMyParticipantWithFetch(challengeId, user.getId())
                .orElseThrow(() -> new ChallengeNotFoundException("챌린지 참여 정보와 스코어가 없습니다."));

        // 나보다 점수가 높은 사람의 수를 카운트하므로 +1을 해야 등수가 나온다.
        long myRanking = challengeParticipantRepository.countHigherScore(challengeId, challengeParticipant.getRankingScore()) + 1;

        // 유저 프로필 사진이 null인지 확인한다.
        String profile;
        UserProfilePicture userProfilePicture = challengeParticipant.getUser().getUserProfilePicture();
        if (userProfilePicture == null) {
            profile = null;
        }
        else {
            profile = userProfilePicture.getImageUrl();
        }

        RankingResponse myRank = new RankingResponse(user.getId(), user.getNickname(), profile, challengeParticipant.getRankingScore(), (int) myRanking);

        // 전체 랭킹과 내 랭킹을 response로 반환한다.
        return new RankingResultResponse(topParticipants, myRank);
    }

    /*
        랭킹 점수를 12시마다 집계하는 함수이다.
     */
    @Transactional
    public void updateRankingForChallenge() {
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        do {
            // 현재 진행중인 챌린지들을 검색한다.
            batch = challengeParticipantRepository.findInProgressParticipantsAfterId(lastId, PageRequest.of(0, BATCH_SIZE));

            // 유저 id 리스트를 추출한다.
            List<Long> userIds = batch.stream()
                    .map(cp -> cp.getUser().getId())
                    .toList();

            // 유저 통계를 한 번에 조회한다.
            List<UserStatistics> statsList = userStatisticsRepository.findAllByUserIds(userIds);

            // Map으로 변환하여 유저 스코어 계산 시에 사용한다.
            Map<Long, UserStatistics> statsMap =
                    statsList.stream()
                            .collect(Collectors.toMap(
                                    us -> us.getUser().getId(),
                                    us -> us
                            ));

            for (ChallengeParticipant cp : batch) {
                // 유저 당 유저 통계를 map으로 연결한다.
                UserStatistics stats = statsMap.get(cp.getUser().getId());

                // 유저 통계가 존재하는지 검사하는 방어 코드이다.
                int weeklyActive = stats != null ? stats.getWeeklyActiveDays() : 0;

                // 스코어를 가중치에 따라 계산한다.
                double score =
                        cp.getTotalSuccessDays() * 1.0 +  // 총 수행 일자
                                cp.getCurrentStreakDays() * 1.8 +  // 연속 수행 일자
                                cp.getMaxStreakDays() * 1.2 +  // 최대 연속 수행 일자
                                weeklyActive * 0.5;  // 최근 일주일간 챌린지 수행 일자

                cp.updateRankingScore(score);  // DB에 반영한다.
                lastId = cp.getId();  // 커서를 이동한다.
            }
        } while (!batch.isEmpty());
    }
}
