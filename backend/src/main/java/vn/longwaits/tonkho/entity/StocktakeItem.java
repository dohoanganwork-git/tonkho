package vn.longwaits.tonkho.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stocktake_items")
public class StocktakeItem extends BaseEntity {

    @Column(name = "stocktake_id")
    private String stocktakeId;

    @Column(name = "item_id")
    private String itemId;

    /** Ton so sach tai thoi diem tai file. */
    @Column(name = "book_qty")
    private int bookQty;

    /** Ton thuc te dem duoc. */
    @Column(name = "counted_qty")
    private int countedQty;

    @Column(name = "damaged_qty")
    private int damagedQty;

    @Column(name = "damage_reason")
    private String damageReason;

    /** Chenh lech = thuc te - so sach (cot variance_qty trong CSDL la cot tu tinh). */
    public int getVarianceQty() {
        return countedQty - bookQty;
    }
}
