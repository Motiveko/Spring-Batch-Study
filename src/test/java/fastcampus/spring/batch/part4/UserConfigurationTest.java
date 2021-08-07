package fastcampus.spring.batch.part4;

import fastcampus.spring.batch.TestConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserConfiguration.class, TestConfiguration.class})
class UserConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    // userRepository는 실제 db에 연동되는게 아니라, test context안에서 작동하는거라 실제 db에 insert를 하진 않는듯
    @Autowired
    private UserRepository userRepository;

    @Test
    public void test() throws Exception {

        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        int size = userRepository.findALlByUpdatedDate(LocalDate.now()).size();

        Assertions.assertThat(jobExecution.getStepExecutions().stream()
                .filter(x -> x.getStepName().equals("userLevelUpStep"))
                .mapToInt(StepExecution::getWriteCount)
                .sum())
                .isEqualTo(size)
                .isEqualTo(300);

        Assertions.assertThat(userRepository.count())            // saveUserStep()에서 저장하는데, 400건이 저장되었는가?
                .isEqualTo(400);
    }

}