// 정성우

package me.jsu.springbootdeveloper.service.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarResponse;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import me.jsu.springbootdeveloper.service.post.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지 캘린더의 내용을 사용자에게 보여줄 수 있게 처리하는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 해당 서비스는 조회 전용이다.
public class ChallengeCalendarService {
    private final UserService userService;
    private final ChallengeService challengeService;
    private final PostService postService;

    /*
        캘린더의 제일 상단의 완료 일수와 연속 일수를 찾아 반환해주는 함수이다.
     */
    public ChallengeCalendarResponse getCalendar(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        ChallengeParticipant challengeParticipant = challengeService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);
        return new ChallengeCalendarResponse(challengeParticipant.getTotalSuccessDays(), challengeParticipant.getCurrentStreakDays());
    }

    /*
        캘린더의 월 마다의 챌린지 인증 기록, 즉 사진을 반환해주는 함수이다.
        사진이 없으면 null값을 반환한다.
     */
    public List<ChallengeCalendarPhotoResponse> getCalendarPhotos(String email, Long challengeId, int year, int month) {
        User user = userService.findByEmail(email);

        // 참여를 검증하여 참여자가 아니라면 예외처리 한다.
        challengeService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        LocalDate start = LocalDate.of(year, month, 1);  // 받은 연, 월로 해당 월의 1일 날짜를 만든다.
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());  // start가 속한 연, 월을 유지한 채 day만 마지막 날로 변경한다.

        // 캘린더 정보를 검색하여 반환한다.
        return postService.findCalendarPhotos(user.getId(), challengeId, start, end);
    }

    /*
        인증글들을 불러온다.
     */
    public Page<ChallengeCalendarPostResponse> getPostPhotos(String email, Long challengeId, int page) {

    }
}
