import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, DatePicker, Select, Space, Statistic, Table, Tabs, Tag } from 'antd';
import api from '../api/client';
import { useAuth } from '../auth';
import { DEBT_STATUS, RECON_STATUS, date, dateTime, exportCsv, money } from '../format';
import FileLink from '../components/FileLink';
import PaymentModal from '../components/PaymentModal';
import ReconcileModal from '../components/ReconcileModal';

export default function DebtsPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState([]);
  const [receipts, setReceipts] = useState([]);
  const [payments, setPayments] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [supplierId, setSupplierId] = useState();
  const [range, setRange] = useState([]);
  const [modal, setModal] = useState(null);

  const load = useCallback(() => {
    api.get('/receipts/debt-summary').then((r) => setSummary(r.data));
    api.get('/receipts', {
      params: { status: 'active', supplierId, from: range[0], to: range[1] },
    }).then((r) => setReceipts(r.data));
    api.get('/payments', { params: { supplierId, from: range[0], to: range[1] } }).then((r) => setPayments(r.data));
  }, [supplierId, range]);

  useEffect(() => {
    api.get('/suppliers').then((r) => setSuppliers(r.data));
  }, []);
  useEffect(load, [load]);

  const totalRemaining = summary.reduce((s, x) => s + Number(x.remaining), 0);
  const filters = (
    <div className="toolbar">
      <Select placeholder="Nhà cung cấp" allowClear style={{ width: 260 }} showSearch optionFilterProp="label" value={supplierId}
        options={suppliers.map((s) => ({ value: s.id, label: s.name }))} onChange={setSupplierId} />
      <DatePicker.RangePicker format="DD/MM/YYYY" placeholder={['Từ ngày', 'Đến ngày']}
        onChange={(v) => setRange(v ? [v[0].format('YYYY-MM-DD'), v[1].format('YYYY-MM-DD')] : [])} />
    </div>
  );

  return (
    <>
      <div className="page-header">
        <h2>Công nợ nhà cung cấp</h2>
        <Card size="small"><Statistic title="Tổng công nợ còn lại" value={money(totalRemaining)} /></Card>
      </div>
      <Tabs
        items={[
          {
            key: 'summary',
            label: 'Theo nhà cung cấp',
            children: (
              <Table rowKey="supplierId" dataSource={summary} pagination={false} scroll={{ x: 700 }}
                onRow={(r) => ({ onClick: () => setSupplierId(r.supplierId), style: { cursor: 'pointer' } })}
                columns={[
                  { title: 'Mã', dataIndex: 'supplierCode', width: 90 },
                  { title: 'Nhà cung cấp', dataIndex: 'supplierName' },
                  { title: 'Số phiếu', dataIndex: 'receiptCount', align: 'right', width: 90 },
                  { title: 'Tổng nhập', dataIndex: 'total', align: 'right', render: money },
                  { title: 'Đã trả', dataIndex: 'paid', align: 'right', render: money },
                  { title: 'Còn nợ', dataIndex: 'remaining', align: 'right', render: (v) => <b>{money(v)}</b> },
                ]} />
            ),
          },
          {
            key: 'receipts',
            label: 'Phiếu nhập – thanh toán & đối soát',
            children: (
              <>
                {filters}
                <Table rowKey={(r) => r.receipt.id} dataSource={receipts} scroll={{ x: 1100 }}
                  columns={[
                    { title: 'Số phiếu', render: (_, r) => <a onClick={() => navigate(`/nhap-hang/${r.receipt.id}`)}>{r.receipt.code}</a>, width: 150 },
                    { title: 'Ngày', render: (_, r) => date(r.receipt.receiptDate), width: 100 },
                    { title: 'Nhà cung cấp', dataIndex: 'supplierName' },
                    { title: 'Hóa đơn', render: (_, r) => <FileLink path={r.receipt.invoiceFileUrl}>{r.receipt.invoiceNo || 'Xem'}</FileLink>, width: 130 },
                    { title: 'Tổng', align: 'right', render: (_, r) => money(r.receipt.totalAmount), width: 120 },
                    { title: 'Còn nợ', align: 'right', render: (_, r) => <b>{money(r.receipt.remainingAmount)}</b>, width: 120 },
                    { title: 'Công nợ', width: 120, render: (_, r) => <Tag color={DEBT_STATUS[r.receipt.debtStatus]?.color}>{DEBT_STATUS[r.receipt.debtStatus]?.text}</Tag> },
                    { title: 'Đối soát', width: 140, render: (_, r) => <Tag color={RECON_STATUS[r.receipt.reconcileStatus]?.color}>{RECON_STATUS[r.receipt.reconcileStatus]?.text}</Tag> },
                    ...(can('accountant')
                      ? [{
                        title: '', width: 170, fixed: 'right',
                        render: (_, r) => r.receipt.reconcileStatus !== 'reconciled' && (
                          <Space size={0}>
                            {Number(r.receipt.remainingAmount) > 0 && (
                              <Button size="small" type="link" onClick={() => setModal({ pay: r.receipt })}>Thanh toán</Button>
                            )}
                            <Button size="small" type="link" onClick={() => setModal({ reconcile: r.receipt })}>Đối soát</Button>
                          </Space>
                        ),
                      }]
                      : []),
                  ]} />
              </>
            ),
          },
          {
            key: 'payments',
            label: 'Lịch sử thanh toán',
            children: (
              <>
                {filters}
                <div style={{ marginBottom: 8 }}>
                  <Button onClick={() => exportCsv('lich-su-thanh-toan.csv', [
                    { title: 'Ngày', value: (p) => dateTime(p.payment.paidAt) },
                    { title: 'Phiếu nhập', value: (p) => p.receiptCode },
                    { title: 'Nhà cung cấp', value: (p) => p.supplierName },
                    { title: 'Số tiền', value: (p) => p.payment.amount },
                    { title: 'Hình thức', value: (p) => p.payment.method },
                    { title: 'Trạng thái', value: (p) => (p.payment.status === 'cancelled' ? 'Đã hủy' : 'Hiệu lực') },
                    { title: 'Ghi chú', value: (p) => p.payment.note },
                  ], payments)}>Xuất CSV</Button>
                </div>
                <Table rowKey={(p) => p.payment.id} dataSource={payments} scroll={{ x: 900 }}
                  columns={[
                    { title: 'Ngày', render: (_, p) => dateTime(p.payment.paidAt), width: 140 },
                    { title: 'Phiếu nhập', render: (_, p) => <a onClick={() => navigate(`/nhap-hang/${p.payment.receiptId}`)}>{p.receiptCode}</a> },
                    { title: 'Nhà cung cấp', dataIndex: 'supplierName' },
                    { title: 'Số tiền', align: 'right', render: (_, p) => money(p.payment.amount) },
                    { title: 'Hình thức', render: (_, p) => p.payment.method },
                    { title: 'Minh chứng', render: (_, p) => <FileLink path={p.payment.evidenceFileUrl}>Xem</FileLink> },
                    { title: 'Trạng thái', render: (_, p) => (p.payment.status === 'cancelled' ? <Tag>Đã hủy</Tag> : <Tag color="green">Hiệu lực</Tag>) },
                    { title: 'Kế toán', dataIndex: 'createdByName' },
                  ]} />
              </>
            ),
          },
        ]}
      />
      <PaymentModal open={!!modal?.pay} receipt={modal?.pay} onClose={() => setModal(null)} onSaved={load} />
      <ReconcileModal open={!!modal?.reconcile} receipt={modal?.reconcile} onClose={() => setModal(null)} onSaved={load} />
    </>
  );
}
