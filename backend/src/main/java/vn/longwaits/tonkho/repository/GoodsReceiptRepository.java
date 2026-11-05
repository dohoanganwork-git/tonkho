package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.GoodsReceipt;

import java.util.List;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, String> {

    boolean existsByCode(String code);

    long countByCodeStartingWith(String prefix);

    List<GoodsReceipt> findAllByOrderByReceiptDateDescCreatedAtDesc();
}
