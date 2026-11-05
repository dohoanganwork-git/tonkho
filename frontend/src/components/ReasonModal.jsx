import { Form, Input, Modal } from 'antd';
import { useEffect } from 'react';

/** Hop thoai nhap ly do (huy phieu, duyet / yeu cau kiem ke lai, ...). */
export default function ReasonModal({ open, title, label = 'Lý do', okText = 'Xác nhận', danger, onOk, onCancel, loading }) {
  const [form] = Form.useForm();
  useEffect(() => {
    if (open) form.resetFields();
  }, [open, form]);
  return (
    <Modal
      open={open}
      title={title}
      okText={okText}
      cancelText="Đóng"
      okButtonProps={{ danger, loading }}
      onCancel={onCancel}
      onOk={() => form.validateFields().then((v) => onOk(v.reason))}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="reason" label={label} rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập lý do' }]}>
          <Input.TextArea rows={3} maxLength={255} showCount />
        </Form.Item>
      </Form>
    </Modal>
  );
}
