package fastcampus.spring.batch.part6;

import fastcampus.spring.batch.part4.*;
import fastcampus.spring.batch.part5.JobParametersDecide;
import fastcampus.spring.batch.part5.OrderStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class PartitionAsyncUserConfiguration {

    private final String JOB_NAME = "partitionAsyncUserJob";
    private final int CHUNK_SIZE = 1_000;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final EntityManagerFactory entityManagerFactory;

    private final DataSource dataSource;

    private final TaskExecutor taskExecutor;

    // 실무에선 이렇게 하나의 job에 성격이 다른 여러 step을 연결하지 않는다
    @Bean(JOB_NAME)
    public Job userJob() throws Exception {
        return this.jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .start(clearUserStep())
                .next(saveUserStep())
                .next(this.userLevelUpManagerStep())
                .listener(new LevelUpJobExecutionListener(userRepository))
                .next(new JobParametersDecide("date"))
                .on(JobParametersDecide.CONTINUE.getName())  // 결과가 JobParametersDecide.CONTINUE.getName() 와 같으면 이하 to() 내의 step을 실행한다.
                .to(orderStatisticsStep(null))
                .build()
                .build();
    }

    @Bean(JOB_NAME + "orderStatisticsStep")
    @JobScope
    public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) throws Exception {
        return stepBuilderFactory.get(JOB_NAME + "orderStatisticsStep")
                .<OrderStatistics, OrderStatistics>chunk(CHUNK_SIZE)
                .reader(orderStatisticsItemReader(date))
                .writer(orderStatisticsItemWriter(date))
                .build();
    }

    /**
     * 한달간의 Orders 데이터를 읽어 일별 금액(amount)의 총합을 읽어들인다
     * @param date
     * @return
     * @throws Exception
     */
    private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("startDate", yearMonth.atDay(1) );   // date 달의 1일
        parameters.put("endDate", yearMonth.atEndOfMonth());           // date 달의 마지막일

        // 정렬 -> created_date 기준 오름차순정렬
        Map<String, Order> sortKey = Collections.singletonMap("created_date", Order.ASCENDING);

        JdbcPagingItemReader<OrderStatistics> itemReader = new JdbcPagingItemReaderBuilder<OrderStatistics>()
                .dataSource(this.dataSource)
                .rowMapper((rs, rowNum) -> OrderStatistics.builder()
                        .amount(rs.getString(1))
                        .date(LocalDate.parse(rs.getString(2), DateTimeFormatter.ISO_DATE))
                        .build())
                .pageSize(CHUNK_SIZE)
                .name(JOB_NAME + "_orderStatisticsItemReader")
                .selectClause("sum(amount), created_date")
                .fromClause("orders")
                .whereClause("created_date >= :startDate and created_date <= :endDate")
                .groupClause("created_date ")
                .parameterValues(parameters)
                .sortKeys(sortKey)
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }

    /**
     * orderStatisticsItemReader에서 읽은 데이터를 기준으로 csv 파일을 생성한다.
     * @param date
     * @return
     */
    private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception {

        YearMonth yearMonth = YearMonth.parse(date);

        String fileName = String.format("%s년_%d월_일별_주문_금액.csv", yearMonth.getYear(), yearMonth.getMonthValue());

        BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"amount", "date"});

        DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
                .resource(new FileSystemResource("output/" + fileName))
                .lineAggregator(lineAggregator)
                .name(JOB_NAME + "_orderStatisticsItemWriter")
                .encoding("UTF-8")
                .headerCallback(writer -> writer.write("total_amount, date"))
                .build();

        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    @Bean(JOB_NAME + "_clearUserStep")
    public Step clearUserStep() {
        return stepBuilderFactory.get(JOB_NAME + "_clearUserStep")
                .tasklet(new ClearUserStep(userRepository))
                .build();
    }

    @Bean(JOB_NAME + "_saveUserStep")
    public Step saveUserStep() {
        return stepBuilderFactory.get(JOB_NAME + "_saveUserStep")
                .tasklet(new SaveUserTasklet(userRepository))
                .build();
    }


    @Bean(JOB_NAME + "_userLevelUpStep")
    public Step userLevelUpStep() throws Exception {
        return stepBuilderFactory.get(JOB_NAME + "_userLevelUpStep")
                .<User, Future<User>>chunk(CHUNK_SIZE)
                .reader(itemReader(null, null))           // db에서 User 정보 가져온다
                .processor(itemProcessor())     // 등급 up
                .writer(itemWriter())           // 다시저장
                .build();
    }

    // 이것이 마스터스탭
    @Bean(JOB_NAME + "_userLevelUpStep.manager")
    public Step userLevelUpManagerStep() throws Exception {
        return this.stepBuilderFactory.get(JOB_NAME + "_userLevelUpStep.manager")
                .partitioner(JOB_NAME + "_userLevelUpStep", new UserLevelUpPartitioner(userRepository))
                .step(userLevelUpStep())
                .partitionHandler(taskExecutorPartitionHandler())
                .build();
    }

    /**
     * 파티션을 핸들링 할 수 있는 객체,
     * @return
     * @throws Exception
     */
    @Bean(JOB_NAME + "_taskExecutorPartitionHandler")
    PartitionHandler taskExecutorPartitionHandler() throws Exception {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(userLevelUpStep());
        handler.setTaskExecutor(this.taskExecutor);
        handler.setGridSize(8);

        return handler;
    }

    /**
     * itemReader에서 ExecutionContext를 사용하기 위해서는 @StepScope가 필요하고, @StepScope를 사용하려면 @Bean이 필요하다.
     * @StepScope를 쓰면 return Type을 interface가 아니라 구현체로 정확히 명시해줘야한다.
     * 그 이유는 proxy로 설정이 되기때문이라는데, 정확히는 강사도 모르는듯. 안하면 어쨋든 NullPointException 존나게뜬다.
     */
    @Bean(JOB_NAME + "_itemReader")
    @StepScope
    JpaPagingItemReader<User> itemReader(@Value("#{stepExecutionContext[minId]}") Long minId,
                                          @Value("#{stepExecutionContext[maxId]}") Long maxId) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minId", minId);
        parameters.put("maxId", maxId);
        JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
                .queryString("select u from User u where u.id between :minId and :maxId")
                .parameterValues(parameters)
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .name(JOB_NAME + "_userItemReader")
                .build();
        itemReader.afterPropertiesSet();

        return itemReader;
    }

    // Function<User,User>
    private AsyncItemProcessor<User, User> itemProcessor() {
        ItemProcessor<User, User> itemProcessor = user -> {
            if( user.availableLevelUp()) {
                return user;
            }
            // 등급 상향 대상이 아니라면, null을 return(처리를 하지 않는다)
            return null;
        };

        AsyncItemProcessor<User, User> asyncItemProcessor = new AsyncItemProcessor<>();
        // 기본 itemProcessor를 setDelegate로 감싼다.
        asyncItemProcessor.setDelegate(itemProcessor);
        // 선언해놓은 taskExecutor 빈을 주입한다.
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        return asyncItemProcessor;
    }

    // Consumer<User>
    private AsyncItemWriter<User> itemWriter() {
        ItemWriter<User> itemWriter = users -> users.forEach(x -> {
            x.levelUp();
            userRepository.save(x);
        });

        AsyncItemWriter<User> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(itemWriter);

        return asyncItemWriter;
    }

}
