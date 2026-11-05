package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.SupplierRepository;
import vn.longwaits.tonkho.storage.FileStorageService;
import vn.longwaits.tonkho.web.dto.Dtos.ItemRequest;
import vn.longwaits.tonkho.web.dto.Dtos.SupplierRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static vn.longwaits.tonkho.service.UserService.blankToNull;

/** Danh muc mat hang va nha cung cap. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final FileStorageService storage;

    // ---------- Mat hang ----------
    public List<Item> items(String q, String category, Boolean lowStock, Boolean active) {
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return itemRepository.findAll().stream()
                .filter(i -> keyword.isEmpty()
                        || i.getName().toLowerCase(Locale.ROOT).contains(keyword)
                        || i.getCode().toLowerCase(Locale.ROOT).contains(keyword)
                        || i.getPosCode().toLowerCase(Locale.ROOT).contains(keyword))
                .filter(i -> category == null || category.isBlank() || category.equals(i.getCategory()))
                .filter(i -> lowStock == null || !lowStock || (i.isActive() && i.isLowStock()))
                .filter(i -> active == null || active == i.isActive())
                .sorted(Comparator.comparing(Item::getCode))
                .toList();
    }

    public List<String> categories() {
        return itemRepository.findCategories();
    }

    public Item item(String id) {
        return itemRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy mặt hàng"));
    }

    @Transactional
    public Item createItem(ItemRequest req) {
        Item i = new Item();
        applyItem(i, req);
        return itemRepository.save(i);
    }

    @Transactional
    public Item updateItem(String id, ItemRequest req) {
        Item i = item(id);
        applyItem(i, req);
        return itemRepository.save(i);
    }

    private void applyItem(Item i, ItemRequest req) {
        String code = req.code().trim();
        String pos = req.posCode().trim();
        itemRepository.findByCode(code).filter(o -> !o.getId().equals(i.getId())).ifPresent(o -> {
            throw ApiException.conflict("Mã mặt hàng " + code + " đã tồn tại");
        });
        itemRepository.findByPosCode(pos).filter(o -> !o.getId().equals(i.getId())).ifPresent(o -> {
            throw ApiException.conflict("Mã POS " + pos + " đã được dùng cho mặt hàng " + o.getName());
        });
        i.setCategory(req.category().trim());
        i.setCode(code);
        i.setPosCode(pos);
        i.setName(req.name().trim());
        i.setUnit(req.unit().trim());
        i.setMinStock(req.minStock());
        i.setSalePrice(req.salePrice());
        if (req.active() != null) {
            i.setActive(req.active());
        }
    }

    // ---------- Nha cung cap ----------
    public List<Supplier> suppliers(String q, Boolean active) {
        String keyword = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return supplierRepository.findAll().stream()
                .filter(s -> keyword.isEmpty()
                        || s.getName().toLowerCase(Locale.ROOT).contains(keyword)
                        || s.getCode().toLowerCase(Locale.ROOT).contains(keyword)
                        || (s.getContactName() != null && s.getContactName().toLowerCase(Locale.ROOT).contains(keyword)))
                .filter(s -> active == null || active == s.isActive())
                .sorted(Comparator.comparing(Supplier::getCode))
                .toList();
    }

    public Supplier supplier(String id) {
        return supplierRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhà cung cấp"));
    }

    @Transactional
    public Supplier createSupplier(SupplierRequest req) {
        Supplier s = new Supplier();
        applySupplier(s, req);
        return supplierRepository.save(s);
    }

    @Transactional
    public Supplier updateSupplier(String id, SupplierRequest req) {
        Supplier s = supplier(id);
        applySupplier(s, req);
        return supplierRepository.save(s);
    }

    @Transactional
    public Supplier uploadContract(String id, MultipartFile file) {
        Supplier s = supplier(id);
        s.setContractFileUrl(storage.store(file, "hop-dong"));
        return supplierRepository.save(s);
    }

    private void applySupplier(Supplier s, SupplierRequest req) {
        String code = req.code().trim();
        supplierRepository.findByCode(code).filter(o -> !o.getId().equals(s.getId())).ifPresent(o -> {
            throw ApiException.conflict("Mã nhà cung cấp " + code + " đã tồn tại");
        });
        if (req.contractStart() != null && req.contractEnd() != null && req.contractEnd().isBefore(req.contractStart())) {
            throw ApiException.badRequest("Ngày kết thúc hợp đồng phải sau ngày bắt đầu");
        }
        s.setCode(code);
        s.setName(req.name().trim());
        s.setContactName(blankToNull(req.contactName()));
        s.setPhone(blankToNull(req.phone()));
        s.setEmail(blankToNull(req.email()));
        s.setAddress(blankToNull(req.address()));
        s.setPaymentMethod(blankToNull(req.paymentMethod()));
        s.setContractNo(blankToNull(req.contractNo()));
        s.setContractStart(req.contractStart());
        s.setContractEnd(req.contractEnd());
        s.setContractNote(blankToNull(req.contractNote()));
        if (req.active() != null) {
            s.setActive(req.active());
        }
    }
}
