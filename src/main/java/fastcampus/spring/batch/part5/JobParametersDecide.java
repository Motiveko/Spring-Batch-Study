package fastcampus.spring.batch.part5;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

@RequiredArgsConstructor
public class JobParametersDecide implements JobExecutionDecider {

    // 커스텀하게 FlowExecutionStatus 정의
    public static final FlowExecutionStatus CONTINUE = new FlowExecutionStatus("CONTINUE");

    // jobParameters의 key
    private final String key;

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {

        // JobExecution, StepExecution에서 jobParamters 를 조회한다.
        String value = jobExecution.getJobParameters().getString(key);

        // key에 해당하는 value가 없으면 실행을 안할것이다.
        if(StringUtils.isEmpty(value) ) {
            return FlowExecutionStatus.COMPLETED;
        }
        return CONTINUE;
    }
}
