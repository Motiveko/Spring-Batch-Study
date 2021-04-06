package fastcampus.spring.batch.part3;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChunkProcessingConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job chunkProcessingJob() {
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(this.taskBaseStep())
                .next(this.chunkBaseStep(null) )
                .build();
    }

    @Bean
    @JobScope   // 이 어노테이션으로 인해  chunkProcessingJob() 에서 null을 넘겨줘도 jobParameters를 사용할 수 있다.
    public Step chunkBaseStep(
            @Value("#{jobParameters[chunkSize]}") String chunkSize // Spring EL을 통한 시스템 파라미터에 접근
    ) {
        int chunkSizeInt = StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10;
        return stepBuilderFactory.get("chunkBaseStep")
                .<String, String>chunk(chunkSizeInt)          // <INPUT, OUTPUT>chunk( chunkSize)
                .reader(itemReader())                        // ListItemReader, processor에 INPUT 타입의 개별 아이템을 보낸다.
                .processor(getItemProcessor())               // item을 processing하여 List<Output> 으로 writer로 보낸다(chunkSize만큼의 크기)
                .writer(itemWriter())                        // 받아온 List를 처리한다.
                .build();
    }

    private ItemReader<? extends String> itemReader() {
        return new ListItemReader<>(getItems());
    }

    private ItemProcessor<String, String> getItemProcessor() {
        return item -> item + ", 슷프링 배치";
    }

    private ItemWriter<String> itemWriter() {
        return items -> log.info("chunk items size : {}",  items.size());
    }



    @Bean
    public Step taskBaseStep() {
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(tasklet())
                .build();
    }

    private Tasklet tasklet() {
        // chunksize = 10의 chunk 방식을 tasklet으로 구현

        List<String> items = getItems();

        return (contribution, chunkContext) -> {

            StepExecution stepExecution = contribution.getStepExecution();
            JobParameters jobParameters = stepExecution.getJobParameters();


            // Program Argument 에서 -chunkSize={VALUE} 로 설정한 값을 jobParameters에서 읽을 수 있다.
            String value = jobParameters.getString("chunkSize", "10");
            int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;
            int fromIndex = stepExecution.getReadCount();
            int toIndex = fromIndex + chunkSize;             //

            if( fromIndex >= items.size()) {
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIndex, toIndex);
            log.info("task item size: {}", subList.size());

            stepExecution.setReadCount(toIndex);
            return RepeatStatus.CONTINUABLE;
        };
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
             items.add(i + "Hello");
        }

        return items;
    }
}
