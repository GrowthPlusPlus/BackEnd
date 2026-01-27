package me.jsu.springbootdeveloper.repository.user;

public interface UserFriendshipSearchProjection {
    Long getId();
    String getNickname();
    String getProfileImage();
    String getStatus(); // 'FRIEND', 'PENDING', 'NONE' 등 상태값
}
