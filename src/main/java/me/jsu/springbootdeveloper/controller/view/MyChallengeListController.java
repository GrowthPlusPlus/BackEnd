// 정성우

package me.jsu.springbootdeveloper.controller.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.Status;
import me.jsu.springbootdeveloper.dto.MyChallengeInProgressResponse;
import me.jsu.springbootdeveloper.dto.MyChallengeSuccessOrFailResponse;
import me.jsu.springbootdeveloper.service.view.MyChallengeListService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    유저가 수행하는 진행중, 완료, 실패 챌린지 리스트를 볼 수 있는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class MyChallengeListController {
    private final MyChallengeListService myChallengeListService;

    /*
        유저가 참여한 챌린지 중, 진행중인 챌린지 리스트를 조회한다.
     */
    @GetMapping("/api/challenges/my/inProgress")
    public ResponseEntity<List<MyChallengeInProgressResponse>> getMyChallengesInProgress(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<MyChallengeInProgressResponse> responses = myChallengeListService.getMyChallenges(userDetails.getUsername(), Status.IN_PROGRESS);

        return ResponseEntity.ok(responses);
    }

    /*
        유저가 참여한 챌린지 중, 완료한 챌린지 리스트를 조회한다.
     */
    @GetMapping("/api/challenges/my/success")
    public ResponseEntity<List<MyChallengeSuccessOrFailResponse>> getMyChallengesSuccess(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<MyChallengeSuccessOrFailResponse> responses = myChallengeListService.getMyChallengesSuccessOrFail(userDetails.getUsername(), Status.SUCCESS);

        return ResponseEntity.ok(responses);
    }

    /*
        유저가 참여한 챌린지 중, 실패한 챌린지 리스트를 조회한다.
     */
    @GetMapping("/api/challenges/my/fail")
    public ResponseEntity<List<MyChallengeSuccessOrFailResponse>> getMyChallengesFail(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<MyChallengeSuccessOrFailResponse> responses = myChallengeListService.getMyChallengesSuccessOrFail(userDetails.getUsername(), Status.FAIL);

        return ResponseEntity.ok(responses);
    }
}
