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
                // listener??? ???????????? ???????????? ??????????????? List??? ????????? ????????????.(????????? ???????????? ??????)
                .listener(new SavePersonListener.SavePersonJobExecutionListener())      // SavePersonListener??? ????????? JobExecutionListner ??????
                .listener(new SavePersonListener.SavePersonAnnotationJobExecution())    // SavePersonListener??? ????????? JobExecutionListner ??????
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
                .listener(new SavePersonListener.SavePersonStepExecutionListener())     // SavePersonListener??? ????????? StepExecutionListener(annotation ??????)
                .faultTolerant()                                                        // FaultTolerantStepBuilder??? ????????????, skip??? ?????? ?????? ????????? ????????? ??? ?????? method??? ?????????.
                .skip(NotFoundNameException.class)                                      // NotFoundNameException???
                .skipLimit(2)                                                           // 3????????? ???????????????.
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
        return items -> log.info("????????? items ??? : {}", items.size());
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

        // ?????? ?????? ???????????? ??????????????? processor
        ItemProcessor<Person, Person> duplicateValidationProcessor = getDuplicateValidationProcessor(flag);

        // ????????? ???????????? NotFoundNameException??? ????????? processor
        ItemProcessor<Person, Person> validationProcessor = item -> {
            if(item.isNotEmptyName()) {
                return item;
            }
            throw new NotFoundNameException();
        };

        // ????????? ItemProcessor??? ?????????.
        CompositeItemProcessor<Person, Person> itemProcessor = new CompositeItemProcessorBuilder<Person, Person>()
                .delegates( new PersonValidationRetryProcessor(),
                        validationProcessor,
                        duplicateValidationProcessor).build();

        itemProcessor.afterPropertiesSet();
        return itemProcessor;
    }

    private ItemProcessor<Person, Person> getDuplicateValidationProcessor(boolean flag) {
        // ??? dupSet??? step ????????? ?????? ????????????.( ???????????? chunk?????? ?????? ????????? ????????????) , ?????? StepExecutionContext??? ????????? ?????????????
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
