package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.common.Status;
import vn.longwaits.tonkho.entity.GoodsReceipt;
import vn.longwaits.tonkho.entity.Payment;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.repository.GoodsReceiptRepository;
import vn.longwaits.tonkho.repository.PaymentRepository;
import vn.longwaits.tonkho.security.AuthUser;
import vn.longwaits.tonkho.storage.FileStorageService;
import vn.longwaits.tonkho.web.dto.Dtos.PaymentView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static vn.longwaits.tonkho.service.ReceiptService.isBlank;
import static vn.longwaits.tonkho.service.UserService.blankToNull;

/** Thanh toan cong no (Ke toan): ghi nhan kem minh chung, sua, huy. */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final FileStorageService storage;
    private final Lookups lookups;

    public List<PaymentView> list(String receiptId, String supplierId, LocalDate from, LocalDate to, String status) {
        List<Payment> payments = paymentRepository.findAllByOrderByPaidAtDesc().stream()
                .filter(p -> isBlank(receiptId) || receiptId.equals(p.getReceiptId()))
                .filter(p -> from == null || !p.getPaidAt().toLocalDate().isBefore(from))
                .filter(p -> to == null || !p.getPaidAt().toLocalDate().isAfter(to))
                .filter(p -> isBlank(status) || status.equals(p.getStatus()))
                .toList();
        Map<String, GoodsReceipt> receipts = new java.util.HashMap<>();
        receiptRepository.findAllById(payments.stream().map(Payment::getReceiptId).distinct().toList())
                .forEach(r -> receipts.put(r.getId(), r));
        List<Payment> filtered = payments.stream()
                .filter(p -> isBlank(supplierId)
                        || (receipts.get(p.getReceiptId()) != null
                        && supplierId.equals(receipts.get(p.getReceiptId()).getSupplierId())))
                .toList();
        Map<String, Supplier> suppliers = lookups.suppliers(receipts.values().stream().map(GoodsReceipt::getSupplierId).toList());
        Map<String, String> names = lookups.userNames(filtered.stream().map(Payment::getCreatedBy).toList());
        return filtered.stream().map(p -> {
            GoodsReceipt r = receipts.get(p.getReceiptId());
            Supplier s = r == null ? null : suppliers.get(r.getSupplierId());
            return new PaymentView(p, r == null ? "" : r.getCode(), s == null ? "" : s.getName(),
                    names.get(p.getCreatedBy()));
        }).toList();
    }

    @Transactional
    public Payment create(String receiptId, BigDecimal amount, LocalDateTime paidAt, String method, String note,
                          MultipartFile evidence, AuthUser user) {
        GoodsReceipt r = receiptRepository.findById(receiptId)
                .orElseThrow(() -> ApiException.badRequest("Phiếu nhập không tồn tại"));
        ensurePayable(r);
        validateAmount(amount, r.getRemainingAmount());
        if (paidAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw ApiException.badRequest("Ngày thanh toán không được ở tương lai");
        }
        if (evidence == null || evidence.isEmpty()) {
            throw ApiException.badRequest("Cần đính kèm file minh chứng thanh toán");
        }
        Payment p = new Payment();
        p.setReceiptId(r.getId());
        p.setCreatedBy(user.id());
        p.setAmount(amount);
        p.setPaidAt(paidAt);
        p.setMethod(method.trim());
        p.setNote(blankToNull(note));
        p.setEvidenceFileUrl(storage.store(evidence, "minh-chung"));
        paymentRepository.save(p);

        r.setPaidAmount(r.getPaidAmount().add(amount));
        r.refreshDebtStatus();
        receiptRepository.save(r);
        return p;
    }

    @Transactional
    public Payment update(String id, BigDecimal amount, LocalDateTime paidAt, String method, String note,
                          MultipartFile evidence) {
        Payment p = get(id);
        if (Status.CANCELLED.equals(p.getStatus())) {
            throw ApiException.badRequest("Khoản thanh toán đã hủy, không thể sửa");
        }
        GoodsReceipt r = receiptRepository.findById(p.getReceiptId()).orElseThrow();
        ensurePayable(r);
        BigDecimal maxAllowed = r.getRemainingAmount().add(p.getAmount());
        validateAmount(amount, maxAllowed);
        r.setPaidAmount(r.getPaidAmount().subtract(p.getAmount()).add(amount));
        r.refreshDebtStatus();
        receiptRepository.save(r);

        p.setAmount(amount);
        p.setPaidAt(paidAt);
        p.setMethod(method.trim());
        p.setNote(blankToNull(note));
        if (evidence != null && !evidence.isEmpty()) {
            p.setEvidenceFileUrl(storage.store(evidence, "minh-chung"));
        }
        return paymentRepository.save(p);
    }

    @Transactional
    public Payment cancel(String id, String reason) {
        Payment p = get(id);
        if (Status.CANCELLED.equals(p.getStatus())) {
            throw ApiException.badRequest("Khoản thanh toán đã được hủy trước đó");
        }
        GoodsReceipt r = receiptRepository.findById(p.getReceiptId()).orElseThrow();
        if (r.isReconciled()) {
            throw ApiException.badRequest("Phiếu nhập đã đối soát xong và đã khóa, không thể hủy thanh toán");
        }
        p.setStatus(Status.CANCELLED);
        String oldNote = p.getNote() == null ? "" : p.getNote() + " | ";
        p.setNote(oldNote + "Hủy: " + reason.trim());
        paymentRepository.save(p);

        r.setPaidAmount(r.getPaidAmount().subtract(p.getAmount()));
        r.refreshDebtStatus();
        receiptRepository.save(r);
        return p;
    }

    private Payment get(String id) {
        return paymentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khoản thanh toán"));
    }

    private void ensurePayable(GoodsReceipt r) {
        if (r.isCancelled()) {
            throw ApiException.badRequest("Phiếu nhập đã bị hủy");
        }
        if (r.isReconciled()) {
            throw ApiException.badRequest("Phiếu nhập đã đối soát xong và đã khóa");
        }
    }

    private void validateAmount(BigDecimal amount, BigDecimal max) {
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.badRequest("Số tiền thanh toán phải lớn hơn 0");
        }
        if (amount.compareTo(max) > 0) {
            throw ApiException.badRequest("Số tiền vượt quá số còn nợ (" + max.toPlainString() + " đ)");
        }
    }
}
