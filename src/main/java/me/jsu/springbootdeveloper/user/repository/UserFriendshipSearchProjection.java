package me.jsu.springbootdeveloper.user.repository;

public interface UserFriendshipSearchProjection {
    Long getId();
    String getNickname();
    String getProfileImage();
    String getStatus(); // 'FRIEND', 'PENDING', 'NONE' 등 상태값
}
