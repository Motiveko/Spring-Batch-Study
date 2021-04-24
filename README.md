# Spring-Batch-Study
스프링 배치 학습을 위한 프로잭트

강의자료 - fastcampus 대규모 서비스를 위한 스프링 Cloud와 Batch








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





