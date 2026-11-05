package vn.longwaits.tonkho.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.longwaits.tonkho.common.ApiException;
import vn.longwaits.tonkho.common.Status;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return user;
    }

    public static boolean isStaff() {
        return Status.ROLE_STAFF.equals(get().role());
    }
}
