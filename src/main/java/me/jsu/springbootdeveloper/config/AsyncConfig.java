// 정성우

package me.jsu.springbootdeveloper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/*
    비동기 처리를 위한 설정을 하는 클래스이다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    /*
        shareImageTaskExecutor에서 해당 ThreadPool bean을 생성한다.
     */
    @Bean(name = "shareImageTaskExecutor")
    public Executor shareImageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  // 스프링에서 제공하는 스레드이다.
        executor.setCorePoolSize(2);  // 항상 2개의 worker thread를 유지한다.
        executor.setMaxPoolSize(8);  // 최대 생성 가능한 스레드 개수이다.
        executor.setQueueCapacity(100);  // 대기 가능한 작업 큐의 크기이다.
        executor.setThreadNamePrefix("share-image-");  // 스레드 이름으로 디버깅에서 어느 작업 스레드인지 구분이 가능하다.
        executor.initialize();  // 초기화 한다.
        return executor;
    }
}
