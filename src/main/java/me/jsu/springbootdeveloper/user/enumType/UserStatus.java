// 정성우

package me.jsu.springbootdeveloper.user.enumType;

/*
    유저 상태를 나타내는 열거형 클래스이다.
 */
public enum UserStatus {
    NEW,  // 처음 서비스에 로그인하여 초기 설정을 해야하는 유저
    ACTIVE,  // 일반적으로 사용하는 유저
    SUSPENDED,  // 관리자에 의해 제재된 유저
    DELETE,  // 회원 탈퇴 상태인 유저

    // 관리자 회원가입 전용 상태
    ADMIN_EMAIL_PENDING,  // 이메일 인증 전
    ADMIN_APPROVAL_PENDING  // 이메일 인증 완료, 관리자 승인 대기
}
