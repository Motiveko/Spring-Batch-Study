package fastcampus.spring.batch.part4;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class LevelUpJobExecutionListener implements JobExecutionListener {

    private final UserRepository userRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        //none
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 등급 상향된 건수와, job의 처리 시간을 log로 찍을것이다.
        Collection<User> updatedUsers = userRepository.findALlByUpdatedDate(LocalDate.now());

        long executionTime = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
        log.info("회원 등급 업데이트 배치 프로그램");
        log.info("------------------------");
        log.info("총 데이터 처리 {}건, 처리 시간 {}millis", updatedUsers.size(), executionTime);

    }
}
