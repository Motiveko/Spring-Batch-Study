# Spring-Batch-Study
스프링 배치 학습을 위한 프로잭트

강의자료 - fastcampus 대규모 서비스를 위한 스프링 Cloud와 Batch

<br><br>

### 데이터베이스 셋업

 1.  docker를 이용한 실행 전 local 환경 mysql 셋업(yml 3307로 설정되어있음)

```
docker run -d -e MYSQL_ALLOW_EMPTY_PASSWORD=true --rm -v {LOCAL_DIRECTORY}:/var/lib/mysql --name mysql -p {LOCAL_PORT}:3306 mysql
```

```
docker run -d -e MYSQL_ALLOW_EMPTY_PASSWORD=true --rm -v /Users/donggi/Desktop/Programing/mysql_tmp:/var/lib/mysql --name mysql -p 3307:3306 mysql
```

2. spring_batch database 생성
``` 
create database spring_batch;
```

3. spring batch 에서 제공하는 mysql sql실행



<br><br>


## JobParameters 

---
> JobParameters를 사용하여 외부에서 필요한 parameter를 주입해 Batch Job을 유동적으로 만들 수 있다.

<br>

1. 주입 방법
- Application 실행 시 Program Argument로 -{KEY}={VALUE} 형태로 주입
- 예 : -chunkSize=10

2. 접근 방법
- `stepExecution.getJobParameters()`로 JobParameters 객체에 접근
- SpringEL을 이용한 접근 -> ( `@Value("#{jobParameters[key]}")`) 
    - springEL을 활용한 접근에는 `Scope` 어노테이션이 필수로 적용되어야 한다.

3. JobParameters를 사용하기 위해서는 @Scope 관련 어노테이션이 필수다. 이유는 Bean의 생성시점때문

<br><br>

## @JobScope, @StepScope
---
> @Scope을 이용해, Job, Step 단위에서 Bean의 LifeCycle을 관리할 수 있다.(@Bean이 붙은 곳에서만 사용 가능)

<br>

1. @JobScope
 - @Scope( value="job", proxyMode = ScopedProxyMode.TARGET_CLASS)와 동일
 - Job 실행 시점에 Bean을 생성한다.
 - 따라서 Job 하위의 Step에서 사용 가능

<br>

2. @StepScope
 - @Scope( value="step", proxyMode = ScopedProxyMode.TARGET_CLASS)와 동일
 - Step 실행 시점에 Bean을 생성한다.
 - 따라서 Step 하위의 tasklet, itemReader | Processor | Writer 에서 사용 가능하다.

<br>

3. SpringEL을 이용한 JobParameter로의 접근
 - Spring EL 이 jobParameters 객체에 접근하는것은 @Scope의 라이프사이클에 의해 동작된다.
 - 따라서 SpringEL을 사용하기 위해서는 @Scope가 필수
 - 매 step/job 마다 새로운 Bean을 생성하고, 이 때 SpringEL을 이용해 jobParametes에 접근한다.
 - 매 Job/Step마다 새로운 Bean을 실행하기때문에 ThreadSafe한 배치를 구현할 수 있다.

<br>

## Intellij template

---

> Itellij의 template 기능을 활용해 Configuration 클래스를 템플릿으로 만들 수 있다

<br>

-  Tools > SaveFileAsTemplate으로 파일을 템플릿으로 저장 후 새로운 클래스 생성시 해당 템플릿을 생성하고 변수명 입력하는 방식

<br>

## ItemReader
> ItemReader는 Batch에서 어떤 대상(DB, file, network)에서 데이터를 읽는 역할을 수행한다.

- Step에서 ItemReader는 필수
- Spring Batch에서 기본 재공하는 `ItemReader`의 구현체
    - file, jdbc, jpa, hibernate, kafka, ...
