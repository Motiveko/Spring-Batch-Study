package fastcampus.spring.batch.part4;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@RequiredArgsConstructor
public class ClearUserStep implements Tasklet {

    private final UserRepository userRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        this.userRepository.deleteAll();
        return RepeatStatus.FINISHED;
    }
}
