package fastcampus.spring.batch.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TestConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;


    @Bean
    public Job testJob() {
        return jobBuilderFactory.get("testJob")
                .incrementer(new RunIdIncrementer())
                .start(this.testStep())
                .build();
    }

//    @Bean
    public Step testStep() {
        return stepBuilderFactory.get("testStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("쓰까무라~~");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
