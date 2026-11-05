import { useState } from 'react';
import { App, Button, Card, Col, Form, Input, Row, Tag } from 'antd';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { ROLE_LABEL } from '../format';

export default function ProfilePage() {
  const { user, refresh } = useAuth();
  const { message } = App.useApp();
  const [saving, setSaving] = useState(false);
  const [pwForm] = Form.useForm();

  const saveProfile = async (v) => {
    setSaving(true);
    try {
      const { data } = await api.put('/auth/profile', v);
      refresh(data);
      message.success('Đã cập nhật thông tin');
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (v) => {
    try {
      await api.put('/auth/password', { currentPassword: v.currentPassword, newPassword: v.newPassword });
      message.success('Đổi mật khẩu thành công');
      pwForm.resetFields();
    } catch (e) {
      message.error(errorMessage(e));
    }
  };

  return (
    <>
      <div className="page-header"><h2>Thông tin cá nhân</h2></div>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title={<>Tài khoản <b>{user.username}</b> <Tag>{ROLE_LABEL[user.role]}</Tag></>}>
            <Form layout="vertical" initialValues={user} onFinish={saveProfile}>
              <Form.Item name="fullName" label="Họ tên" rules={[{ required: true, whitespace: true }]}><Input /></Form.Item>
              <Form.Item name="email" label="Email" rules={[{ type: 'email', message: 'Email không hợp lệ' }]}><Input /></Form.Item>
              <Form.Item name="phone" label="Điện thoại"><Input /></Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>Lưu</Button>
            </Form>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Đổi mật khẩu">
            <Form form={pwForm} layout="vertical" onFinish={changePassword}>
              <Form.Item name="currentPassword" label="Mật khẩu hiện tại" rules={[{ required: true }]}><Input.Password /></Form.Item>
              <Form.Item name="newPassword" label="Mật khẩu mới" rules={[{ required: true }, { min: 6, message: 'Tối thiểu 6 ký tự' }]}><Input.Password /></Form.Item>
              <Form.Item name="confirm" label="Nhập lại mật khẩu mới" dependencies={['newPassword']}
                rules={[{ required: true }, ({ getFieldValue }) => ({
                  validator: (_, v) => (!v || v === getFieldValue('newPassword') ? Promise.resolve() : Promise.reject(new Error('Mật khẩu nhập lại không khớp'))),
                })]}>
                <Input.Password />
              </Form.Item>
              <Button type="primary" htmlType="submit">Đổi mật khẩu</Button>
            </Form>
          </Card>
        </Col>
      </Row>
    </>
  );
}
