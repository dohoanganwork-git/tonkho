# Hướng dẫn triển khai: Railway (backend + MySQL) và Vercel (giao diện)

Làm một lần. Sau đó mỗi lần có code mới đẩy lên nhánh `main`, Railway và Vercel tự cập nhật.

## Bước 1 – Railway: tạo MySQL

1. Vào https://railway.com, đăng nhập bằng GitHub.
2. **New Project → Deploy MySQL**. Đợi MySQL chạy xong.

## Bước 2 – Railway: tạo backend từ GitHub

1. Trong cùng project: **New → GitHub Repo →** chọn `dohoanganwork-git/tonkho`.
2. Mở service vừa tạo → **Settings**:
   - **Root Directory**: `backend` (Railway sẽ tự nhận `Dockerfile`).
   - **Networking → Generate Domain** để có địa chỉ dạng `https://tonkho-production.up.railway.app`.
3. **Variables** của service backend, thêm:

   | Biến | Giá trị |
   |---|---|
   | `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
   | `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
   | `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |
   | `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
   | `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
   | `JWT_SECRET` | một chuỗi ngẫu nhiên dài ≥ 32 ký tự (tự gõ, không chia sẻ) |
   | `FRONTEND_URL` | địa chỉ Vercel ở Bước 4, ví dụ `https://tonkho.vercel.app` (tạm để `*` rồi sửa sau) |
   | `SEED_PASSWORD` | mật khẩu ban đầu cho 3 tài khoản mẫu |

   Cú pháp `${{MySQL.…}}` là tham chiếu biến của service MySQL; nếu service MySQL tên khác, thay `MySQL` bằng tên đó.

4. **Volume** (để file hóa đơn, minh chứng, file POS không mất khi deploy lại):
   chuột phải service backend → **Attach Volume** → Mount path: `/data/uploads`.
5. Deploy. Kiểm tra: mở `https://<địa-chỉ-backend>/api/health` thấy `{"status":"ok"}` là thành công.
   Bảng CSDL được Flyway tạo tự động ở lần chạy đầu, kèm tài khoản và dữ liệu mẫu.

## Bước 3 – Vercel: giao diện

1. Vào https://vercel.com, đăng nhập bằng GitHub → **Add New → Project →** chọn repo `tonkho`.
2. **Root Directory**: `frontend`. Framework: Vite (tự nhận).
3. **Environment Variables**: `VITE_API_URL` = địa chỉ backend Railway (không có `/` ở cuối).
4. Deploy. Vercel cho địa chỉ dạng `https://tonkho.vercel.app`.

## Bước 4 – Nối hai bên

Quay lại Railway, sửa biến `FRONTEND_URL` của backend thành đúng địa chỉ Vercel (có thể nhiều địa chỉ, cách nhau dấu phẩy).
Railway tự khởi động lại backend.

## Bước 5 – Trước khi dùng thật

- Đăng nhập từng tài khoản mẫu và đổi mật khẩu (menu tên người dùng → Thông tin cá nhân).
- Sửa hoặc ngừng các nhà cung cấp, mặt hàng mẫu và thêm danh mục thật.
- Nếu muốn CSDL trống ngay từ đầu, đặt `SEED_DATA=false` trước lần deploy đầu tiên.

## Xử lý sự cố

| Hiện tượng | Cách xử lý |
|---|---|
| Giao diện báo "Không kết nối được máy chủ" | Kiểm tra `VITE_API_URL` trên Vercel và `FRONTEND_URL` trên Railway; sau khi đổi biến trên Vercel phải **Redeploy** |
| Backend lỗi `Communications link failure` | Biến `MYSQL…` chưa tham chiếu đúng service MySQL |
| Backend lỗi `JWT_SECRET phai dai it nhat 32 ky tu` | Đặt lại `JWT_SECRET` dài hơn |
| File tải lên biến mất sau khi deploy | Chưa gắn Volume vào `/data/uploads` |
