package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Mat hang. Loai hang luu o cot category. */
@Getter
@Setter
@Entity
@Table(name = "items")
public class Item extends BaseEntity {

    private String category;
    private String code;

    @Column(name = "pos_code")
    private String posCode;

    private String name;
    private String unit;

    /** Moc canh bao ton thap. */
    @Column(name = "min_stock")
    private int minStock;

    /** Ton kho so sach hien tai. */
    @Column(name = "current_stock")
    private int currentStock;

    /** Gia nhap gan nhat, dung de tinh gia tri ton. */
    @Column(name = "last_cost")
    private BigDecimal lastCost = BigDecimal.ZERO;

    @Column(name = "sale_price")
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "is_active")
    private boolean active = true;

    public boolean isLowStock() {
        return currentStock <= minStock;
    }
}
