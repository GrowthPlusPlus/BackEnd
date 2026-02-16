
package me.jsu.springbootdeveloper.challenge.enumType;

/*
    챌린지 초대 링크의 타입을 나타내는 열거형 클래스이다.
    링크의 만료 조건을 결정한다.
 */
public enum InviteLinkType {
    /*
        1회용 링크이다. 한 명이 링크를 통해 챌린지에 참여하면 즉시 만료된다.
     */
    SINGLE_USE,
    
    /*
        시간 기반 링크이다. 생성 후 일정 시간이 지나면 만료된다.
        expiresAt 필드에 명시된 시간까지 유효하다.
     */
    TIME_BASED,
    
    /*
        다중 사용 링크이다. 최대 사용 횟수(maxUses)까지 여러 명이 사용할 수 있다.
        maxUses가 null이면 무제한 사용 가능하다.
     */
    MULTI_USE
}
