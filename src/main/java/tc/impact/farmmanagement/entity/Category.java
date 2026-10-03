package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "code", "type"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 50)
    private String code;

    /**
     * Display name in English (or primary language).
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Optional Bengali display name (or secondary language).
     */
    @Column(name = "name_bn", length = 100)
    private String nameBn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    /**
     * When false, category is hidden from dropdowns but historical
     * records still reference it correctly.
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Prevents deletion of system-default categories (FEED, MILK, etc.).
     * Users can deactivate but not delete these.
     */
    @Column(name = "system_default", nullable = false)
    private boolean systemDefault = false;

    private Integer displayOrder = 100;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.displayOrder == null) {
            this.displayOrder = 100;
        }
    }

    public enum CategoryType {
        EXPENSE,   // for expenses
        PRODUCT    // for sale products
    }
}