- ItemReader 인터페이스를 직접 구현하는 구현체를 사용해도 된다.
- ItemReader는 read()가 null을 반환하는 순간 chunk의 반복이 끝난다.
- JpaCursorItemReader 등이 상속하는 AbstractItemStreamItemReader는 ItemReader와 ItemStream을 구현하는데, ItemStream은 update()로 `ExecutionContext에` 정보를 저장한다.

<br>

### 1. FlatFileItemReader 
> File을 읽는데 쓰이는 ItemReader 구현체, Csv등을 읽을 수 있다.

설정요소 
- name : ItemReader의 name
- encoding : encoding 방식
- resource : 읽을 파일
- lineToSkip : 데이터의 시작 열, default : 1로하면 첫줄(keys)는 제외하고 읽는다
- lineMapper : lineMapper, 핵심
    - tokenizer객체를 이용해 keys를 우리가 원하는 값으로 맵핑한다
    ```
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        
        // csv의 컬럼 id, 이름, 연령, 주소 -> id, name, age, address로 참조가 가능해진다
        tokenizer.setNames("id","name","age","address");
        
        lineMapper.setLineTokenizer(tokenizer);
    ```
    - fieldSetMapper를 이용해 읽은 값을 원하는 객체로 맵핑한다
    ```
        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");            // tokenizer를 통해 setName한 값들로 접근한다
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            return new Person(id,name,age,address);
        });
    ``` 
- afterPropertiesSet()로 필수값이 올바르게 설정되었는지 검사한다. lineMapper가 null이면 throw Excpetion
```
itemReader.afterPropertiesSet();
```

### 2. JdbcItemReader
> Jdbc를 이용해 db에서 데이터를 읽어올 수 있다. JdbcCursorItemReader, JdbcPagingItemReader가 있다

JdbcCursorItemReader
 - cursor 기반 JdbcItemReader
 - 배치가 끝날때까지 connection 유지
 - 한번의 connection으로 처리하기때문에 성능은 좋으나 긴 connection 시간과, thread safe 하지 않음
 - 모든 결과를 메모리에 할당하기 때문에 많은 메모리 사용

JdbcPagingItemReader
 - 페이징 단위로 Db Connection
 - 여러번의 Connection을 하기때문에 성능은 낮으나 짧은 connection 시간과 thread safe함
 - 페이징 단위의 결과만 메모리에 할당하기때문에 메모리 사용 적음

JdbcCursorItemReader 설정 요소
 - name: ItemReader의 이름
 - datasource : 연결할 DataSource
 - sql : 조회할 쿼리
 - rowMapper / beanMapper : 쿼리 실행 결과를 객체에 맵핑
 - afterPropertiesSet : 필수값이 올바르게 설정되었는지 검사. throws Exception()

### 3. JpaItemReader
> Jpa기반으로 db를 조회할 수 있는 itemReader. 역시 JpaCursorItemReader, JpaPagingItemReader가 있다.

jdbc와 사용법이 비슷하기에 차이점만 정리
 | JdbcItemReader | JpaItemReader |
|---|:---:|
| datasource | entityManager | 
| native Query | jpql Query |
| row/bean Mapper | Entity 객체 |


<br>

## ItemWriter

> ItemWriter는 Batch에서 Db, file등에 write/update/delete 등을 수행하거나 발송 대상에게 메일블 보내는 등의 배치 데이터의 최종 처리를 수행한다. Chunk방식 Step에서 필수값.

### 1. FlatFileItemWriter
 > File을 쓰는데 쓰이는 ItemWriter 구현체, Csv등을 쓸 수 있다.

설정요소
 - name : writer의 이름
 - encoding : 인코딩 설정
 - resource : FileSystemResource 객체로 output 경로 설정
 - lineAggregator : 핵심, output 객체와 write할 파일을 맵핑한다.
```
        BeanWrapperFieldExtractor<Person> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"id", "name", "age", "address"});              // 이 순서로 객체 필드에서 가져와 write

        DelimitedLineAggregator<Person> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");   // csv이기때문에 comma seperated
        lineAggregator.setFieldExtractor(fieldExtractor);
```

 - headerCallback : 헤더 설정 ( FlatFileHeaderCallback)
 - footerCallback : 푸터 설정 ( FlatFileFooterCallback)
 - append : true로 설정 시 같은 resource에 쓰기 시 연결해서 쓴다.


