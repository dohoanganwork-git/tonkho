package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.SalesLine;
import vn.longwaits.tonkho.importer.SpreadsheetReader;
import vn.longwaits.tonkho.importer.SpreadsheetReader.ParsedSheet;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.SalesLineRepository;
import vn.longwaits.tonkho.security.AuthUser;
import vn.longwaits.tonkho.storage.FileStorageService;
import vn.longwaits.tonkho.web.dto.Dtos.SalesImportResult;
import vn.longwaits.tonkho.web.dto.Dtos.SalesImportSummary;
import vn.longwaits.tonkho.web.dto.Dtos.SalesLineView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nhap doanh so ban hang xuat tu POS cuoi ngay: kiem tra file, luu file goc,
 * tao dong xuat ban va tru ton kho (toi thieu bang 0, phan vuot ghi nhan de canh bao).
 */
@Service
@RequiredArgsConstructor
public class SalesService {

    /** Ten cot chap nhan trong file POS (da chuan hoa khong dau). */
    static final String[] COL_CODE = {"ma_pos", "ma_hang_pos", "ma_hang", "ma_sp", "sku", "ma"};
    static final String[] COL_QTY = {"so_luong", "so_luong_ban", "sl", "quantity"};
    static final String[] COL_PRICE = {"don_gia", "gia_ban", "don_gia_ban", "gia", "price"};
    static final String[] COL_DATE = {"ngay", "ngay_ban", "date"};

    private final SalesLineRepository salesRepository;
    private final ItemRepository itemRepository;
    private final FileStorageService storage;
    private final Lookups lookups;

    public List<SalesImportSummary> imports() {
        Map<LocalDate, List<SalesLine>> byDate = new TreeMap<>(Comparator.reverseOrder());
        for (SalesLine s : salesRepository.findAllByOrderBySalesDateDesc()) {
            byDate.computeIfAbsent(s.getSalesDate(), d -> new ArrayList<>()).add(s);
        }
        Map<String, String> names = lookups.userNames(
                byDate.values().stream().flatMap(List::stream).map(SalesLine::getImportedBy).toList());
        List<SalesImportSummary> out = new ArrayList<>();
        byDate.forEach((d, lines) -> out.add(summary(d, lines, names)));
        return out;
    }

    public List<SalesLineView> lines(LocalDate date) {
        return toViews(salesRepository.findBySalesDate(date));
    }

