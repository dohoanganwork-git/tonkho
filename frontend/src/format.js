import dayjs from 'dayjs';

export const ROLE_LABEL = { manager: 'Quản lý', staff: 'Nhân viên', accountant: 'Kế toán' };

export const DEBT_STATUS = {
  unpaid: { text: 'Chưa trả', color: 'red' },
  partial: { text: 'Trả một phần', color: 'orange' },
  paid: { text: 'Đã trả', color: 'green' },
};

export const RECON_STATUS = {
  pending: { text: 'Chưa đối soát', color: 'default' },
  has_difference: { text: 'Còn chênh lệch', color: 'orange' },
  reconciled: { text: 'Đã khớp (khóa)', color: 'green' },
};

export const STOCKTAKE_STATUS = {
  pending_review: { text: 'Chờ Quản lý xem xét', color: 'gold' },
  no_variance: { text: 'Không chênh lệch', color: 'green' },
  adjusted: { text: 'Đã điều chỉnh tồn', color: 'blue' },
  recount_requested: { text: 'Yêu cầu kiểm kê lại', color: 'red' },
};

export const money = (v) =>
  v === null || v === undefined ? '—' : `${Number(v).toLocaleString('vi-VN')} đ`;

export const num = (v) => (v === null || v === undefined ? '—' : Number(v).toLocaleString('vi-VN'));

export const date = (v) => (v ? dayjs(v).format('DD/MM/YYYY') : '—');

export const dateTime = (v) => (v ? dayjs(v).format('DD/MM/YYYY HH:mm') : '—');

/** Xuat bang ra file CSV (mo duoc bang Excel). */
export function exportCsv(filename, columns, rows) {
  const esc = (v) => {
    const s = v === null || v === undefined ? '' : String(v);
    return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const lines = [columns.map((c) => esc(c.title)).join(',')];
  rows.forEach((r) => lines.push(columns.map((c) => esc(c.value(r))).join(',')));
  const blob = new Blob(['﻿' + lines.join('\n')], { type: 'text/csv;charset=utf-8' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}
