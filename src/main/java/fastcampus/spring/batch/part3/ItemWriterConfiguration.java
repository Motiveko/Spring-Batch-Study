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
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemWriterConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFcatory;


    @Bean
    public Job itemWriterJob() throws Exception {
        return jobBuilderFactory.get("itemWriterJob")
                .incrementer(new RunIdIncrementer())
                .start(this.csvItemWriterStep())
                // .next(this.jdbcBatchItemWriterStep())
                .next(this.jpaItemWriterStep())
                .build();
    }

    @Bean
    public Step csvItemWriterStep() throws Exception {
        return stepBuilderFactory.get("csvItemWriterStep")
                .<Person, Person>chunk (10)
                .reader(itemReader())
                .writer(csvFileItemWriter())
                .build();
    }

    @Bean
    public Step jdbcBatchItemWriterStep() {
        return stepBuilderFactory.get("jdbcBatchItemWriterStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public Step jpaItemWriterStep() throws Exception {
        return stepBuilderFactory.get("jpaItemWriterStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .writer(jpaitemWriter())
                .build();
    }

    private ItemWriter<? super Person> jpaitemWriter() throws Exception {
        /* 실행해보면 select - insert가 한셋트루 100세트 실행된다. 뭐냐?
        * usePersist(true) 설정을 안하면 entityManger.merge()가 실행되는데, entity가 수정 대상인지 확인하기 위해 select하고 있는거면 update, 없는거면 insert한다. 따라서 하나씩 다 조회해보고 이런다.
        *   ->  id할당 안하면 알아서 insert해야하는애라는걸 안다.
        * entityManger.persist()로 하면 그냥 insert한다. 성능상 이점이있다.
        *
        * */

        JpaItemWriter<Person> itemWriter = new JpaItemWriterBuilder<Person>()
                .entityManagerFactory(entityManagerFcatory)
                // .usePersist(true)
                .build();
        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    private ItemWriter<? super Person> jdbcBatchItemWriter() {
        JdbcBatchItemWriter<Person> itemWriter = new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(dataSource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())     // person 클래스를 자동으로 parameter로 설정할 수 있다.
                .sql("insert into person(name, age, address) values(:name, :age, :address)")            // 자연스럽게 맵핑이 되었따
                .build();

        itemWriter.afterPropertiesSet();
        return itemWriter;
    }

    private ItemWriter<Person> csvFileItemWriter() throws Exception {

        // 객체 -> 필드 맵핑을 위한 설정
        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"id", "name", "age", "address"});              // 이 순서로 값을 write

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");   // csv이기때문에 comma seperated
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Person> itemWriter = new FlatFileItemWriterBuilder<Person>()
                .name("csvFileItemWriter")
                .encoding("UTF-8")
                .resource(new FileSystemResource("output/test-output.csv"))       // Reader에서는 ClassPathResource로 설정함
                .lineAggregator(lineAggregator)                                         // 맵핑 설정
                .headerCallback(writer -> writer.write("id, 이름, 나이, 거주지"))        // 헤더 설정
                .footerCallback(writer -> writer.write("==================\n"))     // footer 설정
                .append(true)                                                           // append true를 해주면 중복된 파일명으로 생성 시 뒤에 계속 붙여준다.
                .build();

        itemWriter.afterPropertiesSet();
        return itemWriter;
    }

    private ItemReader<Person> itemReader() {
        return new CustomItemReader<Person>(getItems());
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();

        for( int i = 0 ; i < 100; i++) {
            items.add(new Person("이름 " + i, "나이"+i, "주소"+i));
        }
        return items;
    }
}
