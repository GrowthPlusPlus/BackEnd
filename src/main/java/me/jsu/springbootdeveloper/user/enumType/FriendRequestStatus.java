
package me.jsu.springbootdeveloper.user.enumType;

/*
    친구 요청의 상태를 관리하는 열거형 클래스이다.
 */
public enum FriendRequestStatus {
    REQUESTED,  // 현재 친구 요청이 유효한 상태이다.
    ACCEPTED,  // 처리 완료 상태로 친구 관계가 생성된 상태이다.
    REJECTED,  // 친구 요청이 거절된 상태이다.
    CANCELED  // 친구 요청를 보낸 사람이 해당 요청을 취소한 상태이다.
}
