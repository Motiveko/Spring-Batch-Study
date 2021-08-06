package fastcampus.spring.batch.part4;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface UserRepository extends JpaRepository<User, Long> {


    Collection<User> findALlByUpdatedDate(LocalDate now);
}
