package fastcampus.spring.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchExampleApplication {


    public static void main(String[] args) {

        SpringApplication.run(SpringBatchExampleApplication.class, args);

/*        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse("+1 10-941-2319", "KR");
        Arrays.asList(
                "+1 917-941-2319",
                "+82 10-5165-2006",
                "+1 010-5165-2006",
                "+82 2-1234-5678",
                "+1 404-516-2006"
                )
                .stream()
                .map(phone -> {
                    try {
                        return phoneUtil.parse(phone, "KR");
                    } catch (NumberParseException e) {
                        e.printStackTrace();
                    } return null;
                })
                .map(phone -> phoneUtil.format(phone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
                .forEach(System.out::println);
        String pre = phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        System.out.println(pre);*/
    }

}
