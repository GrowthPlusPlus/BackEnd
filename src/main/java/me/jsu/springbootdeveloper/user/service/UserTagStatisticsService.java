// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.dto.TagGraphResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
    유저의 태그별 챌린지 성공 횟수를 조회하기 위한 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class UserTagStatisticsService {
    private final UserService userService;
    private final ChallengeQueryService challengeQueryService;

    public List<TagGraphResponse> getTagGraph(String email) {
        User user = userService.findByEmail(email);

        List<Object[]> tagList = challengeQueryService.findTagStats(user.getId());  // 태그별 총 수행 횟수를 가져온다.

        List<TagGraphResponse> result = new ArrayList<>();  // 컨트롤러에 반환할 값이다.
        int others = 0;  // 7위 이하 태그들의 챌린지 수행 횟수를 누적하기 위한 변수이다.

        for (int i = 0; i < tagList.size(); i++) {  // 검색한 태그 이름 개수만큼 반복한다.
            Object[] row = tagList.get(i);  // 태그 리스트의 오브젝트 값을 뽑아낸다.

            String tagName = (String) row[0];
            Long count = (Long) row[1];

            if (i < 6) {  // 해당 태그가 상위 6위 이내인 경우이다.
                result.add(TagGraphResponse.builder()
                        .tagName(tagName)
                        .count(count.intValue())
                        .build()
                );
            }
            else {  // 7위 이후인 경우이다.
                others += count.intValue();
            }
        }

        if (others > 0) {  // 7위 이후의 태그 값이 존재할 경우, 결과 리스트에 추가한다.
            result.add(TagGraphResponse.builder()
                    .tagName("기타")
                    .count(others)
                    .build()
            );
        }

        return result;
    }
}
