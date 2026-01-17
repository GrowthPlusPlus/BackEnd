// 정성우

package me.jsu.springbootdeveloper.domain.user;

/*
    유저 상태를 나타내는 열거형 클래스이다.
 */
public enum UserStatus {
    NEW,  // 처음 서비스에 로그인하여 초기 설정을 해야하는 유저
    ACTIVE,  // 일반적으로 사용하는 유저
    SUSPENDED  // 관리자에 의해 제재된 유저
}
