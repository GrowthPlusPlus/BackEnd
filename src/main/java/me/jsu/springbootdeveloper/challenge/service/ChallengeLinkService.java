package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import me.jsu.springbootdeveloper.challenge.enumType.InviteLinkType;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteLinkRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeLinkService {
    /*
       작성된 함수 목록
       - 챌린지 초대 링크 생성 메서드/private String createInviteLink(Challenge challenge, User user,
                                    InviteLinkType linkType,int expirationDays)
       - 고유한 초대 코드 생성 메서드/public String generateUniqueInviteCode()
     */

    private final ChallengeInviteLinkRepository challengeInviteLinkRepository;
    //base url(application.yml의 base url값 불러옴)
    @Value("${app.base-url}")
    private String baseUrl;
    /*
        챌린지 초대 링크를 생성하는 헬퍼 메서드이다.

        @param challenge: 초대 링크를 생성할 챌린지
        @param user: 링크를 생성하는 사용자 (챌린지 생성자)
        @param linkType: 링크 타입 (SINGLE_USE, TIME_BASED, MULTI_USE)
        @param expirationDays: 만료 일수 (TIME_BASED 타입의 경우 사용)
        @return 초대 링크 스트링 문자열
     */
    protected String createInviteLink(Challenge challenge, User user) {
        return createInviteLink(challenge, user, InviteLinkType.TIME_BASED, 30);
    }

    private String createInviteLink(Challenge challenge, User user,
                                    InviteLinkType linkType,int expirationDays) {

        // 고유한 초대 코드 생성 (중복 체크 포함)
        String inviteCode = generateUniqueInviteCode();

        // ChallengeInviteLink 객체 생성
        ChallengeInviteLink challengeInviteLink = ChallengeInviteLink.builder()
                .challenge(challenge)
                .user(user)
                .linktype(linkType)
                .inviteCode(inviteCode)
                .build();

        // 생성 직후 링크를 사용 가능 상태로 둔다. (기본값 false면 랜딩/resolve 모두 만료 처리됨)
        challengeInviteLink.setActive(true);

        //링크 타입 - TimeBased 일 경우 만료 시간 계산 (현재 시간 + expirationDays일)
        if (linkType == InviteLinkType.TIME_BASED){
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);
            challengeInviteLink.setExpiresAt(expiresAt);
        }

        // 링크 타입 - MultiUse 일 경우 최대 사용 횟수 설정
        if (linkType == InviteLinkType.MULTI_USE) {
            int maxUses = 10;
            challengeInviteLink.setMaxUses(maxUses);
        }

        // 링크 타입 - SingleUse 일 경우 사용 횟수 설정 및 활성화 해제
        if(linkType == InviteLinkType.SINGLE_USE){
            challengeInviteLink.setUsedCount(1);
            challengeInviteLink.setActive(false);
        }
        // 초대 링크 저장
        challengeInviteLinkRepository.save(challengeInviteLink);
        // 초대 링크 생성
        String InviteLink= baseUrl + "/v/" + challengeInviteLink.getInviteCode();
        return InviteLink;
    }
    /*
        고유한 초대 코드를 생성하는 메서드이다.
        도메인 메서드를 사용하여 초대 코드를 생성하고, 중복 체크를 통해 고유성을 보장한다.

        @param inviteLink: 초대 코드를 생성할 ChallengeInviteLink 객체
     */
    public String generateUniqueInviteCode() {
        int maxAttempts = 100;  // 최대 재시도 횟수이다. 중복 코드가 생성될 경우를 대비하여 재시도한다.
        int attempt = 0;


        //초대 코드 생성
        while (attempt < maxAttempts) {
            // 도메인 메서드를 사용하여 초대 코드 생성 (UUID의 앞 8자리 사용)
            String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            // 중복 체크: 이미 존재하는 코드인지 확인
            if (!challengeInviteLinkRepository.findByInviteCode(inviteCode).isPresent()) {
                return inviteCode;  // 고유한 코드이면 종료
            }

            attempt++;  // 중복이면 재시도
        }

        // 최대 재시도 횟수를 초과한 경우 예외 발생
        // 이는 매우 드문 경우이지만, 코드 길이를 늘리거나 재시도 횟수를 늘려야 할 수 있다.
        throw new IllegalStateException(
                "초대 코드 생성에 실패했습니다. 최대 재시도 횟수(" + maxAttempts + "회)를 초과했습니다."
        );
    }
}
