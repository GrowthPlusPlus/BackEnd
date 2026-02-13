package me.jsu.springbootdeveloper.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.dto.*;
import me.jsu.springbootdeveloper.user.repository.UserFriendshipSearchProjection;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.service.LogoutService;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserWithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/*
    해당 클래스는 REST API 방식으로 유저 정보를 검색하는 기능을 제공한다.
    최초 작성자: 정성우
 */
@RequiredArgsConstructor
@RestController
public class UserApiController {
    private final UserService userService;
    private final LogoutService logoutService;
    private final UserWithdrawalService userWithdrawalService;  // 회원탈퇴 서비스이다.
    private final UserRepository userRepository;

    //유저 검색-> 나와 해당 유저와의 관계 상태도 같이 반환
    @Operation(
            summary = "닉네임으로 유저 검색",
            description = """
        유저 검색 탭에서 닉네임으로 검색하면 해당 검색 글자를 포함하는 유저를 모두 반환한다. 
        유저당 반환되는 정보
        1. 유저id
        2. 유저닉네임
        3. 유저 프로필 이미지 url
        4. 나와 해당 유저와의 관계 상태 ( FRIEND, PENDING_SENT, PENDING_RECEIVED, NONE 중 하나)
   
        ex) GET /api/users/search/안녕난유저
        """
    )

    @GetMapping("/api/users/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String nickname,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new IllegalArgumentException("현재 유저를 찾을 수 없습니다."));

        List<UserFriendshipSearchProjection> results =
                userRepository.searchAllUsersWithRelationship(nickname, currentUser.getId());

        List<UserSearchResponse> response = results.stream()
                .map(UserSearchResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
    /*
        닉네임을 통해 유저 정보를 검색하는 GET 요청 API이다.
        @param nickname 검색할 유저의 닉네임
        @return 검색된 유저 정보 (UserResponse 객체)
     */
    @Operation(
            summary = "닉네임으로 유저 조회",
            description = """
        닉네임을 이용해 유저 정보를 조회한다.
        
        해당 api 는 단순 유저 검색으로 프로필 이미지, 나와 해당 유저와의 관계 상태 등을 반환하지 않습니다
        api/users/search 와 상황에 맞게 구분하여 사용해 주세요
        
        반환되는 정보
        1. 유저id
        2. 유저 이메일
        3. 유저 닉네임
        4. 유저 권한 (ROLE_USER, ROLE_ADMIN 중 하나)
        
        ex) GET /api/users/nickname/안녕난유저
        """
    )
    @GetMapping("/api/users/nickname/{nickname}")
    public ResponseEntity<UserResponse> findUserByNickname(
            @Parameter(description = "검색할 유저의 닉네임", required = true, example = "안녕난유저")
            @PathVariable String nickname) {
        User user = userService.findByNickname(nickname);
        return ResponseEntity.ok(user.toResponse());
    }

    /*
        이메일을 통해 유저 정보를 검색하는 GET 요청 API이다.
        @param email 검색할 유저의 이메일
        @return 검색된 유저 정보 (UserResponse 객체)
     */
    @Operation(
            summary = "이메일로 유저 조회",
            description = """
        이메일을 이용해 유저 정보를 조회한다.
        ex) GET /api/users/email/hello@naver.com
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 조회 성공"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @GetMapping("/api/users/email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(
            @Parameter(description = "검색할 유저의 이메일", required = true, example = "hello@naver.com")
            @PathVariable String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(user.toResponse());
    }

    /*
        해당 함수로 유저의 닉네임을 변경한다.
     */
    @Operation(
            summary = "닉네임 변경",
            description = """
        로그인한 사용자의 닉네임을 변경한다.
        ex) PATCH /api/users/me/nickname
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "닉네임 변경 성공"),
            @ApiResponse(responseCode = "400", description = "닉네임 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 닉네임")
    })
    @PatchMapping("/api/users/me/nickname")
    public ResponseEntity<Void> updateNickname(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateNicknameRequest request
    ) {
        userService.updateNickname(userDetails.getUsername(), request.getNickname());
        return ResponseEntity.noContent().build();  // 처리에 성공하면 응답 바디가 없다.
    }

    /*
        해당 함수로 유저의 프로필 사진을 등록한다.
     */
    @Operation(
            summary = "프로필 이미지 등록",
            description = """
        로그인한 사용자의 프로필 이미지를 등록하거나 변경한다.
        이미지 파일은 multipart/form-data의 Body 안의 image 파트로 전달받는다.
        ex) POST /api/users/me/profile-image
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "프로필 이미지 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "이미지 업로드 또는 삭제 실패")
    })
    @PostMapping("/api/users/me/profile-image")
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "업로드할 프로필 이미지 파일", required = true)
            @RequestPart("image") MultipartFile image
    ) {
        userService.updateProfileImage(userDetails.getUsername(), image);
        return ResponseEntity.noContent().build();
    }

    /*
        해당 컨트롤러로 유저의 프로필 사진을 삭제한다.
     */
    @Operation(
            summary = "프로필 이미지 삭제",
            description = """
        로그인한 사용자의 프로필 이미지를 삭제한다.
        ex) DELETE /api/users/me/profile-image
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "프로필 이미지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/api/users/me/profile-image")
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.removeProfileImage(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /*
        해당 함수로 유저의 한 줄 소개를 업데이트 한다.
     */
    @Operation(
            summary = "한 줄 소개 수정",
            description = """
        로그인한 사용자의 한 줄 소개를 수정한다. (최대 50자)
        ex) PATCH /api/users/me/introduction
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "한 줄 소개 수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/api/users/me/introduction")
    public ResponseEntity<Void> updateIntroduction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid UpdateIntroductionRequest request  // valid 옵션으로 50자 이하만 받는다. 넘으면 http 400을 반환한다.
    ) {
        userService.updateIntroduction(userDetails.getUsername(), request.getIntroduction());
        return ResponseEntity.noContent().build();
    }

    /*
        유저가 로그아웃 시 호출하는 함수이다.
     */
    @Operation(
            summary = "로그아웃",
            description = """
        로그인한 사용자의 로그아웃을 처리한다.
        서버에선 리프레시 토큰만 삭제하고 클라이언트의 엑세스 토큰은 삭제하지 못한다.
        따라서 앱에서 엑세스 토큰을 따로 삭제해야 한다.
        ex) DELETE /api/me/logout
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/api/me/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        logoutService.logout(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /*
        유저가 회원탈퇴 시 호출하는 함수이다.
     */
    @Operation(
            summary = "회원 탈퇴",
            description = """
        로그인한 사용자의 계정을 탈퇴 처리한다.
        리프레시 토큰, 유저 정보, 유저가 없으면 의미가 없는 정보 등을 함께 삭제한다.
        추후 서버 통계에 사용될 수 있는 유저 통계나 챌린지 로그 등의 기록은 삭제하지 않는다.
        이미 구글 OAuth 인증을 받은 유저이므로, 추후에 다시 회원가입 해도 로그인 하는 과정과 같다.
        ex) DELETE /api/me
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/api/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userWithdrawalService.withdraw(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /*
        마이페이지 상단의 유저 정보를 반환하는 함수이다.
     */
    @Operation(
            summary = "내 프로필 조회",
            description = "로그인한 사용자의 프로필 사진, 이름, 한 줄 소개, 관심 태그를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/users/me/profile")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        MyProfileResponse response = userService.getMyProfile(userDetails.getUsername());

        return ResponseEntity.ok(response);
    }
}
