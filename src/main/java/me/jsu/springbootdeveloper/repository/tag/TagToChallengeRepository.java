// 정성우

package me.jsu.springbootdeveloper.repository.tag;

import me.jsu.springbootdeveloper.domain.tag.TagToChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    챌린지와 태그를 저장하는 DB이다.
 */
public interface TagToChallengeRepository extends JpaRepository<TagToChallenge, Long> {
    // 쿼리문으로 태그의 이름까지 한 번에 검색할 수 있도록 한다.
    @Query("""
        select ttc
        from TagToChallenge ttc
        join fetch ttc.tag
        where ttc.challenge.id = :challengeId
    """)
    List<TagToChallenge> findWithTagByChallengeId(@Param("challengeId") Long challengeId);  // 챌린지 id로 태그를 검색한다.

    void deleteByChallenge_Id(Long challengeId);  // 챌린지 id로 해당 챌린지의 모든 태그를 삭제한다.

    boolean existsByChallenge_IdAndTag_Id(Long challengeId, Long tagId);  // 이미 챌린지에 중복된 태그가 존재하는지 검사한다.
}
