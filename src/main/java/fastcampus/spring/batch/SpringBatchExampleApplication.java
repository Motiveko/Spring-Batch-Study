package fastcampus.spring.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchExampleApplication {
    public static void main(String[] args) {
        // async로 실행 시 종료가 안될때가 있어 배치가 종류가 안될때가 있어 추가함
        System.exit(SpringApplication.exit(SpringApplication.run(SpringBatchExampleApplication.class, args)));
    }

    @Bean
    @Primary
    TaskExecutor taskExecutor() {
        // multi-thread로 배치 실행을 위해 TaskExecutor 빈을 직접 선언해준다.
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        // 로그에 찍히는 쓰레드명의 앞에 batch-thread-가 붙는다
        taskExecutor.setThreadNamePrefix("batch-thread-");
        taskExecutor.initialize();
        return taskExecutor;
    }
}
