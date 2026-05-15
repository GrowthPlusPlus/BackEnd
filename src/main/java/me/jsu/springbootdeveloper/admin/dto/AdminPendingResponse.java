// 정성우

package me.jsu.springbootdeveloper.admin.dto;

import lombok.Builder;
import lombok.Getter;

/*
    현재 관리자 승인을 대기중인 계정들의 목록을 가져오는 dto이다.
 */
@Getter
@Builder
public class AdminPendingResponse {
    public Long userId;
    public String email;
    public String nickname;
}
