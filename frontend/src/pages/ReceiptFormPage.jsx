import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Select, Space, Table, Typography } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api, { errorMessage } from '../api/client';
import { money } from '../format';

const moneyInput = {
  formatter: (v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.'),
  parser: (v) => (v || '').replace(/\./g, ''),
};

export default function ReceiptFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [suppliers, setSuppliers] = useState([]);
  const [items, setItems] = useState([]);
  const [lines, setLines] = useState([{ key: 1 }]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.get('/suppliers', { params: { active: true } }).then((r) => setSuppliers(r.data));
    api.get('/items', { params: { active: true } }).then((r) => setItems(r.data));
    if (isEdit) {
      api.get(`/receipts/${id}`).then(({ data }) => {
        form.setFieldsValue({
          supplierId: data.receipt.supplierId,
          invoiceNo: data.receipt.invoiceNo,
          receiptDate: dayjs(data.receipt.receiptDate),
        });
        setLines(data.lines.map((l, i) => ({ key: i + 1, itemId: l.itemId, quantity: l.quantity, unitPrice: l.unitPrice })));
      });
    } else {
      form.setFieldsValue({ receiptDate: dayjs() });
    }
  }, [id, isEdit, form]);

  const itemMap = useMemo(() => Object.fromEntries(items.map((i) => [i.id, i])), [items]);
  const total = lines.reduce((s, l) => s + (l.quantity || 0) * (l.unitPrice || 0), 0);

  const updateLine = (key, patch) => setLines((ls) => ls.map((l) => (l.key === key ? { ...l, ...patch } : l)));
  const addLine = () => setLines((ls) => [...ls, { key: Math.max(0, ...ls.map((l) => l.key)) + 1 }]);

  const submit = async () => {
    const header = await form.validateFields();
    const valid = lines.filter((l) => l.itemId);
    if (valid.length === 0) return message.error('Thêm ít nhất một mặt hàng');
    if (valid.some((l) => !l.quantity || l.quantity <= 0)) return message.error('Số lượng phải lớn hơn 0');
    if (valid.some((l) => l.unitPrice === undefined || l.unitPrice === null)) return message.error('Nhập đơn giá cho mọi dòng');
    const body = {
      supplierId: header.supplierId,
      invoiceNo: header.invoiceNo,
      receiptDate: header.receiptDate.format('YYYY-MM-DD'),
      lines: valid.map((l) => ({ itemId: l.itemId, quantity: l.quantity, unitPrice: l.unitPrice })),
    };
    setSaving(true);
    try {
      const { data } = isEdit ? await api.put(`/receipts/${id}`, body) : await api.post('/receipts', body);
      message.success(isEdit ? 'Đã cập nhật phiếu nhập' : `Đã lưu phiếu ${data.receipt.code}, tồn kho và công nợ đã được cập nhật`);
      navigate(`/nhap-hang/${data.receipt.id}`);
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const used = new Set(lines.map((l) => l.itemId).filter(Boolean));

  return (
    <>
      <div className="page-header">
        <h2>{isEdit ? 'Sửa phiếu nhập' : 'Lập phiếu nhập hàng'}</h2>
      </div>
      <Card>
        <Form form={form} layout="vertical">
          <Space wrap size="large">
            <Form.Item name="supplierId" label="Nhà cung cấp" rules={[{ required: true, message: 'Chọn nhà cung cấp' }]}>
              <Select style={{ width: 300 }} showSearch optionFilterProp="label" placeholder="Chọn nhà cung cấp"
                options={suppliers.map((s) => ({ value: s.id, label: `${s.code} – ${s.name}` }))} />
            </Form.Item>
            <Form.Item name="invoiceNo" label="Số hóa đơn">
              <Input style={{ width: 180 }} />
            </Form.Item>
            <Form.Item name="receiptDate" label="Ngày nhập" rules={[{ required: true, message: 'Chọn ngày nhập' }]}>
              <DatePicker format="DD/MM/YYYY" disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
            </Form.Item>
          </Space>
        </Form>
        <Table
          rowKey="key"
          dataSource={lines}
          pagination={false}
          size="small"
          scroll={{ x: 760 }}
          columns={[
            {
              title: 'Mặt hàng', width: 320,
              render: (_, l) => (
                <Select style={{ width: '100%' }} showSearch optionFilterProp="label" placeholder="Chọn mặt hàng" value={l.itemId}
                  options={items.map((i) => ({ value: i.id, label: `${i.code} – ${i.name}`, disabled: used.has(i.id) && i.id !== l.itemId }))}
                  onChange={(v) => updateLine(l.key, { itemId: v, unitPrice: l.unitPrice ?? (Number(itemMap[v]?.lastCost) || undefined) })} />
              ),
            },
            { title: 'ĐVT', width: 70, render: (_, l) => itemMap[l.itemId]?.unit || '' },
            {
              title: 'Số lượng', width: 120,
              render: (_, l) => <InputNumber min={1} value={l.quantity} onChange={(v) => updateLine(l.key, { quantity: v })} style={{ width: '100%' }} />,
            },
            {
              title: 'Đơn giá (đ)', width: 160,
              render: (_, l) => <InputNumber min={0} step={1000} value={l.unitPrice} {...moneyInput} onChange={(v) => updateLine(l.key, { unitPrice: v })} style={{ width: '100%' }} />,
            },
            { title: 'Thành tiền', align: 'right', width: 140, render: (_, l) => money((l.quantity || 0) * (l.unitPrice || 0)) },
            {
              title: '', width: 50,
              render: (_, l) => <Button type="text" danger icon={<DeleteOutlined />} disabled={lines.length === 1}
                onClick={() => setLines((ls) => ls.filter((x) => x.key !== l.key))} aria-label="Xóa dòng" />,
            },
          ]}
        />
        <Button icon={<PlusOutlined />} onClick={addLine} style={{ marginTop: 12 }}>Thêm dòng</Button>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 20, flexWrap: 'wrap', gap: 12 }}>
          <Typography.Title level={4} style={{ margin: 0 }}>Tổng tiền: {money(total)}</Typography.Title>
          <Space>
            <Button onClick={() => navigate(-1)}>Hủy</Button>
            <Button type="primary" loading={saving} onClick={submit}>{isEdit ? 'Lưu thay đổi' : 'Lưu phiếu nhập'}</Button>
          </Space>
        </div>
        {!isEdit && (
          <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
            Khi lưu, hệ thống tăng tồn kho các mặt hàng và tự tạo công nợ (mặc định trả sau) với nhà cung cấp.
          </Typography.Paragraph>
        )}
      </Card>
    </>
  );
}