rewriteBatchedStatements=true => 벌크 insert를 사용하기 위한 mysql 옵션

<br>

### 2. JdbcBatchItemWriter
> jdbc를 이용해 db에 insert/update/delete등을 수행. 

설정 요소 
 - datasource: datasource
 - itemSqlParameterSourceProvider : processor에서 받은 객체를 쿼리의 parameter로 맵핑해주는 설정
 - sql : 쿼리

<br>

### 3. JpaItemWriter 
> entityManager를 통해 db에 insert/update/delete등을 수행. JdbcBatchItemWriter처럼 bulk로 처리하지 않고 단건단건 수행한다.

> functional interface 중 Consumer<T> 와 같다.( void write(T) )

설정 요소

 - entityManagerFactory : entityManager를 생성하는 factory  설정
 - usePersist(boolean) : 
    - 기본값 false, .merge() 실행 시 entity에 id값이 존재하면 update/insert 여부를 판별하기 위해 해당 entity의 id로 select를 해본다. 성능이 구려진다.
    - false : entityManager.merge()
    - true : entityManager.persist() 
    
### 4. (추가) CompositeItemWriter
> 여러개의 ItemWriter를 동시에 사용할 수 있게 하는 ItemWriter
-  List<ItemWriter> 를 만들어 ItemWriter추가 후 personCompositeItemWriter.setDelegates(delegates) 해주기만 하면 된다.

<br><br>

## ItemProcessor 
> Item Reader에서 읽은 item들을 처리 후 output의 junksize 크기의 list형태로 ItemWriter에 넘겨준다. Chunk Process에서 필수는 아니고, ItemProcessor의 로직이 Writer/Reader에 존재할 수 있으나 명확한 책임 분리를 위해 사용.

> return null시 해당 item은 Writer에 넘기지 않는다.
> functional interface 중 Function<T,R> 과 같다. ( R process(T) )

<br><br>

## Test Spring Batch

1. JobLauncher 로 Job과 Step을 실행하게 된다.

2. JobLauncherTestUtils class는 내부적으로 JobLaucher를 포함하고 있어 Test code에서 자유롭게 Job, Step을 실행할 수 있다.


<br><br>

## Listener
> 스프링 배치에서 전/후 처리 하는 다양한 Listener가 존재한다. 
 - 스프링 배치에서 제공하는 Interface 구현체 Listener
 - Annotation기반 Listener

<br>

### 1. JobExecutionListener
> Job의 실행 전,후 처리를 위한 Listener
 - Interface 구현체 기반 Listener
 ```
class SavePersonJobExecutionListener implements JobExecutionListener {
        @Override
        public void beforeJob(JobExecution jobExecution) {
            // job 전처리
        }

        @Override
        public void afterJob(JobExecution jobExecution) {
            // job 후처리
        }
}
 ```

 - annotation 기반 Listener

 ```
    public static class SavePersonAnnotationJobExecution {
        @BeforeJob
        public void beforeJob(JobExecution jobExecution) {
            // Job 전처리
        }

        @AfterJob
        public void afterJob(JobExecution jobExecution) {       
            // Job 후처리
        }
    }
 ```

 - 설정
  > job bean에 설정, 여러개의 listener을 연달아 설정하면 내부적으로 List\<Listener\> 로 가지고 설정한 순서대로 실행된다.

  ```
    jobBuilderFactory.get("savePersonJob")
        .listener(new SavePersonJobExecutionListener())
        .listener(newSavePersonAnnotationJobExecution())
        .build()
  ```


