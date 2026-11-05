import { useEffect, useState } from 'react';
import { App, Button, DatePicker, Descriptions, Drawer, Form, Input, Modal, Switch, Table, Tag, Upload } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { date } from '../format';
import FileLink from '../components/FileLink';

export default function SuppliersPage() {
  const { can } = useAuth();
  const { message } = App.useApp();
  const [list, setList] = useState([]);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null);
  const [viewing, setViewing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    api.get('/suppliers', { params: { q: q || undefined } }).then((r) => setList(r.data)).finally(() => setLoading(false));
  };
  useEffect(load, [q]);

  const openForm = (s) => {
    setEditing(s || {});
    form.setFieldsValue(
      s
        ? { ...s, contractStart: s.contractStart ? dayjs(s.contractStart) : null, contractEnd: s.contractEnd ? dayjs(s.contractEnd) : null }
        : { active: true },
    );
  };

  const save = async () => {
    const v = await form.validateFields();
    const body = {
      ...v,
      contractStart: v.contractStart ? v.contractStart.format('YYYY-MM-DD') : null,
      contractEnd: v.contractEnd ? v.contractEnd.format('YYYY-MM-DD') : null,
    };
    setSaving(true);
    try {
      if (editing.id) await api.put(`/suppliers/${editing.id}`, body);
      else await api.post('/suppliers', body);
      message.success('Đã lưu nhà cung cấp');
      setEditing(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const uploadContract = async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    try {
      const { data } = await api.post(`/suppliers/${viewing.id}/contract-file`, fd);
      setViewing(data);
      message.success('Đã tải file hợp đồng');
      load();
    } catch (e) {
      message.error(errorMessage(e));
    }
    return false;
  };

  return (
    <>
      <div className="page-header">
        <h2>Nhà cung cấp</h2>
        {can('manager') && <Button type="primary" icon={<PlusOutlined />} onClick={() => openForm(null)}>Thêm nhà cung cấp</Button>}
      </div>
      <div className="toolbar">
        <Input.Search placeholder="Tìm theo tên, mã, người liên hệ" allowClear style={{ width: 300 }} onSearch={setQ} />
      </div>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={list}
        scroll={{ x: 900 }}
        columns={[
          { title: 'Mã', dataIndex: 'code', width: 90 },
          { title: 'Tên nhà cung cấp', dataIndex: 'name' },
          { title: 'Người liên hệ', dataIndex: 'contactName' },
          { title: 'Điện thoại', dataIndex: 'phone', width: 130 },
          { title: 'Thanh toán', dataIndex: 'paymentMethod', width: 130 },
          { title: 'Hợp đồng đến', dataIndex: 'contractEnd', width: 120, render: (v) => (v && dayjs(v).isBefore(dayjs()) ? <Tag color="red">{date(v)}</Tag> : date(v)) },
          { title: 'Trạng thái', dataIndex: 'active', width: 110, render: (v) => (v ? <Tag color="green">Hợp tác</Tag> : <Tag>Ngừng</Tag>) },
          {
            title: '', width: 130,
            render: (_, r) => (
              <>
                <Button size="small" type="link" onClick={() => setViewing(r)}>Xem</Button>
                {can('manager') && <Button size="small" type="link" onClick={() => openForm(r)}>Sửa</Button>}
              </>
            ),
          },
        ]}
      />

      <Drawer open={!!viewing} onClose={() => setViewing(null)} title={viewing?.name} width={Math.min(520, window.innerWidth)}>
        {viewing && (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="Mã">{viewing.code}</Descriptions.Item>
            <Descriptions.Item label="Người liên hệ">{viewing.contactName || '—'}</Descriptions.Item>
            <Descriptions.Item label="Điện thoại">{viewing.phone || '—'}</Descriptions.Item>
            <Descriptions.Item label="Email">{viewing.email || '—'}</Descriptions.Item>
            <Descriptions.Item label="Địa chỉ">{viewing.address || '—'}</Descriptions.Item>
            <Descriptions.Item label="Phương thức thanh toán">{viewing.paymentMethod || '—'}</Descriptions.Item>
            <Descriptions.Item label="Số hợp đồng">{viewing.contractNo || '—'}</Descriptions.Item>
            <Descriptions.Item label="Hiệu lực">{date(viewing.contractStart)} → {date(viewing.contractEnd)}</Descriptions.Item>
            <Descriptions.Item label="Điều khoản / ghi chú">{viewing.contractNote || '—'}</Descriptions.Item>
            <Descriptions.Item label="File hợp đồng">
              <FileLink path={viewing.contractFileUrl} />
              {can('manager') && (
                <Upload beforeUpload={uploadContract} showUploadList={false} accept=".pdf,.png,.jpg,.jpeg,.doc,.docx">
                  <Button size="small" icon={<UploadOutlined />} style={{ marginLeft: 8 }}>Tải lên</Button>
                </Upload>
              )}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      <Modal open={!!editing} title={editing?.id ? 'Sửa nhà cung cấp' : 'Thêm nhà cung cấp'} okText="Lưu" cancelText="Hủy"
        confirmLoading={saving} onOk={save} onCancel={() => setEditing(null)} destroyOnClose width={640}>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="code" label="Mã nhà cung cấp" rules={[{ required: true, whitespace: true }]}><Input /></Form.Item>
          <Form.Item name="name" label="Tên nhà cung cấp" rules={[{ required: true, whitespace: true }]}><Input /></Form.Item>
          <Form.Item name="contactName" label="Người liên hệ"><Input /></Form.Item>
          <Form.Item name="phone" label="Điện thoại"><Input /></Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input /></Form.Item>
          <Form.Item name="address" label="Địa chỉ"><Input /></Form.Item>
          <Form.Item name="paymentMethod" label="Phương thức thanh toán"><Input placeholder="Chuyển khoản, tiền mặt..." /></Form.Item>
          <Form.Item name="contractNo" label="Số hợp đồng"><Input /></Form.Item>
          <Form.Item label="Thời hạn hợp đồng" style={{ marginBottom: 0 }}>
            <Form.Item name="contractStart" style={{ display: 'inline-block', marginRight: 8 }}><DatePicker format="DD/MM/YYYY" placeholder="Bắt đầu" /></Form.Item>
            <Form.Item name="contractEnd" style={{ display: 'inline-block' }}><DatePicker format="DD/MM/YYYY" placeholder="Kết thúc" /></Form.Item>
          </Form.Item>
          <Form.Item name="contractNote" label="Điều khoản / ghi chú hợp đồng"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="active" label="Đang hợp tác" valuePropName="checked"><Switch /></Form.Item>
        </Form>
      </Modal>
    </>
  );
}
