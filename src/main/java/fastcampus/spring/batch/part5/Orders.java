package fastcampus.spring.batch.part5;

import fastcampus.spring.batch.part4.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;

    private int amount;

    private LocalDate createdDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Orders(String itemName, int amount, LocalDate createdDate) {
        this.itemName = itemName;
        this.amount = amount;
        this.createdDate = createdDate;
    }
}
