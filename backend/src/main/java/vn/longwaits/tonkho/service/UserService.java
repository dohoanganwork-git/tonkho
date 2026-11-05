package vn.longwaits.tonkho.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.entity.User;
import vn.longwaits.tonkho.repository.UserRepository;
import vn.longwaits.tonkho.security.CurrentUser;
import vn.longwaits.tonkho.security.JwtService;
import vn.longwaits.tonkho.web.dto.Dtos.LoginRequest;
import vn.longwaits.tonkho.web.dto.Dtos.LoginResponse;
import vn.longwaits.tonkho.web.dto.Dtos.PasswordRequest;
import vn.longwaits.tonkho.web.dto.Dtos.ProfileRequest;
import vn.longwaits.tonkho.web.dto.Dtos.UserRequest;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"));
        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa, liên hệ Quản lý");
        }
        return new LoginResponse(jwtService.generate(user), user);
    }

    public User me() {
        return get(CurrentUser.get().id());
    }

    @Transactional
    public User updateProfile(ProfileRequest req) {
        User u = me();
        u.setFullName(req.fullName().trim());
        u.setEmail(blankToNull(req.email()));
        u.setPhone(blankToNull(req.phone()));
        return userRepository.save(u);
    }

    @Transactional
    public void changePassword(PasswordRequest req) {
        User u = me();
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(u);
    }

    public List<User> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getRole).thenComparing(User::getUsername))
                .toList();
    }

    @Transactional
    public User create(UserRequest req) {
        String username = req.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Tên đăng nhập " + username + " đã tồn tại");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw ApiException.badRequest("Mật khẩu tối thiểu 6 ký tự");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        apply(u, req);
        return userRepository.save(u);
    }

    @Transactional
    public User update(String id, UserRequest req) {
        User u = get(id);
        String username = req.username().trim();
        if (!u.getUsername().equals(username)) {
            if (userRepository.existsByUsername(username)) {
                throw ApiException.conflict("Tên đăng nhập " + username + " đã tồn tại");
            }
            u.setUsername(username);
        }
        boolean self = u.getId().equals(CurrentUser.get().id());
        if (self && (Boolean.FALSE.equals(req.active()) || !u.getRole().equals(req.role()))) {
            throw ApiException.badRequest("Không thể tự vô hiệu hóa hoặc đổi vai trò của chính mình");
        }
        apply(u, req);
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6) {
                throw ApiException.badRequest("Mật khẩu tối thiểu 6 ký tự");
            }
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        return userRepository.save(u);
    }

    @Transactional
    public void resetPassword(String id, String newPassword) {
        User u = get(id);
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    private void apply(User u, UserRequest req) {
        u.setFullName(req.fullName().trim());
        u.setEmail(blankToNull(req.email()));
        u.setPhone(blankToNull(req.phone()));
        u.setRole(req.role());
        if (req.active() != null) {
            u.setActive(req.active());
        }
    }

    private User get(String id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
