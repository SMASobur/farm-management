package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "units",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 20)
    private String code;   // e.g., "LITER", "MANA", "TON"

    @Column(nullable = false, length = 50)
    private String name;   // e.g., "Liter", "মণ", "Ton"

    @Column(name = "name_bn", length = 50)
    private String nameBn;

    /**
     * Optional: abbreviation shown in compact UIs (e.g., "L", "কেজি").
     */
    @Column(length = 10)
    private String abbreviation;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault = false;

    private Integer displayOrder = 100;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.displayOrder == null) this.displayOrder = 100;
    }
}