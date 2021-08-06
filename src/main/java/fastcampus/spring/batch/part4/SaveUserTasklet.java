package fastcampus.spring.batch.part4;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveUserTasklet implements Tasklet {

    private final UserRepository userRepository;

    public SaveUserTasklet(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<User> users = createUsers();

        Collections.shuffle(users);

        userRepository.saveAll(users);

        return RepeatStatus.FINISHED;
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        for ( int i = 0; i < 100; i++) {
            users.add(User.builder()
                .totalAmount(1_000)
                .username("test username" + i)
                .build());
        }
        for ( int i = 100; i < 200; i++ ) {
            users.add(User.builder()
                .totalAmount(200_000)               // 다음 회원 등급 조정 Step에서 이 값을 읽어 회원등급을 최초 NORMAL -> SILVER로 올릴것이다.
                .username("test username" + i)
                .build());
        }

        for ( int i = 200; i < 300; i++ ) {
            users.add(User.builder()
                    .totalAmount(300_000)           // 다음 회원 등급 조정 Step에서 이 값을 읽어 회원등급을 최초 NORMAL -> GOLD 올릴것이다.
                    .username("test username" + i)
                    .build());
        }
        for ( int i = 300; i < 400; i++ ) {
            users.add(User.builder()
                    .totalAmount(500_000)           // 다음 회원 등급 조정 Step에서 이 값을 읽어 회원등급을 최초 NORMAL -> VIP로 올릴것이다.
                    .username("test username" + i)
                    .build());
        }
        return users;
    }

}
