package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job itemReaderJob() throws Exception {
        return jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(csvFileStep())
                .next(jdbcStep())
                .next(jpaStep())
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getItems()))
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

    @Bean
    public Step jdbcStep() throws Exception {
        return stepBuilderFactory.get("jdbcStep")
                .<Person, Person>chunk(10)
                .reader( jdbcCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step jpaStep() throws Exception {
        return stepBuilderFactory.get("jpaStep")
                .<Person, Person> chunk(10)
                .reader(jpaCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    private JpaCursorItemReader<Person> jpaCursorItemReader() throws Exception {
        JpaCursorItemReader<Person> itemReader = new JpaCursorItemReaderBuilder<Person>()
                .name("personJpaCursorItemReader")
                .entityManagerFactory(entityManagerFactory)     // dataSource??? ?????? enetityManager??? ????????????.
                .queryString("select p from Person p")          // Native Query??? ?????? JPQL ????????? ??????????????????.
                .build();
        itemReader.afterPropertiesSet();                        // ?????????????????? ??????????????? ?????????????????? ??????
        return itemReader;
    }

    private JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        JdbcCursorItemReader<Person> jdbcCursorItemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)                                             // ???????????? DI??? DataSource??? ?????????
                .sql("select id, name, age, address from person")                   // ????????? ?????? db ??????
                .rowMapper((rs, rowNum) -> new Person(rs.getInt(1)      //  ????????? ???????????? ????????? ??????
                        , rs.getString(2)
                        , rs.getString(3)
                        , rs.getString(4)
                ))
                .build();
        jdbcCursorItemReader.afterPropertiesSet();                                  // ?????????????????? ??????????????? ?????????????????? ??????
        return jdbcCursorItemReader;
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        tokenizer.setNames("id","name","age","address"); // keys: [id, ??????, ??????, ?????????]??? ????????? ????????? ???????
        lineMapper.setLineTokenizer(tokenizer);

        // csv 1??? ????????? Person ????????? ??????
        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");            // tokenizer??? ?????? setName??? ????????? ????????????
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(id,name,age,address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")                      // ??????
                .encoding("UTF-8")                              // ????????? ??????
                .resource(new ClassPathResource("test.csv"))    // resource
                .linesToSkip(1)                                 // csv????????? ??? ??? ???(keys) skip
                .lineMapper(lineMapper)                         // lineMapper
                .build();

        itemReader.afterPropertiesSet();    // ?????????????????? ??????????????? ?????????????????? ?????? -> lineMapper??? null?????? ??????
        return itemReader;
    }

    private ItemWriter<Person> itemWriter() {
        // ItemWriter<T> ??? void?????? ????????? ??????????????? ????????? ????????? ?????????????????? Consumer<T>??? ???????????? ??? ???
        return items -> log.info(items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", "))
        );
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            items.add(new Person(i+1 ,"TestName : " + i, "TestAge : " + i, "TestAddress : " + i));
        }
        return items;
    }
}