    @Transactional
    public SalesImportResult importFile(LocalDate salesDate, MultipartFile file, AuthUser user) {
        if (salesDate.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("Ngày bán không được sau ngày hôm nay");
        }
        if (salesRepository.existsBySalesDate(salesDate)) {
            throw ApiException.conflict("Doanh số ngày " + salesDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " đã được nhập trước đó");
        }
        ParsedSheet sheet = SpreadsheetReader.read(file);
        if (!sheet.hasAny(COL_CODE) || !sheet.hasAny(COL_QTY)) {
            throw ApiException.badRequest("File POS thiếu cột bắt buộc: cần cột \"Mã POS\" và \"Số lượng\"",
                    List.of("Các cột đọc được: " + String.join(", ", sheet.headers())));
        }

        List<String> errors = new ArrayList<>();
        Map<String, Integer> qtyByItem = new LinkedHashMap<>();
        Map<String, BigDecimal> priceByItem = new LinkedHashMap<>();
        Map<String, Item> itemById = new LinkedHashMap<>();
        int rowNo = 1;
        for (Map<String, String> row : sheet.rows()) {
            rowNo++;
            String code = SpreadsheetReader.pick(row, COL_CODE);
            Integer qty = SpreadsheetReader.parseInt(SpreadsheetReader.pick(row, COL_QTY));
            String dateText = SpreadsheetReader.pick(row, COL_DATE);
            if (code == null) {
                errors.add("Dòng " + rowNo + ": thiếu mã hàng");
                continue;
            }
            if (qty == null || qty <= 0) {
                errors.add("Dòng " + rowNo + " (" + code + "): số lượng không hợp lệ");
                continue;
            }
            if (dateText != null) {
                LocalDate rowDate = parseDate(dateText);
                if (rowDate == null || !rowDate.equals(salesDate)) {
                    errors.add("Dòng " + rowNo + " (" + code + "): ngày " + dateText + " không khớp ngày đã chọn");
                    continue;
                }
            }
            Item item = itemRepository.findByPosCode(code).or(() -> itemRepository.findByCode(code)).orElse(null);
            if (item == null) {
                errors.add("Dòng " + rowNo + ": mã " + code + " chưa có trong danh mục, cần bổ sung mặt hàng trước");
                continue;
            }
            BigDecimal price = SpreadsheetReader.parseMoney(SpreadsheetReader.pick(row, COL_PRICE));
            itemById.put(item.getId(), item);
            qtyByItem.merge(item.getId(), qty, Integer::sum);
            priceByItem.putIfAbsent(item.getId(), price != null ? price : item.getSalePrice());
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("File POS có " + errors.size() + " lỗi, chưa có dữ liệu nào được ghi nhận",
                    errors.size() > 30 ? errors.subList(0, 30) : errors);
        }

        String fileUrl = storage.store(file, "pos");
        List<SalesLine> saved = new ArrayList<>();
        for (Map.Entry<String, Integer> e : qtyByItem.entrySet()) {
            Item item = itemRepository.lockById(e.getKey()).orElseThrow();
            int qty = e.getValue();
            int before = item.getCurrentStock();
            int after = Math.max(0, before - qty);
            SalesLine s = new SalesLine();
            s.setImportedBy(user.id());
            s.setItemId(item.getId());
            s.setSalesDate(salesDate);
            s.setOriginalFileUrl(fileUrl);
            s.setQuantity(qty);
            s.setUnitPrice(priceByItem.get(item.getId()));
            s.setStockBefore(before);
            s.setStockAfter(after);
            s.setOverQuantity(Math.max(0, qty - before));
            s.setImportedAt(LocalDateTime.now());
            saved.add(s);
            item.setCurrentStock(after);
            itemRepository.save(item);
        }
        salesRepository.saveAll(saved);

        List<Item> lowStock = itemById.values().stream()
                .filter(i -> i.isActive() && i.isLowStock())
                .sorted(Comparator.comparing(Item::getCode))
                .toList();
        Map<String, String> names = lookups.userNames(List.of(user.id()));
        return new SalesImportResult(summary(salesDate, saved, names), toViews(saved), lowStock);
    }

    private SalesImportSummary summary(LocalDate date, List<SalesLine> lines, Map<String, String> names) {
        int qty = lines.stream().mapToInt(SalesLine::getQuantity).sum();
        BigDecimal amount = lines.stream().map(SalesLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int over = (int) lines.stream().filter(l -> l.getOverQuantity() > 0).count();
        SalesLine first = lines.get(0);
        return new SalesImportSummary(date, lines.size(), qty, amount, over, names.get(first.getImportedBy()),
                first.getImportedAt(), first.getOriginalFileUrl());
    }

    List<SalesLineView> toViews(List<SalesLine> lines) {
        Map<String, Item> items = lookups.items(lines.stream().map(SalesLine::getItemId).toList());
        return lines.stream().map(s -> {
            Item i = items.get(s.getItemId());
            return new SalesLineView(s.getId(), s.getSalesDate(), s.getItemId(), i == null ? "" : i.getCode(),
                    i == null ? "" : i.getName(), i == null ? "" : i.getUnit(), s.getQuantity(), s.getUnitPrice(),
                    s.getAmount(), s.getStockBefore(), s.getStockAfter(), s.getOverQuantity());
        }).sorted(Comparator.comparing(SalesLineView::itemCode)).toList();
    }

    static LocalDate parseDate(String text) {
        String t = text.trim();
        if (t.length() > 10) {
            t = t.substring(0, 10);
        }
        for (String p : List.of("dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "M/d/yy", "MM/dd/yyyy")) {
            try {
                return LocalDate.parse(t, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
                // thu dinh dang tiep theo
            }
        }
        return null;
    }
}
