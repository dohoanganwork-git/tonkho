package vn.longwaits.tonkho.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.service.ReceiptService;
import vn.longwaits.tonkho.web.dto.Dtos.DebtSummary;
import vn.longwaits.tonkho.web.dto.Dtos.ReasonRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptDetail;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptView;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    public List<ReceiptView> list(@RequestParam(required = false) String supplierId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String debtStatus,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String reconcileStatus) {
        return receiptService.list(supplierId, from, to, debtStatus, status, reconcileStatus);
    }

    @GetMapping("/{id}")
    public ReceiptDetail detail(@PathVariable String id) {
        return receiptService.detail(id);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ACCOUNTANT')")
    @GetMapping("/debt-summary")
    public List<DebtSummary> debtSummary() {
        return receiptService.debtSummary();
    }

    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    @PostMapping
    public ReceiptDetail create(@Valid @RequestBody ReceiptRequest req) {
        return receiptService.create(req, CurrentUser.get());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/{id}")
    public ReceiptDetail update(@PathVariable String id, @Valid @RequestBody ReceiptRequest req) {
        return receiptService.update(id, req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/{id}/cancel")
    public ReceiptDetail cancel(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        return receiptService.cancel(id, req.reason(), CurrentUser.get());
    }

    @PostMapping("/{id}/invoice-file")
    public ReceiptDetail invoice(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        return receiptService.uploadInvoice(id, file);
    }

    @PreAuthorize("hasRole('ACCOUNTANT')")
    @PostMapping("/{id}/reconcile")
    public ReceiptDetail reconcile(@PathVariable String id,
                                   @RequestParam("result") String result,
                                   @RequestParam(value = "note", required = false) String note,
                                   @RequestParam(value = "file", required = false) MultipartFile file) {
        return receiptService.reconcile(id, result, note, file);
    }
}
