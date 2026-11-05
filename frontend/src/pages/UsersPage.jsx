import { useEffect, useState } from 'react';
import { App, Button, Form, Input, Modal, Select, Switch, Table, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { ROLE_LABEL, dateTime } from '../format';

export default function UsersPage() {
  const { user: me } = useAuth();
  const { message } = App.useApp();
  const [users, setUsers] = useState([]);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const load = () => api.get('/users').then((r) => setUsers(r.data));
  useEffect(() => { load(); }, []);

  const open = (u) => {
    setEditing(u || {});
    form.setFieldsValue(u ? { ...u, password: '' } : { role: 'staff', active: true });
  };

  const save = async () => {
    const v = await form.validateFields();
    setSaving(true);
    try {
      if (editing.id) await api.put(`/users/${editing.id}`, v);
      else await api.post('/users', v);
      message.success('Đã lưu tài khoản');
      setEditing(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <h2>Tài khoản và phân quyền</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => open(null)}>Thêm tài khoản</Button>
      </div>
      <Table rowKey="id" dataSource={users} scroll={{ x: 800 }}
        columns={[
          { title: 'Tên đăng nhập', dataIndex: 'username' },
          { title: 'Họ tên', dataIndex: 'fullName' },
          { title: 'Vai trò', dataIndex: 'role', render: (v) => <Tag color={v === 'manager' ? 'gold' : v === 'accountant' ? 'blue' : 'default'}>{ROLE_LABEL[v]}</Tag> },
          { title: 'Email', dataIndex: 'email' },
          { title: 'Điện thoại', dataIndex: 'phone' },
          { title: 'Trạng thái', dataIndex: 'active', render: (v) => (v ? <Tag color="green">Hoạt động</Tag> : <Tag color="red">Vô hiệu hóa</Tag>) },
          { title: 'Ngày tạo', dataIndex: 'createdAt', render: dateTime },
          { title: '', render: (_, u) => <Button size="small" onClick={() => open(u)}>Sửa</Button> },
        ]} />
      <Modal open={!!editing} title={editing?.id ? 'Sửa tài khoản' : 'Thêm tài khoản'} okText="Lưu" cancelText="Hủy"
        confirmLoading={saving} onOk={save} onCancel={() => setEditing(null)} destroyOnClose>
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item name="username" label="Tên đăng nhập" rules={[{ required: true, whitespace: true }]}><Input autoComplete="off" /></Form.Item>
          <Form.Item name="password" label={editing?.id ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu'}
            rules={editing?.id ? [{ min: 6, message: 'Tối thiểu 6 ký tự' }] : [{ required: true, message: 'Nhập mật khẩu' }, { min: 6, message: 'Tối thiểu 6 ký tự' }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, whitespace: true }]}><Input /></Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input /></Form.Item>
          <Form.Item name="phone" label="Điện thoại"><Input /></Form.Item>
          <Form.Item name="role" label="Vai trò" rules={[{ required: true }]}>
            <Select disabled={editing?.id === me.id} options={Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label }))} />
          </Form.Item>
          <Form.Item name="active" label="Đang hoạt động" valuePropName="checked"><Switch disabled={editing?.id === me.id} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
}
