package vn.longwaits.tonkho.common;

/** Cac gia tri trang thai luu trong CSDL (khop voi kieu ENUM trong schema). */
public final class Status {

    private Status() {
    }

    // users.role
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_STAFF = "staff";
    public static final String ROLE_ACCOUNTANT = "accountant";

    // goods_receipts.status, payments.status
    public static final String ACTIVE = "active";
    public static final String CANCELLED = "cancelled";

    // goods_receipts.debt_status
    public static final String DEBT_UNPAID = "unpaid";
    public static final String DEBT_PARTIAL = "partial";
    public static final String DEBT_PAID = "paid";

    // goods_receipts.reconcile_status
    public static final String RECON_PENDING = "pending";
    public static final String RECON_DIFFERENCE = "has_difference";
    public static final String RECON_DONE = "reconciled";

    // stocktakes.status
    public static final String ST_PENDING = "pending_review";
    public static final String ST_NO_VARIANCE = "no_variance";
    public static final String ST_ADJUSTED = "adjusted";
    public static final String ST_RECOUNT = "recount_requested";
}
