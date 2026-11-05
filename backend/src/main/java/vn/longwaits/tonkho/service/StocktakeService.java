package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.common.Status;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Stocktake;
import vn.longwaits.tonkho.entity.StocktakeItem;
import vn.longwaits.tonkho.importer.SpreadsheetReader;
import vn.longwaits.tonkho.importer.SpreadsheetReader.ParsedSheet;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.StocktakeItemRepository;
import vn.longwaits.tonkho.repository.StocktakeRepository;
import vn.longwaits.tonkho.security.AuthUser;
import vn.longwaits.tonkho.storage.FileStorageService;
import vn.longwaits.tonkho.web.dto.Dtos.StocktakeDetail;
import vn.longwaits.tonkho.web.dto.Dtos.StocktakeItemView;
import vn.longwaits.tonkho.web.dto.Dtos.StocktakeView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Kiem ke va doi soat ton kho:
 * Nhan vien tai file ton thuc te (+ file hong/huy) -> he thong so voi ton so sach, danh dau chenh lech
 * (chua ghi nhan la ton thuc te) -> Quan ly dong y (dieu chinh ton) hoac yeu cau kiem ke lai cac mat hang lech.
 */
@Service
@RequiredArgsConstructor
public class StocktakeService {

    static final String[] COL_CODE = {"ma_hang", "ma_pos", "ma_sp", "sku", "ma"};
    static final String[] COL_COUNT = {"ton_thuc_te", "so_luong_thuc_te", "so_luong_dem", "so_luong", "sl"};
    static final String[] COL_DAMAGE_QTY = {"so_luong_hong", "so_luong_huy", "so_luong", "sl"};
    static final String[] COL_REASON = {"ly_do", "ly_do_hong_huy", "ghi_chu"};

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository lineRepository;
    private final ItemRepository itemRepository;
    private final FileStorageService storage;
    private final Lookups lookups;

