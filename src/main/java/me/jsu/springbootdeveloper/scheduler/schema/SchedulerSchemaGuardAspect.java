// 정성우

package me.jsu.springbootdeveloper.scheduler.schema;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/*
    스케줄러에서 스키마가 생성됐음을 확인하는 코드를 어노테이션 하나로 줄이기 위한 클래스이다.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class SchedulerSchemaGuardAspect {
    private final SchemaGuardService schemaGuardService;

    // 유저 통계의 스키마가 존재하는지 검증한다.
    @Around("@annotation(RequireUserStatisticsSchema)")
    public Object guard(ProceedingJoinPoint pjp) throws Throwable {  // ProceedingJoinPoint pjp는 원래 실행하려던 객체이다.
        if (!schemaGuardService.isUserStatisticsSchemaReady()) {
            return null; // 조건에 통과하지 못하므로 나머지 코드를 실행하지 않는다.
        }
        return pjp.proceed();  // 조건을 통과하면 나머지 코드도 실행된다.
    }

    // 챌린지 참여의 스코어 스키마가 존재하는지 검증한다.
    @Around("@annotation(RequireRankingScoreSchema)")
    public Object guardRankingScore(ProceedingJoinPoint pjp) throws Throwable {

        if (!schemaGuardService.isRankingScoreSchemaReady()) {
            return null;  // 스키마 준비 안되면 실행 안 함
        }

        return pjp.proceed();
    }
}