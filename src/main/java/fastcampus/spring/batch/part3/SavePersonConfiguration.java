package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SavePersonConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;


    @Bean
    public Job savePersonJob() throws Exception {
        return jobBuilderFactory.get("savePersonJob")
                .incrementer(new RunIdIncrementer())
                .start(this.savePersonStep(null))
                .listener(new SavePersonListener.SavePersonJobExecutionListener())      // SavePersonListener에 선언한 JobExecutionListner 적용
                .listener(new SavePersonListener.SavePersonAnnotationJobExecution())    // SavePersonListener에 선언한 JobExecutionListner 적용
                .build();
    }

    @Bean
    @JobScope
    public Step savePersonStep(
            @Value("#{jobParameters[allow_duplicate]}") String allowDuplicate
    ) throws Exception {
        boolean flag = "true".equals(allowDuplicate) ? true : false;
        return stepBuilderFactory.get("savePersonStep")
                .<Person,Person>chunk(100)
                .reader(csvFileItemReader())
                .processor(itemProcessor(flag))
                .writer(compositeItemWriter())
                .listener(new SavePersonListener.SavePersonStepExecutionListener())     // SavePersonListener에 선언한 StepExecutionListener(annotation 기반)
//                .writer(getPersonJpaItemWriter())
                .build();
    }

    private CompositeItemWriter<? super Person> compositeItemWriter() throws Exception {
        CompositeItemWriter<Person> personCompositeItemWriter = new CompositeItemWriter<>();
        List<ItemWriter<? super Person>> delegates = new ArrayList<>();
        delegates.add(getPersonJpaItemWriter());
        delegates.add(getLogItemWriter());
        personCompositeItemWriter.setDelegates(delegates);

        return personCompositeItemWriter;
    }

    private ItemWriter getLogItemWriter() {
        return items -> log.info("등록한 items 수 : {}", items.size());
    }

    private ItemWriter<Person> getPersonJpaItemWriter() throws Exception {
        JpaItemWriter<Person> itemWriter = new JpaItemWriterBuilder<Person>()
                .entityManagerFactory(entityManagerFactory)
                .build();
        itemWriter.afterPropertiesSet();

        return itemWriter;
    }


    private ItemReader<? extends Person> csvFileItemReader() throws Exception {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setNames("id", "name", "age", "address");
        lineMapper.setLineTokenizer(tokenizer);

        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(id, name, age, address);
        });

        FlatFileItemReader<Person> csvFileItemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")
                .encoding("UTF-8")
                .resource(new ClassPathResource("test.csv"))
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();

        csvFileItemReader.afterPropertiesSet();
        return csvFileItemReader;
    }

    private ItemProcessor<? super Person,? extends Person> itemProcessor(boolean flag) {
        Map<String, String> dupMap = new HashMap<>();
        return item -> {
            if( flag ) {
                return item;
            } else {
                if(dupMap.containsKey(item.getName())) {
                    return null;
                } else {
                    dupMap.put(item.getName(), "");
                    return item;
                }
            }
        };
    }
    



}
