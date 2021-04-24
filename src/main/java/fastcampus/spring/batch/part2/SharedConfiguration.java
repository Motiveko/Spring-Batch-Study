package fastcampus.spring.batch.part2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class SharedConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    /*
        JobExecutionContext는 하나의 Job 내에서(여러Step) 공유할 수 있고   -> db 테이블이 존재한다
        StepExecutionContext는 하나의 Step 내에서만 공유할 수 있다.       -> db 테이블이 없다
    */

    @Bean
    public Job sharedJob() {
        return jobBuilderFactory.get("sharedJob")
                .incrementer(new RunIdIncrementer())
                .start(this.sharedStep())
                .next(this.sharedStep2())
                .build();
    }

    @Bean
    public Step sharedStep() {
        return stepBuilderFactory.get("sharedStep")
                .tasklet((contribution, chunkContext) -> {

                    // Step Execution Context에 값 추가
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();
                    stepExecutionContext.putString("stepKey", "step execution context");

                    // Job Execution Context에 값 추가
                    JobExecution jobExecution = stepExecution.getJobExecution();
                    JobInstance jobInstance = jobExecution.getJobInstance();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
                    jobExecutionContext.putString("jobKey", "job execution context");           // BATCH_JOB_EXECUTION_PARAMS에 저장된다.
                    JobParameters jobParameters = jobExecution.getJobParameters();

                    // jobName, stepName, RunIdIncrementer가 생성한 run.id 시퀀스값 출력
                    log.info("jobName: {}, stepName: {}, paramter: {}",
                            jobInstance.getJobName(),
                            stepExecution.getStepName(),
                            jobParameters.getLong("run.id"));

                    return RepeatStatus.FINISHED;
                }).build();
    }

    @Bean
    public Step sharedStep2() {
        return stepBuilderFactory.get("sharedStep2")
                .tasklet((contribution, chunkContext) -> {

                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

                    // sharedStep 에서 저장한 jobKey와 stepKey 출력,
                    // Step Execution Context는 해당 스탭 안에서만 값이 공유되므로 EmptyStepkey 출력
                    log.info("jobKey: {}, stepKey: {}",
                            jobExecutionContext.getString("jobKey", "EmptyJobkey"),
                            stepExecutionContext.getString("stepKey", "EmptyStepKey"));

                    return RepeatStatus.FINISHED;
                }).build();
    }

}
