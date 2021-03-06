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
        /* ??????????????? select - insert??? ???????????? 100?????? ????????????. ???????
        * usePersist(true) ????????? ????????? entityManger.merge()??? ???????????????, entity??? ?????? ???????????? ???????????? ?????? select?????? ???????????? update, ???????????? insert??????. ????????? ????????? ??? ??????????????? ?????????.
        *   ->  id?????? ????????? ????????? insert???????????????????????? ??????.
        * entityManger.persist()??? ?????? ?????? insert??????. ????????? ???????????????.
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
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())     // person ???????????? ???????????? parameter??? ????????? ??? ??????.
                .sql("insert into person(name, age, address) values(:name, :age, :address)")            // ??????????????? ????????? ?????????
                .build();

        itemWriter.afterPropertiesSet();
        return itemWriter;
    }

    private ItemWriter<Person> csvFileItemWriter() throws Exception {

        // ?????? -> ?????? ????????? ?????? ??????
        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"id", "name", "age", "address"});              // ??? ????????? ?????? write

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");   // csv??????????????? comma seperated
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Person> itemWriter = new FlatFileItemWriterBuilder<Person>()
                .name("csvFileItemWriter")
                .encoding("UTF-8")
                .resource(new FileSystemResource("output/test-output.csv"))       // Reader????????? ClassPathResource??? ?????????
                .lineAggregator(lineAggregator)                                         // ?????? ??????
                .headerCallback(writer -> writer.write("id, ??????, ??????, ?????????"))        // ?????? ??????
                .footerCallback(writer -> writer.write("==================\n"))     // footer ??????
                .append(true)                                                           // append true??? ????????? ????????? ??????????????? ?????? ??? ?????? ?????? ????????????.
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
            items.add(new Person("?????? " + i, "??????"+i, "??????"+i));
        }
        return items;
    }
}
