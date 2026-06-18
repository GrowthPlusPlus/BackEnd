// 정성우

package me.jsu.springbootdeveloper.view.domain;

/*
    mainHome에서 일주일간 챌린지의 상태를 나타내기 위한 enum이다.
 */
public enum DayStatus {
    RED,    // 실패한 챌린지 존재
    GREEN,  // 하나 이상 성공
    GRAY,    // 아무것도 안함
    NONE  // 미래이므로 확정되지 않음
}
