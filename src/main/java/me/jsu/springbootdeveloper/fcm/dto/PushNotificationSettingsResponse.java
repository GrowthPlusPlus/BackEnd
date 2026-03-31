// 정성우

package me.jsu.springbootdeveloper.fcm.dto;

import lombok.Builder;
import lombok.Getter;

/*
    푸시 알림 설정 조회 응답 DTO이다.
    앱의 "푸시 알림 설정" 페이지에서 사용하는 토글 값을 반환한다.
 */
@Getter
@Builder
public class PushNotificationSettingsResponse {
    private boolean allPushNotificationEnabled;
    private boolean friendPushNotificationEnabled;
    //멤버반응 -> 좋아요, 댓글로 변경
    private boolean allLikesPushNotificationEnabled;
    private boolean allCommentsPushNotificationEnabled;
    private boolean allMemberCertificationPushNotificationEnabled;
    private boolean challengeInvitePushNotificationEnabled;
    private boolean motivationPushNotificationEnabled;
    private boolean dailyReminderPushNotificationEnabled;
}

