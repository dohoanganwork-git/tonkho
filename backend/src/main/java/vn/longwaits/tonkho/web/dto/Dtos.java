package vn.longwaits.tonkho.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.longwaits.tonkho.entity.GoodsReceipt;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Payment;
import vn.longwaits.tonkho.entity.Stocktake;
import vn.longwaits.tonkho.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cac doi tuong du lieu vao/ra cua API. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- Tai khoan ----------
    public record LoginRequest(@NotBlank(message = "Nhập tên đăng nhập") String username,
                               @NotBlank(message = "Nhập mật khẩu") String password) {
    }

    public record LoginResponse(String token, User user) {
    }

    public record ProfileRequest(@NotBlank(message = "Nhập họ tên") String fullName, String email, String phone) {
    }

    public record PasswordRequest(@NotBlank String currentPassword,
                                  @NotBlank @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String newPassword) {
    }

    public record UserRequest(@NotBlank(message = "Nhập tên đăng nhập") String username,
                              String password,
                              @NotBlank(message = "Nhập họ tên") String fullName,
                              String email,
                              String phone,
                              @NotBlank @Pattern(regexp = "manager|staff|accountant", message = "Vai trò không hợp lệ") String role,
                              Boolean active) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String newPassword) {
    }

    // ---------- Danh muc ----------
    public record ItemRequest(@NotBlank(message = "Nhập loại hàng") String category,
                              @NotBlank(message = "Nhập mã mặt hàng") String code,
                              @NotBlank(message = "Nhập mã POS") String posCode,
                              @NotBlank(message = "Nhập tên mặt hàng") String name,
                              @NotBlank(message = "Nhập đơn vị tính") String unit,
                              @NotNull @Min(value = 0, message = "Mốc cảnh báo không được âm") Integer minStock,
                              @NotNull @DecimalMin(value = "0", message = "Giá bán không được âm") BigDecimal salePrice,
                              Boolean active) {
    }

    public record SupplierRequest(@NotBlank(message = "Nhập mã nhà cung cấp") String code,
                                  @NotBlank(message = "Nhập tên nhà cung cấp") String name,
                                  String contactName, String phone, String email, String address,
                                  String paymentMethod, String contractNo,
                                  LocalDate contractStart, LocalDate contractEnd, String contractNote,
                                  Boolean active) {
    }

    // ---------- Nhap hang, cong no ----------
    public record ReceiptLineRequest(@NotBlank String itemId,
                                     @NotNull @Min(value = 1, message = "Số lượng phải lớn hơn 0") Integer quantity,
                                     @NotNull @DecimalMin(value = "0", message = "Đơn giá không được âm") BigDecimal unitPrice) {
    }

    public record ReceiptRequest(@NotBlank(message = "Chọn nhà cung cấp") String supplierId,
                                 String invoiceNo,
                                 @NotNull(message = "Chọn ngày nhập") LocalDate receiptDate,
                                 @NotEmpty(message = "Phiếu nhập cần ít nhất một mặt hàng") @Valid List<ReceiptLineRequest> lines) {
    }

    public record ReasonRequest(@NotBlank(message = "Nhập lý do") String reason) {
    }

    public record ReceiptView(GoodsReceipt receipt, String supplierName, String createdByName, String cancelledByName) {
    }

    public record ReceiptLineView(String id, String itemId, String itemCode, String itemName, String unit,
                                  int quantity, BigDecimal unitPrice, BigDecimal lineAmount) {
    }

    public record PaymentView(Payment payment, String receiptCode, String supplierName, String createdByName) {
    }

    public record ReceiptDetail(GoodsReceipt receipt, String supplierName, String createdByName,
                                String cancelledByName, List<ReceiptLineView> lines, List<PaymentView> payments) {
    }

    public record DebtSummary(String supplierId, String supplierCode, String supplierName, long receiptCount,
                              BigDecimal total, BigDecimal paid, BigDecimal remaining) {
    }

    // ---------- Doanh so POS ----------
    public record SalesImportSummary(LocalDate salesDate, int lineCount, int totalQuantity, BigDecimal totalAmount,
                                     int overCount, String importedByName, LocalDateTime importedAt,
                                     String originalFileUrl) {
    }

    public record SalesLineView(String id, LocalDate salesDate, String itemId, String itemCode, String itemName,
                                String unit, int quantity, BigDecimal unitPrice, BigDecimal amount,
                                int stockBefore, int stockAfter, int overQuantity) {
    }

    public record SalesImportResult(SalesImportSummary summary, List<SalesLineView> lines, List<Item> lowStock) {
    }

    // ---------- Kiem ke ----------
    public record StocktakeView(Stocktake stocktake, String createdByName, String reviewedByName,
                                int itemCount, int varianceCount, int damagedTotal) {
    }

    public record StocktakeItemView(String id, String itemId, String itemCode, String itemName, String unit,
                                    int bookQty, int countedQty, int varianceQty, int damagedQty,
                                    String damageReason) {
    }

    public record StocktakeDetail(StocktakeView summary, List<StocktakeItemView> items, boolean recountSubmitted) {
    }

    // ---------- Bao cao ----------
    public record InventoryRow(String itemId, String code, String name, String category, String unit,
                               int currentStock, int minStock, boolean lowStock,
                               BigDecimal lastCost, BigDecimal stockValue) {
    }

    public record InventoryReport(List<InventoryRow> rows, int totalQuantity, BigDecimal totalValue,
                                  int lowStockCount) {
    }

    public record ConsumptionRow(String itemId, String code, String name, String category, String unit,
                                 int quantity, BigDecimal amount) {
    }

    public record ConsumptionReport(LocalDate from, LocalDate to, List<ConsumptionRow> rows,
                                    int totalQuantity, BigDecimal totalAmount) {
    }

    public record AccuracyRow(String stocktakeId, LocalDate stocktakeDate, String status, int itemCount,
                              int matchedCount, double accuracyPercent, int totalAbsVariance, int damagedTotal) {
    }

    public record Dashboard(int itemCount, int lowStockCount, BigDecimal debtRemaining, long unpaidReceipts,
                            long pendingStocktakes, LocalDate lastSalesDate) {
    }

    public record AlertView(List<Item> lowStock, List<SalesLineView> overSold) {
    }
}
