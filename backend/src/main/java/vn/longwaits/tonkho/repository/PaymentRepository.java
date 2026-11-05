package vn.longwaits.tonkho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.longwaits.tonkho.entity.Payment;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByReceiptIdOrderByPaidAtDesc(String receiptId);

    List<Payment> findAllByOrderByPaidAtDesc();

    boolean existsByReceiptIdAndStatus(String receiptId, String status);
}
