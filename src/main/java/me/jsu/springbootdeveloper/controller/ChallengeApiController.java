// 정성우

package me.jsu.springbootdeveloper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.InviteChallenge.DTO.ChallengeFriendInviteListResponse;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse_Sent;
import me.jsu.springbootdeveloper.dto.challenge.*;
import me.jsu.springbootdeveloper.dto.createChallenge.CreateChallengeResponse;
import me.jsu.springbootdeveloper.dto.createChallenge.CreateChallengeRequest;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeRepository;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    챌린지에 관련된 일을 수행하는 api 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeApiController {
    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final UserService userService;

    /*
        챌린지를 생성하는 POST 요청 API이다.
        요청 본문으로 챌린지 정보를 받고, 요청자의 이메일(UserDetails)을 기반으로 오너를 설정한다.

        @param userDetails 인증된 유저 정보(이메일)
        @param request 생성할 챌린지 정보
        @return 생성된 챌린지 상세 정보(ChallengeDetailResponse)
     */
    @Operation(
            summary = "챌린지 생성하기",
            description = """
        요청: 챌린지 생성 화면에 있는 필드 순서대로 값 보내기 -> 응답: (1) 챌린지 방 링크 (2) 친구 목록 with 초대상태
        ex) POST /api/challenges/create
        """
    )

    @Transactional
    @PostMapping("/api/challenges/create")
    public ResponseEntity<CreateChallengeResponse> createChallenge (@AuthenticationPrincipal UserDetails userDetails,
                                                                    @RequestBody CreateChallengeRequest request)

    {
        CreateChallengeResponse response = challengeService.createChallenge(userDetails.getUsername(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    //챌린지 초대 - 친구 목록 with 초대 상태
    @Operation(
            summary = "챌린지 초대 탭 - 친구 목록과 각 친구의 초대 상태 불러오기",
            description = """
        """
    )

    @GetMapping("/api/challenges/{challengeId}/invite")
    public ResponseEntity<List<ChallengeFriendInviteListResponse>> getFriendList_with_InviteStatus (@AuthenticationPrincipal UserDetails userDetails,
                                                                              @PathVariable Long challengeId)

    {
        List<ChallengeFriendInviteListResponse> responses = challengeService.getFriendList_with_InviteStatus(userDetails.getUsername(), challengeId);

        return ResponseEntity.ok(responses);
    }

    //챌린지 초대 - 친구 초대하기
    @Operation(
            summary = "챌린지에 친구 초대하기",
            description = """
        """
    )
    @Transactional
    @PostMapping("/api/challenges/{challengeId}/invite/{friendNickname}")
    public ResponseEntity<Void> inviteChallenge (@AuthenticationPrincipal UserDetails userDetails,
                                                                    @PathVariable Long challengeId,
                                                 @PathVariable String friendNickname)

    {
        challengeService.inviteUser(userDetails.getUsername(), challengeId, friendNickname);

        return ResponseEntity.noContent().build();
    }

    /*
        유저가 챌린지 id로 해당 챌린지에서 나가도록 하는 컨트롤러이다.
        챌린지에서 나가면 의미 없는 챌린지 초대 링크, 챌린지 위크 스테이터스 DB 값도 삭제한다.
        인증글, 챌린지 데일리 로그 등은 그대로 둔다.
     */
    @Operation(
            summary = "챌린지 나가기",
            description = """
        챌린지 id로 로그인한 유저가 나가도록 한다.
        ex) DELETE /api/challenges/1/leaveChallenge
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "챌린지 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 챌린지를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "챌린지 방장은 나갈 수 없음"),
            @ApiResponse(responseCode = "403", description = "허용되지 않은 챌린지 행동"),
    })
    @DeleteMapping("/api/challenges/{challengeId}/leaveChallenge")
    public ResponseEntity<Void> leaveChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "1")
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
    @Operation(
            summary = "챌린지 멤버 목록 조회",
            description = """
        챌린지 멤버 목록을 페이징 조회하며 닉네임 부분 검색을 지원한다.
        해당 정보를 바탕으로 방장 위임, 추방 대상을 정할 수 있다.
        ex1) GET /api/challenges/5/members?page=0
        ex2) GET /api/challenges/5/members?page=0&nickname=김
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "멤버 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 챌린지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/members")
    public ResponseEntity<Page<ChallengeMemberResponse>> getMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "5")
            @PathVariable Long challengeId,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam int page,
            @Parameter(description = "닉네임 부분 검색 (미입력 시 전체 조회)", required = false, example = "김")
            @RequestParam(required = false) String nickname  // 필수가 아니다.
    ) {
        Page<ChallengeMemberResponse> response = challengeService.getMembers(userDetails.getUsername(), challengeId, page, nickname);

        return ResponseEntity.ok(response);
    }

    /*
        챌린지 방장을 위임할때 사용하는 컨트롤러이다.
        ex) /api/challenges/1/owner/delegate
     */
    @Operation(
            summary = "챌린지 방장 위임",
            description = """
        현재 방장이 다른 멤버에게 방장 권한을 위임한다.
        ex) POST /api/challenges/1/owner/delegate
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "방장 위임 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 챌린지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음")
    })
    @PostMapping("/api/challenges/{challengeId}/owner/delegate")
    public ResponseEntity<Void> delegateOwner(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "1")
            @PathVariable Long challengeId,
            @RequestBody @Valid ChallengeOwnerDelegateRequest request
    ) {
        challengeService.delegateOwner(userDetails.getUsername(), challengeId, request.getDelegateMemberId());

        return ResponseEntity.noContent().build();
    }

    /*
        챌린지에서 유저를 추방할 때 사용하는 컨트롤러이다.
        ex) /api/challenges/1/members/kick
     */
    @Operation(
            summary = "챌린지 멤버 추방",
            description = """
        챌린지 방장이 특정 멤버를 추방한다.
        ex) /api/challenges/1/members/kick
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "멤버 추방 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 챌린지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음")
    })
    @PostMapping("/api/challenges/{challengeId}/members/kick")
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "3")
            @PathVariable Long challengeId,
            @RequestBody @Valid KickMemberRequest request
    ) {
        challengeService.kickMember(userDetails.getUsername(), challengeId, request.getTargetUserId());

        return ResponseEntity.noContent().build();
    }

    /*
        챌린지를 삭제할 때 사용하는 컨트롤러이다.
     */
    @Operation(
            summary = "챌린지 삭제",
            description = """
        챌린지를 완전히 삭제한다. 데일리 로그, 인증글 등을 제외한 나머지 종속 정보도 삭제한다.
        ex) DELETE /api/challenges/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "챌린지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 챌린지를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    })
    @DeleteMapping("/api/challenges/{challengeId}")
    public ResponseEntity<Void> deleteChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "10")
            @PathVariable Long challengeId
    ) {
        challengeService.deleteChallenge(userDetails.getUsername(), challengeId);

        return ResponseEntity.noContent().build();
    }

    /*
        수정 화면 진입 시 기존 설정 값을 내려주기 위한 컨트롤러다.
     */
    @Operation(
            summary = "챌린지 설정 조회",
            description = """
        챌린지 수정 화면 진입 시 기존 설정 값을 조회한다.
        ex) GET /api/challenges/1/settings
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "챌린지 설정 조회 성공"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/settings")
    public ResponseEntity<ChallengeSettingsResponse> getSettings(
            @Parameter(description = "챌린지 ID", required = true, example = "2")
            @PathVariable Long challengeId
    ) {
        ChallengeSettingsResponse response = challengeService.getSettings(challengeId);

        return ResponseEntity.ok(response);
    }

    /*
        클라이언트가 보낸 챌린지 설정을 반영(덮어쓰기)하는 컨트롤러이다.
     */
    @Operation(
            summary = "챌린지 설정 수정",
            description = """
        클라이언트가 보낸 챌린지 설정으로 기존 설정을 덮어쓴다.
        챌린지 설명 부분을 제외하고 null값이 올 수 없다.
        ex) PUT /api/challenges/1/settings
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "챌린지 설정 수정 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "404", description = "챌린지 또는 태그를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음")
    })
    @PutMapping("/api/challenges/{challengeId}/settings")
    public ResponseEntity<Void> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 ID", required = true, example = "2")
            @PathVariable Long challengeId,
            @RequestBody @Valid ChallengeSettingsUpdateRequest request
    ) {
        challengeService.updateSettings(
                userDetails.getUsername(), challengeId, request.getEndDate(), request.getRequiredWeeklyCount(), request.getTagIds(),
                request.getContent(), request.getPhotoRequired(), request.getChallengeVisibility());

        return ResponseEntity.noContent().build();
    }
}
