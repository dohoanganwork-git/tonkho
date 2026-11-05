package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "goods_receipt_items")
public class GoodsReceiptItem extends BaseEntity {

    @Column(name = "receipt_id")
    private String receiptId;

    @Column(name = "item_id")
    private String itemId;

    private int quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Thanh tien (cot line_amount trong CSDL la cot tu tinh). */
    public BigDecimal getLineAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
