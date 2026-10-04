package tc.impact.farmmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_bn", length = 100)
    private String nameBn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoryType type;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_default", nullable = false)
    private boolean systemDefault = false;

    private Integer displayOrder = 100;

    // ============ NEW: default unit + price ============

    /**
     * Default unit for this category. When a user picks this category
     * in a sale form, this unit is pre-selected.
     * Only meaningful for PRODUCT-type categories.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_unit_id")
    private Unit defaultUnit;

    /**
     * Default price per unit. Used to pre-fill the unit price
     * in sale forms. Nullable — user can always override.
     * Only meaningful for PRODUCT-type categories.
     */
    @Column(name = "default_price", precision = 12, scale = 2)
    private BigDecimal defaultPrice;

    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDate.now();
        if (this.displayOrder == null) this.displayOrder = 100;
    }

    public enum CategoryType {
        EXPENSE, PRODUCT
    }
}