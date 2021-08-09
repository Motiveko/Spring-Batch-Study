package fastcampus.spring.batch.part6;

import fastcampus.spring.batch.part4.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class UserLevelUpPartitioner implements Partitioner {

    private final UserRepository userRepository;


    /**
     * 각 slaveStep 에서 ExecutionContext를 꺼내서 start, end 를 조회해 batch작업을 진행한다.
     * 이거는 1~40,000으로 순차적으로 하면 되는거라 이렇게 쉬운것 같고, 실무 배치에서는 비즈니스 로직에 따라 생각해서 만들어야할듯
     * @param gridSize : slave의 사이즈이다
     * @return {PartitionN : ExecutionContext}
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        long minId = userRepository.findMinId();    // 1
        long maxId = userRepository.findMaxId();    // 40000

        long targetSize = (maxId - minId) / gridSize + 1; // 각 slave step에서 처리할 아이템의 갯수 ( 5000)

        /**
         * partition0 : 1, 5000
         * partition1 : 5001, 10000
         * ...
         * partition7 : 35001, 40000
         */
        Map<String, ExecutionContext> result = new HashMap<>();

        long number = 0;

        long start = minId;

        long end = start + targetSize - 1;

        while( start <= maxId ) {
            ExecutionContext value = new ExecutionContext();
            result.put("partition" + number, value);
            if( end >= maxId ) {
                end = maxId;
            }
            value.putLong("minId", start);
            value.putLong("maxId", end);

            start += targetSize;
            end += targetSize;
            number++;
        }


        return result;
    }
}
