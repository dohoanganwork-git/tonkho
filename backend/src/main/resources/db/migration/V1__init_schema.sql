-- Tài khoản người dùng
CREATE TABLE users (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NULL,
  phone VARCHAR(20) NULL,
  role ENUM('manager','staff','accountant') NOT NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nhà cung cấp và hợp đồng
CREATE TABLE suppliers (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  code VARCHAR(30) NOT NULL,
  name VARCHAR(150) NOT NULL,
  contact_name VARCHAR(100) NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(100) NULL,
  address VARCHAR(255) NULL,
  payment_method VARCHAR(50) NULL,
  contract_no VARCHAR(50) NULL,
  contract_start DATE NULL,
  contract_end DATE NULL,
  contract_note TEXT NULL,
  contract_file_url VARCHAR(255) NULL,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_suppliers_code (code),
  CONSTRAINT ck_suppliers_contract CHECK (contract_end IS NULL OR contract_start IS NULL OR contract_end >= contract_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Mặt hàng (kèm loại hàng)
CREATE TABLE items (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  category VARCHAR(50) NOT NULL,
  code VARCHAR(30) NOT NULL,
  pos_code VARCHAR(50) NOT NULL,
  name VARCHAR(150) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  min_stock INT NOT NULL DEFAULT 0,
  current_stock INT NOT NULL DEFAULT 0,
  last_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
  sale_price DECIMAL(15,2) NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_items_code (code),
  UNIQUE KEY uq_items_pos_code (pos_code),
  KEY idx_items_category (category),
  CONSTRAINT ck_items_min_stock CHECK (min_stock >= 0),
  CONSTRAINT ck_items_current_stock CHECK (current_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Phiếu nhập hàng (kèm công nợ và đối soát)
CREATE TABLE goods_receipts (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  code VARCHAR(30) NOT NULL,
  supplier_id CHAR(36) NOT NULL,
  created_by CHAR(36) NOT NULL,
  cancelled_by CHAR(36) NULL,
  invoice_no VARCHAR(50) NULL,
  invoice_file_url VARCHAR(255) NULL,
  receipt_date DATE NOT NULL,
  total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  remaining_amount DECIMAL(15,2) AS (total_amount - paid_amount) STORED,
  debt_status ENUM('unpaid','partial','paid') NOT NULL DEFAULT 'unpaid',
  status ENUM('active','cancelled') NOT NULL DEFAULT 'active',
  cancel_reason VARCHAR(255) NULL,
  reconcile_status ENUM('pending','has_difference','reconciled') NOT NULL DEFAULT 'pending',
  reconcile_note VARCHAR(255) NULL,
  reconciled_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_goods_receipts_code (code),
  KEY idx_goods_receipts_date (receipt_date),
  CONSTRAINT ck_receipts_paid CHECK (paid_amount >= 0 AND paid_amount <= total_amount),
  CONSTRAINT fk_goods_receipts_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_goods_receipts_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_goods_receipts_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chi tiết phiếu nhập
CREATE TABLE goods_receipt_items (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  receipt_id CHAR(36) NOT NULL,
  item_id CHAR(36) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  line_amount DECIMAL(15,2) AS (quantity * unit_price) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uq_receipt_item (receipt_id, item_id),
  CONSTRAINT ck_gri_quantity CHECK (quantity > 0),
  CONSTRAINT ck_gri_price CHECK (unit_price >= 0),
  CONSTRAINT fk_goods_receipt_items_receipt_id FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_goods_receipt_items_item_id FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Khoản thanh toán công nợ
CREATE TABLE payments (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  receipt_id CHAR(36) NOT NULL,
  created_by CHAR(36) NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  paid_at DATETIME NOT NULL,
  method VARCHAR(50) NOT NULL,
  evidence_file_url VARCHAR(255) NOT NULL,
  status ENUM('active','cancelled') NOT NULL DEFAULT 'active',
  note VARCHAR(255) NULL,
  PRIMARY KEY (id),
  CONSTRAINT ck_payments_amount CHECK (amount > 0),
  CONSTRAINT fk_payments_receipt_id FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Doanh số POS theo ngày và mặt hàng (phiếu xuất bán)
CREATE TABLE sales_lines (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  imported_by CHAR(36) NOT NULL,
  item_id CHAR(36) NOT NULL,
  sales_date DATE NOT NULL,
  original_file_url VARCHAR(255) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(15,2) NOT NULL,
  stock_before INT NOT NULL,
  stock_after INT NOT NULL,
  over_quantity INT NOT NULL DEFAULT 0,
  imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_sales_day_item (sales_date, item_id),
  CONSTRAINT ck_sales_quantity CHECK (quantity > 0),
  CONSTRAINT ck_sales_stock CHECK (stock_after >= 0 AND over_quantity >= 0),
  CONSTRAINT fk_sales_lines_imported_by FOREIGN KEY (imported_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_sales_lines_item_id FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Đợt kiểm kê và đối soát tồn kho
CREATE TABLE stocktakes (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  parent_id CHAR(36) NULL,
  created_by CHAR(36) NOT NULL,
  reviewed_by CHAR(36) NULL,
  stocktake_date DATE NOT NULL,
  count_file_url VARCHAR(255) NOT NULL,
  damage_file_url VARCHAR(255) NULL,
  status ENUM('pending_review','no_variance','adjusted','recount_requested') NOT NULL DEFAULT 'pending_review',
  decision_reason VARCHAR(255) NULL,
  decided_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_stocktakes_date (stocktake_date),
  CONSTRAINT fk_stocktakes_parent_id FOREIGN KEY (parent_id) REFERENCES stocktakes(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stocktakes_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_stocktakes_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chi tiết kiểm kê theo mặt hàng
CREATE TABLE stocktake_items (
  id CHAR(36) NOT NULL DEFAULT (UUID()),
  stocktake_id CHAR(36) NOT NULL,
  item_id CHAR(36) NOT NULL,
  book_qty INT NOT NULL,
  counted_qty INT NOT NULL,
  variance_qty INT AS (counted_qty - book_qty) STORED,
  damaged_qty INT NOT NULL DEFAULT 0,
  damage_reason VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_stocktake_item (stocktake_id, item_id),
  CONSTRAINT ck_sti_qty CHECK (book_qty >= 0 AND counted_qty >= 0 AND damaged_qty >= 0),
  CONSTRAINT fk_stocktake_items_stocktake_id FOREIGN KEY (stocktake_id) REFERENCES stocktakes(id) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_stocktake_items_item_id FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
