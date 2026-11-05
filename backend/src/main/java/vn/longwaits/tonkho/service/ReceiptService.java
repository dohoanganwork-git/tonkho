package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.common.Status;
import vn.longwaits.tonkho.entity.GoodsReceipt;
import vn.longwaits.tonkho.entity.GoodsReceiptItem;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Payment;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.repository.GoodsReceiptItemRepository;
import vn.longwaits.tonkho.repository.GoodsReceiptRepository;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.PaymentRepository;
import vn.longwaits.tonkho.repository.SupplierRepository;
import vn.longwaits.tonkho.security.AuthUser;
import vn.longwaits.tonkho.storage.FileStorageService;
import vn.longwaits.tonkho.web.dto.Dtos.DebtSummary;
import vn.longwaits.tonkho.web.dto.Dtos.PaymentView;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptDetail;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptLineRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptLineView;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static vn.longwaits.tonkho.service.UserService.blankToNull;

/**
 * Phieu nhap hang: lap phieu -> tang ton kho -> tu tao cong no (mac dinh tra sau).
 * Chi Quan ly duoc sua / huy phieu; phieu da co thanh toan phai huy thanh toan truoc.
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptItemRepository lineRepository;
    private final PaymentRepository paymentRepository;
    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final FileStorageService storage;
    private final Lookups lookups;

    // ---------- Doc du lieu ----------
    public List<ReceiptView> list(String supplierId, LocalDate from, LocalDate to, String debtStatus,
                                  String status, String reconcileStatus) {
        List<GoodsReceipt> receipts = receiptRepository.findAllByOrderByReceiptDateDescCreatedAtDesc().stream()
                .filter(r -> isBlank(supplierId) || supplierId.equals(r.getSupplierId()))
                .filter(r -> from == null || !r.getReceiptDate().isBefore(from))
                .filter(r -> to == null || !r.getReceiptDate().isAfter(to))
                .filter(r -> isBlank(debtStatus) || debtStatus.equals(r.getDebtStatus()))
                .filter(r -> isBlank(status) || status.equals(r.getStatus()))
                .filter(r -> isBlank(reconcileStatus) || reconcileStatus.equals(r.getReconcileStatus()))
                .toList();
        return toViews(receipts);
    }

    public ReceiptDetail detail(String id) {
        GoodsReceipt r = get(id);
        List<GoodsReceiptItem> lines = lineRepository.findByReceiptId(id);
        Map<String, Item> items = lookups.items(lines.stream().map(GoodsReceiptItem::getItemId).toList());
        List<ReceiptLineView> lineViews = lines.stream().map(l -> {
            Item i = items.get(l.getItemId());
            return new ReceiptLineView(l.getId(), l.getItemId(), i == null ? "" : i.getCode(),
                    i == null ? "" : i.getName(), i == null ? "" : i.getUnit(),
                    l.getQuantity(), l.getUnitPrice(), l.getLineAmount());
        }).sorted(Comparator.comparing(ReceiptLineView::itemCode)).toList();
        List<Payment> payments = paymentRepository.findByReceiptIdOrderByPaidAtDesc(id);
        Map<String, String> names = lookups.userNames(Stream.concat(
                Stream.of(r.getCreatedBy(), r.getCancelledBy()),
                payments.stream().map(Payment::getCreatedBy)).toList());
        Supplier s = supplierRepository.findById(r.getSupplierId()).orElse(null);
        String supplierName = s == null ? "" : s.getName();
        List<PaymentView> paymentViews = payments.stream()
                .map(p -> new PaymentView(p, r.getCode(), supplierName, names.get(p.getCreatedBy())))
                .toList();
        return new ReceiptDetail(r, supplierName, names.get(r.getCreatedBy()), names.get(r.getCancelledBy()),
                lineViews, paymentViews);
    }

    /** Tong hop cong no con lai theo nha cung cap (chi tinh phieu con hieu luc). */
    public List<DebtSummary> debtSummary() {
        Map<String, BigDecimal[]> acc = new LinkedHashMap<>();
        Map<String, Long> counts = new HashMap<>();
        for (GoodsReceipt r : receiptRepository.findAll()) {
            if (r.isCancelled()) {
                continue;
            }
            BigDecimal[] a = acc.computeIfAbsent(r.getSupplierId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(r.getTotalAmount());
            a[1] = a[1].add(r.getPaidAmount());
            counts.merge(r.getSupplierId(), 1L, Long::sum);
        }
        Map<String, Supplier> suppliers = lookups.suppliers(acc.keySet());
        List<DebtSummary> out = new ArrayList<>();
        acc.forEach((sid, a) -> {
            Supplier s = suppliers.get(sid);
            out.add(new DebtSummary(sid, s == null ? "" : s.getCode(), s == null ? "" : s.getName(),
                    counts.get(sid), a[0], a[1], a[0].subtract(a[1])));
        });
        out.sort(Comparator.comparing(DebtSummary::remaining).reversed());
        return out;
    }

    // ---------- Lap phieu ----------
    @Transactional
    public ReceiptDetail create(ReceiptRequest req, AuthUser user) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> ApiException.badRequest("Nhà cung cấp không tồn tại"));
        if (!supplier.isActive()) {
            throw ApiException.badRequest("Nhà cung cấp " + supplier.getName() + " đã ngừng hợp tác");
        }
        if (req.receiptDate().isAfter(LocalDate.now())) {
            throw ApiException.badRequest("Ngày nhập không được sau ngày hôm nay");
        }
        checkDuplicateItems(req.lines());

        GoodsReceipt r = new GoodsReceipt();
        r.setCode(nextCode(req.receiptDate()));
        r.setSupplierId(supplier.getId());
        r.setCreatedBy(user.id());
        r.setInvoiceNo(blankToNull(req.invoiceNo()));
        r.setReceiptDate(req.receiptDate());

        BigDecimal total = BigDecimal.ZERO;
        List<GoodsReceiptItem> lines = new ArrayList<>();
        for (ReceiptLineRequest l : req.lines()) {
            Item item = lockActiveItem(l.itemId());
            item.setCurrentStock(item.getCurrentStock() + l.quantity());
            item.setLastCost(l.unitPrice());
            itemRepository.save(item);

            GoodsReceiptItem line = new GoodsReceiptItem();
            line.setItemId(item.getId());
            line.setQuantity(l.quantity());
            line.setUnitPrice(l.unitPrice());
            lines.add(line);
            total = total.add(line.getLineAmount());
        }
        r.setTotalAmount(total);
        r.setPaidAmount(BigDecimal.ZERO);
        r.refreshDebtStatus();
        receiptRepository.save(r);
        for (GoodsReceiptItem line : lines) {
            line.setReceiptId(r.getId());
        }
        lineRepository.saveAll(lines);
        return detail(r.getId());
    }

    // ---------- Sua phieu (Quan ly) ----------
    @Transactional
    public ReceiptDetail update(String id, ReceiptRequest req) {
        GoodsReceipt r = get(id);
        ensureEditable(r);
        checkDuplicateItems(req.lines());
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> ApiException.badRequest("Nhà cung cấp không tồn tại"));

        Map<String, Integer> oldQty = new HashMap<>();
        for (GoodsReceiptItem l : lineRepository.findByReceiptId(id)) {
            oldQty.merge(l.getItemId(), l.getQuantity(), Integer::sum);
        }
        Map<String, Integer> newQty = new HashMap<>();
        for (ReceiptLineRequest l : req.lines()) {
            newQty.merge(l.itemId(), l.quantity(), Integer::sum);
        }
        Set<String> allItems = new HashSet<>(oldQty.keySet());
        allItems.addAll(newQty.keySet());
        for (String itemId : allItems) {
            int delta = newQty.getOrDefault(itemId, 0) - oldQty.getOrDefault(itemId, 0);
            if (delta == 0) {
                continue;
            }
            Item item = newQty.containsKey(itemId) && !oldQty.containsKey(itemId)
                    ? lockActiveItem(itemId) : lockItem(itemId);
            if (item.getCurrentStock() + delta < 0) {
                throw ApiException.badRequest("Không thể giảm số lượng " + item.getName()
                        + ": hàng đã bán/xuất, tồn hiện tại chỉ còn " + item.getCurrentStock());
            }
            item.setCurrentStock(item.getCurrentStock() + delta);
            itemRepository.save(item);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<GoodsReceiptItem> lines = new ArrayList<>();
        for (ReceiptLineRequest l : req.lines()) {
            GoodsReceiptItem line = new GoodsReceiptItem();
            line.setReceiptId(id);
            line.setItemId(l.itemId());
            line.setQuantity(l.quantity());
            line.setUnitPrice(l.unitPrice());
            lines.add(line);
            total = total.add(line.getLineAmount());
        }
        if (total.compareTo(r.getPaidAmount()) < 0) {
            throw ApiException.badRequest("Tổng tiền mới nhỏ hơn số đã thanh toán, cần hủy bớt khoản thanh toán trước");
        }
        lineRepository.deleteByReceiptId(id);
        lineRepository.flush();
        lineRepository.saveAll(lines);

        r.setSupplierId(supplier.getId());
        r.setInvoiceNo(blankToNull(req.invoiceNo()));
        r.setReceiptDate(req.receiptDate());
        r.setTotalAmount(total);
        r.refreshDebtStatus();
        receiptRepository.save(r);
        return detail(id);
    }

    // ---------- Huy phieu (Quan ly) ----------
    @Transactional
    public ReceiptDetail cancel(String id, String reason, AuthUser user) {
        GoodsReceipt r = get(id);
        ensureEditable(r);
        if (paymentRepository.existsByReceiptIdAndStatus(id, Status.ACTIVE)) {
            throw ApiException.badRequest("Phiếu đã có khoản thanh toán. Kế toán cần hủy các khoản thanh toán trước khi hủy phiếu");
        }
        for (GoodsReceiptItem l : lineRepository.findByReceiptId(id)) {
            Item item = lockItem(l.getItemId());
            if (item.getCurrentStock() < l.getQuantity()) {
                throw ApiException.badRequest("Không thể hủy phiếu: " + item.getName()
                        + " đã bán/xuất một phần, tồn hiện tại chỉ còn " + item.getCurrentStock());
            }
            item.setCurrentStock(item.getCurrentStock() - l.getQuantity());
            itemRepository.save(item);
        }
        r.setStatus(Status.CANCELLED);
        r.setCancelReason(reason.trim());
        r.setCancelledBy(user.id());
        receiptRepository.save(r);
        return detail(id);
    }

    // ---------- File hoa don ----------
    @Transactional
    public ReceiptDetail uploadInvoice(String id, MultipartFile file) {
        GoodsReceipt r = get(id);
        if (r.isReconciled()) {
            throw ApiException.badRequest("Phiếu đã đối soát xong, không thể thay file hóa đơn");
        }
        r.setInvoiceFileUrl(storage.store(file, "hoa-don"));
        receiptRepository.save(r);
        return detail(id);
    }

    // ---------- Doi soat cong no (Ke toan) ----------
    @Transactional
    public ReceiptDetail reconcile(String id, String result, String note, MultipartFile invoiceFile) {
        GoodsReceipt r = get(id);
        if (r.isCancelled()) {
            throw ApiException.badRequest("Phiếu đã hủy, không cần đối soát");
        }
        if (r.isReconciled()) {
            throw ApiException.badRequest("Phiếu đã đối soát xong và đã khóa");
        }
        if (invoiceFile != null && !invoiceFile.isEmpty()) {
            r.setInvoiceFileUrl(storage.store(invoiceFile, "hoa-don"));
        }
        if (Status.RECON_DONE.equals(result)) {
            if (r.getInvoiceFileUrl() == null) {
                throw ApiException.badRequest("Cần tải file hóa đơn trước khi xác nhận đối soát khớp");
            }
            r.setReconcileStatus(Status.RECON_DONE);
            r.setReconciledAt(LocalDateTime.now());
        } else if (Status.RECON_DIFFERENCE.equals(result)) {
            if (isBlank(note)) {
                throw ApiException.badRequest("Ghi chú nội dung chênh lệch để xử lý");
            }
            r.setReconcileStatus(Status.RECON_DIFFERENCE);
        } else {
            throw ApiException.badRequest("Kết quả đối soát không hợp lệ");
        }
        r.setReconcileNote(blankToNull(note));
        receiptRepository.save(r);
        return detail(id);
    }

    // ---------- Ho tro ----------
    GoodsReceipt get(String id) {
        return receiptRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu nhập"));
    }

    List<ReceiptView> toViews(List<GoodsReceipt> receipts) {
        Map<String, Supplier> suppliers = lookups.suppliers(receipts.stream().map(GoodsReceipt::getSupplierId).toList());
        Map<String, String> names = lookups.userNames(receipts.stream()
                .flatMap(r -> Stream.of(r.getCreatedBy(), r.getCancelledBy())).toList());
        return receipts.stream().map(r -> {
            Supplier s = suppliers.get(r.getSupplierId());
            return new ReceiptView(r, s == null ? "" : s.getName(), names.get(r.getCreatedBy()),
                    names.get(r.getCancelledBy()));
        }).toList();
    }

    private void ensureEditable(GoodsReceipt r) {
        if (r.isCancelled()) {
            throw ApiException.badRequest("Phiếu đã bị hủy");
        }
        if (r.isReconciled()) {
            throw ApiException.badRequest("Phiếu đã đối soát công nợ xong và đã khóa, không thể sửa/hủy");
        }
    }

    private void checkDuplicateItems(List<ReceiptLineRequest> lines) {
        Set<String> seen = new HashSet<>();
        for (ReceiptLineRequest l : lines) {
            if (!seen.add(l.itemId())) {
                throw ApiException.badRequest("Một mặt hàng chỉ được nhập một dòng trong phiếu");
            }
        }
    }

    private Item lockItem(String itemId) {
        return itemRepository.lockById(itemId).orElseThrow(() -> ApiException.badRequest("Mặt hàng không tồn tại"));
    }

    private Item lockActiveItem(String itemId) {
        Item item = lockItem(itemId);
        if (!item.isActive()) {
            throw ApiException.badRequest("Mặt hàng " + item.getName() + " đã ngừng kinh doanh");
        }
        return item;
    }

    private String nextCode(LocalDate date) {
        String prefix = "PN" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long n = receiptRepository.countByCodeStartingWith(prefix) + 1;
        String code;
        do {
            code = prefix + String.format("%03d", n++);
        } while (receiptRepository.existsByCode(code));
        return code;
    }

    static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
