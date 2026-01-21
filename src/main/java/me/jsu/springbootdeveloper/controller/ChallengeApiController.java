// 정성우

package me.jsu.springbootdeveloper.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.challenge.*;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/*
    챌린지에 관련된 일을 수행하는 api 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeApiController {
    private final ChallengeService challengeService;

    /*
        유저가 챌린지 id로 해당 챌린지에서 나가도록 하는 컨트롤러이다.
        챌린지에서 나가면 의미 없는 챌린지 초대 링크, 챌린지 위크 스테이터스 DB 값도 삭제한다.
        인증글, 챌린지 데일리 로그 등은 그대로 둔다.
     */
    @DeleteMapping("/api/challenges/{challengeId}/leaveChallenge")
    public ResponseEntity<Void> leaveChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId
    ) {
        challengeService.leaveChallenge(userDetails.getUsername(), challengeId);

        return ResponseEntity.noContent().build();
    }

    /*
        챌린지 멤버 목록을 페이징 하여 조회한다.
        위임, 추방 대상 선택용으로 닉네임 부분 검색을 지원한다.
        닉네임 부분 검색 없이 검색하는 경우: GET /api/challenges/5/members?page=0
        닉네임 부분 검색으로 검색하는 경우:  GET /api/challenges/5/members?page=0&nickname=김
     */
    @GetMapping("/api/challenges/{challengeId}/members")
    public ResponseEntity<Page<ChallengeMemberResponse>> getMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestParam int page,
            @RequestParam(required = false) String nickname  // 필수가 아니다.
    ) {
        Page<ChallengeMemberResponse> response = challengeService.getMembers(userDetails.getUsername(), challengeId, page, nickname);

        return ResponseEntity.ok(response);
    }

    /*
        챌린지 방장을 위임할때 사용하는 컨트롤러이다.
        ex) /api/challenges/1/owner/delegate
     */
    @PostMapping("/api/challenges/{challengeId}/owner/delegate")
    public ResponseEntity<Void> delegateOwner(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestBody @Valid ChallengeOwnerDelegateRequest request
    ) {
        challengeService.delegateOwner(userDetails.getUsername(), challengeId, request.getDelegateMemberId(), request.isLeaveAfterDelegate());

        return ResponseEntity.noContent().build();
    }

    /*
        챌린지에서 유저를 추방할 때 사용하는 컨트롤러이다.
        ex) /api/challenges/1/members/kick
     */
    @PostMapping("/api/challenges/{challengeId}/members/kick")
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestBody @Valid KickMemberRequest request
    ) {
        challengeService.kickMember(userDetails.getUsername(), challengeId, request.getTargetUserId());

        return ResponseEntity.noContent().build();
    }

    /*
        챌린지를 삭제할 때 사용하는 컨트롤러이다.
     */
    @DeleteMapping("/api/challenges/{challengeId}")
    public ResponseEntity<Void> deleteChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId
    ) {
        challengeService.deleteChallenge(userDetails.getUsername(), challengeId);

        return ResponseEntity.noContent().build();
    }

    /*
        수정 화면 진입 시 기존 설정 값을 내려주기 위한 컨트롤러다.
     */
    @GetMapping("/api/challenges/{challengeId}/settings")
    public ResponseEntity<ChallengeSettingsResponse> getSettings(
            @PathVariable Long challengeId
    ) {
        ChallengeSettingsResponse response = challengeService.getSettings(challengeId);

        return ResponseEntity.ok(response);
    }

    /*
        클라이언트가 보낸 챌린지 설정을 반영(덮어쓰기)하는 컨트롤러이다.
     */
    @PutMapping("/api/challenges/{challengeId}/settings")
    public ResponseEntity<Void> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestBody @Valid ChallengeSettingsUpdateRequest request
    ) {
        challengeService.updateSettings(
                userDetails.getUsername(), challengeId, request.getEndDate(), request.getRequiredWeeklyCount(), request.getTagIds(),
                request.getContent(), request.getPhotoRequired(), request.getChallengeVisibility());

        return ResponseEntity.noContent().build();
    }
}