### 2. StepExecutionListener
> Step의 실행 전,후 처리를 위한 Listener, JobExecutionListener와 설정, 작동 원리가 같다.

 - Interface 구현체 기반 Listener
 ```
    class SavePersonStepExecutionListener implements StepExecutionListener {
        @Override
        public void beforeStep(StepExecution stepExecution) {
            // Step 전처리
        }

        @Override
        public void afterStep(StepExecution stepExecution) {
            // Step 후처리
        }
}
 ```

 - annotation 기반 Listener

 ```
    class SavePersonAnnotationStepExecution {
        @BeforeStep
        public void beforeStep(StepExecution stepExecution) {
            // Step 전처리
        }

        @AfterStep
        public void afterStep(StepExecution stepExecution) {       
            // Step 후처리
        }
    }
 ```

 - 설정
  > step bean에 설정, 여러개의 listener을 연달아 설정하면 내부적으로 List\<Listener\> 로 가지고 설정한 순서대로 실행된다.

  ```
    StepBuilderFactory.get("savePersonStep")
        .listener(new SavePersonStepExecutionListener())
        .listener(SavePersonAnnotationStepExecution())
        .build()
  ```

<br><br>

## StepListener
> Step 전 후의 처리를 담당하는 Listener. StepExecutionListener도 StepListener의 구현체 중 하나이다.

- SkipListener 
    - Skip 이란 : Step의 예외처리 방법 중 하나
    - onSkipInRead: @OnSkipInRead
        - ItemReader에서 Skip이 발생한 경우 호출
    - onSkipInWrite: @OnSkipInWrite
        - ItemWriter에서 Skip이 발생한 경우 호출
    - onSkipInProccess: @OnSkipInProccess
        - ItemProccessor에서 Skip이 발생한 경우 호출

- ItemReaderListener
    - 아이템 읽기 전,후,중에 호출
    - beforeRead: @BeforeRead
        - ItemReader.read() 메소드 호출 전 호출
    - afterRead: @AfterRead
        - ItemReader.read() 메소드 호출 후 호출
    - OnReadError: @OnReadError
        - ItemReader.read()중 에러 발생 시 호출                

- ItemProcessListener
    - ItemReaderListener 참고

- ItemWriterListener
    - ItemReaderListener 참고

- ChunkListener
    - chunk 실행 전,후,중에 호출
    - ItemReaderListener 참고

- RetryListener
    - Retry 부분 참고


<br><br>

## 예외처리

### Skip(건너뛰기)
> skip은 데이터가 없는 등의 재실행 하지 않을 특정 에러의 처리에 대해 일정 횟수만큼 허용하는 방식으로 에러를 처리한다.

- StepBuilder.faultTolerant().skip(Exception.class).skipLimit(N)
- 허용 횟수를 초과한 Exception발생시 해당 Step은 실패한것으로 처리되는데, Step은 Chunk 1개 기준으로 transaction이 발생하기때문에, 실패한 chunk 전 후의 성공 chunk rollback되지 않는다.
    - 예) 10개의 chunk을 수행하는 step이 있다고 가정할 때, 10번째 chunk에서 실패하게 되면, 1-9 chunk 성공처리
    - 그러나 1개의 chunk 실패했기때문에, 해당 step은 실패처리 되는데, 후에 step을 재실행 할 때 성공한 1-9의 chunk는 재시작 하지 않게 설계해야한다.

- SkipListner 가 실행되는 조건
    - 에러 발생 횟수가 skipLimit()으로 설정한 값 이하
    - faultTolerant() 이하에 SkipListener를 등록해줘야 한다.

<br>

### Retry(재시도)
> db데드락, 네트워크 타임아웃 등의 간헐적으로 발생하지만, 재시도하면 성공할 수 있는 에러의 경우 retry로 처리한다.

- skip과 설정이 비슷하다. Step 수행 중 Exception 발생시 재시도 설정
    - StepBuilder.faultTolerant().retry(Exception.class).retryLimit(N)
- RetryTemplate 을 사용하면 재시도 후 횟수가 초과하면 특정 동작을 하게 만들 수 있다.

