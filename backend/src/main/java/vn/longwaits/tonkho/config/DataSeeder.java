package vn.longwaits.tonkho.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.longwaits.tonkho.common.Status;
import vn.longwaits.tonkho.entity.Item;
import vn.longwaits.tonkho.entity.Supplier;
import vn.longwaits.tonkho.entity.User;
import vn.longwaits.tonkho.repository.ItemRepository;
import vn.longwaits.tonkho.repository.SupplierRepository;
import vn.longwaits.tonkho.repository.UserRepository;
import vn.longwaits.tonkho.security.AuthUser;
import vn.longwaits.tonkho.service.ReceiptService;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptLineRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ReceiptRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tao du lieu mau khi CSDL con trong: 3 tai khoan (Quan ly, Nhan vien, Ke toan),
 * nha cung cap, mat hang cua LongWaits va vai phieu nhap de co ton kho, cong no demo.
 * Tat bang bien moi truong SEED_DATA=false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final ReceiptService receiptService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled}")
    private boolean enabled;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User manager = user("quanly", "Quản lý LongWaits", Status.ROLE_MANAGER);
        user("nhanvien", "Nhân viên kho", Status.ROLE_STAFF);
        user("ketoan", "Kế toán", Status.ROLE_ACCOUNTANT);
        log.info("Da tao 3 tai khoan mau: quanly / nhanvien / ketoan");
        if (!enabled) {
            return;
        }

        Map<String, Supplier> s = new HashMap<>();
        s.put("NCC01", supplier("NCC01", "Bewine (mẫu)", "Chuyển khoản"));
        s.put("NCC02", supplier("NCC02", "Tê Tê Craft Beer (mẫu)", "Chuyển khoản"));
        s.put("NCC03", supplier("NCC03", "Thơm Brewing Co. (mẫu)", "Chuyển khoản"));
        s.put("NCC04", supplier("NCC04", "Hanoi Cider (mẫu)", "Tiền mặt"));
        s.put("NCC05", supplier("NCC05", "Nhà phân phối đồ uống nhập khẩu (mẫu)", "Chuyển khoản"));
        s.put("NCC06", supplier("NCC06", "Xưởng sản xuất merchandise (mẫu)", "Chuyển khoản"));

        Map<String, Item> i = new HashMap<>();
        i.put("SP001", item("SP001", "POS-BEWINE-330", "Rượu vang Bewine 330ml", "Rượu", "chai", 10, 180000));
        i.put("SP002", item("SP002", "POS-TETE", "Bia thủ công Tê Tê Craft Beer", "Bia", "chai", 24, 90000));
        i.put("SP003", item("SP003", "POS-RIESLING", "Rượu vang trắng Riesling", "Rượu", "chai", 6, 650000));
        i.put("SP004", item("SP004", "POS-VISTANA", "Rượu vang đỏ Vistana", "Rượu", "chai", 6, 590000));
        i.put("SP005", item("SP005", "POS-JUNBUNCHA", "Trà Junbuncha đóng chai", "Đồ uống đóng chai", "chai", 12, 75000));
        i.put("SP006", item("SP006", "POS-BUNDABERG", "Nước có ga Bundaberg", "Đồ uống đóng chai", "chai", 24, 65000));
        i.put("SP007", item("SP007", "POS-THOM", "Bia thủ công Thơm Brewing", "Bia", "chai", 24, 85000));
        i.put("SP008", item("SP008", "POS-HNCIDER", "Hanoi Cider đóng lon", "Đồ uống đóng lon", "lon", 24, 60000));
        i.put("SP009", item("SP009", "POS-CD-MUSE", "Đĩa CD The Muse Poetry Jazz", "Merchandise", "đĩa", 5, 300000));
        i.put("SP010", item("SP010", "POS-USB-NANGTHO", "USB Album Nàng Thơ", "Merchandise", "cái", 5, 390000));
        i.put("SP011", item("SP011", "POS-TOTE", "Túi Tote kỉ niệm LongWaits", "Merchandise", "cái", 10, 150000));

        AuthUser by = new AuthUser(manager.getId(), manager.getUsername(), manager.getFullName(), manager.getRole());
        LocalDate d = LocalDate.now().minusDays(7);
        receipt(by, s.get("NCC01"), "HD-BW-0001", d, i.get("SP001"), 36, 95000, i.get("SP003"), 12, 320000, i.get("SP004"), 12, 280000);
        receipt(by, s.get("NCC02"), "HD-TT-0001", d, i.get("SP002"), 48, 45000);
        receipt(by, s.get("NCC03"), "HD-TH-0001", d.plusDays(1), i.get("SP007"), 48, 42000);
        receipt(by, s.get("NCC04"), "HD-HC-0001", d.plusDays(1), i.get("SP008"), 48, 30000);
        receipt(by, s.get("NCC05"), "HD-PP-0001", d.plusDays(2), i.get("SP005"), 24, 38000, i.get("SP006"), 48, 32000);
        receipt(by, s.get("NCC06"), "HD-MC-0001", d.plusDays(2), i.get("SP009"), 4, 150000, i.get("SP010"), 10, 200000, i.get("SP011"), 30, 60000);
        log.info("Da tao du lieu mau: 6 nha cung cap, 11 mat hang, 6 phieu nhap");
    }

    private User user(String username, String fullName, String role) {
        User u = new User();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(seedPassword));
        return userRepository.save(u);
    }

    private Supplier supplier(String code, String name, String payment) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName(name);
        s.setContactName("Người liên hệ " + code);
        s.setPhone("09000000" + code.substring(3));
        s.setPaymentMethod(payment);
        s.setContractNo("HĐ-" + code + "/2026");
        s.setContractStart(LocalDate.of(2026, 1, 1));
        s.setContractEnd(LocalDate.of(2026, 12, 31));
        s.setContractNote("Dữ liệu mẫu để demo, thay bằng thông tin hợp đồng thật");
        return supplierRepository.save(s);
    }

    private Item item(String code, String pos, String name, String category, String unit, int min, long price) {
        Item i = new Item();
        i.setCode(code);
        i.setPosCode(pos);
        i.setName(name);
        i.setCategory(category);
        i.setUnit(unit);
        i.setMinStock(min);
        i.setSalePrice(BigDecimal.valueOf(price));
        return itemRepository.save(i);
    }

    /** lines: Item, so luong, don gia, Item, so luong, don gia, ... */
    private void receipt(AuthUser by, Supplier supplier, String invoice, LocalDate date, Object... lines) {
        List<ReceiptLineRequest> list = new ArrayList<>();
        for (int k = 0; k < lines.length; k += 3) {
            Item item = (Item) lines[k];
            int qty = (Integer) lines[k + 1];
            long price = ((Number) lines[k + 2]).longValue();
            list.add(new ReceiptLineRequest(item.getId(), qty, BigDecimal.valueOf(price)));
        }
        receiptService.create(new ReceiptRequest(supplier.getId(), invoice, date, list), by);
    }
}
