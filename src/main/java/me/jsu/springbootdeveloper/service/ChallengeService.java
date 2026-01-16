// 정성우

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Challenge;
import me.jsu.springbootdeveloper.repository.ChallengeRepository;
import org.springframework.stereotype.Service;

/*
    챌린지와 관련된 일을 처리하는 클래스이다.
 */
@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeService {
    private final ChallengeRepository challengeRepository;

    /*
        챌린지 id로 챌린지를 검색해주는 함수이다.
     */
    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_NOT_FOUND_FOR_ID"));
    }
}
