// 정성우

package me.jsu.springbootdeveloper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.tag.Tag;
import me.jsu.springbootdeveloper.domain.tag.TagToUser;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.TagResponse;
import me.jsu.springbootdeveloper.repository.tag.TagRepository;
import me.jsu.springbootdeveloper.repository.tag.TagToUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/*
    태그와 관련된 처리를 하는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagToUserRepository tagToUserRepository;
    private final UserService userService;

    /*
        클라이언트에게 전체 태그 목록을 제공한다.
        TagResponse로 정제해서 반환한다.
     */
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll()  // 모든 태그를 List로 리턴받는다.
                .stream()  // List를 stream 형태로 바꾼다.
                .map(tag -> new TagResponse(  // tag를 TagResponse에 매핑한다.
                        tag.getId(),
                        tag.getTagName(),
                        tag.getTagCategory()
                ))
                .toList();  // List 형태로 다시 바꾼다.
    }

    /*
        유저가 선택한 태그들을 관심 태그로 저장한다.
     */
    @Transactional
    public void selectTags(String userEmail, List<Long> tagIds) {
        User user = userService.findByEmail(userEmail);

        if (tagIds == null || tagIds.isEmpty()) {  // 빈 리스트가 들어온 경우 리턴한다.
            return;
        }

        for (Long tagId : tagIds) {  // 태그 id로 하나씩 찾아가며 DB에 관계를 저장한다.
            if (tagToUserRepository.existsByUser_IdAndTag_Id(user.getId(), tagId)) {
                continue;  // 태그가 두 번 저장되지 않도록 중복을 방지한다.
            }

            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new TagNotFoundException("TAG_NOT_FOUND_FOR_ID"));

            tagToUserRepository.save(TagToUser.builder()  // 유저와 태그 관계를 DB에 저장한다.
                    .tag(tag)
                    .user(user)
                    .build());
        }
    }
}