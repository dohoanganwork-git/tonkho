package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.GoodsReceiptItem;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, String> {

    List<GoodsReceiptItem> findByReceiptId(String receiptId);

    void deleteByReceiptId(String receiptId);
}
