// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.TagToChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
    List<TagToChallenge> findWithTagByChallengeId(Long challengeId);  // 챌린지 id로 태그를 검색한다.
}
