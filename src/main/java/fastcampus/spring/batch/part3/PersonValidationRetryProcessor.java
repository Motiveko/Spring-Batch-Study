package fastcampus.spring.batch.part3;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

/**
 * RetryTemplate을 이용해서 retry를 처리한다!
 */
public class PersonValidationRetryProcessor implements ItemProcessor<Person, Person> {

    private final RetryTemplate retryTemplate;

    public PersonValidationRetryProcessor() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(3)
                .retryOn(NotFoundNameException.class)
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
}
