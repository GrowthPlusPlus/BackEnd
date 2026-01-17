// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    클라이언트에게 태그 id를 받아서 유저와 태그를 연결하도록 요청한다.
 */
@Getter
@NoArgsConstructor
public class TagSelectRequest {
    private List<Long> tagIds;
}