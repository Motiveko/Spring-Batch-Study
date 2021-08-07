package fastcampus.spring.batch.part4;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Enumerated(EnumType.STRING)
    private Level level = Level.NORMAL;

    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Orders> orders;

    private LocalDate updatedDate;

    @Builder
    public User(String username, List<Orders> orders) {
        this.username = username;
        this.orders = orders;
    }

    public boolean availableLevelUp() {
        return Level.availableLevelUp(this.getLevel(), this.getTotalAmount());
    }

    private int getTotalAmount() {
        return this.orders.stream()
                .mapToInt(Orders::getAmount)
                .sum();
    }

    public Level levelUp() {
        Level nextLevel = Level.getNextLevel(this.getTotalAmount());
        this.level = nextLevel;
        this.updatedDate = LocalDate.now();

        return nextLevel;
    }

    public enum Level {
        VIP(500_000, null),
        GOLD(500_000, VIP),
        SILVER(300_000, GOLD),
        NORMAL(200_000, SILVER);

        private final int nextAmount;
        private final Level nextLevel;

        Level(int nextAmount, Level nextLevel) {
            this.nextAmount = nextAmount;
            this.nextLevel = nextLevel;
        }

        private static boolean availableLevelUp(Level level, int totalAmount) {
            if(Objects.isNull(level)) {             // 걍 null처리인듯
                return false;
            }
            if( Objects.isNull(level.nextLevel)) {  // VIP
                return false;
            }
            return totalAmount >= level.nextAmount;
        }

        private static Level getNextLevel(int totalAmount) {
            if(totalAmount >= VIP.nextAmount) {
                return VIP;
            } else if (totalAmount >= GOLD.nextAmount) {
                return GOLD.nextLevel;
            } else if (totalAmount >= SILVER.nextAmount) {
                return SILVER.nextLevel;
            }else if (totalAmount >= NORMAL.nextAmount) {
                return NORMAL.nextLevel;
            }
            return NORMAL;
        }
    }
}
