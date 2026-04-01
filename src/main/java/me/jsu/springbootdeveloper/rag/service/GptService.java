// 정성우

package me.jsu.springbootdeveloper.rag.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.rag.domain.GptResponse;
import me.jsu.springbootdeveloper.rag.dto.QueryResponseDto;
import me.jsu.springbootdeveloper.rag.exception.AiGenerationException;
import me.jsu.springbootdeveloper.rag.repository.GptResponseRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/*
    DB 기반으로 하루 1회 제한 + 이미 존재한다면 결과를 재사용 처리한다.
 */
@Service
@RequiredArgsConstructor
public class GptService {
    private final GptResponseRepository gptResponseRepository;
    private final UserService userService;
    private final AiCoachService aiCoachService;
    private final AiChallengeRecommendService aiChallengeRecommendService;

    /*
        공통 처리 로직이다.
     */
    @Transactional
    public String handleRequest(String email, String type) {
        User user = userService.findByEmail(email);

        // 오늘 이미 코칭 값이 존재하는지 확인한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        GptResponse gptResponse = gptResponseRepository.findByUser_IdAndType(user.getId(), type)
                .orElse(null);

        if (gptResponse == null) {  // 만약 기존 값이 없다면 새로 생성한다.
            String result = generateResponse(user, type);  // ai 답변을 생성한다.

            // 새로운 엔티티를 생성한다.
            GptResponse newEntity = GptResponse.builder()
                    .user(user)
                    .type(type)
                    .response(result)
                    .createdDate(today)
                    .build();

            // db에 생성한 값을 저장한다.
            gptResponseRepository.save(newEntity);

            // ai 답변을 반환한다.
            return result;
        }
        else if (!gptResponse.getCreatedDate().equals(today)) {  // 기존값이 존재하지만 오늘 생성한적이 없다면 기존 값을 대체한다.
            String result = generateResponse(user, type);  // ai 답변을 생성한다.

            // 기존 엔티티 값을 업데이트한다.
            gptResponse.updateResponse(result, today);

            return result;
        }

        else {  // 기존값이 존재한다면 원래 있던 결과를 반환한다.
            return gptResponse.getResponse();
        }
    }

    /*
        ai 답변을 생성하기 위해 ragService를 호출하는 함수이다.
     */
    private String generateResponse(User user, String type) {
        if ("coach".equals(type)) {  // type이 맞춤형 코칭인 경우이다.
            QueryResponseDto response = aiCoachService.coach(user);

            return response.getAnswer();
        }
        else if ("challengeRecommend".equals(type)) {  // type이 챌린지 추천인 경우이다.
            QueryResponseDto response = aiChallengeRecommendService.recommend(user);

            return response.getAnswer();
        }
        else {
            throw new AiGenerationException("지원하지 않는 type을 사용했습니다.");
        }
    }

    /*
        맞춤형 코칭을 컨트롤러에서 호출하는 경우이다. type 이름에 대한 책임을 서비스로 옮긴다.
     */
    public String coach(String email) {
        return handleRequest(email, "coach");
    }

    /*
        챌린지 추천을 컨트롤러에서 호출하는 경우이다. type 이름에 대한 책임을 서비스로 옮긴다.
     */
    public String challengeRecommend(String email) {
        return handleRequest(email, "challengeRecommend");
    }
}
