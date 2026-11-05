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
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.entity.Payment;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.service.PaymentService;
import vn.longwaits.tonkho.web.dto.Dtos.PaymentView;
import vn.longwaits.tonkho.web.dto.Dtos.ReasonRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasAnyRole('MANAGER','ACCOUNTANT')")
    @GetMapping
    public List<PaymentView> list(@RequestParam(required = false) String receiptId,
                                  @RequestParam(required = false) String supplierId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String status) {
        return paymentService.list(receiptId, supplierId, from, to, status);
    }

    @PreAuthorize("hasRole('ACCOUNTANT')")
    @PostMapping
    public Payment create(@RequestParam("receiptId") String receiptId,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam("paidAt") String paidAt,
                          @RequestParam("method") String method,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "file", required = false) MultipartFile file) {
        return paymentService.create(receiptId, amount, parse(paidAt), method, note, file, CurrentUser.get());
    }

    @PreAuthorize("hasRole('ACCOUNTANT')")
    @PutMapping("/{id}")
    public Payment update(@PathVariable String id,
                          @RequestParam("amount") BigDecimal amount,
                          @RequestParam("paidAt") String paidAt,
                          @RequestParam("method") String method,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(value = "file", required = false) MultipartFile file) {
        return paymentService.update(id, amount, parse(paidAt), method, note, file);
    }

    @PreAuthorize("hasRole('ACCOUNTANT')")
    @PostMapping("/{id}/cancel")
    public Payment cancel(@PathVariable String id, @Valid @RequestBody ReasonRequest req) {
        return paymentService.cancel(id, req.reason());
    }

    private static LocalDateTime parse(String s) {
        try {
            return s.length() == 10 ? LocalDate.parse(s).atStartOfDay() : LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Ngày thanh toán không hợp lệ");
        }
    }
}
