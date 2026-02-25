// 정성우

package me.jsu.springbootdeveloper.tag.service;

import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
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

import java.util.*;
import java.util.stream.Collectors;

/*
    태그와 관련된 처리를 하는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagToUserRepository tagToUserRepository;
    private final TagToChallengeRepository tagToChallengeRepository;
    private final ChallengeRepository challengeRepository;  // 순환참조를 방지하기 위해 사용한다.
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

        // 추가하려는 태그를 한 번에 조회한다.
        List<Tag> tags = tagRepository.findAllById(tagIds);

        // 들어온 태그 개수랑 검색한 태그 개수랑 일치하는지 확인한다.
        if (tags.size() != tagIds.size()) {
            throw new TagNotFoundException("TAG_NOT_FOUND_FOR_ID");
        }

        // 유저의 기존 태그 관계를 한 번에 조회한다.
        List<TagToUser> existingMappings = tagToUserRepository.findAllByUser_Id(user.getId());

        // 기존 태그 관계의 id를 set으로 만든다.
        Set<Long> existingTagIds = existingMappings.stream()
                .map(tu -> tu.getTag().getId())
                .collect(Collectors.toSet());

        // id를 비교하여 기존 태그 관계에서 없는 태그만 새로 태그 관계로 저장한다.
        List<TagToUser> newMappings = tags.stream()
                .filter(tag -> !existingTagIds.contains(tag.getId()))
                .map(tag -> TagToUser.builder()
                        .tag(tag)
                        .user(user)
                        .build())
                .toList();

        tagToUserRepository.saveAll(newMappings);  // 태그를 db에 저장한다.
    }

    /*
        유저의 관심 태그를 삭제하는 함수이다.
     */
    @Transactional
    public void deleteTags(String userEmail, List<Long> tagIds) {
        User user = userService.findByEmail(userEmail);

        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        // 관심 태그를 삭제한다.
        tagToUserRepository.deleteByUser_IdAndTag_IdIn(user.getId(), tagIds);
    }

    /*
        해당 챌린지와 관련된 태그를 반환한다.
     */
    public List<TagResponse> getChallengeTags(Long challengeId) {
        return searchChallengeTags(challengeId)
                .stream()
                .map(tagToChallenge -> {
                    Tag tag = tagToChallenge.getTag();
                    return new TagResponse(tag.getId(), tag.getTagName(), tag.getTagCategory());
                })
                .toList();
    }

    /*
        챌린지와 관련된 태그를 검색한다.
     */
    public List<TagToChallenge> searchChallengeTags(Long challengeId) {
        return tagToChallengeRepository.findWithTagByChallengeId(challengeId);
    }

    /*
        여러 챌린지들의 태그들을 한 번에 조회한다.
        키워드로 챌린지를 검색하는 함수에서 사용한다.
     */
    public Map<Long, List<TagResponse>> getChallengeTagsByOnce(List<Long> challengeIds) {
        List<TagToChallenge> mappings = tagToChallengeRepository.findWithTagByChallengeIdsByOnce(challengeIds);

        Map<Long, List<TagResponse>> result = new HashMap<>();  // 챌린지 id로 해당 챌린지에 포함된 태그들을 매핑한다.
        for (TagToChallenge ttc : mappings) {
            Long challengeId = ttc.getChallenge().getId();
            Tag tag = ttc.getTag();

            result
                    .computeIfAbsent(challengeId, k -> new ArrayList<>())
                    .add(new TagResponse(tag.getId(), tag.getTagName(), tag.getTagCategory()));  // 챌린지 id에 매핑되는 태그들이다.
        }

        return result;
    }

    /*
        선택한 챌린지를 연결하는 함수로, 컨트롤러와 실제로 매핑되는 부분이다.
        재사용을 위해 실제 태그를 저장하는 부분과 분리한다.
     */
    @Transactional
    public void selectChallengeTags(String email, Long challengeId, List<Long> tagIds) {
        User user = userService.findByEmail(email);

        Challenge challenge = authentication(user.getId(), challengeId, tagIds);  // 검증한다.
        updateChallengeTags(tagIds, challenge);  // 태그를 저장한다.
    }

    /*
        선택한 챌린지 태그들을 해제하는 함수이다.
     */
    @Transactional
    public void deleteChallengeTags(String email, Long challengeId, List<Long> tagIds) {
        User user = userService.findByEmail(email);

        // 챌린지의 태그를 삭제할 권한이 있는지 검사한다.
        authentication(user.getId(), challengeId, tagIds);

        // 태그가 빈 리스트인지 검사한다.
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        // 챌린지 태그를 삭제한다.
        tagToChallengeRepository.deleteByChallenge_IdAndTag_IdIn(challengeId, tagIds);
    }

    /*
        챌린지를 연결/해제 하기 위해서 수행하는 검증 함수이다.
     */
    public Challenge authentication(Long userId, Long challengeId, List<Long> tagIds) {
        Challenge challenge = challengeRepository.findById(challengeId)  // 챌린지를 검색한다.
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_NOT_FOUND_FOR_ID"));

        boolean owner = challengeRepository.existsByIdAndUser_Id(challengeId, userId);
        if (!owner) {  // 방장이 아니면 태그를 선택할 수 없다.
            throw new NotAllowedChallengeActionException("NOT_CHALLENGE_OWNER");
        }

        return challenge;
    }

    /*
        유저가 선택한 태그들을 챌린지의 태그로 실제로 저장한다.
     */
    public void updateChallengeTags(List<Long> tagIds, Challenge challenge) {
        if (tagIds == null || tagIds.isEmpty()) {  // 빈 리스트가 들어온 경우 리턴한다.
            return;
        }

        // 추가하려는 태그를 한 번에 조회한다.
        List<Tag> tags = tagRepository.findAllById(tagIds);

        // 들어온 태그 개수랑 검색한 태그 개수랑 일치하는지 확인한다.
        if (tags.size() != tagIds.size()) {
            throw new TagNotFoundException("TAG_NOT_FOUND_FOR_ID");
        }

        // 챌린지의 기존 태그를 조회한다.
        List<TagToChallenge> existing = tagToChallengeRepository.findAllByChallenge_Id(challenge.getId());

        // 기존 태그 관계의 id를 set으로 만든다.
        Set<Long> existingTagIds = existing.stream()
                .map(t -> t.getTag().getId())
                .collect(Collectors.toSet());

        // 기존에 없는 태그만 추가한다.
        List<TagToChallenge> newMappings = tags.stream()
                .filter(tag -> !existingTagIds.contains(tag.getId()))  // 있던 것은 제외한다.
                .map(tag -> TagToChallenge.builder()
                        .tag(tag)
                        .challenge(challenge)
                        .build())
                .toList();

        tagToChallengeRepository.saveAll(newMappings);  // db에 저장한다.
    }

    /*
        해당 챌린지와 관련된 모든 태그를 삭제한다.
     */
    public void deleteAllChallenge_Id(Long challengeId) {
        tagToChallengeRepository.deleteByChallenge_Id(challengeId);  // 모든 태그 삭제
    }
}