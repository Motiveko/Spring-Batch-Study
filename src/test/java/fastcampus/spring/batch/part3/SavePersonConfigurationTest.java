package fastcampus.spring.batch.part3;

import fastcampus.spring.batch.TestConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SavePersonConfiguration.class, TestConfiguration.class })
// Junit5에서 @Autowired사용하기 위해서 필수 설정
@ExtendWith(SpringExtension.class)
@SpringBatchTest // @Scope의 정상 작동을 위한 annotation
class SavePersonConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PersonRepository personRepository;

//    @AfterEach
//    public void tearDown() throws Exception {
//        // 이걸 하지 않으면 not allow의 person Repository의 데이터가 저장된상태로 allow method로 넘어가게 된다는데, 우린 안해도 된다? junit 5라 그런것인가 싶다.
//        personRepository.deleteAll();
//    }

    @Test
    public void test_step() {
        // job parameter 로 null을 넘겨주면 false로 실행하기로 정했었다.
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("csvChunkStep");

        Assertions.assertThat(
                jobExecution.getStepExecutions()
                        .stream()
                        .mapToInt(StepExecution::getWriteCount)
                        .sum())
                .isEqualTo(personRepository.count())            // JobExecution의 count() === jpaRepository의 count() ( 10 )
                .isEqualTo(10)
        ;
    }

    @Test
    public void test_not_allow_duplicate() throws Exception {

         // given - 필요한 데이터
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("allow_duplicate", "false")
                .toJobParameters();

        // when - 테스트 대상
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then - 데이터 검증
        Assertions.assertThat(
                jobExecution.getStepExecutions()
                        .stream()
                        .mapToInt(StepExecution::getWriteCount)
                        .sum())
                .isEqualTo(personRepository.count())            // JobExecution의 count() === jpaRepository의 count() ( 10 )
                .isEqualTo(10)
                ;
        // 어떻게 spring-data-jpa 구현체인 jpaRepository가 이 count를 아는걸까? jpaWriter에 entityManager를 설정한 것 뿐인데.. jpa 원리를 좀 더 알아야할듯

        System.out.println("하이:" + personRepository.count());
    }

    @Test
    public void test_allow_duplicate() throws Exception {

        // given - 필요한 데이터
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("allow_duplicate", "true")
                .toJobParameters();

        // when - 테스트 대상
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then - 데이터 검증
        Assertions.assertThat(
                jobExecution.getStepExecutions()
                        .stream()
                        .mapToInt(StepExecution::getWriteCount)
                        .sum())
                .isEqualTo(personRepository.count())            // JobExecution의 count() === jpaRepository의 count() ( 10 )
                .isEqualTo(100)
        ;
        // 어떻게 spring-data-jpa 구현체인 jpaRepository가 이 count를 아는걸까? jpaWriter에 entityManager를 설정한 것 뿐인데.. jpa 원리를 좀 더 알아야할듯

        System.out.println("하이:" + personRepository.count());
    }
}
