# Ứng dụng web quản lý tồn kho và nhà cung cấp – LongWaits Jazz Club

Khóa luận tốt nghiệp – Đỗ Hoàng An, Khoa Hệ thống thông tin quản lý, Đại học Kinh tế Quốc dân.
Hệ thống phục vụ cơ sở LongWaits Jazz Club (Công ty TNHH Sông Đêm): nhập sản phẩm từ nhà cung cấp để bán lại,
theo dõi tồn kho, công nợ, đối soát với doanh số POS và kiểm kê định kỳ.

## Công nghệ

| Thành phần | Công nghệ | Nơi chạy |
|---|---|---|
| Giao diện | ReactJS 18.3.1, Vite, Ant Design | Vercel |
| Backend | Java 17, Spring Boot 3.3 (Web, Data JPA, Security + JWT, Validation) | Railway |
| Cơ sở dữ liệu | MySQL 8 (9 bảng, tạo tự động bằng Flyway) | Railway |
| Đọc file | Apache POI (Excel), Commons CSV | |

## Cấu trúc thư mục

```
backend/    Spring Boot API (Maven). Schema CSDL: src/main/resources/db/migration/V1__init_schema.sql
frontend/   Giao diện React. File mẫu tải từ giao diện: public/mau/
samples/    File mẫu để demo: doanh số POS, tồn thực tế, hỏng/hủy (.csv, .xlsx), hóa đơn, minh chứng
docs/       Hướng dẫn triển khai (DEPLOY.md)
```

## Vai trò và chức năng

| Chức năng | Quản lý | Nhân viên | Kế toán |
|---|:-:|:-:|:-:|
| Đăng nhập, đổi mật khẩu, cập nhật thông tin | ✓ | ✓ | ✓ |
| Quản lý tài khoản, phân quyền | ✓ | | |
| Thêm/sửa mặt hàng, nhà cung cấp, file hợp đồng | ✓ | xem | xem |
| Lập phiếu nhập (tăng tồn, tự tạo công nợ) | ✓ | ✓ | xem |
| Sửa / hủy phiếu nhập | ✓ | | |
| Thanh toán công nợ kèm minh chứng, sửa/hủy thanh toán | xem | | ✓ |
| Đối soát công nợ với hóa đơn (khớp thì khóa phiếu) | xem | | ✓ |
| Nhập file doanh số POS cuối ngày (trừ tồn, tối thiểu 0) | ✓ | xem | xem |
| Tải file kiểm kê (tồn thực tế + hỏng/hủy) | ✓ | ✓ | xem |
| Đồng ý điều chỉnh tồn / yêu cầu kiểm kê lại | ✓ | | |
| Cảnh báo tồn thấp | ✓ | ✓ | ✓ |
| Cảnh báo bán vượt tồn | ✓ | | |
| Báo cáo tồn kho (giá trị tồn chỉ Quản lý, Kế toán), tiêu thụ, độ chính xác kho | ✓ | ✓ | ✓ |

## Tài khoản mẫu

Lần chạy đầu tiên, hệ thống tự tạo 3 tài khoản (mật khẩu mặc định `LongWaits@2026`, đổi bằng biến `SEED_PASSWORD`):

| Tên đăng nhập | Vai trò |
|---|---|
| `quanly` | Quản lý |
| `nhanvien` | Nhân viên |
| `ketoan` | Kế toán |

Kèm 6 nhà cung cấp, 11 mặt hàng của LongWaits và 6 phiếu nhập mẫu. Đặt `SEED_DATA=false` nếu chỉ muốn tài khoản, không muốn dữ liệu mẫu.
**Hãy đổi mật khẩu các tài khoản mẫu trước khi dùng thật.**

## Chạy trên máy cá nhân

Cần: Java 17, Maven, Node.js 18+, MySQL 8 (hoặc Docker).

```bash
# Cách 1: Docker (MySQL + backend)
docker compose up --build

# Cách 2: tự chạy backend (MySQL đã cài sẵn, user root không mật khẩu)
cd backend
mvn spring-boot:run

# Giao diện
cd frontend
cp .env.example .env.local     # sửa VITE_API_URL nếu backend không ở localhost:8080
npm install
npm run dev                    # mở http://localhost:5173
```

## Kịch bản demo nhanh

1. Đăng nhập `quanly` → Doanh số POS → Nhập file POS cuối ngày → chọn `samples/doanh-so-pos-mau.csv` (hoặc tải mẫu ngay trong hộp thoại).
   Hệ thống trừ tồn, báo mặt hàng tồn thấp và mặt hàng bán vượt tồn (Đĩa CD).
2. Đăng nhập `nhanvien` → Kiểm kê → Tải file kiểm kê với `kiem-ke-ton-thuc-te-mau.csv` và `kiem-ke-hong-huy-mau.csv`.
3. Đăng nhập `quanly` → mở đợt kiểm kê → Đồng ý điều chỉnh tồn hoặc Yêu cầu kiểm kê lại.
4. Đăng nhập `ketoan` → Công nợ → Thanh toán một phiếu (đính kèm `minh-chung-thanh-toan-mau.png`) → Đối soát (tải `hoa-don-mau.pdf`, chọn Khớp).

## Định dạng file nhập

Tên cột không phân biệt hoa thường, có dấu hay không dấu. Hỗ trợ `.csv` (UTF-8, phân cách `,` hoặc `;`), `.xlsx`, `.xls`.

| File | Cột bắt buộc | Cột tùy chọn |
|---|---|---|
| Doanh số POS | `Mã POS` (hoặc `Mã hàng`), `Số lượng` | `Đơn giá` (mặc định lấy giá bán), `Ngày` (phải trùng ngày chọn) |
| Tồn thực tế | `Mã hàng` (hoặc `Mã POS`), `Tồn thực tế` | `Tên hàng` |
| Hỏng/hủy | `Mã hàng`, `Số lượng`, `Lý do` | `Tên hàng` |

## Triển khai

Xem [docs/DEPLOY.md](docs/DEPLOY.md): đưa backend + MySQL lên Railway, giao diện lên Vercel.
