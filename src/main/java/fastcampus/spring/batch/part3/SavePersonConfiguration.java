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
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.persistence.EntityManagerFactory;
import java.util.*;

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
                // listener를 연속으로 설정하면 내부적으로 List에 담아서 실행한다.(설정한 순서대로 실행)
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
                .<Person,Person>chunk(10)
                .reader(csvFileItemReader())
                .processor(itemProcessor(flag))
                .writer(compositeItemWriter())
                .listener(new SavePersonListener.SavePersonStepExecutionListener())     // SavePersonListener에 선언한 StepExecutionListener(annotation 기반)
                .faultTolerant()                                                        // FaultTolerantStepBuilder를 반환하고, skip과 같은 예외 처리를 설정할 수 있는 method가 생긴다.
                .skip(NotFoundNameException.class)                                      // NotFoundNameException을
                .skipLimit(2)                                                           // 3번까지 허용하겠다.
//                .retry(NotFoundNameException.class)
//                .retryLimit(3)
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

    private ItemProcessor<? super Person,? extends Person> itemProcessor(boolean flag) throws Exception {

        // 사람 이름 중복여부 체크해주는 processor
        ItemProcessor<Person, Person> duplicateValidationProcessor = getDuplicateValidationProcessor(flag);

        // 이름이 없을경우 NotFoundNameException을 던지는 processor
        ItemProcessor<Person, Person> validationProcessor = item -> {
            if(item.isNotEmptyName()) {
                return item;
            }
            throw new NotFoundNameException();
        };

        // 두개의 ItemProcessor를 묶는다.
        CompositeItemProcessor<Person, Person> itemProcessor = new CompositeItemProcessorBuilder<Person, Person>()
                .delegates( new PersonValidationRetryProcessor(),
                        validationProcessor,
                        duplicateValidationProcessor).build();

        itemProcessor.afterPropertiesSet();
        return itemProcessor;
    }

    private ItemProcessor<Person, Person> getDuplicateValidationProcessor(boolean flag) {
        // 이 dupSet은 step 전체에 걸쳐 사용된다.( 여러번의 chunk에서 계속 상태를 유지한다) , 이게 StepExecutionContext와 관련이 있는건가?
        Set<String> dupSet = new HashSet<>();
        ItemProcessor<Person, Person> duplicateValidationProcessor =  item -> {
            if(flag) {
                return item;
            } else {
                if(dupSet.contains(item.getName())) {
                    return null;
                } else {
                    dupSet.add(item.getName());
                    return item;
                }
            }
        };
        return duplicateValidationProcessor;
    }
}
