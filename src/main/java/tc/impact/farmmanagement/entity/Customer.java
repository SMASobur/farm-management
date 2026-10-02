package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String phone;

    private String address;

    @Enumerated(EnumType.STRING)
    private CustomerType type;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.type == null) {
            this.type = CustomerType.HOUSEHOLD;
        }
    }

    public enum CustomerType {
        HOUSEHOLD,   // বাড়িতে ব্যবহারের জন্য
        SHOP,        // দোকান
        TEA_STALL,   // চায়ের দোকান
        RESTAURANT,  // রেস্টুরেন্ট
        WHOLESALER,  // পাইকার
        OTHER
    }
}