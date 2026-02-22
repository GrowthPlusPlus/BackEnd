package me.jsu.springbootdeveloper.challenge.enumType;

/*
    챌린지 초대 상태를 관리하는 열거형 클래스이다.
 */
public enum InviteStatus {
    INVITED,  // 초대된 상태 (아직 응답하지 않음)
    ACCEPTED,  // 초대를 수락한 상태
    REJECTED  // 초대를 거절한 상태
}