    public List<StocktakeView> list(String status) {
        List<Stocktake> all = stocktakeRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> status == null || status.isBlank() || status.equals(s.getStatus()))
                .toList();
        Map<String, String> names = lookups.userNames(all.stream()
                .flatMap(s -> Stream.of(s.getCreatedBy(), s.getReviewedBy())).toList());
        return all.stream().map(s -> view(s, lineRepository.findByStocktakeId(s.getId()), names)).toList();
    }

    public StocktakeDetail detail(String id) {
        Stocktake s = get(id);
        List<StocktakeItem> lines = lineRepository.findByStocktakeId(id);
        Map<String, String> names = lookups.userNames(Stream.of(s.getCreatedBy(), s.getReviewedBy()).toList());
        Map<String, Item> items = lookups.items(lines.stream().map(StocktakeItem::getItemId).toList());
        List<StocktakeItemView> views = lines.stream().map(l -> {
            Item i = items.get(l.getItemId());
            return new StocktakeItemView(l.getId(), l.getItemId(), i == null ? "" : i.getCode(),
                    i == null ? "" : i.getName(), i == null ? "" : i.getUnit(), l.getBookQty(), l.getCountedQty(),
                    l.getVarianceQty(), l.getDamagedQty(), l.getDamageReason());
        }).sorted(Comparator.comparing((StocktakeItemView v) -> v.varianceQty() == 0 ? 1 : 0)
                .thenComparing(StocktakeItemView::itemCode)).toList();
        return new StocktakeDetail(view(s, lines, names), views, stocktakeRepository.existsByParentId(id));
    }

    // ---------- Nhan vien tai file kiem ke ----------
    @Transactional
    public StocktakeDetail upload(LocalDate date, MultipartFile countFile, MultipartFile damageFile,
                                  String parentId, AuthUser user) {
        if (date.isAfter(LocalDate.now())) {
            throw ApiException.badRequest("Ngày kiểm kê không được sau ngày hôm nay");
        }
        List<String> parentVarianceItems = List.of();
        if (parentId != null && !parentId.isBlank()) {
            Stocktake parent = get(parentId);
            if (!Status.ST_RECOUNT.equals(parent.getStatus())) {
                throw ApiException.badRequest("Đợt kiểm kê gốc không ở trạng thái yêu cầu kiểm kê lại");
            }
            if (stocktakeRepository.existsByParentId(parentId)) {
                throw ApiException.badRequest("Đợt này đã được gửi kết quả kiểm kê lại");
            }
            parentVarianceItems = lineRepository.findByStocktakeId(parentId).stream()
                    .filter(l -> l.getVarianceQty() != 0).map(StocktakeItem::getItemId).toList();
        } else {
            parentId = null;
        }
        if (stocktakeRepository.existsByStatus(Status.ST_PENDING)) {
            throw ApiException.badRequest("Đang có đợt kiểm kê chờ Quản lý xem xét, vui lòng chờ xử lý xong");
        }

        List<String> errors = new ArrayList<>();
        Map<String, Integer> counted = readCountFile(countFile, errors);
        Map<String, Integer> damagedQty = new LinkedHashMap<>();
        Map<String, String> damagedReason = new LinkedHashMap<>();
        if (damageFile != null && !damageFile.isEmpty()) {
            readDamageFile(damageFile, damagedQty, damagedReason, errors);
            for (String itemId : damagedQty.keySet()) {
                if (!counted.containsKey(itemId)) {
                    Item i = itemRepository.findById(itemId).orElseThrow();
                    errors.add("File hỏng/hủy: mặt hàng " + i.getCode() + " không có trong file kiểm kê");
                }
            }
        }
        for (String itemId : parentVarianceItems) {
            if (!counted.containsKey(itemId)) {
                Item i = itemRepository.findById(itemId).orElseThrow();
                errors.add("Kiểm kê lại cần có mặt hàng lệch " + i.getCode() + " - " + i.getName());
            }
        }
        if (!errors.isEmpty()) {
            throw ApiException.badRequest("File kiểm kê có " + errors.size() + " lỗi, vui lòng sửa và tải lại",
                    errors.size() > 30 ? errors.subList(0, 30) : errors);
        }

        Stocktake s = new Stocktake();
        s.setParentId(parentId);
        s.setCreatedBy(user.id());
        s.setStocktakeDate(date);
        s.setCountFileUrl(storage.store(countFile, "kiem-ke"));
        if (damageFile != null && !damageFile.isEmpty()) {
            s.setDamageFileUrl(storage.store(damageFile, "kiem-ke"));
        }
        List<StocktakeItem> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counted.entrySet()) {
            Item item = itemRepository.findById(e.getKey()).orElseThrow();
            StocktakeItem l = new StocktakeItem();
            l.setItemId(item.getId());
            l.setBookQty(item.getCurrentStock());
            l.setCountedQty(e.getValue());
            l.setDamagedQty(damagedQty.getOrDefault(item.getId(), 0));
            l.setDamageReason(damagedReason.get(item.getId()));
            lines.add(l);
        }
        boolean hasVariance = lines.stream().anyMatch(l -> l.getVarianceQty() != 0);
        if (hasVariance) {
            s.setStatus(Status.ST_PENDING);
        } else {
            s.setStatus(Status.ST_NO_VARIANCE);
            s.setDecidedAt(LocalDateTime.now());
        }
        stocktakeRepository.save(s);
        for (StocktakeItem l : lines) {
            l.setStocktakeId(s.getId());
        }
        lineRepository.saveAll(lines);
        return detail(s.getId());
    }

    // ---------- Quan ly dong y dieu chinh ----------
    @Transactional
    public StocktakeDetail approve(String id, String reason, AuthUser user) {
        Stocktake s = getPending(id);
        for (StocktakeItem l : lineRepository.findByStocktakeId(id)) {
            int variance = l.getVarianceQty();
            if (variance == 0) {
                continue;
            }
            // Dieu chinh theo do lech de khong ghi de cac phat sinh (nhap hang, POS) sau thoi diem kiem ke
            Item item = itemRepository.lockById(l.getItemId()).orElseThrow();
            item.setCurrentStock(Math.max(0, item.getCurrentStock() + variance));
            itemRepository.save(item);
        }
        s.setStatus(Status.ST_ADJUSTED);
        s.setReviewedBy(user.id());
        s.setDecisionReason(reason.trim());
        s.setDecidedAt(LocalDateTime.now());
        stocktakeRepository.save(s);
        return detail(id);
    }

    // ---------- Quan ly yeu cau kiem ke lai ----------
    @Transactional
    public StocktakeDetail requestRecount(String id, String reason, AuthUser user) {
        Stocktake s = getPending(id);
        s.setStatus(Status.ST_RECOUNT);
        s.setReviewedBy(user.id());
        s.setDecisionReason(reason.trim());
        s.setDecidedAt(LocalDateTime.now());
        stocktakeRepository.save(s);
        return detail(id);
    }

    // ---------- Doc file ----------
    private Map<String, Integer> readCountFile(MultipartFile file, List<String> errors) {
        ParsedSheet sheet = SpreadsheetReader.read(file);
        if (!sheet.hasAny(COL_CODE) || !sheet.hasAny(COL_COUNT)) {
            throw ApiException.badRequest("File kiểm kê thiếu cột bắt buộc: cần cột \"Mã hàng\" và \"Tồn thực tế\"",
                    List.of("Các cột đọc được: " + String.join(", ", sheet.headers())));
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        int rowNo = 1;
        for (Map<String, String> row : sheet.rows()) {
            rowNo++;
            String code = SpreadsheetReader.pick(row, COL_CODE);
            Integer qty = SpreadsheetReader.parseInt(SpreadsheetReader.pick(row, COL_COUNT));
            if (code == null) {
                errors.add("File kiểm kê, dòng " + rowNo + ": thiếu mã hàng");
                continue;
            }
            if (qty == null || qty < 0) {
                errors.add("File kiểm kê, dòng " + rowNo + " (" + code + "): số lượng không hợp lệ");
                continue;
            }
            Item item = findItem(code);
            if (item == null) {
                errors.add("File kiểm kê, dòng " + rowNo + ": mã " + code + " không có trong danh mục");
                continue;
            }
            if (result.containsKey(item.getId())) {
                errors.add("File kiểm kê, dòng " + rowNo + ": mã " + code + " bị lặp");
                continue;
            }
            result.put(item.getId(), qty);
        }
        return result;
    }

    private void readDamageFile(MultipartFile file, Map<String, Integer> qtyOut, Map<String, String> reasonOut,
                                List<String> errors) {
        ParsedSheet sheet = SpreadsheetReader.read(file);
        if (!sheet.hasAny(COL_CODE) || !sheet.hasAny(COL_DAMAGE_QTY)) {
            throw ApiException.badRequest("File hỏng/hủy thiếu cột bắt buộc: cần cột \"Mã hàng\", \"Số lượng\" và \"Lý do\"",
                    List.of("Các cột đọc được: " + String.join(", ", sheet.headers())));
        }
        int rowNo = 1;
        for (Map<String, String> row : sheet.rows()) {
            rowNo++;
            String code = SpreadsheetReader.pick(row, COL_CODE);
            Integer qty = SpreadsheetReader.parseInt(SpreadsheetReader.pick(row, COL_DAMAGE_QTY));
            String reason = SpreadsheetReader.pick(row, COL_REASON);
            if (code == null) {
                errors.add("File hỏng/hủy, dòng " + rowNo + ": thiếu mã hàng");
                continue;
            }
            if (qty == null || qty <= 0) {
                errors.add("File hỏng/hủy, dòng " + rowNo + " (" + code + "): số lượng không hợp lệ");
                continue;
            }
            if (reason == null) {
                errors.add("File hỏng/hủy, dòng " + rowNo + " (" + code + "): thiếu lý do hỏng/hủy");
                continue;
            }
            Item item = findItem(code);
            if (item == null) {
                errors.add("File hỏng/hủy, dòng " + rowNo + ": mã " + code + " không có trong danh mục");
                continue;
            }
            qtyOut.merge(item.getId(), qty, Integer::sum);
            reasonOut.merge(item.getId(), reason, (a, b) -> a + "; " + b);
        }
    }

    private Item findItem(String code) {
        return itemRepository.findByCode(code).or(() -> itemRepository.findByPosCode(code)).orElse(null);
    }

    private StocktakeView view(Stocktake s, List<StocktakeItem> lines, Map<String, String> names) {
        int variance = (int) lines.stream().filter(l -> l.getVarianceQty() != 0).count();
        int damaged = lines.stream().mapToInt(StocktakeItem::getDamagedQty).sum();
        return new StocktakeView(s, names.get(s.getCreatedBy()), names.get(s.getReviewedBy()),
                lines.size(), variance, damaged);
    }

    private Stocktake get(String id) {
        return stocktakeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt kiểm kê"));
    }

    private Stocktake getPending(String id) {
        Stocktake s = get(id);
        if (!Status.ST_PENDING.equals(s.getStatus())) {
            throw ApiException.badRequest("Đợt kiểm kê này không ở trạng thái chờ xem xét");
        }
        return s;
    }
}
