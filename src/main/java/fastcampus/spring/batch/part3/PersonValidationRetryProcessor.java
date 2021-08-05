package fastcampus.spring.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

/**
 * RetryTemplate을 이용해서 retry를 처리한다!
 */
@Slf4j
public class PersonValidationRetryProcessor implements ItemProcessor<Person, Person> {

    private final RetryTemplate retryTemplate;

    public PersonValidationRetryProcessor() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(3)
                .retryOn(NotFoundNameException.class)
                .withListener(new ServerPersonRetryListener())
                .build();
    }

    @Override
    public Person process(Person item) throws Exception {
        return this.retryTemplate.execute(context -> {
          // RetryCallback -> 기본적인 process를 실행한다(이부분이 retry됨)
          if(item.isNotEmptyName()) {
              return item;
          }
          throw new NotFoundNameException();
        }, context -> {
            // RecoveryCallback -> 허용 횟수를 초과해서 Exception이 발생하면 Recovery Callback으로 processing 하게된다.
            return item.unknown();
        });
    }

    public static class ServerPersonRetryListener implements RetryListener {

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            // retry를 시작하는 설정, return true여야 retry가 적용된다.
            // 이게 false가 된 채로 retryTemplate에 적용되어서 step에 적용되면(예제처럼 processor에 적용 했다면) 한번도 제대로 실행되지 않고 Exception 뜨고 멈춘다.
            log.info("on open");
            return false;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            // retry 종료 후 적용
            log.info("on close");
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            // retry에 정의한 Exception 발생 시 적용
            log.error("on error");
        }
    }
}
