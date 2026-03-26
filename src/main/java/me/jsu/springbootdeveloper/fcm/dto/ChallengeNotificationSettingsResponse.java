// 정성우

package me.jsu.springbootdeveloper.fcm.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

/*
    챌린지 알림 설정 조회 응답 DTO이다.
    로그인한 사용자가 특정 챌린지에서 받는 푸시 알림 토글 상태를 반환한다.
 */
@Getter
@Builder
public class ChallengeNotificationSettingsResponse {
    private boolean challengeAllPushEnabled;                    // 1. 해당 챌린지 전체 알림 활성화 여부

    private boolean dailyReminderPushEnabled;                   // 2. 해당 챌린지 일일(주간) 리마인더 활성화 여부
    private LocalTime dailyReminderTime;                        // 2. 해당 챌린지 리마인더 발송 시간(현재는 스케줄러 고정값)
    private boolean dailyReminderDisabledByGlobalSetting;       // 2. 전체 리마인더(전역) 설정이 꺼져 있는지

    private boolean memberReactionPushEnabled;                  // 3. 해당 챌린지 멤버 반응 소식 활성화 여부
    private boolean memberReactionDisabledByGlobalSetting;      // 3. 전체 멤버 반응 소식(전역) 설정이 꺼져 있는지

    private boolean memberCertificationPushEnabled;             // 4. 해당 챌린지 멤버 인증 소식 활성화 여부
    private boolean memberCertificationDisabledByGlobalSetting; // 4. 전체 멤버 인증 소식(전역) 설정이 꺼져 있는지
}