- retryTemplate.execute( retryContext -> {}, recoveryContext -> {}) 를 하는데, recoveryCallback은 기본으로 실행하는 작업이고, 여기서 retryLimit 수 만큼 재시도 한 후 recoveryCallback으로 넘어가게 된다.

- RetryListener
    - `RetryListener.open`: open()
        - return true -> RetryTemplate.Callback
        - return false -> RetryListener.close
    - RetryTemplate.RetryCallback
    - `RetryListener.error`: error()
        - RetryTemplate.Callback 에서 에러 발생 시 max Attemps 설정값 만큼 반복
    - RetryTemplate.RecoveryCallback
        - max Attemps 값만큼 시도 후에도 에러 발생 시 실행
    - `RetryListener.close`: close()
    
    

## 예제/실전 개발 중 추가 사항 정리

### FlatFileItemWriter 의 작동 시점
-  어떤 chunkprocess에서 결과값을 csv등의 File로 저장할 대, FlatFileItemWriter는 매 chunk마다 file을 생성할까?
    > 그렇지 않다! FlatFileItemWriter는 write()parameter인 List를 memory에 모두 저장했다가  step이 끝나는 시점에 write를 실행한다!
     
    > 따라서 chunk사이즈로 인해 read/process가 여러번이뤄지고 write도 여러번 이뤄지는것에 대한 걱정은 하지 않아도 된다
 
 <br><br>

### JobExecutionDecider
> 배치 실행 시 상태에 따라 배치를 실행할 지 결정하는 인터페이스

- decide() 구현체는 FlowExecutionStatus를 결정하고, 이 상태에 따라 배치 실행 여부를 결정할 수 있다.
    - 예제는 jobParameters에 date가 있을 때 orderStatisticsStep을 실행시키고 없으면 시키지 않는다.
- 설정 방법은jobBuilder에서 
    - .decide({{JobExecutionDecider구현체}})  -> decider 주입   
    - .on(STATUS)                           -> 원하는 STATUS
    - .to(Step)                             -> 결과가 일치하면 실행할 Step
    - .build()                              -> FlowBuilder -> FlowJobBuilder로 

<br><br>

### UserConfiguration에서 성능 개선을 위한 수정

- 먼저 실행되어야 할 Step 먼저 실행
- 서로 영향없는 Step의 동시실행
- Step별 성능평가
    - 40,000건의 고객 데이터 저장 및 levelup, 일별 금액집계 
    - step별 소요시간
        - SimpleStep
            - 기본스탭
            - 9763, 10055, 9774
        - AsyncStep
            - ItemProcessor와 ItemWriter를 기준으로 Async처리되어 실행한다.
            - Future 기반 asynchronous -> processor가 output을 Future<T>형태로 감싼다.
            - Async를 사용하기 위해 spring-batch-integration필요하다.
            - 9997, 9968, 9993
        - Multi-Thread Step
            - chunk 기준으로 multi-thread처리되어 실행
            - cursor기반 jpa 등 thread-safe하지 않은 itemWriter사용시 적용하면 안됨
            - 적용을 원하는 step에 taskExecutor설정해주고 throttleLimit으로 thread수 설정할 수 있다.
            - 7101
        - Partition Step
            - step기준으로 partition되어 실행(점점 적용범위가 넓어진다.)
            - Master Step이 여러개의 SalveStep을 만들어 전체 건을 나눠처리한다.
                - chunk로 나누는거랑 뭐가 다른걸까? -> partition으로 나누고 chunk로 한번 더 나누게 된다. 이 때 partition으로 쪼개진 스탭은 비동기로 처리된다고한다.
            - Salve Step은 각각 하나의 Step으로 동작한다
            - 7961, 7900, 7891
        - Async + Partition Step
            - PartitionStep에 ItemProcessor와 Writer만 Async걸로 바꿔준 방법
            - 실행시간 PartitionStep과 거의 비슷하다.
        - Parallel Step
        - Partition + Parallel Step
