import { useEffect, useState } from 'react';
import { App, AutoComplete, Button, Form, Input, InputNumber, Modal, Select, Space, Switch, Table, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { money, num } from '../format';

const DEFAULT_CATEGORIES = ['Đồ uống đóng lon', 'Đồ uống đóng chai', 'Rượu', 'Bia', 'Merchandise'];

export default function ItemsPage() {
  const { can } = useAuth();
  const { message } = App.useApp();
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filters, setFilters] = useState({ q: '', category: undefined, lowStock: false });
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    api.get('/items', { params: { q: filters.q || undefined, category: filters.category, lowStock: filters.lowStock || undefined } })
      .then((r) => setItems(r.data))
      .finally(() => setLoading(false));
  };

  useEffect(load, [filters]);
  useEffect(() => {
    api.get('/items/categories').then((r) => setCategories(r.data));
  }, [items.length]);

  const openForm = (item) => {
    setEditing(item || {});
    form.setFieldsValue(item ? { ...item } : { minStock: 0, salePrice: 0, active: true });
  };

  const save = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      if (editing.id) await api.put(`/items/${editing.id}`, values);
      else await api.post('/items', values);
      message.success('Đã lưu mặt hàng');
      setEditing(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const allCategories = [...new Set([...DEFAULT_CATEGORIES, ...categories])];

  return (
    <>
      <div className="page-header">
        <h2>Danh mục mặt hàng</h2>
        {can('manager') && <Button type="primary" icon={<PlusOutlined />} onClick={() => openForm(null)}>Thêm mặt hàng</Button>}
      </div>
      <div className="toolbar">
        <Input.Search placeholder="Tìm theo tên, mã, mã POS" allowClear style={{ width: 260 }}
          onSearch={(q) => setFilters((f) => ({ ...f, q }))} />
        <Select placeholder="Loại hàng" allowClear style={{ width: 200 }} value={filters.category}
          options={allCategories.map((c) => ({ value: c, label: c }))}
          onChange={(category) => setFilters((f) => ({ ...f, category }))} />
        <Space>
          <Switch checked={filters.lowStock} onChange={(lowStock) => setFilters((f) => ({ ...f, lowStock }))} />
          Chỉ hàng tồn thấp
        </Space>
      </div>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={items}
        scroll={{ x: 900 }}
        pagination={{ pageSize: 20, showSizeChanger: false }}
        columns={[
          { title: 'Mã', dataIndex: 'code', width: 90, fixed: 'left' },
          { title: 'Mã POS', dataIndex: 'posCode', width: 150 },
          { title: 'Tên mặt hàng', dataIndex: 'name' },
          { title: 'Loại', dataIndex: 'category', width: 160 },
          { title: 'ĐVT', dataIndex: 'unit', width: 70 },
          {
            title: 'Tồn', dataIndex: 'currentStock', align: 'right', width: 90,
            render: (v, r) => (r.lowStock && r.active ? <Tag color="red">{num(v)}</Tag> : num(v)),
            sorter: (a, b) => a.currentStock - b.currentStock,
          },
          { title: 'Mốc cảnh báo', dataIndex: 'minStock', align: 'right', width: 110, render: num },
          { title: 'Giá bán', dataIndex: 'salePrice', align: 'right', width: 120, render: money },
          { title: 'Trạng thái', dataIndex: 'active', width: 110, render: (v) => (v ? <Tag color="green">Kinh doanh</Tag> : <Tag>Ngừng</Tag>) },
          ...(can('manager')
            ? [{ title: '', width: 70, render: (_, r) => <Button size="small" onClick={() => openForm(r)}>Sửa</Button> }]
            : []),
        ]}
      />
      <Modal
        open={!!editing}
        title={editing?.id ? 'Sửa mặt hàng' : 'Thêm mặt hàng'}
        okText="Lưu"
        cancelText="Hủy"
        confirmLoading={saving}
        onOk={save}
        onCancel={() => setEditing(null)}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Space wrap style={{ display: 'flex' }}>
            <Form.Item name="code" label="Mã mặt hàng" rules={[{ required: true, whitespace: true }]} >
              <Input placeholder="VD: SP012" />
            </Form.Item>
            <Form.Item name="posCode" label="Mã trên POS" rules={[{ required: true, whitespace: true }]} >
              <Input placeholder="Mã hàng trong Sapo POS" />
            </Form.Item>
          </Space>
          <Form.Item name="name" label="Tên mặt hàng" rules={[{ required: true, whitespace: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="category" label="Loại hàng" rules={[{ required: true, whitespace: true }]}>
            <AutoComplete options={allCategories.map((c) => ({ value: c }))} placeholder="Chọn hoặc nhập loại mới" />
          </Form.Item>
          <Space wrap>
            <Form.Item name="unit" label="Đơn vị tính" rules={[{ required: true, whitespace: true }]}>
              <Input style={{ width: 120 }} placeholder="chai, lon..." />
            </Form.Item>
            <Form.Item name="minStock" label="Mốc cảnh báo tồn thấp" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: 160 }} />
            </Form.Item>
            <Form.Item name="salePrice" label="Giá bán (đ)" rules={[{ required: true }]}>
              <InputNumber min={0} step={1000} style={{ width: 160 }} formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')} parser={(v) => v.replace(/\./g, '')} />
            </Form.Item>
          </Space>
          <Form.Item name="active" label="Còn kinh doanh" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
