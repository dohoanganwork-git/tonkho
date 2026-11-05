import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Select, Table, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import api from '../api/client';
import { useAuth } from '../auth';
import { DEBT_STATUS, RECON_STATUS, date, money } from '../format';

export default function ReceiptsPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [filters, setFilters] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    api.get('/suppliers').then((r) => setSuppliers(r.data));
  }, []);

  useEffect(() => {
    setLoading(true);
    api.get('/receipts', { params: filters }).then((r) => setRows(r.data)).finally(() => setLoading(false));
  }, [filters]);

  const set = (k, v) => setFilters((f) => ({ ...f, [k]: v || undefined }));

  return (
    <>
      <div className="page-header">
        <h2>Phiếu nhập hàng</h2>
        {can('staff', 'manager') && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/nhap-hang/moi')}>Lập phiếu nhập</Button>
        )}
      </div>
      <div className="toolbar">
        <Select placeholder="Nhà cung cấp" allowClear style={{ width: 240 }} showSearch optionFilterProp="label"
          options={suppliers.map((s) => ({ value: s.id, label: s.name }))} onChange={(v) => set('supplierId', v)} />
        <DatePicker.RangePicker format="DD/MM/YYYY" onChange={(v) => setFilters((f) => ({
          ...f, from: v?.[0]?.format('YYYY-MM-DD'), to: v?.[1]?.format('YYYY-MM-DD'),
        }))} />
        <Select placeholder="Công nợ" allowClear style={{ width: 150 }}
          options={Object.entries(DEBT_STATUS).map(([value, o]) => ({ value, label: o.text }))} onChange={(v) => set('debtStatus', v)} />
        <Select placeholder="Trạng thái phiếu" allowClear style={{ width: 160 }}
          options={[{ value: 'active', label: 'Hiệu lực' }, { value: 'cancelled', label: 'Đã hủy' }]} onChange={(v) => set('status', v)} />
      </div>
      <Table
        rowKey={(r) => r.receipt.id}
        loading={loading}
        dataSource={rows}
        scroll={{ x: 1000 }}
        onRow={(r) => ({ onClick: () => navigate(`/nhap-hang/${r.receipt.id}`), style: { cursor: 'pointer' } })}
        columns={[
          { title: 'Số phiếu', render: (_, r) => <b>{r.receipt.code}</b>, width: 150 },
          { title: 'Ngày nhập', render: (_, r) => date(r.receipt.receiptDate), width: 110 },
          { title: 'Nhà cung cấp', dataIndex: 'supplierName' },
          { title: 'Số hóa đơn', render: (_, r) => r.receipt.invoiceNo || '—', width: 120 },
          { title: 'Tổng tiền', align: 'right', width: 130, render: (_, r) => money(r.receipt.totalAmount) },
          { title: 'Còn nợ', align: 'right', width: 130, render: (_, r) => money(r.receipt.remainingAmount) },
          {
            title: 'Công nợ', width: 130,
            render: (_, r) => r.receipt.status === 'cancelled'
              ? <Tag>Đã hủy phiếu</Tag>
              : <Tag color={DEBT_STATUS[r.receipt.debtStatus]?.color}>{DEBT_STATUS[r.receipt.debtStatus]?.text}</Tag>,
          },
          { title: 'Đối soát', width: 140, render: (_, r) => <Tag color={RECON_STATUS[r.receipt.reconcileStatus]?.color}>{RECON_STATUS[r.receipt.reconcileStatus]?.text}</Tag> },
          { title: 'Người lập', dataIndex: 'createdByName', width: 140, responsive: ['lg'] },
        ]}
      />
    </>
  );
}
