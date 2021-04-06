package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.*;

import java.util.List;

@RequiredArgsConstructor
public class CustomItemReader<T> implements ItemReader<T> {

    private final List<T> items;

    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if(!items.isEmpty()) {
            return items.remove(0);
        }
        // ItemReader가 null을 반환할 때 정크 반복이 끝난다
        return null;
    }
}
