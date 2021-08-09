package fastcampus.spring.batch.part4;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface UserRepository extends JpaRepository<User, Long> {


    Collection<User> findALlByUpdatedDate(LocalDate now);

    @Query(value = "select min(u.id) from User u")
    long findMinId();

    @Query(value = "select max(u.id) from User u")
    long findMaxId();
}
