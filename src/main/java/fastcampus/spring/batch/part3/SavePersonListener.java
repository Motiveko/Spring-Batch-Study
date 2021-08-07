package fastcampus.spring.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.annotation.BeforeStep;

@Slf4j
public class SavePersonListener {


    // JobExecutionListener 구현체 기반
    public static class SavePersonJobExecutionListener implements JobExecutionListener {

        @Override
        public void beforeJob(JobExecution jobExecution) {
            log.info("beforeJob");
        }

        @Override
        public void afterJob(JobExecution jobExecution) {

            int sum = jobExecution.getStepExecutions().stream().mapToInt(StepExecution::getWriteCount).sum();

            log.info("afterJob : {}", sum);

        }
    }

    // Annotation기반
    public static class SavePersonAnnotationJobExecution {

        @BeforeJob
        public void beforeJob(JobExecution jobExecution) {
            log.info("annotationBeforeJob");
        }

        @AfterJob
        public void afterJob(JobExecution jobExecution) {

            int sum = jobExecution.getStepExecutions().stream().mapToInt(StepExecution::getWriteCount).sum();

            log.info("annotationAfterJob : {}", sum);

        }
    }

    public static class SavePersonStepExecutionListener {
        @BeforeStep
        public void beforeStep(StepExecution stepExecution) {
            log.info("beforeStep");
        }

        @AfterStep
        public ExitStatus afterStep(StepExecution stepExecution) {
            // StepExecutionListener의 afterStep은 ExitStatus를 반환할 수 있다
            log.info("afterStep : {}" , stepExecution.getWriteCount());
            // Spring Batch에서 Batch step 이 성공/실패 등에 대한 상태를 StepExecution에 저장하는데, 이 값을 그대로 돌려주자.
            return stepExecution.getExitStatus();

            // 돌려주는 상태를 우리가 직접 구현해도 된다(상태변경됨)
//            if (stepExecution.getWriteCount() == 0 ) {
//                return ExitStatus.FAILED;
//            }
        }
    }

}
