package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;


    @Bean
    public Job itemReaderJob() throws Exception {
        return jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(csvFileStep())
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getIems()))
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step csvFileStep() throws Exception {
        return stepBuilderFactory.get("csvFileStep")
                .<Person, Person>chunk(10)
                .reader(csvFileItemReader())
                .writer(itemWriter())
                .build();
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setNames("id","name","age","address"); // keys: [id, 이름, 나이, 거주지]를 각각의 값으로 참조?
        lineMapper.setLineTokenizer(tokenizer);

        // csv 1줄 읽어서 Person 객체로 맾핑
        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");            // tokenizer를 통해 setName한 값들로 접근한다
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(id,name,age,address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")                      // 이름
                .encoding("UTF-8")                              // 인코딩 타입
                .resource(new ClassPathResource("test.csv"))    // resource
                .linesToSkip(1)                                 // csv파일의 맨 첫 줄(keys) skip
                .lineMapper(lineMapper)                         // lineMapper
                .build();

        itemReader.afterPropertiesSet();    // 필수설정값이 정상적으로 설정되었는지 검증 -> lineMapper가 null인지 확인
        return itemReader;
    }

    private ItemWriter<Person> itemWriter() {
        // ItemWriter 는 void반환 타입의 추상매소드 하나를 가지는 인터페이스로 Consumer로 생각해도 될 듯
        return items -> log.info( items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", "))
        );
    }

    private List<Person> getIems() {
        List<Person> items = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            items.add(new Person(i+1 ,"TestName : " + i, "TestAge : " + i, "TestAddress : " + i));
        }
        return items;
    }
}
