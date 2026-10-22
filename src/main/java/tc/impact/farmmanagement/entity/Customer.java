package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "phone"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_type_id")
    private CustomerType customerType;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
    }

    @Column(name = "is_guest")
    private boolean guest = false;

    public boolean isGuest() { return guest; }
    public void setGuest(boolean guest) { this.guest = guest; }
}