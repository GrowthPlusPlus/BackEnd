// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeMemberService;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.post.service.PostQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final PostQueryService postQueryService;
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeMemberService challengeMemberService;

    /*
        캘린더의 제일 상단의 완료 일수와 연속 일수를 찾아 반환해주는 함수이다.
        또한 챌린지 방장 여부를 반환한다.
     */
    public ChallengeCalendarResponse getCalendar(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        // 유저 id와 챌린지 id로 챌린지 참여 DB의 정보를 가져온다. 전체 완료 일수와 연속 일수를 가져오기 위함이다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        // 유저가 챌린지의 방장인지 검사한다.
        boolean challengeOwner = challengeMemberService.isOwner(user.getId(), challengeId);

        return new ChallengeCalendarResponse(challengeParticipant.getChallenge().getTitle(), challengeParticipant.getTotalSuccessDays(), challengeParticipant.getCurrentStreakDays(), challengeOwner);
    }

    /*
        캘린더의 월 마다의 챌린지 인증 기록, 즉 사진을 반환해주는 함수이다.
        사진이 없으면 null값을 반환한다.
     */
    public List<ChallengeCalendarPhotoResponse> getCalendarPhotos(String email, Long challengeId, int year, int month) {
        User user = userService.findByEmail(email);

        // 참여를 검증하여 참여자가 아니라면 예외처리 한다.
        challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        LocalDate start = LocalDate.of(year, month, 1);  // 받은 연, 월로 해당 월의 1일 날짜를 만든다.
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());  // start가 속한 연, 월을 유지한 채 day만 마지막 날로 변경한다.

        // 캘린더 정보를 검색하여 반환한다.
        return postQueryService.findCalendarPhotos(user.getId(), challengeId, start, end);
    }

    /*
        인증글들을 불러온다.
        페이지 단위로 연 월에 따라 유저의 인증글을 최신 순서대로 불러온다.
     */
    public Page<ChallengeCalendarPostResponse> getPostPhotos(String email, Long challengeId, int year, int month, int page) {
        User user = userService.findByEmail(email);

        // 참여를 검증하여 참여자가 아니라면 예외처리 한다.
        challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        Pageable pageable = PageRequest.of(
                page,           // page
                40,             // 한 페이지 당 읽을 인증글 개수
                Sort.by("createdAt").descending()  // 생성일을 기준으로 내림차순으로 검색한다.
        );

        LocalDate start = LocalDate.of(year, month, 1);  // 받은 연, 월로 해당 월의 1일 날짜를 만든다.
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());  // start가 속한 연, 월을 유지한 채 day만 마지막 날로 변경한다.

        return postQueryService.findCalendarPosts(user.getId(), challengeId, start, end, pageable);
    }
}
