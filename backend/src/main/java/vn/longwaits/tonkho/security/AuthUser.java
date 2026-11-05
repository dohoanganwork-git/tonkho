package vn.longwaits.tonkho.security;

/** Thong tin nguoi dang dang nhap, lay tu token. */
public record AuthUser(String id, String username, String fullName, String role) {
}
