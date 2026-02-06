// 정성우

package me.jsu.springbootdeveloper.tag.service;

import me.jsu.springbootdeveloper.tag.domain.TagCategory;
import me.jsu.springbootdeveloper.tag.exception.TagNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.tag.domain.Tag;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.tag.repository.TagRepository;
import me.jsu.springbootdeveloper.tag.repository.TagToChallengeRepository;
import me.jsu.springbootdeveloper.tag.repository.TagToUserRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
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
    private final TagToChallengeRepository tagToChallengeRepository;
    private final UserService userService;
    /*
        새로운 태그를 저장한다. 이미 존재하는 태그 이름이면 예외를 던진다.
     */
    @Transactional
    public TagResponse createTag(String tagName, TagCategory tagCategory) {
        // 태그 이름 중복 검사
        if (tagRepository.existsByTagName(tagName)) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + tagName);
        }
        //태그 생성
        Tag tag = Tag.builder()
                .tagName(tagName)
                .tagCategory(tagCategory)
                .build();

        Tag savedTag = tagRepository.save(tag);

        return new TagResponse(savedTag.getId(), savedTag.getTagName(), savedTag.getTagCategory());
    }

    /*
        클라이언트에게 전체 태그 목록을 제공한다.
        TagResponse로 정제해서 반환한다.
     */
    public List<TagResponse> getAllTags() {
        return tagRepository.findAllByOrderByTagCategoryAscTagNameAsc
                        ()  // 모든 태그를 List로 리턴받는다.
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

    /*
        해당 챌린지와 관련된 태그를 반환한다.
     */
    public List<TagResponse> getChallengeTags(Long challengeId) {
        return tagToChallengeRepository.findWithTagByChallengeId(challengeId)
                .stream()
                .map(tagToChallenge -> {
                    Tag tag = tagToChallenge.getTag();
                    return new TagResponse(tag.getId(), tag.getTagName(), tag.getTagCategory());
                })
                .toList();
    }

    /*
        유저가 선택한 태그들을 챌린지의 태그로 저장한다.
     */
    @Transactional
    public void updateChallengeTags(List<Long> tagIds, Challenge challenge) {
        if (tagIds == null || tagIds.isEmpty()) {  // 빈 리스트가 들어온 경우 리턴한다.
            return;
        }

        for (Long tagId : tagIds) {  // 태그 id로 하나씩 찾아가며 DB에 관계를 저장한다.
            if (tagToChallengeRepository.existsByChallenge_IdAndTag_Id(challenge.getId(), tagId)) {
                continue;  // 태그가 두 번 저장되지 않도록 중복을 방지한다.
            }

            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new TagNotFoundException("TAG_NOT_FOUND_FOR_ID"));

            tagToChallengeRepository.save(TagToChallenge.builder()  // 챌린지와 태그 관계를 DB에 저장한다.
                    .tag(tag)
                    .challenge(challenge)
                    .build());
        }
    }

    /*
        해당 챌린지와 관련된 모든 태그를 삭제한다.
     */
    public void deleteAllChallenge_Id(Long challengeId) {
        tagToChallengeRepository.deleteByChallenge_Id(challengeId);  // 모든 태그 삭제
    }
}