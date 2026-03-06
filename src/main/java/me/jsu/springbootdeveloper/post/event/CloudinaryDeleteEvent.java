// 정성우

package me.jsu.springbootdeveloper.post.event;

/*
    클라우디너리 삭제를 위한 이벤트 클래스이다.
 */
public record CloudinaryDeleteEvent(String publicId) {
}
