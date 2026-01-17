// 정성우

package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.TagResponse;
import me.jsu.springbootdeveloper.dto.TagSelectRequest;
import me.jsu.springbootdeveloper.service.tag.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    /*
        태그 전체를 조회한다.
        태그 전체를 조회하므로 카테고리 별 분류는 클라이언트에서 수행한다.
     */
    @GetMapping("/api/tags")
    public ResponseEntity<List<TagResponse>> getTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }

    /*
        유저가 선택한 여러 태그가 DB상에서 연결되도록 한다.
     */
    @PostMapping("/api/tags")
    public ResponseEntity<Void> selectTags(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TagSelectRequest request
    ) {
        tagService.selectTags(userDetails.getUsername(), request.getTagIds());
        return ResponseEntity.noContent().build();
    }
}
