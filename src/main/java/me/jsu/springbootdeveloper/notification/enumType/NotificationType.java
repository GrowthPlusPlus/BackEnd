// 정성우

package me.jsu.springbootdeveloper.notification.enumType;

/*
    알림의 유형을 표시하기 위한 열거형 클래스이다.
 */
public enum NotificationType {
    FRIEND_REQUEST,  // 친구 요청
    FRIEND_ACCEPT,
    COMMENT,  // 댓글
    LIKE,  // 좋아요
    CHALLENGE,  // 챌린지 관련 (일반)
    CHALLENGE_KICK,  // 챌린지 멤버 강퇴
    CHALLENGE_LEADER,  // 챌린지 방장 위임
    SYSTEM,  // 시스템 공지
    MOTIVATION,  // 동기부여 메시지
}
