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




